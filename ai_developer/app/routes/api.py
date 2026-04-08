from flask import Blueprint, jsonify, session
import logging

from app.services.database import Database
from app.services.docker_service import DockerService
from app.routes.auth import login_required

logger = logging.getLogger(__name__)

api_bp = Blueprint('api', __name__, url_prefix='/api')


@api_bp.route('/health')
def health_check():
    """Health check endpoint."""
    db = session.get('db')
    docker_service = session.get('docker_service')
    
    status = {
        'status': 'healthy',
        'database': 'connected' if db else 'disconnected',
        'docker': 'available' if docker_service and docker_service.is_available() else 'unavailable'
    }
    
    return jsonify(status)


@api_bp.route('/system/status')
@login_required
def system_status():
    """Get system status information."""
    db = session.get('db')
    docker_service = session.get('docker_service')
    
    status = {
        'database': {
            'status': 'connected' if db else 'disconnected'
        },
        'docker': {
            'available': docker_service.is_available() if docker_service else False
        },
        'projects': {
            'total': len(db.get_all_projects()) if db else 0
        }
    }
    
    return jsonify(status)


@api_bp.route('/system/prune', methods=['POST'])
@login_required
def prune_resources():
    """Prune unused Docker resources."""
    docker_service = session.get('docker_service')
    
    if not docker_service:
        return jsonify({'error': 'Docker service not available'}), 500
    
    try:
        result = docker_service.prune_resources()
        return jsonify(result)
    except Exception as e:
        logger.error(f"Error pruning resources: {e}")
        return jsonify({'error': str(e)}), 500


@api_bp.route('/task/<task_id>')
@login_required
def get_task_status(task_id):
    """Get task status by ID."""
    from app.services.task_manager import task_manager
    
    task = task_manager.get_task(task_id)
    if not task:
        return jsonify({'error': 'Task not found'}), 404
    
    return jsonify(task)
