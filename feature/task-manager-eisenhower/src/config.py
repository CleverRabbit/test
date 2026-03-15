"""
Конфигурация приложения через pydantic-settings.
Все секреты читаются из переменных окружения.
"""
from pydantic_settings import BaseSettings
from typing import Optional


class Settings(BaseSettings):
    """Настройки приложения"""
    
    # Telegram Bot
    telegram_bot_token: str
    
    # OpenRouter API
    openrouter_api_key: str
    openrouter_model: str = "Qwen/Qwen2.5-7B-Instruct"
    
    # Database
    postgres_db: str = "taskmanager_db"
    postgres_user: str = "taskmanager_user"
    postgres_password: str
    database_url: Optional[str] = None
    
    # Application
    app_env: str = "production"
    log_level: str = "INFO"
    secret_key: str
    
    # Admin Panel
    admin_username: str = "admin"
    admin_password: str = "admin"
    
    @property
    def db_url(self) -> str:
        """Возвращает URL базы данных"""
        if self.database_url:
            return self.database_url
        return f"postgresql://{self.postgres_user}:{self.postgres_password}@db:5432/{self.postgres_db}"
    
    class Config:
        env_file = ".env"
        case_sensitive = False


# Глобальный экземпляр настроек
settings = Settings()
