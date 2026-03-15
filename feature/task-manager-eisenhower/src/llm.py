"""
Сервис для работы с LLM через OpenRouter API.
Включает функции анонимизации данных перед отправкой.
"""
import re
import json
import httpx
from typing import Optional, Dict, Any
from loguru import logger

from src.config import settings


class Anonymizer:
    """Класс для анонимизации текста перед отправкой в LLM"""
    
    # Паттерны для замены чувствительных данных
    PATTERNS = {
        'phone': r'\+?[\d\s\-\(\)]{10,}',
        'email': r'\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}\b',
        'name': r'\b(?:Александр|Алексей|Анна|Мария|Иван|Петр|Сергей|Дмитрий|Елена|Ольга|Наталья|Юлия|Максим|Владимир|Andrey|Alexander|Alexey|Anna|Maria|Ivan|Petr|Sergey|Dmitry|Elena|Olga|Natalya|Yulia|Maxim|Vladimir)\b',
        'company': r'\b(?:ООО|ЗАО|ОАО|ИП|Ltd|LLC|Inc|Corp|Company|GmbH)\s+[A-Za-zА-Яа-яЁё\d\s]+\b',
    }
    
    REPLACEMENTS = {
        'phone': '[PHONE]',
        'email': '[EMAIL]',
        'name': '[PERSON]',
        'company': '[COMPANY]',
    }
    
    @classmethod
    def anonymize(cls, text: str) -> str:
        """
        Анонимизирует текст, заменяя чувствительные данные на плейсхолдеры.
        
        Args:
            text: Исходный текст
            
        Returns:
            Анонимизированный текст
        """
        result = text
        for key, pattern in cls.PATTERNS.items():
            replacement = cls.REPLACEMENTS.get(key, '[REDACTED]')
            result = re.sub(pattern, replacement, result, flags=re.IGNORECASE)
        
        return result


class LLMService:
    """Сервис для взаимодействия с OpenRouter API"""
    
    def __init__(self):
        self.api_key = settings.openrouter_api_key
        self.model = settings.openrouter_model
        self.base_url = "https://openrouter.ai/api/v1"
        self.anonymizer = Anonymizer()
    
    async def analyze_task(self, text: str) -> Optional[Dict[str, Any]]:
        """
        Анализирует задачу через LLM и возвращает структурированные данные.
        
        Args:
            text: Текст задачи от пользователя
            
        Returns:
            Словарь с полями: title, description, quadrant, deadline (опционально)
            или None при ошибке
        """
        try:
            # Шаг 1: Анонимизация
            anonymized_text = self.anonymizer.anonymize(text)
            logger.info(f"Анонимизированный текст: {anonymized_text}")
            
            # Шаг 2: Формирование промпта
            prompt = f"""
Ты — ассистент для управления задачами по методу Эйзенхауэра.
Проанализируй следующую задачу и верни ответ ТОЛЬКО в формате JSON без дополнительного текста.

Задача: {anonymized_text}

Верни JSON со следующими полями:
- title: краткий заголовок задачи (до 50 символов)
- description: подробное описание (если есть детали)
- quadrant: один из вариантов "q1", "q2", "q3", "q4"
  - q1: Срочно и Важно (дедлайн сегодня/завтра, критично)
  - q2: Не срочно, но Важно (стратегические задачи, развитие)
  - q3: Срочно, но Не важно (рутина, чужие просьбы)
  - q4: Не срочно и Не важно (поглотители времени)
- deadline: дата дедлайна в формате YYYY-MM-DD или null

Пример ответа:
{{"title": "Подготовить отчет", "description": "Ежемесячный отчет по продажам", "quadrant": "q1", "deadline": "2024-01-15"}}
"""
            
            # Шаг 3: Вызов API
            async with httpx.AsyncClient(timeout=30.0) as client:
                response = await client.post(
                    f"{self.base_url}/chat/completions",
                    headers={
                        "Authorization": f"Bearer {self.api_key}",
                        "Content-Type": "application/json",
                        "HTTP-Referer": "https://localhost",  # Требуется OpenRouter
                        "X-Title": "Task Manager Eisenhower"
                    },
                    json={
                        "model": self.model,
                        "messages": [
                            {"role": "system", "content": "Ты полезный ассистент. Отвечай ТОЛЬКО валидным JSON."},
                            {"role": "user", "content": prompt}
                        ],
                        "temperature": 0.3,  # Низкая температура для консистентности
                        "max_tokens": 500
                    }
                )
                
                if response.status_code != 200:
                    logger.error(f"OpenRouter API error: {response.status_code} - {response.text}")
                    return None
                
                data = response.json()
                content = data.get("choices", [{}])[0].get("message", {}).get("content", "")
                
                # Шаг 4: Парсинг JSON ответа
                # Очищаем ответ от возможных markdown-оберток
                content = content.strip()
                if content.startswith("```json"):
                    content = content[7:]
                if content.endswith("```"):
                    content = content[:-3]
                content = content.strip()
                
                try:
                    result = json.loads(content)
                    logger.info(f"LLM результат: {result}")
                    
                    # Валидация обязательных полей
                    if not all(k in result for k in ["title", "quadrant"]):
                        logger.warning("Отсутствуют обязательные поля в ответе LLM")
                        return None
                    
                    # Добавляем анонимизированный текст
                    result["anonymized_text"] = anonymized_text
                    return result
                    
                except json.JSONDecodeError as e:
                    logger.error(f"Ошибка парсинга JSON от LLM: {e}")
                    return None
                    
        except Exception as e:
            logger.error(f"Ошибка в LLM сервисе: {e}")
            return None


# Глобальный экземпляр сервиса
llm_service = LLMService()
