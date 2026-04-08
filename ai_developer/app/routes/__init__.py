from .auth import auth_bp, login_required
from .dashboard import dashboard_bp
from .api import api_bp

__all__ = ['auth_bp', 'dashboard_bp', 'api_bp', 'login_required']
