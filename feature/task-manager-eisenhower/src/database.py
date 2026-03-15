"""
Модели базы данных и SQLAlchemy конфигурация.
Определяет таблицы users и tasks.
"""
from sqlalchemy import create_engine, Column, Integer, String, Text, DateTime, Enum as SQLEnum, ForeignKey, Boolean
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker, relationship
from datetime import datetime
import enum

from src.config import settings


# Перечисления для статусов и квадрантов
class TaskQuadrant(str, enum.Enum):
    """Квадранты матрицы Эйзенхауэра"""
    Q1 = "q1"  # Срочно и Важно
    Q2 = "q2"  # Не срочно, но Важно
    Q3 = "q3"  # Срочно, но Не важно
    Q4 = "q4"  # Не срочно и Не важно


class TaskStatus(str, enum.Enum):
    """Статусы задачи"""
    TODO = "todo"
    IN_PROGRESS = "in_progress"
    DONE = "done"


# SQLAlchemy setup
engine = create_engine(settings.db_url)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()


class User(Base):
    """Таблица пользователей"""
    __tablename__ = "users"
    
    id = Column(Integer, primary_key=True, index=True)
    telegram_id = Column(String, unique=True, index=True, nullable=False)
    username = Column(String, nullable=True)
    created_at = Column(DateTime, default=datetime.utcnow)
    
    # Связь с задачами
    tasks = relationship("Task", back_populates="user", cascade="all, delete-orphan")


class Task(Base):
    """Таблица задач"""
    __tablename__ = "tasks"
    
    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    
    # Исходный текст от пользователя
    original_text = Column(Text, nullable=False)
    
    # Анонимизированный текст (для отправки в LLM)
    anonymized_text = Column(Text, nullable=True)
    
    # Обработанные данные от AI
    title = Column(String(255), nullable=False)
    description = Column(Text, nullable=True)
    
    # Квадрант Эйзенхауэра
    quadrant = Column(SQLEnum(TaskQuadrant), default=TaskQuadrant.Q2)
    
    # Статус выполнения
    status = Column(SQLEnum(TaskStatus), default=TaskStatus.TODO)
    
    # Дедлайн (если определен)
    deadline = Column(DateTime, nullable=True)
    
    # Временные метки
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    
    # Связь с пользователем
    user = relationship("User", back_populates="tasks")


def init_db():
    """Инициализация базы данных (создание таблиц)"""
    Base.metadata.create_all(bind=engine)


def get_db():
    """Генератор сессий БД для зависимостей FastAPI"""
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
