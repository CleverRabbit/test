"""
Pydantic модели для API запросов и ответов.
Обеспечивают валидацию данных.
"""
from pydantic import BaseModel, Field
from typing import Optional, List
from datetime import datetime
from enum import Enum


class QuadrantEnum(str, Enum):
    """Квадранты для API"""
    Q1 = "q1"
    Q2 = "q2"
    Q3 = "q3"
    Q4 = "q4"


class StatusEnum(str, Enum):
    """Статусы для API"""
    TODO = "todo"
    IN_PROGRESS = "in_progress"
    DONE = "done"


# Модели для создания задачи
class TaskCreate(BaseModel):
    """Модель создания задачи (от бота)"""
    original_text: str = Field(..., min_length=1, max_length=2000)
    telegram_id: str


class TaskCreateResponse(BaseModel):
    """Ответ после создания задачи"""
    id: int
    title: str
    quadrant: QuadrantEnum
    status: StatusEnum
    message: str


# Модели для обновления задачи
class TaskUpdate(BaseModel):
    """Модель обновления задачи"""
    title: Optional[str] = Field(None, max_length=255)
    description: Optional[str] = None
    quadrant: Optional[QuadrantEnum] = None
    status: Optional[StatusEnum] = None
    deadline: Optional[datetime] = None


# Полная модель задачи для ответа
class TaskResponse(BaseModel):
    """Полная модель задачи"""
    id: int
    user_id: int
    original_text: str
    anonymized_text: Optional[str] = None
    title: str
    description: Optional[str] = None
    quadrant: QuadrantEnum
    status: StatusEnum
    deadline: Optional[datetime] = None
    created_at: datetime
    updated_at: datetime
    
    class Config:
        from_attributes = True


# Модель пользователя
class UserResponse(BaseModel):
    """Модель пользователя"""
    id: int
    telegram_id: str
    username: Optional[str] = None
    created_at: datetime
    
    class Config:
        from_attributes = True


# Health check ответ
class HealthResponse(BaseModel):
    """Ответ health check"""
    status: str
    database: str
    timestamp: datetime
