"""
AI Developer - Основное приложение Flask
Легковесное веб-приложение для генерации кода через Gemini API
с управлением проектами через Docker и Git
"""

import os
import logging
from flask import Flask, request, jsonify, render_template, send_from_directory, g, make_response
from config import get_config, Config
from models import db, User, Project, ChatMessage, AuditLog, login_required, admin_required
from gemini_client import get_gemini_client
from docker_manager import get_docker_manager
from project_manager import get_project_manager

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

# Инициализация менеджеров
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


@app.route('/project/<int:project_id>')
@login_required
def project_page(project_id):
    """Страница проекта"""
    return render_template('project.html', project_id=project_id)


@app.route('/settings')
@login_required
def settings_page():
    """Страница настроек"""
    return render_template('settings.html')


# ==================== API Авторизации ====================

@app.route('/api/register', methods=['POST'])
def api_register():
    """Регистрация нового пользователя"""
    data = request.get_json()
    
    username = data.get('username', '').strip()
    password = data.get('password', '')
    email = data.get('email', '').strip()
    
    if not username or not password:
        return jsonify({'error': 'Имя пользователя и пароль обязательны'}), 400
    
    if len(username) < 3:
        return jsonify({'error': 'Имя пользователя должно быть не менее 3 символов'}), 400
    
    if len(password) < 6:
        return jsonify({'error': 'Пароль должен быть не менее 6 символов'}), 400
    
    # Проверка существования
    existing = User.get_by_username(username)
    if existing:
        return jsonify({'error': 'Пользователь уже существует'}), 409
    
    # Определение первого пользователя как админа
    all_users = db.fetchall('SELECT id FROM users')
    role = 'admin' if not all_users else 'user'
    
    try:
        user = User.create(username, password, email, role)
        AuditLog.log('register', user_id=user['id'], details=f'Регистрация: {username}')
        
        return jsonify({
            'message': 'Пользователь зарегистрирован',
            'user': {
                'id': user['id'],
                'username': user['username'],
                'role': user['role']
            }
        }), 201
    except Exception as e:
        logger.error(f'Ошибка регистрации: {e}')
        return jsonify({'error': 'Ошибка при регистрации'}), 500


@app.route('/api/login', methods=['POST'])
def api_login():
    """Вход в систему"""
    data = request.get_json()
    username = data.get('username', '').strip()
    password = data.get('password', '')
    
    if not username or not password:
        return jsonify({'error': 'Введите имя пользователя и пароль'}), 400
    
    ip_address = request.remote_addr
    user = User.verify_login(username, password)
    
    if not user:
        AuditLog.log('login_failed', details=f'Неудачная попытка входа: {username}', ip_address=ip_address)
        return jsonify({'error': 'Неверное имя пользователя или пароль'}), 401
    
    # Создание сессии
    session_token = User.create_session(user['id'], ip_address)
    
    AuditLog.log('login', user_id=user['id'], ip_address=ip_address)
    
    response = make_response(jsonify({
        'message': 'Вход выполнен',
        'user': {
            'id': user['id'],
            'username': user['username'],
            'role': user['role'],
            'api_key': user['api_key']
        }
    }))
    response.set_cookie('session_token', session_token, httponly=True, max_age=7200)
    
    return response


@app.route('/api/logout', methods=['POST'])
@login_required
def api_logout():
    """Выход из системы"""
    session_token = request.cookies.get('session_token')
    if session_token:
        User.delete_session(session_token)
    
    AuditLog.log('logout', user_id=g.current_user['id'])
    
    response = make_response(jsonify({'message': 'Выход выполнен'}))
    response.delete_cookie('session_token')
    
    return response


@app.route('/api/me', methods=['GET'])
@login_required
def api_me():
    """Получение информации о текущем пользователе"""
    return jsonify({
        'user': {
            'id': g.current_user['id'],
            'username': g.current_user['username'],
            'email': g.current_user['email'],
            'role': g.current_user['role'],
            'api_key': g.current_user['api_key']
        }
    })


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
    
    # Сохранение сообщения пользователя
    ChatMessage.create(g.current_user['id'], 'user', message, project_id)
    
    # Получение контекста
    context = ChatMessage.get_conversation(g.current_user['id'], project_id, limit=10)
    context.reverse()
    
    # Генерация ответа
    response = gemini_client.generate_code(message, context)
    
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


@app.route('/api/system/info', methods=['GET'])
@login_required
def api_system_info():
    """Получение системной информации"""
    docker_available = docker_mgr.check_docker_available()
    
    info = {
        'docker_available': docker_available,
        'used_ports': docker_mgr.get_used_ports(),
        'config': {
            'max_projects': Config.MAX_PROJECTS_PER_USER,
            'max_containers': Config.MAX_CONCURRENT_CONTAINERS,
            'memory_limit': Config.DEFAULT_CONTAINER_MEMORY_LIMIT,
            'cpu_limit': Config.DEFAULT_CONTAINER_CPU_LIMIT
        }
    }
    
    if g.current_user['role'] == 'admin':
        info['system'] = docker_mgr.get_system_info()
    
    return jsonify(info)


# ==================== API Файлов ====================

@app.route('/api/projects/<int:project_id>/files/<path:filepath>', methods=['GET'])
@login_required
def api_get_file(project_id, filepath):
    """Получение содержимого файла"""
    project = Project.get_by_id(project_id)
    
    if not project or project['user_id'] != g.current_user['id']:
        return jsonify({'error': 'Проект не найден'}), 404
    
    result = project_mgr.get_file_content(project_id, filepath)
    
    if result['success']:
        return jsonify({'content': result['content']})
    else:
        return jsonify({'error': result['error']}), 404


@app.route('/api/projects/<int:project_id>/files/<path:filepath>', methods=['PUT'])
@login_required
def api_save_file(project_id, filepath):
    """Сохранение файла"""
    project = Project.get_by_id(project_id)
    
    if not project or project['user_id'] != g.current_user['id']:
        return jsonify({'error': 'Проект не найден'}), 404
    
    data = request.get_json()
    content = data.get('content', '')
    
    result = project_mgr.save_file(project_id, filepath, content)
    
    if result['success']:
        project_mgr.commit_changes(project_id, f"Update {filepath}")
        return jsonify({'message': 'Файл сохранен'})
    else:
        return jsonify({'error': result['error']}), 500


# ==================== Запуск приложения ====================

if __name__ == '__main__':
    # Валидация конфигурации
    try:
        Config.validate()
    except ValueError as e:
        logger.error(f'Ошибка конфигурации: {e}')
        print(f"ERROR: {e}")
        print("Please set GEMINI_API_KEY in .env file")
        exit(1)
    
    # Создание директорий
    os.makedirs(Config.DOCKER_PROJECTS_PATH, exist_ok=True)
    os.makedirs('templates', exist_ok=True)
    os.makedirs('static', exist_ok=True)
    
    logger.info(f"Запуск AI Developer на {Config.HOST}:{Config.PORT}")
    print(f"\n{'='*50}")
    print(f"AI Developer запущен!")
    print(f"URL: http://{Config.HOST}:{Config.PORT}")
    print(f"{'='*50}\n")
    
    app.run(
        host=Config.HOST,
        port=Config.PORT,
        debug=False,
        threaded=True
    )
