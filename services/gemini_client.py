"""
Клиент для Gemini API
Оптимизирован для минимального потребления памяти
"""

import requests
import json
import time
import logging
from config import Config

logger = logging.getLogger(__name__)


class GeminiClient:
    """Клиент для работы с Gemini API"""
    
    def __init__(self, api_key=None):
        self.api_key = api_key or Config.GEMINI_API_KEY
        self.model = Config.GEMINI_MODEL
        self.base_url = 'https://generativelanguage.googleapis.com/v1beta/models'
        self.session = requests.Session()
        # Оптимизация: не держать много соединений
        self.session.mount('https://', requests.adapters.HTTPAdapter(
            pool_connections=1,
            pool_maxsize=2,
            max_retries=0  # Отключаем встроенные retry, используем свои
        ))
    
    def _make_request(self, payload, max_retries=3):
        """
        Выполнение запроса к API с обработкой ошибок 429
        
        Args:
            payload: Данные запроса
            max_retries: Максимальное количество попыток
        
        Returns:
            dict: Ответ API или ошибка
        """
        retries = 0
        base_delay = 5  # Базовая задержка в секундах
        
        while retries <= max_retries:
            try:
                response = self.session.post(
                    f"{self.base_url}/{self.model}:generateContent?key={self.api_key}",
                    json=payload,
                    headers={"Content-Type": "application/json"},
                    timeout=60
                )
                
                # Обработка ошибки 429 (Too Many Requests)
                if response.status_code == 429:
                    retries += 1
                    if retries > max_retries:
                        logger.warning(f"Превышено количество попыток после 429 ошибки")
                        return {
                            'success': False,
                            'error': 'Слишком много запросов к API. Повторите позже.',
                            'error_type': 'rate_limit'
                        }
                    
                    # Экспоненциальная задержка: 5, 10, 20 секунд
                    delay = base_delay * (2 ** (retries - 1))
                    logger.warning(f"Получена ошибка 429. Попытка {retries}/{max_retries}. Ожидание {delay}с...")
                    time.sleep(delay)
                    continue
                
                response.raise_for_status()
                return response.json()
                
            except requests.exceptions.Timeout:
                return {
                    'success': False,
                    'error': 'Превышено время ожидания ответа от API'
                }
            except requests.exceptions.RequestException as e:
                # Не логируем ключ в ошибке
                error_msg = str(e)
                if 'key=' in error_msg:
                    error_msg = error_msg.split('key=')[0] + 'key=[REDACTED]'
                logger.error(f"Ошибка запроса к Gemini API: {error_msg}")
                return {
                    'success': False,
                    'error': f'Ошибка соединения с API: {str(e)}'
                }
        
        return {
            'success': False,
            'error': 'Не удалось выполнить запрос после нескольких попыток'
        }
    
    def generate_code(self, prompt, context=None, language='python', system_prompt=None):
        """
        Генерация кода через Gemini API
        
        Args:
            prompt: Запрос пользователя
            context: Контекст (история диалога)
            language: Язык программирования
        
        Returns:
            dict: Ответ API с генерацией
        """
        system_instruction = f"""Ты опытный разработчик, специализирующийся на создании чистого, 
эффективного и хорошо документированного кода. Твоя задача - помогать пользователям создавать 
проекты, писать код, объяснять концепции и решать проблемы.

Важные правила:
1. Пиши чистый, читаемый код с комментариями
2. Следуй лучшим практикам для {language}
3. Объясняй сложные моменты
4. Предлагай оптимизации когда это уместно
5. Учитывай ограничения ресурсов (2GB RAM, 1vCPU)
6. Всегда проверяй код на потенциальные ошибки

Отвечай в формате JSON:
{{
    "code": "сгенерированный код",
    "explanation": "объяснение решения",
    "files": [
        {{"name": "filename.ext", "content": "содержимое файла"}}
    ],
    "dependencies": ["список зависимостей"],
    "instructions": "инструкции по запуску"
}}
"""
        # Использование кастомного системного промпта если предоставлен
        if system_prompt:
            system_instruction = system_prompt
        
        messages = []
        
        # Добавляем системную инструкцию
        messages.append({
            "role": "user",
            "parts": [{"text": system_instruction}]
        })
        messages.append({
            "role": "model", 
            "parts": [{"text": "Понял. Буду помогать с разработкой кода, следуя лучшим практикам."}]
        })
        
        # Добавляем контекст если есть
        if context:
            for msg in context[-10:]:  # Ограничиваем контекст для экономии памяти
                messages.append({
                    "role": "user" if msg['role'] == 'user' else "model",
                    "parts": [{"text": msg['content'][:4000]}]  # Обрезаем длинные сообщения
                })
        
        # Добавляем текущий запрос
        messages.append({
            "role": "user",
            "parts": [{"text": f"{prompt}\n\nЯзык: {language}"}]
        })
        
        payload = {
            "contents": messages,
            "generationConfig": {
                "temperature": 0.7,
                "topK": 40,
                "topP": 0.95,
                "maxOutputTokens": 4096,
            },
            "safetySettings": [
                {
                    "category": "HARM_CATEGORY_HARASSMENT",
                    "threshold": "BLOCK_MEDIUM_AND_ABOVE"
                },
                {
                    "category": "HARM_CATEGORY_HATE_SPEECH",
                    "threshold": "BLOCK_MEDIUM_AND_ABOVE"
                },
                {
                    "category": "HARM_CATEGORY_SEXUALLY_EXPLICIT",
                    "threshold": "BLOCK_MEDIUM_AND_ABOVE"
                },
                {
                    "category": "HARM_CATEGORY_DANGEROUS_CONTENT",
                    "threshold": "BLOCK_MEDIUM_AND_ABOVE"
                }
            ]
        }
        
        try:
            result = self._make_request(payload)
            
            if not isinstance(result, dict):
                return {
                    'success': False,
                    'error': 'Некорректный формат ответа от API'
                }
            
            # Парсим ответ
            if 'candidates' in result and len(result['candidates']) > 0:
                content = result['candidates'][0]['content']['parts'][0]['text']
                
                # Пытаемся распарсить JSON из ответа
                try:
                    # Ищем JSON в ответе
                    json_start = content.find('{')
                    json_end = content.rfind('}') + 1
                    if json_start >= 0 and json_end > json_start:
                        json_str = content[json_start:json_end]
                        parsed = json.loads(json_str)
                        return {
                            'success': True,
                            'data': parsed,
                            'raw_response': content,
                            'tokens_used': result.get('usageMetadata', {}).get('totalTokenCount', 0)
                        }
                except json.JSONDecodeError:
                    pass
                
                # Если не удалось распарсить JSON, возвращаем как текст
                return {
                    'success': True,
                    'data': {
                        'code': content,
                        'explanation': 'Код сгенерирован в текстовом формате',
                        'files': [],
                        'dependencies': [],
                        'instructions': ''
                    },
                    'raw_response': content,
                    'tokens_used': result.get('usageMetadata', {}).get('totalTokenCount', 0)
                }
            
            return {
                'success': False,
                'error': 'Нет кандидатов в ответе',
                'raw_response': result
            }
            
        except Exception as e:
            return {
                'success': False,
                'error': f'Неожиданная ошибка: {str(e)}'
            }
    
    def chat(self, message, conversation_history=None):
        """
        Обычный чат (без генерации кода)
        
        Args:
            message: Сообщение пользователя
            conversation_history: История переписки
        
        Returns:
            dict: Ответ AI
        """
        messages = []
        
        # Добавляем историю
        if conversation_history:
            for msg in conversation_history[-10:]:
                messages.append({
                    "role": "user" if msg['role'] == 'user' else "model",
                    "parts": [{"text": msg['content'][:4000]}]
                })
        
        # Добавляем текущее сообщение
        messages.append({
            "role": "user",
            "parts": [{"text": message}]
        })
        
        payload = {
            "contents": messages,
            "generationConfig": {
                "temperature": 0.8,
                "topK": 40,
                "topP": 0.95,
                "maxOutputTokens": 2048,
            }
        }
        
        try:
            result = self._make_request(payload)
            
            if not isinstance(result, dict):
                return {
                    'success': False,
                    'error': 'Некорректный формат ответа от API'
                }
            
            if 'candidates' in result and len(result['candidates']) > 0:
                content = result['candidates'][0]['content']['parts'][0]['text']
                return {
                    'success': True,
                    'response': content,
                    'tokens_used': result.get('usageMetadata', {}).get('totalTokenCount', 0)
                }
            
            return {
                'success': False,
                'error': 'Нет кандидатов в ответе'
            }
            
        except Exception as e:
            return {
                'success': False,
                'error': str(e)
            }
    
    def is_available(self):
        """Проверка доступности API (наличие ключа)"""
        if not self.api_key:
            return False
        if self.api_key == 'your_gemini_api_key_here':
            return False
        if self.api_key == 'test_key_placeholder':
            return False
        if len(self.api_key) < 10:
            return False
        return True
    
    def close(self):
        """Закрытие сессии"""
        self.session.close()


# Глобальный экземпляр клиента
gemini_client = None


def get_gemini_client():
    """Получение экземпляра клиента (singleton)"""
    global gemini_client
    if gemini_client is None:
        gemini_client = GeminiClient()
    return gemini_client
