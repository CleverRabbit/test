"""
Сервис работы с Gemini API
Обертка над клиентом для удобного использования
"""

import logging
from config import Config
from services.gemini_client import GeminiClient, get_gemini_client as _get_gemini_client

logger = logging.getLogger(__name__)


class GeminiService:
    """Сервис для работы с Gemini API"""
    
    def __init__(self):
        self.client = None
    
    def get_client(self, api_key=None):
        """Получение клиента Gemini"""
        if api_key:
            return GeminiClient(api_key)
        return _get_gemini_client()
    
    def generate_code(self, prompt, context=None, language='python', api_key=None, system_prompt=None):
        """Генерация кода через Gemini API"""
        client = self.get_client(api_key)
        return client.generate_code(prompt, context, language, system_prompt)
    
    def chat(self, message, context=None, api_key=None, system_prompt=None):
        """Чат с AI через Gemini API"""
        client = self.get_client(api_key)
        return client.chat(message, context, system_prompt)
    
    def test_connection(self, api_key=None):
        """Проверка соединения с Gemini API"""
        client = self.get_client(api_key)
        return client.test_connection()
    
    def is_available(self, api_key=None):
        """Проверка доступности API"""
        client = self.get_client(api_key)
        return client.is_available()


# Глобальный экземпляр сервиса
gemini_service = GeminiService()


def get_gemini_service():
    """Получение экземпляра сервиса"""
    return gemini_service
