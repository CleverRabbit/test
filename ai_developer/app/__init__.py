"""
AI Developer - Main Application Entry Point
A lightweight web application for AI-powered code generation and Docker project management.
"""
import os
import logging
from logging.handlers import RotatingFileHandler
from flask import Flask, session, g
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

# Import services
from app.services.database import Database
from app.services.gemini_client import GeminiClient, RedactedFilter
from app.services.docker_service import DockerService
from app.routes.auth import auth_bp
from app.routes.dashboard import dashboard_bp
from app.routes.api import api_bp


def setup_logging():
    """Configure application logging with API key redaction."""
    log_dir = os.getenv('LOG_DIR', './logs')
    os.makedirs(log_dir, exist_ok=True)
    
    # Create formatter
    formatter = logging.Formatter(
        '%(asctime)s - %(name)s - %(levelname)s - %(message)s'
    )
    
    # File handler with rotation
    file_handler = RotatingFileHandler(
        os.path.join(log_dir, 'app.log'),
        maxBytes=10*1024*1024,  # 10MB
        backupCount=5
    )
    file_handler.setFormatter(formatter)
    file_handler.setLevel(logging.INFO)
    
    # Console handler
    console_handler = logging.StreamHandler()
    console_handler.setFormatter(formatter)
    console_handler.setLevel(logging.INFO)
    
    # Add redaction filter to all handlers
    redact_filter = RedactedFilter()
    file_handler.addFilter(redact_filter)
    console_handler.addFilter(redact_filter)
    
    # Configure root logger
    logging.basicConfig(level=logging.INFO)
    root_logger = logging.getLogger()
    root_logger.addHandler(file_handler)
    root_logger.addHandler(console_handler)
    
    return logging.getLogger(__name__)


def create_app():
    """Application factory function."""
    app = Flask(__name__)
    
    # Configuration
    app.config['SECRET_KEY'] = os.getenv('SECRET_KEY', os.urandom(32).hex())
    app.config['DATABASE_PATH'] = os.getenv('DATABASE_PATH', './data/ai_developer.db')
    app.config['PROJECTS_DIR'] = os.getenv('PROJECTS_DIR', './projects')
    
    # Ensure directories exist
    os.makedirs(os.path.dirname(app.config['DATABASE_PATH']), exist_ok=True)
    os.makedirs(app.config['PROJECTS_DIR'], exist_ok=True)
    
    # Setup logging
    logger = setup_logging()
    logger.info("Starting AI Developer application")
    
    # Initialize services before first request
    @app.before_request
    def before_request():
        """Initialize services for each request."""
        try:
            # Database
            if not hasattr(g, 'db') or g.db is None:
                g.db = Database(app.config['DATABASE_PATH'])
            
            # Store in session for route handlers
            session['db'] = g.db
            
            # Docker service
            if not hasattr(g, 'docker_service') or g.docker_service is None:
                g.docker_service = DockerService(app.config['PROJECTS_DIR'])
            
            session['docker_service'] = g.docker_service
            
            # Gemini client (if API key available)
            if not hasattr(session, 'gemini_client') or session.get('gemini_client') is None:
                api_key = g.db.get_api_key('gemini')
                if api_key:
                    try:
                        g.gemini_client = GeminiClient(api_key)
                        session['gemini_client'] = g.gemini_client
                    except Exception as e:
                        logger.warning(f"Failed to initialize Gemini client: {e}")
                        session['gemini_client'] = None
                else:
                    session['gemini_client'] = None
            
        except Exception as e:
            logger.error(f"Error initializing services: {e}")
    
    # Register blueprints
    app.register_blueprint(auth_bp)
    app.register_blueprint(dashboard_bp)
    app.register_blueprint(api_bp)
    
    # Health check endpoint (no auth required)
    @app.route('/health')
    def health():
        return {'status': 'healthy'}
    
    # Error handlers
    @app.errorhandler(404)
    def not_found(e):
        return {'error': 'Not found'}, 404
    
    @app.errorhandler(500)
    def internal_error(e):
        logger.error(f"Internal error: {e}")
        return {'error': 'Internal server error'}, 500
    
    @app.errorhandler(Exception)
    def handle_exception(e):
        logger.error(f"Unhandled exception: {e}")
        return {'error': str(e)}, 500
    
    logger.info("Application initialized successfully")
    return app


if __name__ == '__main__':
    app = create_app()
    
    host = os.getenv('HOST', '0.0.0.0')
    port = int(os.getenv('PORT', 5000))
    debug = os.getenv('DEBUG', 'false').lower() == 'true'
    
    logging.getLogger(__name__).info(f"Starting server on {host}:{port}")
    app.run(host=host, port=port, debug=debug)
