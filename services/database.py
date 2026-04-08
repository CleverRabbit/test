"""
Модуль базы данных AI Developer
Работа с SQLite базой данных
"""

import sqlite3
import logging
from functools import wraps
from flask import g, request, jsonify
from config import Config

logger = logging.getLogger(__name__)


class Database:
    """Класс для работы с базой данных SQLite"""
    
    def __init__(self, db_path=None):
        self.db_path = db_path or Config.DB_PATH
        self._init_db()
    
    def _init_db(self):
        """Инициализация схемы БД"""
        with self.get_connection() as conn:
            cursor = conn.cursor()
            
            # Таблица пользователей
            cursor.execute('''
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT UNIQUE NOT NULL,
                    password_hash TEXT NOT NULL,
                    email TEXT,
                    role TEXT DEFAULT 'user',
                    api_key TEXT UNIQUE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    last_login TIMESTAMP
                )
            ''')
            
            # Таблица сессий
            cursor.execute('''
                CREATE TABLE IF NOT EXISTS sessions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    token TEXT UNIQUE NOT NULL,
                    ip_address TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    expires_at TIMESTAMP NOT NULL,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                )
            ''')
            
            # Таблица проектов
            cursor.execute('''
                CREATE TABLE IF NOT EXISTS projects (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    description TEXT,
                    user_id INTEGER NOT NULL,
                    status TEXT DEFAULT 'created',
                    port INTEGER,
                    container_id TEXT,
                    system_prompt TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                )
            ''')
            
            # Таблица сообщений чата
            cursor.execute('''
                CREATE TABLE IF NOT EXISTS chat_messages (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    role TEXT NOT NULL,
                    content TEXT NOT NULL,
                    project_id INTEGER,
                    tokens_used INTEGER DEFAULT 0,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
                )
            ''')
            
            # Таблица аудита
            cursor.execute('''
                CREATE TABLE IF NOT EXISTS audit_log (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER,
                    action TEXT NOT NULL,
                    resource_type TEXT,
                    resource_id INTEGER,
                    details TEXT,
                    ip_address TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
                )
            ''')
            
            # Таблица блокировок входа
            cursor.execute('''
                CREATE TABLE IF NOT EXISTS login_attempts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL,
                    ip_address TEXT,
                    attempt_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    success BOOLEAN DEFAULT FALSE
                )
            ''')
            
            conn.commit()
            logger.info("База данных инициализирована")
    
    def get_connection(self):
        """Получение соединения с БД"""
        return sqlite3.connect(self.db_path)
    
    def execute(self, query, params=(), fetch=False, many=False):
        """Выполнение SQL запроса"""
        try:
            with self.get_connection() as conn:
                cursor = conn.cursor()
                if many:
                    if not isinstance(params, list):
                        params = [params]
                    cursor.executemany(query, params)
                else:
                    cursor.execute(query, params)
                
                if fetch:
                    return cursor.fetchall()
                
                conn.commit()
                return cursor.lastrowid
        except sqlite3.Error as e:
            logger.error(f"Ошибка БД: {e}")
            raise
    
    def fetchall(self, query, params=()):
        """Выполнение SELECT запроса с возвратом всех результатов"""
        try:
            with self.get_connection() as conn:
                cursor = conn.cursor()
                cursor.execute(query, params)
                rows = cursor.fetchall()
                columns = [description[0] for description in cursor.description] if cursor.description else []
                return [dict(zip(columns, row)) for row in rows]
        except sqlite3.Error as e:
            logger.error(f"Ошибка БД: {e}")
            raise
    
    def fetchone(self, query, params=()):
        """Выполнение SELECT запроса с возвратом одной строки"""
        try:
            with self.get_connection() as conn:
                cursor = conn.cursor()
                cursor.execute(query, params)
                row = cursor.fetchone()
                if row:
                    columns = [description[0] for description in cursor.description]
                    return dict(zip(columns, row))
                return None
        except sqlite3.Error as e:
            logger.error(f"Ошибка БД: {e}")
            raise
    
    def delete(self, table, condition, params=()):
        """Удаление записей из таблицы"""
        query = f"DELETE FROM {table} WHERE {condition}"
        with self.get_connection() as conn:
            cursor = conn.cursor()
            cursor.execute(query, params)
            conn.commit()
            return cursor.rowcount


# Глобальный экземпляр БД
db = Database()


# ==================== Декораторы авторизации ====================

def login_required(f):
    """Декоратор для защиты маршрутов, требующих авторизации"""
    @wraps(f)
    def decorated_function(*args, **kwargs):
        if not hasattr(g, 'current_user') or not g.current_user:
            return jsonify({'error': 'Требуется авторизация'}), 401
        return f(*args, **kwargs)
    return decorated_function


def admin_required(f):
    """Декоратор для защиты маршрутов, требующих прав администратора"""
    @wraps(f)
    def decorated_function(*args, **kwargs):
        if not hasattr(g, 'current_user') or not g.current_user:
            return jsonify({'error': 'Требуется авторизация'}), 401
        if g.current_user['role'] != 'admin':
            return jsonify({'error': 'Требуется роль администратора'}), 403
        return f(*args, **kwargs)
    return decorated_function
