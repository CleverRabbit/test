"""
AI Developer - Основное приложение Flask
Легковесное веб-приложение для генерации кода через Gemini API
с управлением проектами через Docker и Git

Оптимизировано для работы с ограниченными ресурсами (2GB RAM, 1vCPU)
"""

import os
import logging
from flask import Flask, request, jsonify, render_template, send_from_directory, g, make_response

from config import Config
from services.database import db, login_required, admin_required
from services.models import User, Project, ChatMessage, AuditLog
from services.gemini_client import get_gemini_client
from services.docker_manager import get_docker_manager
from services.project_manager import get_project_manager
from routes.auth_routes import auth_bp

# Настройка логирования
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler(Config.LOG_FILE),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)

# Создание приложения Flask
app = Flask(__name__, 
            template_folder='templates',
            static_folder='static')
app.config.from_object(Config)

# Регистрация blueprint маршрутов
app.register_blueprint(auth_bp)

# Инициализация менеджеров (ленивая загрузка)
gemini_client = None
docker_mgr = None
project_mgr = None


@app.before_request
def before_request():
    """Подготовка перед каждым запросом"""
    global gemini_client, docker_mgr, project_mgr
    
    # Ленивая инициализация
    if gemini_client is None:
        gemini_client = get_gemini_client()
    if docker_mgr is None:
        docker_mgr = get_docker_manager()
    if project_mgr is None:
        project_mgr = get_project_manager()
    
    # Очистка старых сессий периодически
    User.cleanup_expired_sessions()


@app.after_request
def after_request(response):
    """Добавление заголовков после запроса"""
    response.headers.add('Access-Control-Allow-Origin', '*')
    response.headers.add('Access-Control-Allow-Headers', 'Content-Type,Authorization,X-API-Key')
    response.headers.add('Access-Control-Allow-Methods', 'GET,PUT,POST,DELETE,OPTIONS')
    return response


# ==================== Веб страницы ====================

@app.route('/')
def index():
    """Главная страница"""
    return render_template('index.html')


@app.route('/login')
def login_page():
    """Страница входа"""
    return render_template('login.html')


@app.route('/dashboard')
@login_required
def dashboard():
    """Панель управления"""
    return render_template('dashboard.html')


@app.route('/settings')
@login_required
def settings_page():
    """Страница настроек"""
    return render_template('settings.html')


# ==================== API Проектов ====================

@app.route('/api/projects', methods=['GET'])
@login_required
def api_get_projects():
    """Получение списка проектов"""
    projects = Project.get_by_user(g.current_user['id'])
    
    result = []
    for p in projects:
        container_status = docker_mgr.get_container_status(p['id'])
        result.append({
            'id': p['id'],
            'name': p['name'],
            'description': p['description'],
            'status': p['status'],
            'port': p['port'],
            'container': container_status,
            'created_at': p['created_at'],
            'updated_at': p['updated_at']
        })
    
    return jsonify({'projects': result})


@app.route('/api/projects', methods=['POST'])
@login_required
def api_create_project():
    """Создание нового проекта"""
    data = request.get_json()
    name = data.get('name', '').strip()
    description = data.get('description', '').strip()
    
    if not name:
        return jsonify({'error': 'Название проекта обязательно'}), 400
    
    # Проверка лимита проектов
    count = Project.count_by_user(g.current_user['id'])
    if count >= Config.MAX_PROJECTS_PER_USER:
        return jsonify({'error': f'Достигнут лимит проектов ({Config.MAX_PROJECTS_PER_USER})'}), 400
    
    try:
        # Создание записи в БД
        project = Project.create(name, g.current_user['id'], description)
        
        if not project:
            return jsonify({'error': 'Ошибка создания записи проекта'}), 500
        
        # Создание файлов проекта
        pm_result = project_mgr.create_project(project['id'], name, description)
        
        if not pm_result['success']:
            Project.delete(project['id'])
            return jsonify({'error': pm_result['error']}), 500
        
        AuditLog.log('project_create', user_id=g.current_user['id'], 
                    resource_type='project', resource_id=project['id'])
        
        return jsonify({
            'message': 'Проект создан',
            'project': {
                'id': project['id'],
                'name': project['name'],
                'path': pm_result['path']
            }
        }), 201
    except Exception as e:
        logger.error(f'Ошибка создания проекта: {e}')
        return jsonify({'error': 'Ошибка при создании проекта'}), 500


@app.route('/api/projects/<int:project_id>', methods=['GET'])
@login_required
def api_get_project(project_id):
    """Получение информации о проекте"""
    project = Project.get_by_id(project_id)
    
    if not project or project['user_id'] != g.current_user['id']:
        return jsonify({'error': 'Проект не найден'}), 404
    
    files = project_mgr.get_project_files(project_id)
    stats = project_mgr.get_project_stats(project_id)
    git_log = project_mgr.get_git_log(project_id)
    container_status = docker_mgr.get_container_status(project_id)
    
    return jsonify({
        'project': dict(project),
        'files': files.get('files', []),
        'stats': stats.get('stats', {}),
        'git_log': git_log.get('commits', []),
        'container': container_status
    })


@app.route('/api/projects/<int:project_id>', methods=['DELETE'])
@login_required
def api_delete_project(project_id):
    """Удаление проекта"""
    project = Project.get_by_id(project_id)
    
    if not project or project['user_id'] != g.current_user['id']:
        return jsonify({'error': 'Проект не найден'}), 404
    
    try:
        # Остановка контейнера если запущен
        container_status = docker_mgr.get_container_status(project_id)
        if container_status.get('running'):
            docker_mgr.stop_container(project_id)
            docker_mgr.remove_image(project_id)
        
        # Удаление файлов
        project_mgr.delete_project(project_id)
        
        # Удаление сообщений чата
        ChatMessage.delete_by_project(project_id)
        
        # Удаление из БД
        Project.delete(project_id)
        
        AuditLog.log('project_delete', user_id=g.current_user['id'],
                    resource_type='project', resource_id=project_id)
        
        return jsonify({'message': 'Проект удален'})
    except Exception as e:
        logger.error(f'Ошибка удаления проекта: {e}')
        return jsonify({'error': 'Ошибка при удалении проекта'}), 500


@app.route('/api/projects/<int:project_id>/export', methods=['GET'])
@login_required
def api_export_project(project_id):
    """Экспорт проекта в ZIP"""
    project = Project.get_by_id(project_id)
    
    if not project or project['user_id'] != g.current_user['id']:
        return jsonify({'error': 'Проект не найден'}), 404
    
    output_path = f"/tmp/project_{project_id}.zip"
    result = project_mgr.export_project(project_id, output_path)
    
    if result['success']:
        return send_from_directory(
            '/tmp',
            f'project_{project_id}.zip',
            as_attachment=True,
            download_name=f"{project['name']}.zip"
        )
    else:
        return jsonify({'error': result['error']}), 500


@app.route('/api/projects/<int:project_id>/start', methods=['POST'])
@login_required
def api_start_project(project_id):
    """Запуск проекта в Docker"""
    project = Project.get_by_id(project_id)
    
    if not project or project['user_id'] != g.current_user['id']:
        return jsonify({'error': 'Проект не найден'}), 404
    
    # Проверка лимита контейнеров
    containers = docker_mgr.list_containers()
    running_count = sum(1 for c in containers if 'Up' in c.get('status', ''))
    
    if running_count >= Config.MAX_CONCURRENT_CONTAINERS:
        return jsonify({'error': f'Достигнут лимит контейнеров ({Config.MAX_CONCURRENT_CONTAINERS})'}), 400
    
    # Поиск свободного порта
    port = docker_mgr.get_free_port()
    if not port:
        return jsonify({'error': 'Нет свободных портов'}), 500
    
    project_path = os.path.join(Config.DOCKER_PROJECTS_PATH, f"project_{project_id}")
    
    result = docker_mgr.create_container(project_id, project['name'], project_path, port)
    
    if result['success']:
        Project.update_status(project_id, 'running', port=port, container_id=result['container_id'])
        AuditLog.log('project_start', user_id=g.current_user['id'],
                    resource_type='project', resource_id=project_id)
        
        return jsonify({
            'message': f'Проект запущен на порту {port}',
            'port': port,
            'container_id': result['container_id']
        })
    else:
        return jsonify({'error': result['error']}), 500


@app.route('/api/projects/<int:project_id>/stop', methods=['POST'])
@login_required
def api_stop_project(project_id):
    """Остановка проекта"""
    project = Project.get_by_id(project_id)
    
    if not project or project['user_id'] != g.current_user['id']:
        return jsonify({'error': 'Проект не найден'}), 404
    
    result = docker_mgr.stop_container(project_id)
    
    if result['success']:
        Project.update_status(project_id, 'stopped')
        AuditLog.log('project_stop', user_id=g.current_user['id'],
                    resource_type='project', resource_id=project_id)
        return jsonify({'message': 'Проект остановлен'})
    else:
        return jsonify({'error': result['error']}), 500


@app.route('/api/projects/<int:project_id>/prompt', methods=['PUT'])
@login_required
def api_update_project_prompt(project_id):
    """Обновление системного промпта проекта"""
    project = Project.get_by_id(project_id)
    
    if not project or project['user_id'] != g.current_user['id']:
        return jsonify({'error': 'Проект не найден'}), 404
    
    data = request.get_json()
    system_prompt = data.get('system_prompt', '')
    
    if not system_prompt:
        return jsonify({'error': 'Системный промпт не может быть пустым'}), 400
    
    try:
        Project.update_system_prompt(project_id, system_prompt)
        AuditLog.log('project_prompt_update', user_id=g.current_user['id'],
                    resource_type='project', resource_id=project_id)
        
        return jsonify({'message': 'Системный промпт обновлен'})
    except Exception as e:
        logger.error(f'Ошибка обновления промпта: {e}')
        return jsonify({'error': 'Ошибка при обновлении промпта'}), 500


# ==================== API Чата ====================

@app.route('/api/chat', methods=['POST'])
@login_required
def api_chat():
    """Отправка сообщения в чат"""
    data = request.get_json()
    message = data.get('message', '').strip()
    project_id = data.get('project_id')
    
    if not message:
        return jsonify({'error': 'Сообщение не может быть пустым'}), 400
    
    # Получение проекта для системного промпта
    system_prompt = None
    if project_id:
        project = Project.get_by_id(project_id)
        if project and project.get('system_prompt'):
            system_prompt = project['system_prompt']
    
    # Сохранение сообщения пользователя
    ChatMessage.create(g.current_user['id'], 'user', message, project_id)
    
    # Получение контекста
    context = ChatMessage.get_conversation(g.current_user['id'], project_id, limit=10)
    context.reverse()
    
    # Генерация ответа
    response = gemini_client.generate_code(message, context, system_prompt=system_prompt)
    
    if response['success']:
        # Сохранение ответа AI
        ai_message = response['data'].get('code', str(response['data']))
        ChatMessage.create(
            g.current_user['id'], 
            'assistant', 
            ai_message, 
            project_id,
            response.get('tokens_used', 0)
        )
        
        return jsonify({
            'response': response['data'],
            'tokens_used': response.get('tokens_used', 0)
        })
    else:
        return jsonify({'error': response.get('error', 'Ошибка генерации')}), 500


@app.route('/api/chat/history', methods=['GET'])
@login_required
def api_chat_history():
    """Получение истории чата"""
    project_id = request.args.get('project_id', type=int)
    limit = request.args.get('limit', 20, type=int)
    
    messages = ChatMessage.get_conversation(g.current_user['id'], project_id, limit)
    messages.reverse()
    
    return jsonify({'messages': [dict(m) for m in messages]})


# ==================== API Контейнеров ====================

@app.route('/api/containers', methods=['GET'])
@admin_required
def api_list_containers():
    """Получение списка всех контейнеров"""
    containers = docker_mgr.list_containers()
    system_info = docker_mgr.get_system_info()
    
    return jsonify({
        'containers': containers,
        'system': system_info
    })


@app.route('/api/containers/cleanup', methods=['POST'])
@admin_required
def api_cleanup_containers():
    """Очистка неиспользуемых ресурсов"""
    hours = request.get_json().get('hours', 24) if request.get_json() else 24
    result = docker_mgr.cleanup_unused_resources(hours)
    
    AuditLog.log('cleanup', user_id=g.current_user['id'], details=str(result))
    
    return jsonify({
        'message': 'Очистка выполнена',
        'result': result
    })


# ==================== API Системы ====================

@app.route('/api/system/info', methods=['GET'])
@login_required
def api_system_info():
    """Получение системной информации"""
    from services.gemini_client import get_gemini_client
    
    gemini_available = gemini_client.is_available() if gemini_client else False
    
    return jsonify({
        'version': '1.0.0',
        'python_version': os.popen('python3 --version').read().strip(),
        'docker': docker_mgr.get_system_info() if docker_mgr else {},
        'gemini_configured': gemini_available,
        'disk_usage': project_mgr.get_disk_usage() if project_mgr else {}
    })


@app.route('/api/system/selftest', methods=['GET'])
@admin_required
def api_selftest():
    """Самотестирование системы"""
    results = {}
    
    # Проверка БД
    try:
        projects_count = len(Project.get_by_user(g.current_user['id']))
        results['database'] = {
            'status': 'ok',
            'message': f'БД работает. Проектов: {projects_count}'
        }
    except Exception as e:
        results['database'] = {'status': 'error', 'message': str(e)}
    
    # Проверка Docker
    try:
        containers = docker_mgr.list_containers()
        running = sum(1 for c in containers if 'Up' in c.get('status', ''))
        results['docker'] = {
            'status': 'ok',
            'message': f'Docker: {running} контейнеров запущено'
        }
    except Exception as e:
        results['docker'] = {'status': 'error', 'message': str(e)}
    
    # Проверка файловой системы
    try:
        test_file = os.path.join(Config.DOCKER_PROJECTS_PATH, '.write_test')
        with open(test_file, 'w') as f:
            f.write('test')
        os.remove(test_file)
        results['file_system'] = {
            'status': 'ok',
            'message': f'Запись в {Config.DOCKER_PROJECTS_PATH} доступна'
        }
    except Exception as e:
        results['file_system'] = {'status': 'error', 'message': str(e)}
    
    # Проверка Gemini API
    try:
        if gemini_client and gemini_client.is_available():
            results['gemini_api'] = {
                'status': 'ok',
                'message': 'Gemini API настроен и доступен'
            }
        else:
            results['gemini_api'] = {
                'status': 'warning',
                'message': 'Gemini API не настроен или недоступен'
            }
    except Exception as e:
        results['gemini_api'] = {'status': 'error', 'message': str(e)}
    
    # Проверка Git
    try:
        import subprocess
        git_version = subprocess.check_output(['git', '--version']).decode().strip()
        results['git'] = {
            'status': 'ok',
            'message': git_version
        }
    except Exception as e:
        results['git'] = {'status': 'error', 'message': str(e)}
    
    return jsonify({'tests': results})


@app.route('/api/settings/gemini', methods=['GET'])
@admin_required
def api_get_gemini_settings():
    """Получение настроек Gemini API"""
    # Возвращаем только маску ключа
    key = Config.GEMINI_API_KEY
    if key and len(key) > 4:
        masked_key = f"****{key[-4:]}"
    else:
        masked_key = "Не настроен"
    
    return jsonify({
        'api_key_masked': masked_key,
        'is_configured': bool(key and len(key) > 10),
        'model': Config.GEMINI_MODEL
    })


@app.route('/api/settings/gemini', methods=['POST'])
@admin_required
def api_update_gemini_settings():
    """Обновление настроек Gemini API"""
    data = request.get_json()
    new_key = data.get('api_key', '').strip()
    
    if not new_key:
        return jsonify({'error': 'API ключ не может быть пустым'}), 400
    
    # Обновление в конфиге (для текущего процесса)
    Config.GEMINI_API_KEY = new_key
    
    # Пересоздание клиента с новым ключом
    global gemini_client
    gemini_client = get_gemini_client()
    
    AuditLog.log('settings_update', user_id=g.current_user['id'], 
                details='Обновлен Gemini API ключ')
    
    return jsonify({'message': 'Настройки обновлены'})


@app.route('/api/settings/gemini/test', methods=['POST'])
@admin_required
def api_test_gemini_connection():
    """Проверка соединения с Gemini API"""
    data = request.get_json()
    api_key = data.get('api_key', Config.GEMINI_API_KEY)
    
    if not api_key:
        return jsonify({'error': 'API ключ не указан'}), 400
    
    # Создание временного клиента для теста
    from services.gemini_client import GeminiClient
    test_client = GeminiClient(api_key)
    
    result = test_client.test_connection()
    
    if result['success']:
        return jsonify({'message': 'Соединение успешно', 'details': result})
    else:
        return jsonify({'error': result.get('error', 'Ошибка соединения')}), 500


@app.route('/api/settings/prompt', methods=['GET'])
@admin_required
def api_get_system_prompt():
    """Получение системного промпта по умолчанию"""
    return jsonify({
        'system_prompt': Config.DEFAULT_SYSTEM_PROMPT
    })


@app.route('/api/settings/prompt', methods=['PUT'])
@admin_required
def api_update_system_prompt():
    """Обновление системного промпта по умолчанию"""
    data = request.get_json()
    new_prompt = data.get('system_prompt', '')
    
    if not new_prompt:
        return jsonify({'error': 'Промпт не может быть пустым'}), 400
    
    # Обновление в конфиге
    Config.DEFAULT_SYSTEM_PROMPT = new_prompt
    
    AuditLog.log('settings_update', user_id=g.current_user['id'],
                details='Обновлен системный промпт по умолчанию')
    
    return jsonify({'message': 'Системный промпт обновлен'})


# ==================== Обработчики ошибок ====================

@app.errorhandler(404)
def not_found(error):
    return jsonify({'error': 'Не найдено'}), 404


@app.errorhandler(500)
def internal_error(error):
    logger.error(f'Внутренняя ошибка: {error}')
    return jsonify({'error': 'Внутренняя ошибка сервера'}), 500


# ==================== Запуск приложения ====================

if __name__ == '__main__':
    # Валидация конфигурации
    try:
        Config.validate()
    except ValueError as e:
        logger.warning(f'Предупреждение конфигурации: {e}')
    
    logger.info(f"AI Developer запускается на {Config.HOST}:{Config.PORT}")
    app.run(host=Config.HOST, port=Config.PORT, debug=Config.DEBUG)
