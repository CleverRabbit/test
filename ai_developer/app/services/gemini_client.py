import logging
import time
import re
from typing import Optional, List, Dict, Any
from functools import wraps

try:
    import google.generativeai as genai
except ImportError:
    genai = None

logger = logging.getLogger(__name__)


class RedactedFilter(logging.Filter):
    """Filter to redact API keys from logs."""
    
    def __init__(self, patterns: List[str] = None):
        super().__init__()
        self.patterns = patterns or [
            r'AIza[0-9A-Za-z\-_]{35}',  # Google API key pattern
            r'key=[^&\s]+',  # Generic key parameter
            r'api_key=[^&\s]+',  # api_key parameter
            r'token=[^&\s]+',  # token parameter
        ]
    
    def filter(self, record):
        if isinstance(record.msg, str):
            for pattern in self.patterns:
                record.msg = re.sub(pattern, '[REDACTED]', record.msg)
        if record.args:
            record.args = tuple(
                re.sub(pattern, '[REDACTED]', str(arg)) if isinstance(arg, str) else arg
                for arg in record.args
                for pattern in self.patterns
            )
        return True


def redact_api_key(key: str) -> str:
    """Redact API key for display."""
    if not key or len(key) < 10:
        return '[REDACTED]'
    return f"{key[:4]}...{key[-4:]}"


class GeminiClient:
    """Google Gemini AI client with retry logic and error handling."""
    
    def __init__(self, api_key: str):
        if not genai:
            raise ImportError("google-generativeai package is required")
        
        self.api_key = api_key
        self.model = None
        self._configure()
    
    def _configure(self):
        """Configure the Gemini client."""
        try:
            genai.configure(api_key=self.api_key)
            self.model = genai.GenerativeModel('gemini-pro')
            logger.info(f"Gemini client configured with key: {redact_api_key(self.api_key)}")
        except Exception as e:
            logger.error(f"Failed to configure Gemini client: {e}")
            raise
    
    def _retry_with_exponential_backoff(self, func, max_retries: int = 5, 
                                       base_delay: float = 1.0, max_delay: float = 60.0):
        """
        Execute function with exponential backoff retry logic.
        Specifically handles 429 (Too Many Requests) errors.
        """
        retries = 0
        delay = base_delay
        
        while retries <= max_retries:
            try:
                return func()
            except Exception as e:
                error_str = str(e).lower()
                
                # Check for rate limit errors
                if '429' in error_str or 'quota' in error_str or 'rate limit' in error_str:
                    retries += 1
                    if retries > max_retries:
                        logger.error(f"Max retries ({max_retries}) exceeded for rate limit")
                        raise
                    
                    actual_delay = min(delay, max_delay)
                    logger.warning(
                        f"Rate limit hit. Retrying in {actual_delay:.1f}s "
                        f"(attempt {retries}/{max_retries})"
                    )
                    time.sleep(actual_delay)
                    delay *= 2  # Exponential backoff
                else:
                    # For other errors, don't retry
                    logger.error(f"Gemini API error: {e}")
                    raise
        
        return None
    
    def generate_code(self, prompt: str, system_prompt: str = None, 
                     context: List[Dict[str, str]] = None) -> str:
        """
        Generate code based on prompt with optional system prompt and context.
        """
        def _generate():
            # Build the full prompt
            full_prompt = ""
            
            if system_prompt:
                full_prompt += f"{system_prompt}\n\n"
            
            if context:
                full_prompt += "Conversation history:\n"
                for msg in context[-10:]:  # Last 10 messages for context
                    role = "User" if msg['role'] == 'user' else "Assistant"
                    full_prompt += f"{role}: {msg['content']}\n"
                full_prompt += "\n"
            
            full_prompt += f"Current request: {prompt}"
            
            try:
                response = self.model.generate_content(full_prompt)
                
                if hasattr(response, 'text'):
                    return response.text
                elif hasattr(response, 'candidates') and response.candidates:
                    return response.candidates[0].content.parts[0].text
                else:
                    raise ValueError("Unexpected response format from Gemini")
                    
            except Exception as e:
                logger.error(f"Error generating code: {e}")
                raise
        
        return self._retry_with_exponential_backoff(_generate)
    
    def analyze_project_idea(self, idea: str) -> Dict[str, Any]:
        """
        Analyze a project idea and return structured information.
        Returns: name, description, suggested_port, tech_stack, files_needed
        """
        prompt = f"""Analyze this project idea and return a JSON object with the following fields:
- name: A short, descriptive name for the project (lowercase, no spaces)
- description: A brief description of what the project does
- suggested_port: A port number between 3000 and 9000
- tech_stack: Array of technologies needed
- files_needed: Array of filenames that should be created

Project idea: {idea}

Return ONLY valid JSON, no markdown formatting."""

        def _analyze():
            try:
                response = self.model.generate_content(prompt)
                text = response.text if hasattr(response, 'text') else str(response)
                
                # Extract JSON from response
                import json
                # Try to find JSON in the response
                json_match = re.search(r'\{[^{}]*\}', text, re.DOTALL)
                if json_match:
                    return json.loads(json_match.group())
                
                # Fallback: try to parse entire response
                return json.loads(text)
            except Exception as e:
                logger.error(f"Error analyzing project idea: {e}")
                # Return default structure
                return {
                    'name': 'my-project',
                    'description': idea[:100],
                    'suggested_port': 8000,
                    'tech_stack': ['python', 'flask'],
                    'files_needed': ['app.py', 'requirements.txt']
                }
        
        return self._retry_with_exponential_backoff(_analyze)
    
    def validate_api_key(self) -> bool:
        """Validate that the API key works."""
        try:
            test_prompt = "Say 'OK' if you can read this."
            response = self.model.generate_content(test_prompt)
            return response is not None
        except Exception as e:
            logger.error(f"API key validation failed: {e}")
            return False
    
    def chat(self, message: str, conversation_history: List[Dict[str, str]] = None,
             system_prompt: str = None) -> str:
        """
        Chat with Gemini maintaining conversation context.
        """
        return self.generate_code(message, system_prompt, conversation_history)
