"""
Модели данных AI Developer
Работа с пользователями, проектами, сообщениями и аудитом
"""

import hashlib
import secrets
import logging
from datetime import datetime, timedelta
from config import Config
from services.database import db

logger = logging.getLogger(__name__)


class User:
    """Модель пользователя"""
    
    @staticmethod
    def hash_password(password):
        """Хеширование пароля"""
        return hashlib.sha256(password.encode()).hexdigest()
    
    @staticmethod
    def generate_api_key():
        """Генерация API ключа"""
        return f"ak_{secrets.token_hex(16)}"
    
    @staticmethod
    def create(username, password, email=None, role='user'):
        """Создание нового пользователя"""
        password_hash = User.hash_password(password)
        api_key = User.generate_api_key()
        
        db.execute('''
            INSERT INTO users (username, password_hash, email, role, api_key)
            VALUES (?, ?, ?, ?, ?)
        ''', (username, password_hash, email, role, api_key))
        
        return User.get_by_username(username)
    
    @staticmethod
    def get_by_id(user_id):
        """Получение пользователя по ID"""
        return db.fetchone('SELECT * FROM users WHERE id = ?', (user_id,))
    
    @staticmethod
    def get_by_username(username):
        """Получение пользователя по имени"""
        return db.fetchone('SELECT * FROM users WHERE username = ?', (username,))
    
    @staticmethod
    def get_by_api_key(api_key):
        """Получение пользователя по API ключу"""
        return db.fetchone('SELECT * FROM users WHERE api_key = ?', (api_key,))
    
    @staticmethod
    def verify_login(username, password):
        """Проверка логина и пароля"""
        user = User.get_by_username(username)
        if not user:
            return None
        
        password_hash = User.hash_password(password)
        if user['password_hash'] != password_hash:
            return None
        
        # Обновление времени последнего входа
        db.execute('UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE id = ?', (user['id'],))
        
        return user
    
    @staticmethod
    def create_session(user_id, ip_address=None):
        """Создание сессии"""
        token = secrets.token_urlsafe(32)
        expires_at = datetime.now() + timedelta(minutes=Config.SESSION_TIMEOUT_MINUTES)
        
        db.execute('''
            INSERT INTO sessions (user_id, token, ip_address, expires_at)
            VALUES (?, ?, ?, ?)
        ''', (user_id, token, ip_address, expires_at))
        
        return token
    
    @staticmethod
    def get_session(token):
        """Получение сессии по токену"""
        session = db.fetchone('''
            SELECT s.*, u.username, u.role, u.email, u.api_key
            FROM sessions s
            JOIN users u ON s.user_id = u.id
            WHERE s.token = ? AND s.expires_at > CURRENT_TIMESTAMP
        ''', (token,))
        return session
    
    @staticmethod
    def delete_session(token):
        """Удаление сессии"""
        return db.delete('sessions', 'token = ?', (token,))
    
    @staticmethod
    def cleanup_expired_sessions():
        """Очистка истекших сессий"""
        db.delete('sessions', 'expires_at < CURRENT_TIMESTAMP')
    
    @staticmethod
    def update_gemini_key(user_id, gemini_key):
        """Обновление Gemini API ключа пользователя"""
        db.execute('UPDATE users SET gemini_api_key = ? WHERE id = ?', (gemini_key, user_id))
        return True
    
    @staticmethod
    def get_all():
        """Получение всех пользователей"""
        return db.fetchall('SELECT id, username, email, role, created_at, last_login FROM users')


class Project:
    """Модель проекта"""
    
    @staticmethod
    def create(name, user_id, description=None, system_prompt=None):
        """Создание проекта"""
        cursor = db.execute('''
            INSERT INTO projects (name, description, user_id, system_prompt)
            VALUES (?, ?, ?, ?)
        ''', (name, description, user_id, system_prompt))
        
        return Project.get_by_id(cursor)
    
    @staticmethod
    def get_by_id(project_id):
        """Получение проекта по ID"""
        return db.fetchone('SELECT * FROM projects WHERE id = ?', (project_id,))
    
    @staticmethod
    def get_by_user(user_id):
        """Получение проектов пользователя"""
        return db.fetchall('SELECT * FROM projects WHERE user_id = ? ORDER BY created_at DESC', (user_id,))
    
    @staticmethod
    def update_status(project_id, status, port=None, container_id=None):
        """Обновление статуса проекта"""
        updates = ['status = ?', 'updated_at = CURRENT_TIMESTAMP']
        params = [status]
        
        if port is not None:
            updates.append('port = ?')
            params.append(port)
        
        if container_id is not None:
            updates.append('container_id = ?')
            params.append(container_id)
        
        params.append(project_id)
        query = f"UPDATE projects SET {', '.join(updates)} WHERE id = ?"
        db.execute(query, tuple(params))
        
        return True
    
    @staticmethod
    def update_system_prompt(project_id, system_prompt):
        """Обновление системного промпта проекта"""
        db.execute('UPDATE projects SET system_prompt = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?', 
                  (system_prompt, project_id))
        return True
    
    @staticmethod
    def delete(project_id):
        """Удаление проекта"""
        return db.delete('projects', 'id = ?', (project_id,))
    
    @staticmethod
    def count_by_user(user_id):
        """Подсчет количества проектов пользователя"""
        result = db.fetchone('SELECT COUNT(*) as count FROM projects WHERE user_id = ?', (user_id,))
        return result['count'] if result else 0


class ChatMessage:
    """Модель сообщения чата"""
    
    @staticmethod
    def create(user_id, role, content, project_id=None, tokens_used=0):
        """Создание сообщения"""
        db.execute('''
            INSERT INTO chat_messages (user_id, role, content, project_id, tokens_used)
            VALUES (?, ?, ?, ?, ?)
        ''', (user_id, role, content, project_id, tokens_used))
    
    @staticmethod
    def get_conversation(user_id, project_id=None, limit=20):
        """Получение истории переписки"""
        if project_id:
            return db.fetchall('''
                SELECT * FROM chat_messages 
                WHERE user_id = ? AND project_id = ?
                ORDER BY created_at DESC LIMIT ?
            ''', (user_id, project_id, limit))
        else:
            return db.fetchall('''
                SELECT * FROM chat_messages 
                WHERE user_id = ?
                ORDER BY created_at DESC LIMIT ?
            ''', (user_id, limit))
    
    @staticmethod
    def delete_by_project(project_id):
        """Удаление сообщений проекта"""
        return db.delete('chat_messages', 'project_id = ?', (project_id,))
    
    @staticmethod
    def get_stats(user_id):
        """Получение статистики использования"""
        result = db.fetchone('''
            SELECT COUNT(*) as total_messages, SUM(tokens_used) as total_tokens
            FROM chat_messages WHERE user_id = ?
        ''', (user_id,))
        return result or {'total_messages': 0, 'total_tokens': 0}


class AuditLog:
    """Модель аудита"""
    
    @staticmethod
    def log(action, user_id=None, resource_type=None, resource_id=None, details=None, ip_address=None):
        """Логирование действия"""
        db.execute('''
            INSERT INTO audit_log (action, user_id, resource_type, resource_id, details, ip_address)
            VALUES (?, ?, ?, ?, ?, ?)
        ''', (action, user_id, resource_type, resource_id, details, ip_address))
    
    @staticmethod
    def get_logs(user_id=None, limit=100):
        """Получение логов аудита"""
        if user_id:
            return db.fetchall('''
                SELECT * FROM audit_log WHERE user_id = ?
                ORDER BY created_at DESC LIMIT ?
            ''', (user_id, limit))
        else:
            return db.fetchall('''
                SELECT * FROM audit_log ORDER BY created_at DESC LIMIT ?
            ''', (limit,))
    
    @staticmethod
    def cleanup_old_logs(days=30):
        """Очистка старых логов"""
        db.delete('audit_log', 'created_at < datetime("now", "-? days")', (days,))


class LoginAttempt:
    """Модель попыток входа"""
    
    @staticmethod
    def log_attempt(username, ip_address, success=False):
        """Логирование попытки входа"""
        db.execute('''
            INSERT INTO login_attempts (username, ip_address, success)
            VALUES (?, ?, ?)
        ''', (username, ip_address, success))
    
    @staticmethod
    def get_failed_attempts(username, ip_address, minutes=15):
        """Получение неудачных попыток входа"""
        result = db.fetchone('''
            SELECT COUNT(*) as count FROM login_attempts
            WHERE username = ? AND ip_address = ? AND success = FALSE
            AND attempt_time > datetime("now", "-? minutes")
        ''', (username, ip_address, minutes))
        return result['count'] if result else 0
    
    @staticmethod
    def cleanup_old_attempts(days=7):
        """Очистка старых записей"""
        db.delete('login_attempts', 'attempt_time < datetime("now", "-? days")', (days,))
