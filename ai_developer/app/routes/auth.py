from flask import Blueprint, render_template, redirect, url_for, request, flash, session, jsonify
from functools import wraps
import logging

from app.services.database import Database
from app.services.gemini_client import GeminiClient, redact_api_key
from app.services.docker_service import DockerService
from app.models.auth import hash_password, verify_password

logger = logging.getLogger(__name__)

auth_bp = Blueprint('auth', __name__, url_prefix='/auth')


def login_required(f):
    """Decorator to require login for routes."""
    @wraps(f)
    def decorated_function(*args, **kwargs):
        if 'user_id' not in session:
            flash('Please log in to access this page.', 'warning')
            return redirect(url_for('auth.login'))
        return f(*args, **kwargs)
    return decorated_function


@auth_bp.route('/login', methods=['GET', 'POST'])
def login():
    """Handle user login."""
    if request.method == 'POST':
        username = request.form.get('username', '').strip()
        password = request.form.get('password', '')
        
        if not username or not password:
            flash('Username and password are required.', 'error')
            return render_template('login.html')
        
        db = session.get('db')
        if not db:
            flash('Database not available.', 'error')
            return render_template('login.html')
        
        # Check for brute force
        client_ip = request.remote_addr
        failed_attempts = db.get_recent_failed_attempts(client_ip, minutes=5)
        if failed_attempts >= 5:
            flash('Too many failed attempts. Please try again later.', 'error')
            return render_template('login.html')
        
        # Get user
        user = db.get_user_by_username(username)
        
        if user and verify_password(password, user['password_hash']):
            # Successful login
            session['user_id'] = user['id']
            session['username'] = user['username']
            session['role'] = user['role']
            
            db.update_user_last_login(user['id'])
            db.log_login_attempt(client_ip, username, True)
            
            flash(f'Welcome back, {user["username"]}!', 'success')
            return redirect(url_for('dashboard.index'))
        else:
            # Failed login
            db.log_login_attempt(client_ip, username, False)
            flash('Invalid username or password.', 'error')
    
    return render_template('login.html')


@auth_bp.route('/register', methods=['GET', 'POST'])
def register():
    """Handle user registration."""
    if request.method == 'POST':
        username = request.form.get('username', '').strip()
        password = request.form.get('password', '')
        confirm_password = request.form.get('confirm_password', '')
        
        if not username or not password:
            flash('Username and password are required.', 'error')
            return render_template('register.html')
        
        if len(username) < 3:
            flash('Username must be at least 3 characters long.', 'error')
            return render_template('register.html')
        
        if len(password) < 6:
            flash('Password must be at least 6 characters long.', 'error')
            return render_template('register.html')
        
        if password != confirm_password:
            flash('Passwords do not match.', 'error')
            return render_template('register.html')
        
        db = session.get('db')
        if not db:
            flash('Database not available.', 'error')
            return render_template('register.html')
        
        # Check if user exists
        existing_user = db.get_user_by_username(username)
        if existing_user:
            flash('Username already taken.', 'error')
            return render_template('register.html')
        
        # Create user
        try:
            password_hash = hash_password(password)
            user_id = db.create_user(username, password_hash)
            
            flash('Registration successful! Please log in.', 'success')
            return redirect(url_for('auth.login'))
        except Exception as e:
            logger.error(f"Registration error: {e}")
            flash('An error occurred during registration.', 'error')
    
    return render_template('register.html')


@auth_bp.route('/logout')
def logout():
    """Handle user logout."""
    session.clear()
    flash('You have been logged out.', 'info')
    return redirect(url_for('auth.login'))


@auth_bp.route('/check-session')
def check_session():
    """Check if user is logged in (for AJAX)."""
    if 'user_id' in session:
        return jsonify({
            'logged_in': True,
            'username': session.get('username'),
            'role': session.get('role')
        })
    return jsonify({'logged_in': False})
