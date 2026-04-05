"""
Модели данных для AI Developer
Использует SQLite для легковесности и минимального потребления памяти
"""

from datetime import datetime, timedelta
from functools import wraps
import sqlite3
import hashlib
import secrets
import threading


class Database:
    """Легковесная обертка над SQLite с пулом соединений"""
    
    _local = threading.local()
    
    def __init__(self, db_path='ai_developer.db'):
        self.db_path = db_path
        self.init_db()
    
    def get_connection(self):
        """Получение соединения из пула (thread-local)"""
        if not hasattr(self._local, 'connection') or self._local.connection is None:
            self._local.connection = sqlite3.connect(
                self.db_path,
                check_same_thread=False,
                timeout=30
            )
            self._local.connection.row_factory = sqlite3.Row
        return self._local.connection
    
    def init_db(self):
        """Инициализация схемы БД"""
        conn = self.get_connection()
        cursor = conn.cursor()
        
        # Таблица пользователей
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT UNIQUE NOT NULL,
                password_hash TEXT NOT NULL,
                email TEXT,
                role TEXT DEFAULT 'user',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                last_login TIMESTAMP,
                login_attempts INTEGER DEFAULT 0,
                locked_until TIMESTAMP,
                api_key TEXT UNIQUE
            )
        ''')
        
        # Таблица проектов
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS projects (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                description TEXT,
                user_id INTEGER NOT NULL,
                port INTEGER,
                container_id TEXT,
                status TEXT DEFAULT 'created',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                git_repo_path TEXT,
                docker_compose_path TEXT,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
            )
        ''')
        
        # Таблица сообщений чата
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS chat_messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                project_id INTEGER,
                role TEXT NOT NULL,
                content TEXT NOT NULL,
                timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                tokens_used INTEGER DEFAULT 0,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE SET NULL
            )
        ''')
        
        # Таблица сессий
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS sessions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                session_token TEXT UNIQUE NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                expires_at TIMESTAMP NOT NULL,
                ip_address TEXT,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
            )
        ''')
        
        # Таблица логов
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS audit_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER,
                action TEXT NOT NULL,
                resource_type TEXT,
                resource_id INTEGER,
                details TEXT,
                ip_address TEXT,
                timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
            )
        ''')
        
        # Индексы для оптимизации запросов
        cursor.execute('CREATE INDEX IF NOT EXISTS idx_projects_user_id ON projects(user_id)')
        cursor.execute('CREATE INDEX IF NOT EXISTS idx_projects_status ON projects(status)')
        cursor.execute('CREATE INDEX IF NOT EXISTS idx_chat_user_id ON chat_messages(user_id)')
        cursor.execute('CREATE INDEX IF NOT EXISTS idx_sessions_token ON sessions(session_token)')
        cursor.execute('CREATE INDEX IF NOT EXISTS idx_audit_user_id ON audit_logs(user_id)')
        
        conn.commit()
    
    def execute(self, query, params=None, fetch=False, many=False):
        """Выполнение SQL запроса"""
        conn = self.get_connection()
        cursor = conn.cursor()
        
        is_select = query.strip().upper().startswith('SELECT')
        
        if params:
            if many and not is_select:
                cursor.executemany(query, params)
            else:
                cursor.execute(query, params)
        else:
            cursor.execute(query)
        
        if not is_select:
            conn.commit()
        
        if fetch:
            if many and not is_select:
                return cursor.fetchall()
            # Для SELECT всегда используем fetchall, для DML - fetchone
            if is_select:
                result = cursor.fetchall()
                return [dict(row) for row in result]
            else:
                result = cursor.fetchone()
                return dict(result) if result else None
        return None
    
    def fetchall(self, query, params=None):
        """Получение всех результатов"""
        # Для SELECT запросов используем обычный execute без many
        conn = self.get_connection()
        cursor = conn.cursor()
        
        if params:
            cursor.execute(query, params)
        else:
            cursor.execute(query)
        
        result = cursor.fetchall()
        return [dict(row) for row in result] if result else []
    
    def fetchone(self, query, params=None):
        """Получение одного результата"""
        conn = self.get_connection()
        cursor = conn.cursor()
        
        if params:
            cursor.execute(query, params)
        else:
            cursor.execute(query)
        
        result = cursor.fetchone()
        return dict(result) if result else None


# Глобальный экземпляр БД
db = Database()


def hash_password(password):
    """Хеширование пароля"""
    salt = secrets.token_hex(16)
    password_hash = hashlib.pbkdf2_hmac('sha256', password.encode(), salt.encode(), 100000)
    return f"{salt}${password_hash.hex()}"


def verify_password(password, password_hash):
    """Проверка пароля"""
    try:
        salt, hash_value = password_hash.split('$')
        new_hash = hashlib.pbkdf2_hmac('sha256', password.encode(), salt.encode(), 100000)
        return new_hash.hex() == hash_value
    except Exception:
        return False


def generate_api_key():
    """Генерация API ключа"""
    return f"ak_{secrets.token_urlsafe(32)}"


class User:
    """Модель пользователя"""
    
    @staticmethod
    def create(username, password, email=None, role='user'):
        """Создание нового пользователя"""
        password_hash = hash_password(password)
        api_key = generate_api_key()
        
        db.execute('''
            INSERT INTO users (username, password_hash, email, role, api_key)
            VALUES (?, ?, ?, ?, ?)
        ''', (username, password_hash, email, role, api_key))
        
        return User.get_by_username(username)
    
    @staticmethod
    def get_by_username(username):
        """Получение пользователя по имени"""
        return db.fetchone('SELECT * FROM users WHERE username = ?', (username,))
    
    @staticmethod
    def get_by_id(user_id):
        """Получение пользователя по ID"""
        return db.fetchone('SELECT * FROM users WHERE id = ?', (user_id,))
    
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
        
        # Проверка блокировки
        if user['locked_until']:
            locked_until = datetime.fromisoformat(user['locked_until'])
            if datetime.now() < locked_until:
                return None
            else:
                # Сброс блокировки
                db.execute('UPDATE users SET login_attempts = 0, locked_until = NULL WHERE id = ?', 
                          (user['id'],))
        
        if not verify_password(password, user['password_hash']):
            # Увеличение счетчика попыток
            attempts = user['login_attempts'] + 1
            if attempts >= 5:
                # Блокировка на 30 минут
                locked_until = datetime.now() + timedelta(minutes=30)
                db.execute('''
                    UPDATE users SET login_attempts = ?, locked_until = ? WHERE id = ?
                ''', (attempts, locked_until.isoformat(), user['id']))
            else:
                db.execute('UPDATE users SET login_attempts = ? WHERE id = ?', (attempts, user['id']))
            return None
        
        # Успешный вход
        db.execute('''
            UPDATE users SET login_attempts = 0, last_login = CURRENT_TIMESTAMP WHERE id = ?
        ''', (user['id'],))
        
        return user
    
    @staticmethod
    def create_session(user_id, ip_address=None, timeout_minutes=120):
        """Создание сессии"""
        session_token = secrets.token_urlsafe(32)
        expires_at = datetime.now() + timedelta(minutes=timeout_minutes)
        
        db.execute('''
            INSERT INTO sessions (user_id, session_token, expires_at, ip_address)
            VALUES (?, ?, ?, ?)
        ''', (user_id, session_token, expires_at.isoformat(), ip_address))
        
        return session_token
    
    @staticmethod
    def validate_session(session_token):
        """Проверка сессии"""
        session = db.fetchone('''
            SELECT s.*, u.* FROM sessions s
            JOIN users u ON s.user_id = u.id
            WHERE s.session_token = ? AND s.expires_at > ?
        ''', (session_token, datetime.now().isoformat()))
        
        if session:
            # Продление сессии
            new_expires = datetime.now() + timedelta(minutes=120)
            db.execute('UPDATE sessions SET expires_at = ? WHERE session_token = ?',
                      (new_expires.isoformat(), session_token))
            return session
        
        return None
    
    @staticmethod
    def delete_session(session_token):
        """Удаление сессии"""
        db.execute('DELETE FROM sessions WHERE session_token = ?', (session_token,))
    
    @staticmethod
    def cleanup_expired_sessions():
        """Очистка просроченных сессий"""
        db.execute('DELETE FROM sessions WHERE expires_at <= ?', (datetime.now().isoformat(),))


class Project:
    """Модель проекта"""
    
    @staticmethod
    def create(name, user_id, description=None):
        """Создание проекта"""
        db.execute('''
            INSERT INTO projects (name, description, user_id, status)
            VALUES (?, ?, ?, 'created')
        ''', (name, description, user_id))
        
        return Project.get_by_id(db.get_connection().cursor().lastrowid)
    
    @staticmethod
    def get_by_id(project_id):
        """Получение проекта по ID"""
        return db.fetchone('SELECT * FROM projects WHERE id = ?', (project_id,))
    
    @staticmethod
    def get_by_user(user_id):
        """Получение всех проектов пользователя"""
        return db.fetchall('SELECT * FROM projects WHERE user_id = ? ORDER BY created_at DESC', (user_id,))
    
    @staticmethod
    def update_status(project_id, status, **kwargs):
        """Обновление статуса проекта"""
        fields = ['status = ?', 'updated_at = CURRENT_TIMESTAMP']
        params = [status]
        
        for key, value in kwargs.items():
            if key in ['port', 'container_id', 'git_repo_path', 'docker_compose_path']:
                fields.append(f'{key} = ?')
                params.append(value)
        
        params.append(project_id)
        db.execute(f'''
            UPDATE projects SET {', '.join(fields)} WHERE id = ?
        ''', params)
        
        return Project.get_by_id(project_id)
    
    @staticmethod
    def delete(project_id):
        """Удаление проекта"""
        db.execute('DELETE FROM projects WHERE id = ?', (project_id,))
    
    @staticmethod
    def count_by_user(user_id):
        """Подсчет количества проектов у пользователя"""
        result = db.fetchone('SELECT COUNT(*) as count FROM projects WHERE user_id = ?', (user_id,))
        return result['count'] if result else 0


class ChatMessage:
    """Модель сообщения чата"""
    
    @staticmethod
    def create(user_id, role, content, project_id=None, tokens_used=0):
        """Создание сообщения"""
        db.execute('''
            INSERT INTO chat_messages (user_id, project_id, role, content, tokens_used)
            VALUES (?, ?, ?, ?, ?)
        ''', (user_id, project_id, role, content, tokens_used))
        
        return db.get_connection().cursor().lastrowid
    
    @staticmethod
    def get_conversation(user_id, project_id=None, limit=20):
        """Получение истории переписки"""
        if project_id:
            return db.fetchall('''
                SELECT * FROM chat_messages 
                WHERE user_id = ? AND project_id = ?
                ORDER BY timestamp DESC LIMIT ?
            ''', (user_id, project_id, limit))
        else:
            return db.fetchall('''
                SELECT * FROM chat_messages 
                WHERE user_id = ?
                ORDER BY timestamp DESC LIMIT ?
            ''', (user_id, limit))
    
    @staticmethod
    def delete_by_project(project_id):
        """Удаление сообщений проекта"""
        db.execute('DELETE FROM chat_messages WHERE project_id = ?', (project_id,))


class AuditLog:
    """Модель аудита"""
    
    @staticmethod
    def log(action, user_id=None, resource_type=None, resource_id=None, 
            details=None, ip_address=None):
        """Запись лога"""
        db.execute('''
            INSERT INTO audit_logs (user_id, action, resource_type, resource_id, details, ip_address)
            VALUES (?, ?, ?, ?, ?, ?)
        ''', (user_id, action, resource_type, resource_id, details, ip_address))


def login_required(f):
    """Декоратор для защиты маршрутов"""
    @wraps(f)
    def decorated_function(*args, **kwargs):
        from flask import request, jsonify, g
        
        # Проверка API ключа в заголовке
        api_key = request.headers.get('X-API-Key')
        if api_key:
            user = User.get_by_api_key(api_key)
            if user:
                g.current_user = user
                return f(*args, **kwargs)
        
        # Проверка сессии
        session_token = request.headers.get('Authorization', '').replace('Bearer ', '')
        if not session_token:
            session_token = request.cookies.get('session_token')
        
        if session_token:
            session = User.validate_session(session_token)
            if session:
                g.current_user = session
                return f(*args, **kwargs)
        
        return jsonify({'error': 'Требуется авторизация'}), 401
    
    return decorated_function


def admin_required(f):
    """Декоратор для защиты маршрутов администратора"""
    @wraps(f)
    @login_required
    def decorated_function(*args, **kwargs):
        from flask import g, jsonify
        
        if g.current_user['role'] != 'admin':
            return jsonify({'error': 'Требуется роль администратора'}), 403
        
        return f(*args, **kwargs)
    
    return decorated_function
