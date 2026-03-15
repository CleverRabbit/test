"""
Render Automation Module
Деплой приложений на Render через API
"""
import os
import httpx
from typing import Tuple, Optional, Dict, Any


class RenderAutomation:
    """Автоматизация деплоя на Render"""
    
    def __init__(self):
        self.api_key = os.getenv("RENDER_API_KEY", "")
        self.base_url = "https://api.render.com/v1"
        self.headers = {
            "Authorization": f"Bearer {self.api_key}",
            "Content-Type": "application/json"
        }
    
    async def create_web_service(self, 
                                  name: str, 
                                  repo_url: str, 
                                  branch: str = "main",
                                  env_vars: Dict[str, str] = None) -> Tuple[bool, str]:
        """
        Создание веб-сервиса на Render
        
        Args:
            name: Название сервиса
            repo_url: URL GitHub репозитория
            branch: Ветка для деплоя
            env_vars: Переменные окружения
        
        Returns:
            (success, message) - успех и URL сервиса или ошибка
        """
        if not self.api_key:
            return False, "Render API ключ не настроен"
        
        # Определяем тип проекта по наличию Dockerfile
        # Для нашего стека используем Docker
        service_data = {
            "name": name,
            "repoUrl": repo_url,
            "branch": branch,
            "autoDeploy": True,
            "type": "web",
            "env": "docker",  # Используем Docker
            "dockerContext": ".",
            "dockerfilePath": "./Dockerfile",
            "instanceSize": "starter",  # Бесплатный тариф
            "numInstances": 1,
            "healthCheckPath": "/health",
            "envVars": [
                {"key": k, "value": v} for k, v in (env_vars or {}).items()
            ]
        }
        
        try:
            async with httpx.AsyncClient(timeout=120.0) as client:
                response = await client.post(
                    f"{self.base_url}/services",
                    headers=self.headers,
                    json=service_data
                )
                
                if response.status_code == 201:
                    data = response.json()
                    service_url = data.get("service", {}).get("url", "")
                    return True, f"https://{service_url}"
                else:
                    error_msg = response.json().get("message", "Неизвестная ошибка")
                    return False, f"Ошибка создания сервиса: {error_msg}"
                    
        except httpx.HTTPError as e:
            return False, f"HTTP ошибка при создании сервиса: {str(e)}"
        except Exception as e:
            return False, f"Ошибка при создании сервиса: {str(e)}"
    
    async def get_service_status(self, service_id: str) -> Tuple[bool, str]:
        """Получение статуса сервиса"""
        if not self.api_key:
            return False, "Render API ключ не настроен"
        
        try:
            async with httpx.AsyncClient() as client:
                response = await client.get(
                    f"{self.base_url}/services/{service_id}",
                    headers=self.headers
                )
                
                if response.status_code == 200:
                    data = response.json()
                    status = data.get("service", {}).get("state", "unknown")
                    return True, status
                else:
                    return False, "Сервис не найден"
        except Exception as e:
            return False, str(e)
    
    async def delete_service(self, service_id: str) -> Tuple[bool, str]:
        """Удаление сервиса"""
        if not self.api_key:
            return False, "Render API ключ не настроен"
        
        try:
            async with httpx.AsyncClient() as client:
                response = await client.delete(
                    f"{self.base_url}/services/{service_id}",
                    headers=self.headers
                )
                
                if response.status_code == 204:
                    return True, "Сервис удалён"
                else:
                    return False, "Ошибка удаления сервиса"
        except Exception as e:
            return False, str(e)
    
    async def trigger_deploy(self, service_id: str) -> Tuple[bool, str]:
        """Триггер нового деплоя"""
        if not self.api_key:
            return False, "Render API ключ не настроен"
        
        try:
            async with httpx.AsyncClient() as client:
                response = await client.post(
                    f"{self.base_url}/services/{service_id}/deploys",
                    headers=self.headers
                )
                
                if response.status_code == 201:
                    return True, "Деплой запущен"
                else:
                    return False, "Ошибка запуска деплоя"
        except Exception as e:
            return False, str(e)


# Глобальный экземпляр
render_automation = RenderAutomation()