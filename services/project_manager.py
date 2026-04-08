"""
Project Manager - Управление проектами
Включает Git версионирование и управление файлами проектов
"""

import os
import subprocess
import shutil
import json
import zipfile
from datetime import datetime
from config import Config


class ProjectManager:
    """Управление проектами с Git версионированием"""
    
    def __init__(self):
        self.projects_path = Config.DOCKER_PROJECTS_PATH
        os.makedirs(self.projects_path, exist_ok=True)
    
    def _run_git_command(self, cmd, cwd):
        """Выполнение Git команды"""
        try:
            result = subprocess.run(
                cmd,
                shell=True,
                cwd=cwd,
                capture_output=True,
                text=True,
                timeout=30
            )
            return {
                'success': result.returncode == 0,
                'output': result.stdout.strip(),
                'error': result.stderr.strip() if result.stderr else None
            }
        except subprocess.TimeoutExpired:
            return {'success': False, 'error': 'Превышено время выполнения'}
        except Exception as e:
            return {'success': False, 'error': str(e)}
    
    def create_project(self, project_id, project_name, description=None):
        """
        Создание нового проекта
        
        Args:
            project_id: ID проекта
            project_name: Имя проекта
            description: Описание
        
        Returns:
            dict: Результат создания
        """
        project_dir = os.path.join(self.projects_path, f"project_{project_id}")
        
        # Проверка существования
        if os.path.exists(project_dir):
            return {
                'success': False,
                'error': 'Проект уже существует'
            }
        
        # Создание директории
        os.makedirs(project_dir, exist_ok=True)
        
        # Инициализация Git репозитория
        git_result = self._run_git_command('git init', project_dir)
        
        if not git_result['success']:
            return {
                'success': False,
                'error': f'Ошибка инициализации Git: {git_result["error"]}'
            }
        
        # Создание .gitignore
        gitignore_content = '''__pycache__/
*.py[cod]
*$py.class
*.so
.Python
.env
.venv
env/
venv/
ENV/
node_modules/
*.log
.DS_Store
Thumbs.db
'''
        with open(os.path.join(project_dir, '.gitignore'), 'w') as f:
            f.write(gitignore_content)
        
        # Создание README
        readme_content = f'''# {project_name}

{description or 'Проект создан AI Developer'}

## Структура проекта

- `index.html` - Главная страница
- `styles.css` - Стили
- `script.js` - JavaScript код

## Запуск

Проект запускается в Docker контейнере с NGINX.

## Разработка

```bash
# Локальный запуск
python3 -m http.server 8000

# Сборка Docker
docker build -t my-project .
```
'''
        with open(os.path.join(project_dir, 'README.md'), 'w') as f:
            f.write(readme_content)
        
        # Создание базовой структуры для веб-проекта
        self._create_web_template(project_dir)
        
        # Первый коммит
        self.commit_changes(project_id, "Initial commit - проект создан")
        
        return {
            'success': True,
            'path': project_dir,
            'message': 'Проект успешно создан'
        }
    
    def _create_web_template(self, project_dir):
        """Создание базового веб-шаблона"""
        # index.html
        html_content = '''<!DOCTYPE html>
<html lang="ru">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>AI Developer Project</title>
    <link rel="stylesheet" href="styles.css">
</head>
<body>
    <div class="container">
        <h1>Добро пожаловать!</h1>
        <p>Этот проект создан с помощью AI Developer.</p>
        <div id="app"></div>
    </div>
    <script src="script.js"></script>
</body>
</html>
'''
        with open(os.path.join(project_dir, 'index.html'), 'w') as f:
            f.write(html_content)
        
        # styles.css
        css_content = '''* {
    margin: 0;
    padding: 0;
    box-sizing: border-box;
}

body {
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
    line-height: 1.6;
    color: #333;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    min-height: 100vh;
}

.container {
    max-width: 800px;
    margin: 0 auto;
    padding: 40px 20px;
}

h1 {
    color: white;
    margin-bottom: 20px;
    text-align: center;
}

p {
    color: rgba(255, 255, 255, 0.9);
    text-align: center;
    margin-bottom: 30px;
}

#app {
    background: white;
    border-radius: 10px;
    padding: 30px;
    box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}
'''
        with open(os.path.join(project_dir, 'styles.css'), 'w') as f:
            f.write(css_content)
        
        # script.js
        js_content = '''// AI Developer Project Script
document.addEventListener('DOMContentLoaded', function() {
    console.log('Приложение загружено!');
    
    const app = document.getElementById('app');
    app.innerHTML = `
        <h2>Готово к разработке!</h2>
        <p>Используйте чат с AI для генерации кода и управления проектом.</p>
    `;
});
'''
        with open(os.path.join(project_dir, 'script.js'), 'w') as f:
            f.write(js_content)
    
    def commit_changes(self, project_id, message):
        """
        Коммит изменений в Git
        
        Args:
            project_id: ID проекта
            message: Сообщение коммита
        
        Returns:
            dict: Результат коммита
        """
        project_dir = os.path.join(self.projects_path, f"project_{project_id}")
        
        if not os.path.exists(project_dir):
            return {'success': False, 'error': 'Проект не найден'}
        
        # Добавление всех файлов
        add_result = self._run_git_command('git add -A', project_dir)
        
        if not add_result['success']:
            return {'success': False, 'error': add_result['error']}
        
        # Проверка есть ли изменения
        status_result = self._run_git_command('git status --porcelain', project_dir)
        
        if not status_result['output']:
            return {'success': True, 'message': 'Нет изменений для коммита'}
        
        # Коммит
        commit_result = self._run_git_command(
            f'git commit -m "{message}"',
            project_dir
        )
        
        return {
            'success': commit_result['success'],
            'message': message if commit_result['success'] else commit_result['error']
        }
    
    def get_git_log(self, project_id, limit=10):
        """Получение истории коммитов"""
        project_dir = os.path.join(self.projects_path, f"project_{project_id}")
        
        if not os.path.exists(project_dir):
            return {'success': False, 'error': 'Проект не найден'}
        
        log_result = self._run_git_command(
            f'git log --oneline -n {limit}',
            project_dir
        )
        
        commits = []
        if log_result['success'] and log_result['output']:
            for line in log_result['output'].split('\n'):
                parts = line.split(' ', 1)
                if len(parts) >= 2:
                    commits.append({
                        'hash': parts[0],
                        'message': parts[1]
                    })
        
        return {'success': True, 'commits': commits}
    
    def get_project_files(self, project_id):
        """Получение списка файлов проекта"""
        project_dir = os.path.join(self.projects_path, f"project_{project_id}")
        
        if not os.path.exists(project_dir):
            return {'success': False, 'error': 'Проект не найден'}
        
        files = []
        for root, dirs, filenames in os.walk(project_dir):
            # Пропускаем .git директорию
            dirs[:] = [d for d in dirs if d != '.git']
            
            for filename in filenames:
                filepath = os.path.join(root, filename)
                relpath = os.path.relpath(filepath, project_dir)
                
                # Получаем размер файла
                try:
                    size = os.path.getsize(filepath)
                except OSError:
                    size = 0
                
                files.append({
                    'name': relpath,
                    'size': size,
                    'path': filepath
                })
        
        return {'success': True, 'files': files}
    
    def get_file_content(self, project_id, filepath):
        """Получение содержимого файла"""
        project_dir = os.path.join(self.projects_path, f"project_{project_id}")
        full_path = os.path.join(project_dir, filepath)
        
        # Проверка безопасности (защита от path traversal)
        if not full_path.startswith(project_dir):
            return {'success': False, 'error': 'Недопустимый путь'}
        
        if not os.path.exists(full_path):
            return {'success': False, 'error': 'Файл не найден'}
        
        # Проверка размера (макс 1MB для просмотра)
        if os.path.getsize(full_path) > 1024 * 1024:
            return {'success': False, 'error': 'Файл слишком большой для просмотра'}
        
        try:
            with open(full_path, 'r', encoding='utf-8') as f:
                content = f.read()
            return {'success': True, 'content': content}
        except UnicodeDecodeError:
            return {'success': False, 'error': 'Бинарный файл'}
        except Exception as e:
            return {'success': False, 'error': str(e)}
    
    def save_file(self, project_id, filepath, content):
        """Сохранение файла в проекте"""
        project_dir = os.path.join(self.projects_path, f"project_{project_id}")
        full_path = os.path.join(project_dir, filepath)
        
        # Проверка безопасности
        if not full_path.startswith(project_dir):
            return {'success': False, 'error': 'Недопустимый путь'}
        
        # Создание директорий если нужно
        os.makedirs(os.path.dirname(full_path), exist_ok=True)
        
        try:
            with open(full_path, 'w', encoding='utf-8') as f:
                f.write(content)
            return {'success': True, 'message': 'Файл сохранен'}
        except Exception as e:
            return {'success': False, 'error': str(e)}
    
    def delete_project(self, project_id):
        """Удаление проекта"""
        project_dir = os.path.join(self.projects_path, f"project_{project_id}")
        
        if not os.path.exists(project_dir):
            return {'success': False, 'error': 'Проект не найден'}
        
        try:
            shutil.rmtree(project_dir)
            return {'success': True, 'message': 'Проект удален'}
        except Exception as e:
            return {'success': False, 'error': str(e)}
    
    def export_project(self, project_id, output_path):
        """Экспорт проекта в ZIP архив"""
        project_dir = os.path.join(self.projects_path, f"project_{project_id}")
        
        if not os.path.exists(project_dir):
            return {'success': False, 'error': 'Проект не найден'}
        
        try:
            with zipfile.ZipFile(output_path, 'w', zipfile.ZIP_DEFLATED) as zipf:
                for root, dirs, files in os.walk(project_dir):
                    dirs[:] = [d for d in dirs if d != '.git']
                    
                    for file in files:
                        filepath = os.path.join(root, file)
                        arcname = os.path.relpath(filepath, project_dir)
                        zipf.write(filepath, arcname)
            
            return {'success': True, 'path': output_path}
        except Exception as e:
            return {'success': False, 'error': str(e)}
    
    def import_project(self, project_id, zip_path):
        """Импорт проекта из ZIP архива"""
        project_dir = os.path.join(self.projects_path, f"project_{project_id}")
        
        if os.path.exists(project_dir):
            return {'success': False, 'error': 'Проект уже существует'}
        
        try:
            os.makedirs(project_dir)
            
            with zipfile.ZipFile(zip_path, 'r') as zipf:
                zipf.extractall(project_dir)
            
            # Переинициализация Git
            self._run_git_command('git init', project_dir)
            self.commit_changes(project_id, "Imported project")
            
            return {'success': True, 'path': project_dir}
        except Exception as e:
            if os.path.exists(project_dir):
                shutil.rmtree(project_dir)
            return {'success': False, 'error': str(e)}
    
    def get_project_stats(self, project_id):
        """Получение статистики проекта"""
        project_dir = os.path.join(self.projects_path, f"project_{project_id}")
        
        if not os.path.exists(project_dir):
            return {'success': False, 'error': 'Проект не найден'}
        
        stats = {
            'total_files': 0,
            'total_size': 0,
            'languages': {},
            'last_commit': None
        }
        
        # Подсчет файлов
        for root, dirs, files in os.walk(project_dir):
            dirs[:] = [d for d in dirs if d != '.git']
            
            for file in files:
                stats['total_files'] += 1
                filepath = os.path.join(root, file)
                try:
                    stats['total_size'] += os.path.getsize(filepath)
                    
                    # Определение языка по расширению
                    ext = os.path.splitext(file)[1].lower()
                    lang_map = {
                        '.py': 'Python',
                        '.js': 'JavaScript',
                        '.ts': 'TypeScript',
                        '.html': 'HTML',
                        '.css': 'CSS',
                        '.json': 'JSON',
                        '.md': 'Markdown',
                        '.yaml': 'YAML',
                        '.yml': 'YAML',
                        '.sh': 'Shell',
                        '.dockerfile': 'Dockerfile',
                        '.txt': 'Text'
                    }
                    lang = lang_map.get(ext, 'Other')
                    stats['languages'][lang] = stats['languages'].get(lang, 0) + 1
                except OSError:
                    pass
        
        # Последний коммит
        log_result = self._run_git_command('git log -1 --format="%ci"', project_dir)
        if log_result['success']:
            stats['last_commit'] = log_result['output']
        
        return {'success': True, 'stats': stats}


# Глобальный экземпляр
project_manager = None


def get_project_manager():
    """Получение экземпляра менеджера (singleton)"""
    global project_manager
    if project_manager is None:
        project_manager = ProjectManager()
    return project_manager
