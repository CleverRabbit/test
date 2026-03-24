"""
Конфигурация приложения AI Developer
Оптимизировано для работы с ограниченными ресурсами (2GB RAM, 1vCPU)
"""

import os
from dotenv import load_dotenv

# Загрузка переменных окружения
load_dotenv()

class Config:
    """Базовая конфигурация"""
    
    # Flask настройки
    SECRET_KEY = os.getenv('SECRET_KEY', 'dev-secret-key-change-me')
    HOST = os.getenv('HOST', '0.0.0.0')
    PORT = int(os.getenv('PORT', 5000))
    
    # База данных (SQLite для легковесности)
    DATABASE_URL = os.getenv('DATABASE_URL', 'sqlite:///ai_developer.db')
    
    # Gemini API
    GEMINI_API_KEY = os.getenv('GEMINI_API_KEY', '')
    GEMINI_API_URL = 'https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent'
    
    # Docker настройки
    DOCKER_PROJECTS_PATH = os.getenv('DOCKER_PROJECTS_PATH', '/workspace/projects')
    DEFAULT_CONTAINER_MEMORY_LIMIT = os.getenv('DEFAULT_CONTAINER_MEMORY_LIMIT', '256m')
    DEFAULT_CONTAINER_CPU_LIMIT = float(os.getenv('DEFAULT_CONTAINER_CPU_LIMIT', 0.5))
    
    # Авторизация
    SESSION_TIMEOUT_MINUTES = int(os.getenv('SESSION_TIMEOUT_MINUTES', 120))
    MAX_LOGIN_ATTEMPTS = int(os.getenv('MAX_LOGIN_ATTEMPTS', 5))
    
    # Лимиты ресурсов (критично для 2GB RAM / 1vCPU)
    MAX_PROJECTS_PER_USER = int(os.getenv('MAX_PROJECTS_PER_USER', 10))
    MAX_CONCURRENT_CONTAINERS = int(os.getenv('MAX_CONCURRENT_CONTAINERS', 5))
    CLEANUP_UNUSED_CONTAINERS_HOURS = int(os.getenv('CLEANUP_UNUSED_CONTAINERS_HOURS', 24))
    
    # Логи
    LOG_LEVEL = os.getenv('LOG_LEVEL', 'INFO')
    LOG_FILE = os.getenv('LOG_FILE', '/workspace/ai_developer.log')
    
    # Оптимизация памяти
    MAX_CONTENT_LENGTH = 16 * 1024 * 1024  # 16MB макс размер запроса
    
    @classmethod
    def validate(cls):
        """Проверка критических настроек"""
        if not cls.GEMINI_API_KEY or cls.GEMINI_API_KEY == 'your_gemini_api_key_here':
            raise ValueError("GEMINI_API_KEY не настроен! Укажите ваш ключ в .env файле.")
        
        # Создание необходимых директорий
        os.makedirs(cls.DOCKER_PROJECTS_PATH, exist_ok=True)
        
        return True


class DevelopmentConfig(Config):
    """Конфигурация для разработки"""
    DEBUG = True
    LOG_LEVEL = 'DEBUG'


class ProductionConfig(Config):
    """Конфигурация для продакшена"""
    DEBUG = False
    

config_map = {
    'development': DevelopmentConfig,
    'production': ProductionConfig,
    'default': ProductionConfig
}


def get_config():
    """Получение конфигурации в зависимости от окружения"""
    env = os.getenv('FLASK_ENV', 'production')
    return config_map.get(env, config_map['default'])
