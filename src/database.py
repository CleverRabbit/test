"""
База данных и модели для AI Code Factory
"""
from sqlalchemy import create_engine, Column, Integer, String, Text, DateTime, Boolean, ForeignKey
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker, relationship
from datetime import datetime
import os

DATABASE_URL = os.getenv("DATABASE_URL", "postgresql://postgres:postgres_password@db:5432/factory_db")

engine = create_engine(DATABASE_URL)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()


class Project(Base):
    """Модель проекта, созданного фабрикой"""
    __tablename__ = "projects"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(String, index=True)  # Telegram user ID
    idea = Column(Text, nullable=False)  # Исходная идея пользователя
    project_name = Column(String, nullable=False)  # Сгенерированное имя проекта
    github_repo_url = Column(String)  # URL репозитория GitHub
    render_deploy_url = Column(String)  # URL деплоя на Render
    magic_link_hash = Column(String, unique=True, index=True)  # Хэш для магической ссылки
    magic_link_expires = Column(DateTime)  # Срок действия магической ссылки
    status = Column(String, default="pending")  # pending, generating, deployed, failed
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    
    # Связь с шагами генерации
    steps = relationship("GenerationStep", back_populates="project", cascade="all, delete-orphan")


class GenerationStep(Base):
    """Модель шага генерации проекта"""
    __tablename__ = "generation_steps"

    id = Column(Integer, primary_key=True, index=True)
    project_id = Column(Integer, ForeignKey("projects.id"), nullable=False)
    step_name = Column(String, nullable=False)  # name, analyze, generate, github, render
    status = Column(String, default="pending")  # pending, running, completed, failed
    message = Column(Text)  # Сообщение о статусе или ошибке
    created_at = Column(DateTime, default=datetime.utcnow)
    
    project = relationship("Project", back_populates="steps")


class User(Base):
    """Модель пользователя Telegram"""
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)
    telegram_id = Column(String, unique=True, index=True, nullable=False)
    username = Column(String)
    first_name = Column(String)
    last_name = Column(String)
    gemini_api_key = Column(String)  # API ключ Gemini для пользователя
    is_admin = Column(Boolean, default=False)
    created_at = Column(DateTime, default=datetime.utcnow)
    projects = relationship("Project", backref="user_obj")


def init_db():
    """Инициализация базы данных"""
    Base.metadata.create_all(bind=engine)


def get_db():
    """Получение сессии БД"""
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()