"""
Telegram бот для приема задач от пользователей.
Использует aiogram 3.x для асинхронной работы.
"""
import asyncio
from datetime import datetime
from loguru import logger
from telegram import Update
from telegram.ext import ApplicationBuilder, ContextTypes, CommandHandler, MessageHandler, filters

from src.config import settings
from src.database import get_db, User, Task, TaskQuadrant, TaskStatus, init_db
from src.llm import llm_service
from sqlalchemy.orm import Session


class TelegramBot:
    """Класс Telegram бота"""
    
    def __init__(self):
        self.token = settings.telegram_bot_token
        self.app = None
    
    async def start(self, update: Update, context: ContextTypes.DEFAULT_TYPE):
        """Обработчик команды /start"""
        user = update.effective_user
        if not user:
            return
        
        welcome_text = (
            f"👋 Привет, {user.first_name}!\n\n"
            "Я — ваш AI-ассистент для управления задачами по методу Эйзенхауэра.\n\n"
            "📝 *Как пользоваться:*\n"
            "• Просто отправьте мне текст задачи\n"
            "• Я проанализирую её и определю приоритет\n"
            "• Задача появится в вашем веб-интерфейсе\n\n"
            "⌨️ *Команды:*\n"
            "/start - Запустить бота заново\n"
            "/list - Показать последние задачи\n"
            "/help - Помощь\n\n"
            "🌐 Веб-интерфейс: откройте сайт для просмотра всех задач!"
        )
        
        await update.message.reply_text(welcome_text, parse_mode='Markdown')
        
        # Регистрируем пользователя в БД
        try:
            db = next(get_db())
            existing_user = db.query(User).filter(User.telegram_id == str(user.id)).first()
            
            if not existing_user:
                new_user = User(
                    telegram_id=str(user.id),
                    username=user.username or user.first_name
                )
                db.add(new_user)
                db.commit()
                logger.info(f"Новый пользователь зарегистрирован: {user.id}")
            else:
                # Обновляем username если изменился
                existing_user.username = user.username or user.first_name
                db.commit()
                
        except Exception as e:
            logger.error(f"Ошибка регистрации пользователя: {e}")
    
    async def help_command(self, update: Update, context: ContextTypes.DEFAULT_TYPE):
        """Обработчик команды /help"""
        help_text = (
            "📚 *Помощь*\n\n"
            "Этот бот помогает управлять задачами с помощью AI:\n\n"
            "1️⃣ Отправьте задачу текстом\n"
            "2️⃣ AI определит приоритет (квадрант Эйзенхауэра)\n"
            "3️⃣ Задача сохранится и появится на сайте\n\n"
            "🔒 *Безопасность:* Перед отправкой в AI ваши данные анонимизируются.\n\n"
            "📊 *Квадранты:*\n"
            "Q1 🔴 - Срочно и Важно (сделать сейчас)\n"
            "Q2 🟢 - Важно, не срочно (запланировать)\n"
            "Q3 🟡 - Срочно, не важно (делегировать)\n"
            "Q4 ⚪ - Не срочно, не важно (удалить)"
        )
        await update.message.reply_text(help_text, parse_mode='Markdown')
    
    async def list_tasks(self, update: Update, context: ContextTypes.DEFAULT_TYPE):
        """Обработчик команды /list"""
        user = update.effective_user
        if not user:
            return
        
        try:
            db = next(get_db())
            db_user = db.query(User).filter(User.telegram_id == str(user.id)).first()
            
            if not db_user:
                await update.message.reply_text("❌ Пользователь не найден. Нажмите /start")
                return
            
            tasks = db.query(Task).filter(Task.user_id == db_user.id).order_by(Task.created_at.desc()).limit(5).all()
            
            if not tasks:
                await update.message.reply_text("📭 У вас пока нет задач. Отправьте мне первую задачу!")
                return
            
            response = "📋 *Последние задачи:*\n\n"
            for i, task in enumerate(tasks, 1):
                quadrant_emoji = {"q1": "🔴", "q2": "🟢", "q3": "🟡", "q4": "⚪"}
                status_emoji = {"todo": "⏳", "in_progress": "🔄", "done": "✅"}
                
                q_emoji = quadrant_emoji.get(task.quadrant, '⚪')
                s_emoji = status_emoji.get(task.status, '⏳')
                
                response += (
                    f"{i}. {s_emoji} {q_emoji} "
                    f"*{task.title}*\n"
                    f"   Статус: {task.status}\n\n"
                )
            
            await update.message.reply_text(response, parse_mode='Markdown')
            
        except Exception as e:
            logger.error(f"Ошибка получения списка задач: {e}")
            await update.message.reply_text("❌ Произошла ошибка при получении задач")
    
    async def handle_message(self, update: Update, context: ContextTypes.DEFAULT_TYPE):
        """Обработчик текстовых сообщений (создание задач)"""
        user = update.effective_user
        text = update.message.text
        
        if not user or not text:
            return
        
        # Отправляем сообщение о обработке
        processing_msg = await update.message.reply_text("⏳ Анализирую задачу...")
        
        try:
            db = next(get_db())
            
            # Находим или создаем пользователя
            db_user = db.query(User).filter(User.telegram_id == str(user.id)).first()
            if not db_user:
                db_user = User(telegram_id=str(user.id), username=user.username or user.first_name)
                db.add(db_user)
                db.commit()
                db.refresh(db_user)
            
            # Анализируем задачу через LLM
            llm_result = await llm_service.analyze_task(text)
            
            if not llm_result:
                await processing_msg.edit_text(
                    "❌ Не удалось проанализировать задачу. Попробуйте еще раз или уточните формулировку."
                )
                return
            
            # Создаем задачу в БД
            deadline = None
            if llm_result.get("deadline"):
                try:
                    deadline = datetime.strptime(llm_result["deadline"], "%Y-%m-%d")
                except:
                    pass
            
            new_task = Task(
                user_id=db_user.id,
                original_text=text,
                anonymized_text=llm_result.get("anonymized_text"),
                title=llm_result["title"],
                description=llm_result.get("description"),
                quadrant=TaskQuadrant(llm_result["quadrant"]),
                deadline=deadline
            )
            
            db.add(new_task)
            db.commit()
            db.refresh(new_task)
            
            # Формируем ответ
            quadrant_names = {
                "q1": "🔴 Q1: Срочно и Важно",
                "q2": "🟢 Q2: Важно, не срочно",
                "q3": "🟡 Q3: Срочно, не важно",
                "q4": "⚪ Q4: Не срочно, не важно"
            }
            
            response = (
                f"✅ *Задача создана!*\n\n"
                f"📌 *{llm_result['title']}*\n\n"
                f"📊 Приоритет: {quadrant_names.get(llm_result['quadrant'], 'Не определен')}\n"
            )
            
            if llm_result.get("description"):
                response += f"📝 Описание: {llm_result['description']}\n"
            
            if deadline:
                response += f"📅 Дедлайн: {deadline.strftime('%d.%m.%Y')}\n"
            
            response += "\n🌐 Откройте веб-интерфейс для управления всеми задачами!"
            
            await processing_msg.edit_text(response, parse_mode='Markdown')
            logger.info(f"Задача создана: {new_task.id} для пользователя {db_user.id}")
            
        except Exception as e:
            logger.error(f"Ошибка обработки сообщения: {e}")
            await processing_msg.edit_text("❌ Произошла ошибка при создании задачи. Попробуйте позже.")
    
    async def run_polling(self):
        """Запуск бота в режиме polling"""
        try:
            self.app = ApplicationBuilder().token(self.token).build()
            
            # Добавляем обработчики
            self.app.add_handler(CommandHandler("start", self.start))
            self.app.add_handler(CommandHandler("help", self.help_command))
            self.app.add_handler(CommandHandler("list", self.list_tasks))
            self.app.add_handler(MessageHandler(filters.TEXT & ~filters.COMMAND, self.handle_message))
            
            logger.info("Telegram бот запускается в режиме polling...")
            await self.app.run_polling(allowed_updates=Update.ALL_TYPES)
            
        except Exception as e:
            logger.error(f"Ошибка запуска бота: {e}")
            raise


# Глобальный экземпляр бота
telegram_bot = TelegramBot()
