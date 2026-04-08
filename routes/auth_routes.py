"""
Маршруты авторизации AI Developer
Регистрация, вход, выход, управление сессиями
"""

from flask import Blueprint, request, jsonify, make_response, g
from services.models import User, AuditLog, LoginAttempt
from services.database import login_required

auth_bp = Blueprint('auth', __name__, url_prefix='/api')


@auth_bp.route('/register', methods=['POST'])
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
    all_users = User.get_all()
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
        from config import Config
        import logging
        logger = logging.getLogger(__name__)
        logger.error(f'Ошибка регистрации: {e}')
        return jsonify({'error': 'Ошибка при регистрации'}), 500


@auth_bp.route('/login', methods=['POST'])
def api_login():
    """Вход в систему"""
    data = request.get_json()
    username = data.get('username', '').strip()
    password = data.get('password', '')
    
    if not username or not password:
        return jsonify({'error': 'Введите имя пользователя и пароль'}), 400
    
    ip_address = request.remote_addr
    
    # Проверка блокировки
    from config import Config
    failed_attempts = LoginAttempt.get_failed_attempts(
        username, 
        ip_address, 
        Config.MAX_LOGIN_ATTEMPTS
    )
    
    if failed_attempts >= Config.MAX_LOGIN_ATTEMPTS:
        return jsonify({'error': 'Слишком много неудачных попыток. Попробуйте позже.'}), 429
    
    user = User.verify_login(username, password)
    
    if not user:
        LoginAttempt.log_attempt(username, ip_address, success=False)
        AuditLog.log('login_failed', details=f'Неудачная попытка входа: {username}', ip_address=ip_address)
        return jsonify({'error': 'Неверное имя пользователя или пароль'}), 401
    
    # Создание сессии
    session_token = User.create_session(user['id'], ip_address)
    
    LoginAttempt.log_attempt(username, ip_address, success=True)
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


@auth_bp.route('/logout', methods=['POST'])
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


@auth_bp.route('/me', methods=['GET'])
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
