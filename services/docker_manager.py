"""
Docker Manager - Управление контейнерами
Оптимизировано для работы с ограниченными ресурсами
Использует subprocess вместо docker-py для экономии памяти
"""

import subprocess
import json
import os
import socket
import re
from datetime import datetime
from config import Config


class DockerManager:
    """Управление Docker контейнерами через CLI (легковесная альтернатива docker-py)"""
    
    def __init__(self):
        self.projects_path = Config.DOCKER_PROJECTS_PATH
        self.memory_limit = Config.DEFAULT_CONTAINER_MEMORY_LIMIT
        self.cpu_limit = Config.DEFAULT_CONTAINER_CPU_LIMIT
    
    def _run_command(self, cmd, capture_output=True):
        """Выполнение Docker команды"""
        try:
            result = subprocess.run(
                cmd,
                shell=True,
                capture_output=capture_output,
                text=True,
                timeout=30
            )
            return {
                'success': result.returncode == 0,
                'output': result.stdout.strip(),
                'error': result.stderr.strip() if result.stderr else None
            }
        except subprocess.TimeoutExpired:
            return {'success': False, 'error': 'Превышено время выполнения команды'}
        except Exception as e:
            return {'success': False, 'error': str(e)}
    
    def check_docker_available(self):
        """Проверка доступности Docker"""
        result = self._run_command('docker info')
        return result['success']
    
    def get_free_port(self, start_port=8080, max_attempts=100):
        """Поиск свободного порта"""
        for port in range(start_port, start_port + max_attempts):
            if self._is_port_free(port):
                return port
        return None
    
    def _is_port_free(self, port):
        """Проверка занятости порта"""
        try:
            with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
                s.settimeout(1)
                result = s.connect_ex(('0.0.0.0', port))
                return result != 0
        except Exception:
            return True
    
    def get_used_ports(self):
        """Получение списка используемых портов"""
        result = self._run_command(
            'docker ps --format "{{.Ports}}" | grep -oP "0.0.0.0:\\K\\d+" | sort -u'
        )
        if result['success']:
            ports = [int(p) for p in result['output'].split('\n') if p.isdigit()]
            return ports
        return []
    
    def create_container(self, project_id, project_name, project_path, port):
        """
        Создание и запуск контейнера для проекта
        
        Args:
            project_id: ID проекта в БД
            project_name: Имя проекта
            project_path: Путь к проекту
            port: Порт для маппинга
        
        Returns:
            dict: Результат операции
        """
        container_name = f"ai-dev-project-{project_id}"
        
        # Проверка наличия Dockerfile
        dockerfile_path = os.path.join(project_path, 'Dockerfile')
        if not os.path.exists(dockerfile_path):
            # Создаем базовый Dockerfile
            self._create_default_dockerfile(project_path, project_name)
        
        # Сборка образа
        build_result = self._run_command(
            f'docker build -t {container_name}:latest {project_path}'
        )
        
        if not build_result['success']:
            return {
                'success': False,
                'error': f'Ошибка сборки образа: {build_result["error"]}'
            }
        
        # Запуск контейнера с ограничениями ресурсов
        run_cmd = (
            f'docker run -d '
            f'--name {container_name} '
            f'-p {port}:80 '
            f'--memory {self.memory_limit} '
            f'--cpus {self.cpu_limit} '
            f'--restart unless-stopped '
            f'{container_name}:latest'
        )
        
        run_result = self._run_command(run_cmd)
        
        if not run_result['success']:
            return {
                'success': False,
                'error': f'Ошибка запуска контейнера: {run_result["error"]}'
            }
        
        container_id = run_result['output']
        
        return {
            'success': True,
            'container_id': container_id[:12],
            'port': port,
            'message': f'Контейнер запущен на порту {port}'
        }
    
    def _create_default_dockerfile(self, project_path, project_name):
        """Создание базового Dockerfile"""
        dockerfile_content = f'''FROM nginx:alpine

# Копирование файлов проекта
COPY . /usr/share/nginx/html

# Экспозиция порта
EXPOSE 80

# Healthcheck
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \\
    CMD wget --no-verbose --tries=1 --spider http://localhost/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
'''
        
        dockerfile_path = os.path.join(project_path, 'Dockerfile')
        with open(dockerfile_path, 'w') as f:
            f.write(dockerfile_content)
    
    def stop_container(self, project_id):
        """Остановка контейнера"""
        container_name = f"ai-dev-project-{project_id}"
        
        # Остановка
        stop_result = self._run_command(f'docker stop {container_name}')
        
        # Удаление
        rm_result = self._run_command(f'docker rm {container_name}')
        
        return {
            'success': stop_result['success'] and rm_result['success'],
            'message': 'Контейнер остановлен и удален'
        }
    
    def remove_image(self, project_id):
        """Удаление образа"""
        image_name = f"ai-dev-project-{project_id}:latest"
        result = self._run_command(f'docker rmi {image_name}', capture_output=False)
        return result
    
    def get_container_status(self, project_id):
        """Получение статуса контейнера"""
        container_name = f"ai-dev-project-{project_id}"
        
        # Проверка существования
        inspect_result = self._run_command(
            f'docker inspect {container_name} 2>/dev/null'
        )
        
        if not inspect_result['success']:
            return {'exists': False, 'status': 'not_found'}
        
        try:
            info = json.loads(inspect_result['output'])[0]
            state = info.get('State', {})
            
            return {
                'exists': True,
                'status': state.get('Status', 'unknown'),
                'running': state.get('Running', False),
                'started_at': state.get('StartedAt', ''),
                'health': state.get('Health', {}).get('Status', 'none')
            }
        except (json.JSONDecodeError, IndexError):
            return {'exists': False, 'status': 'error'}
    
    def list_containers(self):
        """Получение списка всех контейнеров проекта"""
        result = self._run_command(
            'docker ps -a --filter "name=ai-dev-project-" --format "{{.Names}}\t{{.Status}}\t{{.Ports}}"'
        )
        
        containers = []
        if result['success'] and result['output']:
            for line in result['output'].split('\n'):
                if line:
                    parts = line.split('\t')
                    if len(parts) >= 3:
                        name = parts[0]
                        project_id = name.replace('ai-dev-project-', '')
                        containers.append({
                            'project_id': project_id,
                            'name': name,
                            'status': parts[1],
                            'ports': parts[2]
                        })
        
        return containers
    
    def cleanup_unused_resources(self, hours=24):
        """
        Очистка неиспользуемых ресурсов Docker
        
        Args:
            hours: Очистить ресурсы старше N часов
        
        Returns:
            dict: Результаты очистки
        """
        results = {
            'stopped_containers': [],
            'unused_images': [],
            'freed_space': 0
        }
        
        # Остановка старых остановленных контейнеров
        old_containers_result = self._run_command(
            f'docker ps -a --filter "status=exited" --filter "status=dead" '
            f'--format "{{{{.Names}}}}\\t{{{{.CreatedAt}}}}" | head -20'
        )
        
        if old_containers_result['success'] and old_containers_result['output']:
            now = datetime.now()
            for line in old_containers_result['output'].split('\n'):
                if '\t' in line:
                    name, created = line.split('\t')
                    # Простая проверка возраста (можно улучшить)
                    if 'hours' in created or 'days' in created:
                        # Удаляем только наши контейнеры
                        if name.startswith('ai-dev-project-'):
                            rm_result = self._run_command(f'docker rm {name}')
                            if rm_result['success']:
                                results['stopped_containers'].append(name)
        
        # Удаление висячих образов
        dangling_result = self._run_command(
            'docker images --filter "dangling=true" --format "{{.ID}}"'
        )
        
        if dangling_result['success'] and dangling_result['output']:
            for image_id in dangling_result['output'].split('\n'):
                if image_id:
                    rm_result = self._run_command(f'docker rmi {image_id}')
                    if rm_result['success']:
                        results['unused_images'].append(image_id[:12])
        
        # Очистка кеша
        prune_result = self._run_command('docker system prune -f')
        
        # Получение информации о диске
        disk_result = self._run_command('docker system df')
        
        return results
    
    def get_system_info(self):
        """Получение информации о системе Docker"""
        info_result = self._run_command(
            'docker info --format "{{.Containers}}\t{{.ContainersRunning}}\t{{.ContainersStopped}}\t{{.Images}}"'
        )
        
        disk_result = self._run_command(
            'docker system df --format "{{.Type}}\t{{.TotalCount}}\t{{.Size}}"'
        )
        
        stats = {}
        
        if info_result['success']:
            parts = info_result['output'].split('\t')
            if len(parts) >= 4:
                stats['containers'] = {
                    'total': int(parts[0]),
                    'running': int(parts[1]),
                    'stopped': int(parts[2])
                }
                stats['images'] = int(parts[3])
        
        if disk_result['success'] and disk_result['output']:
            stats['disk'] = []
            for line in disk_result['output'].split('\n'):
                if '\t' in line:
                    parts = line.split('\t')
                    if len(parts) >= 3:
                        stats['disk'].append({
                            'type': parts[0],
                            'count': parts[1],
                            'size': parts[2]
                        })
        
        return stats
    
    def export_project(self, project_id, output_path):
        """Экспорт проекта в tar архив"""
        container_name = f"ai-dev-project-{project_id}"
        
        # Экспорт файловой системы контейнера
        result = self._run_command(
            f'docker export {container_name} > {output_path}'
        )
        
        return result
    
    def restart_container(self, project_id):
        """Перезапуск контейнера"""
        container_name = f"ai-dev-project-{project_id}"
        result = self._run_command(f'docker restart {container_name}')
        return result
    
    def get_container_logs(self, project_id, lines=100):
        """Получение логов контейнера"""
        container_name = f"ai-dev-project-{project_id}"
        result = self._run_command(f'docker logs --tail {lines} {container_name}')
        return result


# Глобальный экземпляр
docker_manager = None


def get_docker_manager():
    """Получение экземпляра менеджера (singleton)"""
    global docker_manager
    if docker_manager is None:
        docker_manager = DockerManager()
    return docker_manager
