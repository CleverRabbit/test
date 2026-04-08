import logging
import threading
import time
from typing import Dict, Any, Callable

logger = logging.getLogger(__name__)


class AsyncTaskManager:
    """Manager for asynchronous tasks with progress tracking."""
    
    def __init__(self):
        self.tasks: Dict[str, Dict[str, Any]] = {}
        self._lock = threading.Lock()
    
    def create_task(self, task_id: str, description: str) -> str:
        """Create a new task."""
        with self._lock:
            self.tasks[task_id] = {
                'id': task_id,
                'description': description,
                'status': 'pending',
                'progress': 0,
                'stages': [],
                'current_stage': None,
                'result': None,
                'error': None,
                'created_at': time.time(),
                'completed_at': None
            }
        return task_id
    
    def update_task_status(self, task_id: str, status: str, 
                          progress: int = None, stage: str = None):
        """Update task status and progress."""
        with self._lock:
            if task_id not in self.tasks:
                raise ValueError(f"Task {task_id} not found")
            
            task = self.tasks[task_id]
            task['status'] = status
            
            if progress is not None:
                task['progress'] = progress
            
            if stage:
                task['current_stage'] = stage
                if stage not in task['stages']:
                    task['stages'].append(stage)
            
            if status == 'completed':
                task['completed_at'] = time.time()
            elif status == 'failed':
                task['completed_at'] = time.time()
    
    def set_task_result(self, task_id: str, result: Any):
        """Set task result."""
        with self._lock:
            if task_id not in self.tasks:
                raise ValueError(f"Task {task_id} not found")
            self.tasks[task_id]['result'] = result
    
    def set_task_error(self, task_id: str, error: str):
        """Set task error."""
        with self._lock:
            if task_id not in self.tasks:
                raise ValueError(f"Task {task_id} not found")
            self.tasks[task_id]['error'] = error
            self.tasks[task_id]['status'] = 'failed'
            self.tasks[task_id]['completed_at'] = time.time()
    
    def get_task(self, task_id: str) -> Dict[str, Any]:
        """Get task information."""
        with self._lock:
            if task_id not in self.tasks:
                return None
            return self.tasks[task_id].copy()
    
    def run_async(self, task_id: str, func: Callable, *args, **kwargs):
        """Run a function asynchronously with progress tracking."""
        def wrapper():
            try:
                self.update_task_status(task_id, 'running', progress=10)
                result = func(*args, **kwargs, task_manager=self, task_id=task_id)
                self.set_task_result(task_id, result)
                self.update_task_status(task_id, 'completed', progress=100)
                logger.info(f"Task {task_id} completed successfully")
            except Exception as e:
                logger.error(f"Task {task_id} failed: {e}")
                self.set_task_error(task_id, str(e))
        
        thread = threading.Thread(target=wrapper, daemon=True)
        thread.start()
        return thread
    
    def cleanup_old_tasks(self, max_age_hours: int = 24):
        """Clean up old completed/failed tasks."""
        current_time = time.time()
        max_age_seconds = max_age_hours * 3600
        
        with self._lock:
            to_remove = []
            for task_id, task in self.tasks.items():
                if task['completed_at'] and (current_time - task['completed_at']) > max_age_seconds:
                    to_remove.append(task_id)
            
            for task_id in to_remove:
                del self.tasks[task_id]
            
            if to_remove:
                logger.info(f"Cleaned up {len(to_remove)} old tasks")


# Global instance
task_manager = AsyncTaskManager()
