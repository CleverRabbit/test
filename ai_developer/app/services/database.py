import sqlite3
import logging
from contextlib import contextmanager
from typing import Optional, List, Dict, Any
import os

logger = logging.getLogger(__name__)


class Database:
    """Lightweight SQLite database manager with safe query execution."""
    
    def __init__(self, db_path: str):
        self.db_path = db_path
        self._ensure_directory()
        self._migrate()
    
    def _ensure_directory(self):
        """Ensure the database directory exists."""
        db_dir = os.path.dirname(self.db_path)
        if db_dir and not os.path.exists(db_dir):
            os.makedirs(db_dir, exist_ok=True)
    
    @contextmanager
    def get_connection(self):
        """Context manager for database connections."""
        conn = sqlite3.connect(self.db_path)
        conn.row_factory = sqlite3.Row
        try:
            yield conn
            conn.commit()
        except Exception as e:
            conn.rollback()
            logger.error(f"Database error: {e}")
            raise
        finally:
            conn.close()
    
    def _execute_safe(self, query: str, params: tuple = ()) -> List[Dict[str, Any]]:
        """
        Safely execute a query and return results.
        Handles both SELECT and non-SELECT queries properly.
        """
        with self.get_connection() as conn:
            cursor = conn.cursor()
            cursor.execute(query, params)
            
            # Only fetch results for SELECT statements
            if query.strip().upper().startswith('SELECT'):
                return [dict(row) for row in cursor.fetchall()]
            return []
    
    def _execute_many_safe(self, query: str, params_list: List[tuple]) -> int:
        """
        Safely execute multiple queries with executemany.
        Returns the number of affected rows.
        """
        with self.get_connection() as conn:
            cursor = conn.cursor()
            cursor.executemany(query, params_list)
            return cursor.rowcount
    
    def _migrate(self):
        """Run database migrations on startup."""
        migrations = [
            # Users table
            """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT UNIQUE NOT NULL,
                password_hash TEXT NOT NULL,
                role TEXT DEFAULT 'user',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                last_login TIMESTAMP
            )
            """,
            # Login attempts table for brute-force protection
            """
            CREATE TABLE IF NOT EXISTS login_attempts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                ip_address TEXT NOT NULL,
                username TEXT,
                attempt_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                success BOOLEAN DEFAULT 0
            )
            """,
            # Projects table
            """
            CREATE TABLE IF NOT EXISTS projects (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                description TEXT,
                status TEXT DEFAULT 'created',
                port INTEGER UNIQUE,
                docker_container_id TEXT,
                system_prompt TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                user_id INTEGER,
                FOREIGN KEY (user_id) REFERENCES users(id)
            )
            """,
            # Chat messages table
            """
            CREATE TABLE IF NOT EXISTS chat_messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                project_id INTEGER NOT NULL,
                role TEXT NOT NULL,
                content TEXT NOT NULL,
                timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
            )
            """,
            # API keys table (encrypted storage)
            """
            CREATE TABLE IF NOT EXISTS api_keys (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                key_name TEXT UNIQUE NOT NULL,
                key_value TEXT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                user_id INTEGER,
                FOREIGN KEY (user_id) REFERENCES users(id)
            )
            """,
            # Create indexes for performance
            """
            CREATE INDEX IF NOT EXISTS idx_projects_status ON projects(status)
            """,
            """
            CREATE INDEX IF NOT EXISTS idx_chat_messages_project ON chat_messages(project_id)
            """,
            """
            CREATE INDEX IF NOT EXISTS idx_login_attempts_ip ON login_attempts(ip_address)
            """,
        ]
        
        with self.get_connection() as conn:
            cursor = conn.cursor()
            for migration in migrations:
                cursor.execute(migration)
            logger.info("Database migrations completed successfully")
    
    # User operations
    def create_user(self, username: str, password_hash: str, role: str = 'user') -> int:
        """Create a new user."""
        with self.get_connection() as conn:
            cursor = conn.cursor()
            cursor.execute(
                "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)",
                (username, password_hash, role)
            )
            return cursor.lastrowid
    
    def get_user_by_username(self, username: str) -> Optional[Dict[str, Any]]:
        """Get user by username."""
        results = self._execute_safe(
            "SELECT * FROM users WHERE username = ?",
            (username,)
        )
        return results[0] if results else None
    
    def update_user_last_login(self, user_id: int):
        """Update user's last login timestamp."""
        with self.get_connection() as conn:
            cursor = conn.cursor()
            cursor.execute(
                "UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE id = ?",
                (user_id,)
            )
    
    # Login attempts operations
    def log_login_attempt(self, ip_address: str, username: str, success: bool):
        """Log a login attempt."""
        with self.get_connection() as conn:
            cursor = conn.cursor()
            cursor.execute(
                "INSERT INTO login_attempts (ip_address, username, success) VALUES (?, ?, ?)",
                (ip_address, username, 1 if success else 0)
            )
    
    def get_recent_failed_attempts(self, ip_address: str, minutes: int = 5) -> int:
        """Get count of recent failed login attempts from an IP."""
        results = self._execute_safe(
            """
            SELECT COUNT(*) as count FROM login_attempts 
            WHERE ip_address = ? AND success = 0 
            AND attempt_time >= datetime('now', '-' || ? || ' minutes')
            """,
            (ip_address, minutes)
        )
        return results[0]['count'] if results else 0
    
    # Project operations
    def create_project(self, name: str, description: str = None, 
                      user_id: int = None, port: int = None) -> int:
        """Create a new project."""
        with self.get_connection() as conn:
            cursor = conn.cursor()
            cursor.execute(
                """
                INSERT INTO projects (name, description, user_id, port, status) 
                VALUES (?, ?, ?, ?, 'created')
                """,
                (name, description, user_id, port)
            )
            return cursor.lastrowid
    
    def get_project(self, project_id: int) -> Optional[Dict[str, Any]]:
        """Get project by ID."""
        results = self._execute_safe(
            "SELECT * FROM projects WHERE id = ?",
            (project_id,)
        )
        return results[0] if results else None
    
    def get_all_projects(self, user_id: int = None) -> List[Dict[str, Any]]:
        """Get all projects, optionally filtered by user."""
        if user_id:
            return self._execute_safe(
                "SELECT * FROM projects WHERE user_id = ? ORDER BY created_at DESC",
                (user_id,)
            )
        return self._execute_safe("SELECT * FROM projects ORDER BY created_at DESC")
    
    def update_project_status(self, project_id: int, status: str, 
                             container_id: str = None):
        """Update project status."""
        with self.get_connection() as conn:
            cursor = conn.cursor()
            if container_id:
                cursor.execute(
                    """
                    UPDATE projects SET status = ?, docker_container_id = ?, 
                    updated_at = CURRENT_TIMESTAMP WHERE id = ?
                    """,
                    (status, container_id, project_id)
                )
            else:
                cursor.execute(
                    """
                    UPDATE projects SET status = ?, updated_at = CURRENT_TIMESTAMP 
                    WHERE id = ?
                    """,
                    (status, project_id)
                )
    
    def update_project_port(self, project_id: int, port: int):
        """Update project port."""
        with self.get_connection() as conn:
            cursor = conn.cursor()
            cursor.execute(
                "UPDATE projects SET port = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                (port, project_id)
            )
    
    def delete_project(self, project_id: int):
        """Delete a project."""
        with self.get_connection() as conn:
            cursor = conn.cursor()
            cursor.execute("DELETE FROM projects WHERE id = ?", (project_id,))
    
    def get_used_ports(self) -> List[int]:
        """Get all used ports."""
        results = self._execute_safe("SELECT port FROM projects WHERE port IS NOT NULL")
        return [row['port'] for row in results if row['port']]
    
    # Chat message operations
    def add_chat_message(self, project_id: int, role: str, content: str):
        """Add a chat message."""
        with self.get_connection() as conn:
            cursor = conn.cursor()
            cursor.execute(
                "INSERT INTO chat_messages (project_id, role, content) VALUES (?, ?, ?)",
                (project_id, role, content)
            )
    
    def get_chat_history(self, project_id: int, limit: int = 50) -> List[Dict[str, Any]]:
        """Get chat history for a project."""
        return self._execute_safe(
            """
            SELECT * FROM chat_messages 
            WHERE project_id = ? 
            ORDER BY timestamp ASC 
            LIMIT ?
            """,
            (project_id, limit)
        )
    
    def clear_chat_history(self, project_id: int):
        """Clear chat history for a project."""
        with self.get_connection() as conn:
            cursor = conn.cursor()
            cursor.execute(
                "DELETE FROM chat_messages WHERE project_id = ?",
                (project_id,)
            )
    
    # API Key operations
    def save_api_key(self, key_name: str, key_value: str, user_id: int = None):
        """Save or update an API key."""
        with self.get_connection() as conn:
            cursor = conn.cursor()
            cursor.execute(
                """
                INSERT OR REPLACE INTO api_keys (key_name, key_value, user_id) 
                VALUES (?, ?, ?)
                """,
                (key_name, key_value, user_id)
            )
    
    def get_api_key(self, key_name: str) -> Optional[str]:
        """Get an API key by name."""
        results = self._execute_safe(
            "SELECT key_value FROM api_keys WHERE key_name = ?",
            (key_name,)
        )
        return results[0]['key_value'] if results else None
    
    def update_system_prompt(self, project_id: int, prompt: str):
        """Update system prompt for a project."""
        with self.get_connection() as conn:
            cursor = conn.cursor()
            cursor.execute(
                "UPDATE projects SET system_prompt = ? WHERE id = ?",
                (prompt, project_id)
            )
    
    def get_system_prompt(self, project_id: int) -> Optional[str]:
        """Get system prompt for a project."""
        results = self._execute_safe(
            "SELECT system_prompt FROM projects WHERE id = ?",
            (project_id,)
        )
        return results[0]['system_prompt'] if results else None
