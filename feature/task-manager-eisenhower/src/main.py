"""
FastAPI приложение с REST API и запуском Telegram бота.
"""
import asyncio
from datetime import datetime
from contextlib import asynccontextmanager
from typing import List

from fastapi import FastAPI, Depends, HTTPException, status
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy.orm import Session
from loguru import logger

from src.config import settings
from src.database import init_db, get_db, User, Task, TaskStatus, TaskQuadrant
from src.schemas import (
    TaskResponse, TaskUpdate, HealthResponse,
    QuadrantEnum, StatusEnum
)
from src.bot import telegram_bot


# Настройка логирования
logger.add("logs/app.log", rotation="10 MB", level=settings.log_level)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Управление жизненным циклом приложения"""
    # Startup
    logger.info("Приложение запускается...")
    init_db()
    logger.info("База данных инициализирована")
    
    # Запускаем бота в фоновой задаче
    bot_task = asyncio.create_task(telegram_bot.run_polling())
    logger.info("Telegram бот запущен в фоновом режиме")
    
    yield
    
    # Shutdown
    logger.info("Приложение завершает работу...")
    bot_task.cancel()
    try:
        await bot_task
    except asyncio.CancelledError:
        pass


# Создание FastAPI приложения
app = FastAPI(
    title="Task Manager Eisenhower",
    description="Система управления задачами с AI-анализом и Telegram ботом",
    version="1.0.0",
    lifespan=lifespan
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Для продакшена ограничить домены
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# === API Endpoints ===

@app.get("/health", response_model=HealthResponse, tags=["System"])
async def health_check(db: Session = Depends(get_db)):
    """Проверка здоровья приложения"""
    try:
        # Проверяем подключение к БД
        db.execute("SELECT 1")
        db_status = "connected"
    except Exception as e:
        db_status = f"error: {str(e)}"
    
    return HealthResponse(
        status="healthy",
        database=db_status,
        timestamp=datetime.utcnow()
    )


@app.get("/api/tasks", response_model=List[TaskResponse], tags=["Tasks"])
async def get_tasks(
    telegram_id: str,
    quadrant: QuadrantEnum = None,
    status: StatusEnum = None,
    db: Session = Depends(get_db)
):
    """Получить список задач пользователя"""
    # Находим пользователя
    user = db.query(User).filter(User.telegram_id == telegram_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="Пользователь не найден")
    
    # Формируем запрос
    query = db.query(Task).filter(Task.user_id == user.id)
    
    if quadrant:
        query = query.filter(Task.quadrant == quadrant)
    if status:
        query = query.filter(Task.status == status)
    
    tasks = query.order_by(Task.created_at.desc()).all()
    return tasks


@app.get("/api/tasks/{task_id}", response_model=TaskResponse, tags=["Tasks"])
async def get_task(task_id: int, db: Session = Depends(get_db)):
    """Получить конкретную задачу"""
    task = db.query(Task).filter(Task.id == task_id).first()
    if not task:
        raise HTTPException(status_code=404, detail="Задача не найдена")
    return task


@app.patch("/api/tasks/{task_id}", response_model=TaskResponse, tags=["Tasks"])
async def update_task(
    task_id: int,
    task_update: TaskUpdate,
    db: Session = Depends(get_db)
):
    """Обновить задачу"""
    task = db.query(Task).filter(Task.id == task_id).first()
    if not task:
        raise HTTPException(status_code=404, detail="Задача не найдена")
    
    # Обновляем поля
    update_data = task_update.model_dump(exclude_unset=True)
    for field, value in update_data.items():
        setattr(task, field, value)
    
    db.commit()
    db.refresh(task)
    
    logger.info(f"Задача {task_id} обновлена: {update_data}")
    return task


@app.delete("/api/tasks/{task_id}", tags=["Tasks"])
async def delete_task(task_id: int, db: Session = Depends(get_db)):
    """Удалить задачу"""
    task = db.query(Task).filter(Task.id == task_id).first()
    if not task:
        raise HTTPException(status_code=404, detail="Задача не найдена")
    
    db.delete(task)
    db.commit()
    
    logger.info(f"Задача {task_id} удалена")
    return {"message": "Задача успешно удалена"}


@app.get("/api/users/{telegram_id}", tags=["Users"])
async def get_user(telegram_id: str, db: Session = Depends(get_db)):
    """Получить информацию о пользователе"""
    user = db.query(User).filter(User.telegram_id == telegram_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="Пользователь не найден")
    return user


# Запуск для uvicorn
if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
