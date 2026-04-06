"""
Клиент для Gemini API
Оптимизирован для минимального потребления памяти
"""

import requests
import json
from config import Config


class GeminiClient:
    """Клиент для работы с Gemini API"""
    
    def __init__(self, api_key=None):
        self.api_key = api_key or Config.GEMINI_API_KEY
        self.api_url = Config.GEMINI_API_URL
        self.session = requests.Session()
        # Оптимизация: не держать много соединений
        self.session.mount('https://', requests.adapters.HTTPAdapter(
            pool_connections=1,
            pool_maxsize=2,
            max_retries=2
        ))
    
    def generate_code(self, prompt, context=None, language='python'):
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
            response = self.session.post(
                f"{self.api_url}?key={self.api_key}",
                json=payload,
                headers={"Content-Type": "application/json"},
                timeout=60
            )
            response.raise_for_status()
            
            result = response.json()
            
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
            
        except requests.exceptions.Timeout:
            return {
                'success': False,
                'error': 'Превышено время ожидания ответа от API'
            }
        except requests.exceptions.RequestException as e:
            return {
                'success': False,
                'error': f'Ошибка запроса к API: {str(e)}'
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
            response = self.session.post(
                f"{self.api_url}?key={self.api_key}",
                json=payload,
                headers={"Content-Type": "application/json"},
                timeout=60
            )
            response.raise_for_status()
            
            result = response.json()
            
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
