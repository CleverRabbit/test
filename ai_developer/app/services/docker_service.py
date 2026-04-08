import logging
import os
import random
import shutil
from typing import Optional, Dict, Any, List
import docker
from docker.errors import DockerException, NotFound, APIError

logger = logging.getLogger(__name__)


class DockerService:
    """Service for managing Docker containers and projects."""
    
    def __init__(self, projects_dir: str = './projects'):
        self.projects_dir = projects_dir
        self._ensure_projects_dir()
        try:
            self.client = docker.from_env()
            logger.info("Docker client initialized successfully")
        except DockerException as e:
            logger.error(f"Failed to initialize Docker client: {e}")
            self.client = None
    
    def _ensure_projects_dir(self):
        """Ensure the projects directory exists."""
        if not os.path.exists(self.projects_dir):
            os.makedirs(self.projects_dir, exist_ok=True)
    
    def is_available(self) -> bool:
        """Check if Docker is available."""
        if self.client is None:
            return False
        try:
            self.client.ping()
            return True
        except Exception:
            return False
    
    def get_free_port(self, used_ports: List[int] = None, 
                     min_port: int = 3000, max_port: int = 9000) -> int:
        """Get a free port in the specified range."""
        used_ports = used_ports or []
        available_ports = list(range(min_port, max_port + 1))
        
        # Remove used ports
        for port in used_ports:
            if port in available_ports:
                available_ports.remove(port)
        
        if not available_ports:
            raise ValueError("No available ports in the specified range")
        
        # Return a random available port
        return random.choice(available_ports)
    
    def generate_dockerfile(self, project_name: str, tech_stack: List[str] = None) -> str:
        """Generate a Dockerfile for the project."""
        tech_stack = tech_stack or ['python', 'flask']
        
        if 'python' in tech_stack or 'flask' in tech_stack or 'django' in tech_stack:
            return f"""FROM python:3.11-alpine

WORKDIR /app

# Install system dependencies
RUN apk add --no-cache gcc musl-dev linux-headers

# Copy requirements first for better caching
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY . .

# Expose port
EXPOSE 8000

# Run the application
CMD ["python", "app.py"]
"""
        elif 'node' in tech_stack or 'javascript' in tech_stack:
            return """FROM node:18-alpine

WORKDIR /app

COPY package*.json ./
RUN npm install

COPY . .

EXPOSE 3000

CMD ["npm", "start"]
"""
        else:
            # Default Python Flask template
            return f"""FROM python:3.11-alpine

WORKDIR /app

RUN apk add --no-cache gcc musl-dev linux-headers

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

EXPOSE 8000

CMD ["python", "app.py"]
"""
    
    def generate_docker_compose(self, project_name: str, port: int) -> str:
        """Generate a docker-compose.yml file for the project."""
        return f"""version: '3.8'

services:
  {project_name}:
    build: .
    ports:
      - "{port}:8000"
    restart: unless-stopped
    volumes:
      - ./data:/app/data
    environment:
      - PORT=8000
      - PROJECT_NAME={project_name}
"""
    
    def generate_requirements_txt(self, tech_stack: List[str] = None) -> str:
        """Generate a requirements.txt file."""
        tech_stack = tech_stack or ['flask']
        
        requirements = []
        
        if 'flask' in tech_stack:
            requirements.append('Flask==3.0.0')
        if 'django' in tech_stack:
            requirements.append('Django==5.0.0')
        if 'fastapi' in tech_stack:
            requirements.append('fastapi==0.109.0')
            requirements.append('uvicorn==0.27.0')
        if 'requests' in tech_stack:
            requirements.append('requests==2.31.0')
        if 'sqlalchemy' in tech_stack:
            requirements.append('SQLAlchemy==2.0.25')
        
        # Always include some basics
        if not requirements:
            requirements = ['Flask==3.0.0']
        
        return '\n'.join(requirements)
    
    def generate_basic_app(self, project_name: str, tech_stack: List[str] = None) -> str:
        """Generate a basic application file."""
        tech_stack = tech_stack or ['flask']
        
        if 'flask' in tech_stack or 'python' in tech_stack:
            return f'''"""
{project_name} - Auto-generated Flask application
"""
from flask import Flask, jsonify

app = Flask(__name__)

@app.route('/')
def index():
    return jsonify({{
        "message": "Welcome to {project_name}!",
        "status": "running"
    }})

@app.route('/health')
def health():
    return jsonify({{"status": "healthy"}})

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8000)
'''
        return '# Basic application placeholder'
    
    def create_project_structure(self, project_id: int, project_name: str,
                                description: str = None, 
                                tech_stack: List[str] = None) -> str:
        """Create the project directory structure and files."""
        project_path = os.path.join(self.projects_dir, f"{project_id}_{project_name}")
        
        if os.path.exists(project_path):
            logger.warning(f"Project path already exists: {project_path}")
            return project_path
        
        os.makedirs(project_path, exist_ok=True)
        
        # Generate files
        dockerfile_content = self.generate_dockerfile(project_name, tech_stack)
        docker_compose_content = self.generate_docker_compose(project_name, 8000)
        requirements_content = self.generate_requirements_txt(tech_stack)
        app_content = self.generate_basic_app(project_name, tech_stack)
        
        # Write files
        with open(os.path.join(project_path, 'Dockerfile'), 'w') as f:
            f.write(dockerfile_content)
        
        with open(os.path.join(project_path, 'docker-compose.yml'), 'w') as f:
            f.write(docker_compose_content)
        
        with open(os.path.join(project_path, 'requirements.txt'), 'w') as f:
            f.write(requirements_content)
        
        with open(os.path.join(project_path, 'app.py'), 'w') as f:
            f.write(app_content)
        
        # Create data directory
        os.makedirs(os.path.join(project_path, 'data'), exist_ok=True)
        
        # Create README
        readme_content = f"""# {project_name}

Auto-generated project by AI Developer.

## Description
{description or 'No description provided.'}

## Tech Stack
{', '.join(tech_stack) if tech_stack else 'Python, Flask'}

## Running
```bash
docker-compose up --build
```
"""
        with open(os.path.join(project_path, 'README.md'), 'w') as f:
            f.write(readme_content)
        
        logger.info(f"Created project structure at: {project_path}")
        return project_path
    
    def build_project(self, project_id: int, project_name: str) -> bool:
        """Build the Docker image for a project."""
        if not self.client:
            raise DockerException("Docker client not available")
        
        project_path = os.path.join(self.projects_dir, f"{project_id}_{project_name}")
        
        if not os.path.exists(project_path):
            raise FileNotFoundError(f"Project path not found: {project_path}")
        
        image_name = f"{project_name}:latest"
        
        try:
            logger.info(f"Building Docker image: {image_name}")
            self.client.images.build(
                path=project_path,
                tag=image_name,
                rm=True,
                quiet=False
            )
            logger.info(f"Successfully built image: {image_name}")
            return True
        except APIError as e:
            logger.error(f"Docker build error: {e}")
            raise
    
    def run_container(self, project_id: int, project_name: str, 
                     port: int, detach: bool = True) -> str:
        """Run a Docker container for a project."""
        if not self.client:
            raise DockerException("Docker client not available")
        
        project_path = os.path.join(self.projects_dir, f"{project_id}_{project_name}")
        image_name = f"{project_name}:latest"
        container_name = f"{project_name}_container"
        
        # Check if container already exists
        try:
            existing = self.client.containers.get(container_name)
            existing.remove(force=True)
            logger.info(f"Removed existing container: {container_name}")
        except NotFound:
            pass
        
        try:
            logger.info(f"Starting container: {container_name} on port {port}")
            container = self.client.containers.run(
                image_name,
                name=container_name,
                ports={'8000/tcp': port},
                detach=detach,
                restart_policy={'Name': 'unless-stopped'},
                volumes={f'{project_path}/data': {'bind': '/app/data', 'mode': 'rw'}}
            )
            
            container_id = container.short_id
            logger.info(f"Container started: {container_id}")
            return container_id
            
        except APIError as e:
            logger.error(f"Failed to run container: {e}")
            raise
    
    def stop_container(self, container_id: str) -> bool:
        """Stop a running container."""
        if not self.client:
            return False
        
        try:
            container = self.client.containers.get(container_id)
            container.stop(timeout=10)
            logger.info(f"Stopped container: {container_id}")
            return True
        except NotFound:
            logger.warning(f"Container not found: {container_id}")
            return False
        except APIError as e:
            logger.error(f"Error stopping container: {e}")
            return False
    
    def remove_container(self, container_id: str) -> bool:
        """Remove a container."""
        if not self.client:
            return False
        
        try:
            container = self.client.containers.get(container_id)
            container.remove(force=True)
            logger.info(f"Removed container: {container_id}")
            return True
        except NotFound:
            logger.warning(f"Container not found: {container_id}")
            return False
        except APIError as e:
            logger.error(f"Error removing container: {e}")
            return False
    
    def get_container_status(self, container_id: str) -> str:
        """Get the status of a container."""
        if not self.client:
            return 'unknown'
        
        try:
            container = self.client.containers.get(container_id)
            return container.status
        except NotFound:
            return 'not_found'
        except APIError as e:
            logger.error(f"Error getting container status: {e}")
            return 'error'
    
    def delete_project(self, project_id: int, project_name: str, 
                      container_id: str = None) -> bool:
        """Delete a project completely."""
        success = True
        
        # Stop and remove container
        if container_id:
            self.stop_container(container_id)
            self.remove_container(container_id)
        
        # Remove image
        try:
            if self.client:
                image_name = f"{project_name}:latest"
                self.client.images.remove(image_name, force=True)
        except Exception as e:
            logger.warning(f"Could not remove image: {e}")
        
        # Remove project directory
        project_path = os.path.join(self.projects_dir, f"{project_id}_{project_name}")
        if os.path.exists(project_path):
            try:
                shutil.rmtree(project_path)
                logger.info(f"Deleted project directory: {project_path}")
            except Exception as e:
                logger.error(f"Failed to delete project directory: {e}")
                success = False
        
        return success
    
    def prune_resources(self) -> Dict[str, Any]:
        """Clean up unused Docker resources."""
        if not self.client:
            return {'error': 'Docker not available'}
        
        result = {
            'containers_pruned': 0,
            'images_pruned': 0,
            'volumes_pruned': 0,
            'build_cache_pruned': 0
        }
        
        try:
            # Prune stopped containers
            containers_result = self.client.containers.prune()
            result['containers_pruned'] = len(containers_result.get('ContainersDeleted', []))
            
            # Prune dangling images
            images_result = self.client.images.prune()
            result['images_pruned'] = len(images_result.get('ImagesDeleted', []))
            
            # Prune unused volumes
            volumes_result = self.client.volumes.prune()
            result['volumes_pruned'] = len(volumes_result.get('VolumesDeleted', []))
            
            # Prune build cache
            build_result = self.client.api.prune_builds()
            result['build_cache_pruned'] = build_result.get('SpaceReclaimed', 0)
            
            logger.info(f"Pruned Docker resources: {result}")
            
        except Exception as e:
            logger.error(f"Error pruning resources: {e}")
            result['error'] = str(e)
        
        return result
    
    def get_container_logs(self, container_id: str, tail: int = 100) -> str:
        """Get logs from a container."""
        if not self.client:
            return "Docker not available"
        
        try:
            container = self.client.containers.get(container_id)
            logs = container.logs(tail=tail).decode('utf-8')
            return logs
        except NotFound:
            return "Container not found"
        except Exception as e:
            return f"Error getting logs: {e}"
