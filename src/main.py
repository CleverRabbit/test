"""
FastAPI приложение AI Code Factory
Основной файл с API endpoints и логикой
"""
from fastapi import FastAPI, HTTPException, Depends, BackgroundTasks, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import HTMLResponse, JSONResponse
from fastapi.security import HTTPBasic, HTTPBasicCredentials
from pydantic import BaseModel, Field
from typing import Optional, List, Dict, Any
from datetime import datetime, timedelta
import secrets
import hashlib
import os

from src.database import get_db, Project, GenerationStep, User, init_db
from src.llm import llm_client
from src.github_client import github_automation
from src.render_client import render_automation

# Инициализация приложения
app = FastAPI(
    title="AI Code Factory",
    description="Автоматическая генерация и деплой приложений по идее",
    version="1.0.0"
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Basic Auth для админки
security = HTTPBasic()

# ==================== Pydantic Models ====================

class IdeaRequest(BaseModel):
    idea: str = Field(..., description="Идея приложения")
    user_id: str = Field(..., description="Telegram user ID")

class AnswerRequest(BaseModel):
    project_id: int
    answer: str = Field(..., description="Ответ на уточняющий вопрос")

class MagicLinkRequest(BaseModel):
    magic_hash: str

class AdminCredentials(BaseModel):
    login: str
    password: str

class GeminiKeyRequest(BaseModel):
    user_id: str = Field(..., description="Telegram user ID")
    api_key: str = Field(..., description="API ключ Google Gemini")

# ==================== Helper Functions ====================

def generate_magic_hash(project_id: int) -> str:
    """Генерация уникального хэша для магической ссылки"""
    random_part = secrets.token_hex(16)
    data = f"{project_id}:{random_part}:{datetime.utcnow().isoformat()}"
    return hashlib.sha256(data.encode()).hexdigest()[:32]

def verify_admin(credentials: HTTPBasicCredentials = Depends(security)):
    """Проверка админских учётных данных"""
    admin_login = os.getenv("ADMIN_LOGIN", "admin")
    admin_password = os.getenv("ADMIN_PASSWORD", "admin")
    
    if credentials.username != admin_login or credentials.password != admin_password:
        raise HTTPException(status_code=401, detail="Неверные учётные данные")
    return True

async def create_step(db, project_id: int, step_name: str, status: str = "pending", message: str = ""):
    """Создание шага генерации"""
    step = GenerationStep(
        project_id=project_id,
        step_name=step_name,
        status=status,
        message=message
    )
    db.add(step)
    db.commit()
    db.refresh(step)
    return step

async def update_step(db, step_id: int, status: str, message: str = ""):
    """Обновление статуса шага"""
    step = db.query(GenerationStep).filter(GenerationStep.id == step_id).first()
    if step:
        step.status = status
        step.message = message
        db.commit()
    return step

# ==================== Background Tasks ====================

async def generate_project_background(project_id: int, idea: str, db):
    """Фоновая задача генерации проекта"""
    
    try:
        # Получаем проект и пользователя
        project = db.query(Project).filter(Project.id == project_id).first()
        if not project:
            return
        
        user = db.query(User).filter(User.telegram_id == project.user_id).first()
        
        # Проверяем наличие API ключа Gemini у пользователя
        if not user or not user.gemini_api_key:
            await create_step(db, project_id, "error", "failed", 
                            "Ошибка: API ключ Gemini не найден. Используйте команду /setkey в Telegram боте.")
            project.status = "failed"
            db.commit()
            return
        
        # Инициализируем LLM клиент с ключом пользователя
        llm_client.update_api_key(user.gemini_api_key)
        
        # Шаг 1: Анализ идеи
        step_analyze = await create_step(db, project_id, "analyze", "running", "Анализируем идею...")
        analysis = await llm_client.analyze_idea(idea)
        await update_step(db, step_analyze.id, "completed", f"Анализ завершён: {analysis.get('summary', '')}")
        
        # Если нужны уточнения, ставим на паузу
        if not analysis.get('is_clear', True):
            project.status = "waiting_answers"
            db.commit()
            return
        
        # Шаг 2: Генерация спецификации
        step_spec = await create_step(db, project_id, "spec", "running", "Генерируем спецификацию...")
        spec = await llm_client.generate_project_spec(idea)
        await update_step(db, step_spec.id, "completed", "Спецификация готова")
        
        # Шаг 3: Генерация списка файлов
        step_files = await create_step(db, project_id, "files", "running", "Планируем структуру файлов...")
        file_list = await llm_client.generate_file_list(spec)
        await update_step(db, step_files.id, "completed", f"Сгенерировано {len(file_list)} файлов")
        
        # Шаг 4: Генерация кода для каждого файла
        step_code = await create_step(db, project_id, "code", "running", "Пишем код...")
        generated_files = {}
        
        for file_path in file_list[:10]:  # Ограничим для MVP
            code = await llm_client.generate_code(spec, file_path)
            generated_files[file_path] = code
        
        await update_step(db, step_code.id, "completed", f"Код сгенерирован")
        
        # Шаг 5: Создание репозитория GitHub
        step_github = await create_step(db, project_id, "github", "running", "Создаём репозиторий...")
        project_name = f"ai-project-{project_id}"
        success, gh_message = github_automation.create_repository(project_name, f"AI Generated: {idea[:50]}")
        
        if success:
            await update_step(db, step_github.id, "completed", f"Репозиторий создан: {gh_message}")
            
            # Пуш файлов
            step_push = await create_step(db, project_id, "push", "running", "Пушим код...")
            success, push_msg = github_automation.push_files(project_name, generated_files)
            if success:
                await update_step(db, step_push.id, "completed", push_msg)
            else:
                await update_step(db, step_push.id, "failed", push_msg)
        else:
            await update_step(db, step_github.id, "failed", gh_message)
        
        # Шаг 6: Деплой на Render
        step_render = await create_step(db, project_id, "render", "running", "Деплоим на Render...")
        repo_url = github_automation.get_repo_url(project_name)
        
        env_vars = {
            "DATABASE_URL": os.getenv("DATABASE_URL", ""),
            "SECRET_KEY": secrets.token_hex(32)
        }
        
        success, render_url = await render_automation.create_web_service(
            name=project_name,
            repo_url=repo_url,
            env_vars=env_vars
        )
        
        if success:
            await update_step(db, step_render.id, "completed", f"Деплой успешен: {render_url}")
            
            # Обновляем проект
            project.github_repo_url = repo_url
            project.render_deploy_url = render_url
            project.status = "deployed"
            db.commit()
        else:
            await update_step(db, step_render.id, "failed", render_url)
            
    except Exception as e:
        project = db.query(Project).filter(Project.id == project_id).first()
        if project:
            project.status = "failed"
            db.commit()
        
        error_step = await create_step(db, project_id, "error", "failed", str(e))

# ==================== API Endpoints ====================

@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {"status": "ok", "timestamp": datetime.utcnow().isoformat()}

@app.post("/api/start-project")
async def start_project(request: IdeaRequest, background_tasks: BackgroundTasks):
    """Запуск процесса генерации проекта"""
    db = next(get_db())
    
    try:
        # Создаём проект
        magic_hash = generate_magic_hash(0)  # ID будет позже
        project = Project(
            user_id=request.user_id,
            idea=request.idea,
            project_name=f"Project-{secrets.token_hex(4)}",
            magic_link_hash=magic_hash,
            magic_link_expires=datetime.utcnow() + timedelta(hours=24),
            status="pending"
        )
        db.add(project)
        db.commit()
        db.refresh(project)
        
        # Обновляем хэш с ID
        project.magic_link_hash = generate_magic_hash(project.id)
        db.commit()
        
        # Создаём первый шаг
        await create_step(db, project.id, "init", "completed", "Проект создан")
        
        # Запускаем фоновую задачу
        background_tasks.add_task(generate_project_background, project.id, request.idea, db)
        
        return {
            "project_id": project.id,
            "magic_link": f"/app/{project.magic_link_hash}",
            "status": "generating",
            "message": "Проект создаётся, ожидайте завершения..."
        }
    
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/project/{project_id}")
async def get_project_status(project_id: int):
    """Получение статуса проекта"""
    db = next(get_db())
    project = db.query(Project).filter(Project.id == project_id).first()
    
    if not project:
        raise HTTPException(status_code=404, detail="Проект не найден")
    
    steps = db.query(GenerationStep).filter(GenerationStep.project_id == project_id).all()
    
    return {
        "id": project.id,
        "idea": project.idea,
        "project_name": project.project_name,
        "status": project.status,
        "github_url": project.github_repo_url,
        "render_url": project.render_deploy_url,
        "magic_link": f"/app/{project.magic_link_hash}",
        "created_at": project.created_at.isoformat(),
        "steps": [
            {
                "name": s.step_name,
                "status": s.status,
                "message": s.message,
                "created_at": s.created_at.isoformat()
            }
            for s in steps
        ]
    }

@app.get("/app/{magic_hash}")
async def access_via_magic_link(magic_hash: str):
    """Доступ к приложению через магическую ссылку"""
    db = next(get_db())
    project = db.query(Project).filter(Project.magic_link_hash == magic_hash).first()
    
    if not project:
        raise HTTPException(status_code=404, detail="Проект не найден")
    
    if project.magic_link_expires < datetime.utcnow():
        raise HTTPException(status_code=410, detail="Срок действия ссылки истёк")
    
    if project.status != "deployed":
        return {
            "status": project.status,
            "message": "Проект ещё не готов",
            "steps": [
                {
                    "name": s.step_name,
                    "status": s.status,
                    "message": s.message
                }
                for s in project.steps
            ]
        }
    
    return {
        "project_name": project.project_name,
        "render_url": project.render_deploy_url,
        "github_url": project.github_repo_url,
        "message": "Приложение успешно развёрнуто!"
    }

@app.get("/api/projects")
async def list_projects(admin: bool = Depends(verify_admin)):
    """Список всех проектов (только для админа)"""
    db = next(get_db())
    projects = db.query(Project).order_by(Project.created_at.desc()).limit(50).all()
    
    return [
        {
            "id": p.id,
            "user_id": p.user_id,
            "idea": p.idea[:100],
            "project_name": p.project_name,
            "status": p.status,
            "render_url": p.render_deploy_url,
            "created_at": p.created_at.isoformat()
        }
        for p in projects
    ]

# ==================== Startup Event ====================

@app.on_event("startup")
async def startup_event():
    """Инициализация БД при старте"""
    init_db()


# ==================== New API Endpoint for Gemini Key ====================

@app.post("/api/set-gemini-key")
async def set_gemini_key(request: GeminiKeyRequest):
    """Сохранение API ключа Gemini для пользователя"""
    db = next(get_db())
    
    try:
        # Проверяем, существует ли пользователь
        user = db.query(User).filter(User.telegram_id == request.user_id).first()
        
        if not user:
            # Создаём нового пользователя
            user = User(
                telegram_id=request.user_id,
                gemini_api_key=request.api_key
            )
            db.add(user)
        else:
            # Обновляем ключ
            user.gemini_api_key = request.api_key
        
        db.commit()
        
        return {
            "status": "success",
            "message": "API ключ Gemini сохранён"
        }
    
    except Exception as e:
        db.rollback()
        raise HTTPException(status_code=500, detail=str(e))