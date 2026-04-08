from flask import Blueprint, render_template, redirect, url_for, flash, session, request, jsonify
import logging
import uuid

from app.services.database import Database
from app.services.gemini_client import GeminiClient
from app.services.docker_service import DockerService
from app.services.task_manager import task_manager
from app.routes.auth import login_required

logger = logging.getLogger(__name__)

dashboard_bp = Blueprint('dashboard', __name__)


@dashboard_bp.route('/')
@login_required
def index():
    """Main dashboard page."""
    db = session.get('db')
    if not db:
        flash('Database not available.', 'error')
        return render_template('dashboard.html', projects=[])
    
    projects = db.get_all_projects(user_id=session['user_id'])
    return render_template('dashboard.html', projects=projects)


@dashboard_bp.route('/project/<int:project_id>')
@login_required
def project_detail(project_id):
    """View project details."""
    db = session.get('db')
    if not db:
        flash('Database not available.', 'error')
        return redirect(url_for('dashboard.index'))
    
    project = db.get_project(project_id)
    if not project:
        flash('Project not found.', 'error')
        return redirect(url_for('dashboard.index'))
    
    # Check ownership
    if project['user_id'] != session['user_id'] and session.get('role') != 'admin':
        flash('Access denied.', 'error')
        return redirect(url_for('dashboard.index'))
    
    chat_history = db.get_chat_history(project_id)
    system_prompt = db.get_system_prompt(project_id) or "You are a helpful coding assistant."
    
    return render_template('project_detail.html', 
                         project=project, 
                         chat_history=chat_history,
                         system_prompt=system_prompt)


@dashboard_bp.route('/project/create', methods=['GET', 'POST'])
@login_required
def create_project():
    """Create a new project."""
    db = session.get('db')
    docker_service = session.get('docker_service')
    gemini_client = session.get('gemini_client')
    
    if not db:
        flash('Database not available.', 'error')
        return redirect(url_for('dashboard.index'))
    
    if request.method == 'POST':
        project_name = request.form.get('name', '').strip()
        description = request.form.get('description', '').strip()
        idea = request.form.get('idea', '').strip()
        
        if not project_name and not idea:
            flash('Project name or idea is required.', 'error')
            return render_template('create_project.html')
        
        try:
            # If idea provided, analyze with AI
            if idea and gemini_client:
                analysis = gemini_client.analyze_project_idea(idea)
                project_name = analysis.get('name', project_name or 'my-project')
                description = analysis.get('description', description or idea)
                tech_stack = analysis.get('tech_stack', ['python', 'flask'])
                suggested_port = analysis.get('suggested_port', 8000)
            else:
                tech_stack = ['python', 'flask']
                suggested_port = None
            
            # Get unique port
            used_ports = db.get_used_ports()
            if suggested_port and suggested_port not in used_ports:
                port = suggested_port
            elif docker_service:
                port = docker_service.get_free_port(used_ports)
            else:
                port = 8000 + len(used_ports)
            
            # Create project in database
            project_id = db.create_project(
                name=project_name,
                description=description,
                user_id=session['user_id'],
                port=port
            )
            
            # Create project structure
            if docker_service:
                docker_service.create_project_structure(
                    project_id=project_id,
                    project_name=project_name,
                    description=description,
                    tech_stack=tech_stack if idea else None
                )
            
            flash(f'Project "{project_name}" created successfully!', 'success')
            return redirect(url_for('dashboard.project_detail', project_id=project_id))
            
        except Exception as e:
            logger.error(f"Error creating project: {e}")
            flash(f'Error creating project: {str(e)}', 'error')
    
    return render_template('create_project.html')


@dashboard_bp.route('/project/<int:project_id>/start', methods=['POST'])
@login_required
def start_project(project_id):
    """Start a project container."""
    db = session.get('db')
    docker_service = session.get('docker_service')
    
    if not db or not docker_service:
        return jsonify({'success': False, 'error': 'Services not available'}), 500
    
    project = db.get_project(project_id)
    if not project:
        return jsonify({'success': False, 'error': 'Project not found'}), 404
    
    # Check ownership
    if project['user_id'] != session['user_id'] and session.get('role') != 'admin':
        return jsonify({'success': False, 'error': 'Access denied'}), 403
    
    try:
        # Build image
        db.update_project_status(project_id, 'building')
        docker_service.build_project(project_id, project['name'])
        
        # Run container
        container_id = docker_service.run_container(
            project_id=project_id,
            project_name=project['name'],
            port=project['port']
        )
        
        db.update_project_status(project_id, 'running', container_id)
        
        return jsonify({
            'success': True,
            'container_id': container_id,
            'port': project['port']
        })
        
    except Exception as e:
        logger.error(f"Error starting project: {e}")
        db.update_project_status(project_id, 'error')
        return jsonify({'success': False, 'error': str(e)}), 500


@dashboard_bp.route('/project/<int:project_id>/stop', methods=['POST'])
@login_required
def stop_project(project_id):
    """Stop a project container."""
    db = session.get('db')
    docker_service = session.get('docker_service')
    
    if not db or not docker_service:
        return jsonify({'success': False, 'error': 'Services not available'}), 500
    
    project = db.get_project(project_id)
    if not project or not project.get('docker_container_id'):
        return jsonify({'success': False, 'error': 'Project not running'}), 400
    
    # Check ownership
    if project['user_id'] != session['user_id'] and session.get('role') != 'admin':
        return jsonify({'success': False, 'error': 'Access denied'}), 403
    
    try:
        docker_service.stop_container(project['docker_container_id'])
        db.update_project_status(project_id, 'stopped')
        
        return jsonify({'success': True})
        
    except Exception as e:
        logger.error(f"Error stopping project: {e}")
        return jsonify({'success': False, 'error': str(e)}), 500


@dashboard_bp.route('/project/<int:project_id>/delete', methods=['POST'])
@login_required
def delete_project(project_id):
    """Delete a project."""
    db = session.get('db')
    docker_service = session.get('docker_service')
    
    if not db:
        return jsonify({'success': False, 'error': 'Database not available'}), 500
    
    project = db.get_project(project_id)
    if not project:
        return jsonify({'success': False, 'error': 'Project not found'}), 404
    
    # Check ownership
    if project['user_id'] != session['user_id'] and session.get('role') != 'admin':
        return jsonify({'success': False, 'error': 'Access denied'}), 403
    
    try:
        if docker_service:
            docker_service.delete_project(
                project_id=project_id,
                project_name=project['name'],
                container_id=project.get('docker_container_id')
            )
        
        db.delete_project(project_id)
        
        return jsonify({'success': True})
        
    except Exception as e:
        logger.error(f"Error deleting project: {e}")
        return jsonify({'success': False, 'error': str(e)}), 500


@dashboard_bp.route('/project/<int:project_id>/chat', methods=['POST'])
@login_required
def chat_with_project(project_id):
    """Chat with AI about a project."""
    db = session.get('db')
    gemini_client = session.get('gemini_client')
    
    if not db or not gemini_client:
        return jsonify({'success': False, 'error': 'Services not available'}), 500
    
    data = request.get_json()
    message = data.get('message', '').strip()
    
    if not message:
        return jsonify({'success': False, 'error': 'Message is required'}), 400
    
    project = db.get_project(project_id)
    if not project:
        return jsonify({'success': False, 'error': 'Project not found'}), 404
    
    try:
        # Save user message
        db.add_chat_message(project_id, 'user', message)
        
        # Get chat history and system prompt
        chat_history = db.get_chat_history(project_id, limit=10)
        system_prompt = db.get_system_prompt(project_id) or "You are a helpful coding assistant for this project."
        
        # Generate response
        response = gemini_client.chat(message, chat_history, system_prompt)
        
        # Save AI response
        db.add_chat_message(project_id, 'assistant', response)
        
        return jsonify({
            'success': True,
            'response': response
        })
        
    except Exception as e:
        logger.error(f"Error in chat: {e}")
        return jsonify({'success': False, 'error': str(e)}), 500


@dashboard_bp.route('/project/<int:project_id>/logs')
@login_required
def project_logs(project_id):
    """Get project container logs."""
    db = session.get('db')
    docker_service = session.get('docker_service')
    
    if not db or not docker_service:
        return jsonify({'logs': 'Services not available'})
    
    project = db.get_project(project_id)
    if not project or not project.get('docker_container_id'):
        return jsonify({'logs': 'Project not running'})
    
    logs = docker_service.get_container_logs(project['docker_container_id'])
    return jsonify({'logs': logs})


@dashboard_bp.route('/settings', methods=['GET', 'POST'])
@login_required
def settings():
    """User settings page."""
    db = session.get('db')
    gemini_client = session.get('gemini_client')
    
    if not db:
        flash('Database not available.', 'error')
        return redirect(url_for('dashboard.index'))
    
    api_key = db.get_api_key('gemini')
    
    if request.method == 'POST':
        action = request.form.get('action')
        
        if action == 'save_api_key':
            new_key = request.form.get('api_key', '').strip()
            if new_key:
                db.save_api_key('gemini', new_key, session['user_id'])
                flash('API key saved successfully.', 'success')
            else:
                flash('API key cannot be empty.', 'error')
        
        elif action == 'update_global_prompt':
            global_prompt = request.form.get('global_prompt', '').strip()
            # Store in a special project or config table
            flash('Global prompt updated.', 'success')
    
    return render_template('settings.html', 
                         api_key_display=redact_api_key(api_key) if api_key else None,
                         has_api_key=bool(api_key))
