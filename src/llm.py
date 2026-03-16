"""
LLM-интеграция через Google Gemini API
Генерация кода и анализ идей
"""
import asyncio
import httpx
import os
import json
import google.generativeai as genai
from typing import Optional, Dict, Any

GEMINI_API_KEY = os.getenv("GEMINI_API_KEY", "")
GEMINI_MODEL = os.getenv("GEMINI_MODEL", "gemini-1.5-flash")


class LLMClient:
    """Клиент для работы с LLM через Google Gemini"""
    
    def __init__(self, api_key: str = None):
        self.api_key = api_key or GEMINI_API_KEY
        self.model_name = GEMINI_MODEL
        self.model = None
        
        if self.api_key:
            try:
                genai.configure(api_key=self.api_key)
                self.model = genai.GenerativeModel(self.model_name)
            except Exception as e:
                print(f"Ошибка инициализации Gemini: {e}")
    
    def update_api_key(self, api_key: str):
        """Обновление API ключа (для динамической настройки через бота)"""
        self.api_key = api_key
        try:
            genai.configure(api_key=self.api_key)
            self.model = genai.GenerativeModel(self.model_name)
        except Exception as e:
            print(f"Ошибка обновления ключа Gemini: {e}")
            self.model = None
    
    async def chat(self, messages: list, temperature: float = 0.7) -> Optional[str]:
        """
        Отправка запроса к LLM
        
        Args:
            messages: Список сообщений в формате [{"role": "user", "content": "..."}]
            temperature: Температура генерации (0.0 - 1.0)
        
        Returns:
            Ответ от модели или None при ошибке
        """
        if not self.api_key or not self.model:
            return "Ошибка: не настроен GEMINI_API_KEY. Настройте через Telegram бота командой /setkey"
        
        try:
            # Конвертируем сообщения в формат Gemini
            prompt_parts = []
            for msg in messages:
                role = msg.get("role", "user")
                content = msg.get("content", "")
                
                # System prompt добавляем в начало
                if role == "system":
                    prompt_parts.insert(0, f"System instruction: {content}\n\n")
                else:
                    prompt_parts.append(content)
            
            prompt = "".join(prompt_parts)
            
            # Генерируем ответ
            response = await asyncio.get_event_loop().run_in_executor(
                None, 
                lambda: self.model.generate_content(
                    prompt,
                    generation_config=genai.types.GenerationConfig(
                        temperature=temperature
                    )
                )
            )
            
            return response.text
            
        except Exception as e:
            error_msg = str(e)
            if "API_KEY" in error_msg.upper():
                return "Ошибка: неверный API ключ Gemini. Используйте команду /setkey в Telegram боте."
            return f"Ошибка при вызове Gemini: {error_msg}"
    
    async def analyze_idea(self, idea: str) -> Dict[str, Any]:
        """
        Анализ идеи пользователя и уточнение требований
        
        Args:
            idea: Исходная идея пользователя
        
        Returns:
            Словарь с анализом и уточняющими вопросами
        """
        system_prompt = """Ты — старший аналитик требований для AI Code Factory.
Твоя задача: проанализировать идею приложения и задать уточняющие вопросы (если нужны).
Верни ответ ТОЛЬКО в формате JSON без markdown обёрток:
{
    "summary": "Краткое описание идеи (1-2 предложения)",
    "key_features": ["список ключевых функций"],
    "questions": ["список уточняющих вопросов, если нужны"],
    "is_clear": true/false (нужны ли ещё вопросы)
}"""
        
        messages = [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": f"Идея приложения: {idea}"}
        ]
        
        response = await self.chat(messages, temperature=0.3)
        
        if not response:
            return {
                "summary": idea,
                "key_features": [],
                "questions": ["Пожалуйста, опишите идею подробнее"],
                "is_clear": False
            }
        
        try:
            # Пытаемся распарсить JSON из ответа
            # Очищаем от возможных markdown обёрток
            clean_response = response.replace("```json", "").replace("```", "").strip()
            start_idx = clean_response.find("{")
            end_idx = clean_response.rfind("}") + 1
            
            if start_idx >= 0 and end_idx > start_idx:
                json_str = clean_response[start_idx:end_idx]
                return json.loads(json_str)
            else:
                return {
                    "summary": idea,
                    "key_features": [],
                    "questions": ["Пожалуйста, опишите идею подробнее"],
                    "is_clear": False
                }
        except Exception:
            return {
                "summary": idea,
                "key_features": [],
                "questions": ["Не удалось проанализировать идею. Опишите подробнее."],
                "is_clear": False
            }
    
    async def generate_project_spec(self, idea: str, answers: str = "") -> str:
        """
        Генерация спецификации проекта на основе идеи и ответов
        
        Args:
            idea: Исходная идея
            answers: Ответы на уточняющие вопросы
        
        Returns:
            Текстовая спецификация проекта
        """
        system_prompt = """Ты — архитектор ПО. Создай детальную спецификацию проекта на основе идеи.
Включи:
1. Название проекта
2. Описание функционала
3. Структуру базы данных (таблицы, поля)
4. API endpoints
5. Компоненты frontend
6. Особенности реализации

Формат: структурированный текст с заголовками."""
        
        user_content = f"Идея: {idea}\n"
        if answers:
            user_content += f"Ответы на вопросы: {answers}\n"
        user_content += "\nСоздай полную спецификацию проекта."
        
        messages = [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_content}
        ]
        
        return await self.chat(messages, temperature=0.5)
    
    async def generate_code(self, spec: str, file_path: str) -> str:
        """
        Генерация кода для конкретного файла
        
        Args:
            spec: Спецификация проекта
            file_path: Путь к файлу (например, src/main.py)
        
        Returns:
            Сгенерированный код
        """
        system_prompt = f"""Ты — senior разработчик. Твоя задача — написать код для файла: {file_path}
На основе спецификации проекта создай полный, рабочий код.
Требования:
- Код должен быть готов к продакшену
- Обработка ошибок
- Комментарии на русском
- Следуй лучшим практикам

Верни ТОЛЬКО код файла, без объяснений и markdown-обёрток. Если используешь markdown, то только тройные кавычки с языком."""
        
        messages = [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": f"Спецификация проекта:\n{spec}"}
        ]
        
        response = await self.chat(messages, temperature=0.3)
        
        # Очищаем от markdown обёрток если они есть
        if response:
            response = response.replace("```python", "").replace("```javascript", "")
            response = response.replace("```html", "").replace("```css", "")
            response = response.replace("```json", "").replace("```yaml", "")
            response = response.replace("```dockerfile", "").replace("```", "").strip()
        
        return response
    
    async def generate_file_list(self, spec: str) -> list:
        """
        Генерация списка файлов проекта
        
        Args:
            spec: Спецификация проекта
        
        Returns:
            Список путей к файлам
        """
        system_prompt = """Создай полный список файлов для проекта FastAPI + PostgreSQL + HTML/JS.
Включи:
- Docker файлы (Dockerfile, docker-compose.yml)
- Backend файлы (main.py, database.py, models.py, etc.)
- Frontend файлы (index.html, styles.css, app.js)
- Конфигурационные файлы (.env.example, .gitignore, requirements.txt)

Верни ответ ТОЛЬКО в формате JSON массива строк без markdown обёрток: ["file1.py", "file2.html", ...]"""
        
        messages = [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": f"Спецификация проекта:\n{spec}"}
        ]
        
        response = await self.chat(messages, temperature=0.3)
        
        if not response:
            return ["src/main.py", "frontend/index.html"]
        
        try:
            # Очищаем от markdown обёрток
            clean_response = response.replace("```json", "").replace("```", "").strip()
            start_idx = clean_response.find("[")
            end_idx = clean_response.rfind("]") + 1
            
            if start_idx >= 0 and end_idx > start_idx:
                json_str = clean_response[start_idx:end_idx]
                return json.loads(json_str)
            else:
                return ["src/main.py", "frontend/index.html"]
        except Exception:
            return ["src/main.py", "frontend/index.html"]


# Глобальный экземпляр клиента (будет инициализирован позже с ключом)
llm_client = LLMClient()