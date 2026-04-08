import telebot
import logging
import threading
import time
from typing import Dict, Any
import re

logger = logging.getLogger(__name__)


class TelegramBot:
    """Telegram bot for AI Developer integration."""
    
    def __init__(self, token: str, db, docker_service, gemini_client):
        self.token = token
        self.db = db
        self.docker_service = docker_service
        self.gemini_client = gemini_client
        self.bot = None
        self.user_states: Dict[int, Dict[str, Any]] = {}
        
        if token:
            self.bot = telebot.TeleBot(token)
            self._setup_handlers()
    
    def _setup_handlers(self):
        """Setup bot command handlers."""
        
        @self.bot.message_handler(commands=['start'])
        def handle_start(message):
            welcome_text = """
🤖 Welcome to AI Developer Bot!

I can help you create and manage projects powered by AI.

Commands:
/newproject - Create a new project with AI
/projects - View your projects
/help - Show this help message

To get started, send me your project idea!
            """
            self.bot.reply_to(message, welcome_text.strip())
        
        @self.bot.message_handler(commands=['help'])
        def handle_help(message):
            help_text = """
📚 AI Developer Bot Help

Creating a Project:
1. Send /newproject
2. Describe your idea in natural language
3. I'll analyze it and create the project
4. You'll receive a link when it's ready

Managing Projects:
• /projects - List all your projects
• Use inline buttons to start/stop/delete

Tips:
• Be specific in your project description
• Include tech stack preferences if any
• Check progress with /status
            """
            self.bot.reply_to(message, help_text.strip())
        
        @self.bot.message_handler(commands=['newproject'])
        def handle_newproject(message):
            self.user_states[message.from_user.id] = {'state': 'waiting_for_idea'}
            self.bot.reply_to(
                message,
                "🚀 Great! Please describe your project idea.\n\n"
                "Example: 'A REST API for a todo list with user authentication using Flask'\n\n"
                "Send your idea below:"
            )
        
        @self.bot.message_handler(commands=['projects'])
        def handle_projects(message):
            try:
                # Get user by telegram username (simplified - in prod would map properly)
                username = message.from_user.username or f"telegram_{message.from_user.id}"
                user = self.db.get_user_by_username(username)
                
                if not user:
                    # Try to find by ID mapping or create default
                    projects = self.db.get_all_projects()
                else:
                    projects = self.db.get_all_projects(user_id=user['id'])
                
                if not projects:
                    self.bot.reply_to(message, "You don't have any projects yet. Use /newproject to create one!")
                    return
                
                text = "📁 Your Projects:\n\n"
                for proj in projects[:10]:  # Limit to 10
                    status_emoji = {
                        'running': '🟢',
                        'stopped': '🔴',
                        'building': '🟡',
                        'created': '🔵',
                        'error': '⚠️'
                    }.get(proj['status'], '⚪')
                    
                    port_info = f":{proj['port']}" if proj['port'] else ""
                    text += f"{status_emoji} {proj['name']}{port_info}\n"
                    text += f"   Status: {proj['status']}\n"
                    text += f"   Created: {proj['created_at']}\n\n"
                
                self.bot.reply_to(message, text)
                
            except Exception as e:
                logger.error(f"Error in /projects: {e}")
                self.bot.reply_to(message, "Error fetching projects. Please try again.")
        
        @self.bot.message_handler(func=lambda m: True)
        def handle_message(message):
            user_id = message.from_user.id
            
            # Check if user is in a state
            if user_id in self.user_states:
                state = self.user_states[user_id]
                
                if state.get('state') == 'waiting_for_idea':
                    self._process_project_idea(message, state)
                elif state.get('state') == 'creating_project':
                    # Wait for async creation
                    self.bot.reply_to(message, "Project is being created... Please wait.")
    
    def _process_project_idea(self, message, state):
        """Process project idea from user."""
        idea = message.text
        
        # Send progress messages
        progress_msg = self.bot.reply_to(message, "🔍 Analyzing your idea...")
        
        try:
            # Analyze with AI
            if self.gemini_client:
                analysis = self.gemini_client.analyze_project_idea(idea)
                project_name = analysis.get('name', 'my-project')
                description = analysis.get('description', idea)
                tech_stack = analysis.get('tech_stack', ['python', 'flask'])
            else:
                project_name = 'my-project'
                description = idea
                tech_stack = ['python', 'flask']
            
            # Update progress
            self.bot.edit_message_text(
                f"🔍 Analyzed!\n📦 Generating code...",
                chat_id=message.chat.id,
                message_id=progress_msg.message_id
            )
            
            # Get or create user
            username = message.from_user.username or f"telegram_{message.from_user.id}"
            user = self.db.get_user_by_username(username)
            if not user:
                user_id_db = self.db.create_user(username, 'telegram_bot_password')
                user = self.db.get_user_by_username(username)
            
            # Get free port
            used_ports = self.db.get_used_ports()
            port = self.docker_service.get_free_port(used_ports) if self.docker_service else 8000
            
            # Create project
            project_id = self.db.create_project(
                name=project_name,
                description=description,
                user_id=user['id'],
                port=port
            )
            
            # Update progress
            self.bot.edit_message_text(
                f"📦 Creating project structure...",
                chat_id=message.chat.id,
                message_id=progress_msg.message_id
            )
            
            # Create files
            if self.docker_service:
                self.docker_service.create_project_structure(
                    project_id=project_id,
                    project_name=project_name,
                    description=description,
                    tech_stack=tech_stack
                )
            
            # Update progress
            self.bot.edit_message_text(
                f"🏗️ Building Docker image...",
                chat_id=message.chat.id,
                message_id=progress_msg.message_id
            )
            
            # Build and run (async in real impl)
            if self.docker_service:
                try:
                    self.docker_service.build_project(project_id, project_name)
                    container_id = self.docker_service.run_container(project_id, project_name, port)
                    self.db.update_project_status(project_id, 'running', container_id)
                except Exception as e:
                    logger.error(f"Error building/running: {e}")
                    self.db.update_project_status(project_id, 'created')
            
            # Success message
            success_text = f"""
✅ Project created successfully!

📁 Name: {project_name}
📝 Description: {description[:100]}
🔧 Tech Stack: {', '.join(tech_stack)}
🌐 Port: {port}

Access at: http://localhost:{port}

Use /projects to see all your projects.
            """
            self.bot.edit_message_text(success_text.strip(), chat_id=message.chat.id, message_id=progress_msg.message_id)
            
            # Clear state
            del self.user_states[user_id]
            
        except Exception as e:
            logger.error(f"Error processing idea: {e}")
            self.bot.edit_message_text(
                f"❌ Error creating project: {str(e)}",
                chat_id=message.chat.id,
                message_id=progress_msg.message_id
            )
            del self.user_states[user_id]
    
    def start_polling(self):
        """Start bot polling in a separate thread."""
        if not self.bot:
            logger.warning("Telegram bot token not configured")
            return
        
        def poll():
            logger.info("Starting Telegram bot polling")
            self.bot.infinity_polling()
        
        thread = threading.Thread(target=poll, daemon=True)
        thread.start()
        return thread
    
    def stop(self):
        """Stop the bot."""
        if self.bot:
            self.bot.stop_polling()


def create_telegram_bot(token: str, db, docker_service, gemini_client) -> TelegramBot:
    """Factory function to create Telegram bot."""
    if not token:
        logger.info("Telegram bot not configured (no token)")
        return None
    
    return TelegramBot(token, db, docker_service, gemini_client)
