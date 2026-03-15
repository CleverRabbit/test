"""
Telegram Bot для AI Code Factory
Получение идей, уточняющие вопросы, отправка магических ссылок
"""
import os
import asyncio
from telegram import Update, InlineKeyboardButton, InlineKeyboardMarkup
from telegram.ext import Application, CommandHandler, MessageHandler, filters, ContextTypes
import httpx

TELEGRAM_BOT_TOKEN = os.getenv("TELEGRAM_BOT_TOKEN", "")
API_BASE_URL = os.getenv("API_BASE_URL", "http://localhost:8000")

# Состояния пользователей
USER_STATES = {}


async def start(update: Update, context: ContextTypes.DEFAULT_TYPE):
    """Обработчик команды /start"""
    user = update.effective_user
    welcome_text = f"""
👋 Привет, {user.first_name}!

Я — AI Code Factory Bot 🤖

Отправь мне идею приложения, и я:
1️⃣ Проанализирую её
2️⃣ Задам уточняющие вопросы (если нужно)
3️⃣ Создам полный проект с кодом
4️⃣ Задеплою на Render
5️⃣ Дам магическую ссылку на 24 часа

🚀 Напиши свою идею прямо сейчас!

🔧 Команды:
/setkey - Установить API ключ Gemini
/status - Проверить статус проекта
/help - Помощь
    """
    
    await update.message.reply_text(welcome_text)


async def set_key_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    """Команда /setkey - установка API ключа Gemini"""
    user_id = str(update.effective_user.id)
    
    # Показываем инструкцию
    instruction_text = """
🔑 Настройка API ключа Google Gemini

Получи ключ здесь: https://aistudio.google.com/app/apikey

1. Перейди по ссылке выше
2. Нажми "Create API Key"
3. Скопируй ключ
4. Отправь его мне следующим сообщением

Ключ будет сохранён только для твоего аккаунта.
    """
    
    await update.message.reply_text(instruction_text)
    
    # Устанавливаем состояние ожидания ключа
    USER_STATES[user_id] = {
        "waiting_for_key": True,
        "project_id": None,
        "waiting_answers": False
    }


async def handle_api_key(update: Update, context: ContextTypes.DEFAULT_TYPE):
    """Обработка полученного API ключа Gemini"""
    user_id = str(update.effective_user.id)
    api_key = update.message.text.strip()
    
    if user_id not in USER_STATES or not USER_STATES[user_id].get("waiting_for_key"):
        return
    
    # Проверяем формат ключа (начинается с AIza)
    if not api_key.startswith("AIza"):
        await update.message.reply_text(
            "❌ Неверный формат ключа. Ключ Gemini должен начинаться с 'AIza'.\n"
            "Попробуй ещё раз или получи новый ключ на https://aistudio.google.com/app/apikey"
        )
        return
    
    try:
        # Отправляем ключ в API для сохранения
        async with httpx.AsyncClient(timeout=30.0) as client:
            response = await client.post(
                f"{API_BASE_URL}/api/set-gemini-key",
                json={
                    "user_id": user_id,
                    "api_key": api_key
                }
            )
            
            if response.status_code == 200:
                await update.message.reply_text(
                    "✅ API ключ Gemini успешно сохранён!\n\n"
                    "Теперь ты можешь отправлять идеи приложений.\n"
                    "Используй команду /help для справки."
                )
                
                # Сбрасываем состояние
                USER_STATES[user_id]["waiting_for_key"] = False
            else:
                await update.message.reply_text(
                    f"❌ Ошибка при сохранении ключа: {response.text}"
                )
    except Exception as e:
        await update.message.reply_text(f"❌ Ошибка: {str(e)}")


async def handle_idea(update: Update, context: ContextTypes.DEFAULT_TYPE):
    """Обработка идеи пользователя"""
    user_id = str(update.effective_user.id)
    
    # Проверяем, установлен ли API ключ
    if user_id in USER_STATES and USER_STATES[user_id].get("waiting_for_key"):
        await update.message.reply_text(
            "❌ Сначала настрой API ключ Gemini командой /setkey\n"
            "После этого сможешь отправлять идеи приложений."
        )
        return
    
    idea = update.message.text
    
    # Отправляем подтверждение
    await update.message.reply_text(
        "🔄 Принимаю идею... Анализирую...\n\n"
        "Это может занять несколько минут. Я буду держать тебя в курсе!"
    )
    
    try:
        # Отправляем запрос к API
        async with httpx.AsyncClient(timeout=120.0) as client:
            response = await client.post(
                f"{API_BASE_URL}/api/start-project",
                json={
                    "idea": idea,
                    "user_id": user_id
                }
            )
            
            if response.status_code == 200:
                data = response.json()
                project_id = data.get("project_id")
                magic_link = data.get("magic_link")
                
                # Сохраняем состояние
                USER_STATES[user_id] = {
                    "project_id": project_id,
                    "waiting_answers": False
                }
                
                reply_text = f"""
✅ Проект запущен!

📋 ID: {project_id}
🔮 Магическая ссылка: {magic_link}

Статус: {data.get('status', 'generating')}

Я напишу тебе, когда проект будет готов! 🎉
                """
                await update.message.reply_text(reply_text)
                
                # Начинаем мониторинг статуса
                asyncio.create_task(monitor_project_status(user_id, project_id, context))
            else:
                await update.message.reply_text(
                    f"❌ Ошибка при создании проекта: {response.text}"
                )
    
    except Exception as e:
        await update.message.reply_text(f"❌ Ошибка: {str(e)}")


async def monitor_project_status(user_id: str, project_id: int, context: ContextTypes.DEFAULT_TYPE):
    """Мониторинг статуса проекта и уведомления"""
    max_attempts = 60  # 10 минут
    attempt = 0
    
    while attempt < max_attempts:
        await asyncio.sleep(10)  # Проверка каждые 10 секунд
        attempt += 1
        
        try:
            async with httpx.AsyncClient(timeout=10.0) as client:
                response = await client.get(f"{API_BASE_URL}/api/project/{project_id}")
                
                if response.status_code == 200:
                    data = response.json()
                    status = data.get("status")
                    
                    if status == "deployed":
                        render_url = data.get("render_url")
                        magic_link = data.get("magic_link")
                        
                        success_text = f"""
🎉 ГОТОВО! Твоё приложение развёрнуто!

🌐 Веб-интерфейс: {render_url}
🔮 Магическая ссылка: {magic_link}
📦 GitHub: {data.get('github_url', 'N/A')}

Ссылка действует 24 часа. Наслаждайся! 🚀
                        """
                        
                        # Отправляем сообщение пользователю
                        try:
                            await context.bot.send_message(
                                chat_id=user_id,
                                text=success_text
                            )
                        except:
                            pass
                        
                        return
                    
                    elif status == "failed":
                        error_text = "❌ К сожалению, при создании проекта произошла ошибка.\nПопробуй другую идею или обратись к администратору."
                        try:
                            await context.bot.send_message(chat_id=user_id, text=error_text)
                        except:
                            pass
                        return
                    
                    elif status == "waiting_answers":
                        # Нужны уточняющие вопросы
                        steps = data.get("steps", [])
                        last_step = steps[-1] if steps else {}
                        message = last_step.get("message", "")
                        
                        questions_text = f"""
❓ Для уточнения требований ответь на вопросы:

{message}

Просто напиши ответы в следующем сообщении.
                        """
                        
                        try:
                            await context.bot.send_message(chat_id=user_id, text=questions_text)
                            USER_STATES[user_id]["waiting_answers"] = True
                        except:
                            pass
                        return
                
        except Exception as e:
            print(f"Error monitoring project {project_id}: {e}")
            continue
    
    # Таймаут
    timeout_text = "⏱ Превышено время ожидания. Проект всё ещё создаётся.\nПроверь статус позже через /status"
    try:
        await context.bot.send_message(chat_id=user_id, text=timeout_text)
    except:
        pass


async def handle_answer(update: Update, context: ContextTypes.DEFAULT_TYPE):
    """Обработка ответов на уточняющие вопросы"""
    user_id = str(update.effective_user.id)
    answer = update.message.text
    
    if user_id not in USER_STATES or not USER_STATES[user_id].get("waiting_answers"):
        await update.message.reply_text(
            "Сначала отправь идею приложения командой /start или просто текстом."
        )
        return
    
    project_id = USER_STATES[user_id].get("project_id")
    
    # TODO: Отправить ответ в API для продолжения генерации
    await update.message.reply_text(
        f"✅ Ответ принят! Продолжаю генерацию проекта #{project_id}..."
    )
    
    USER_STATES[user_id]["waiting_answers"] = False
    
    # Перезапускаем мониторинг
    asyncio.create_task(monitor_project_status(user_id, project_id, context))


async def status_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    """Команда /status - показать статус последнего проекта"""
    user_id = str(update.effective_user.id)
    
    if user_id not in USER_STATES:
        await update.message.reply_text(
            "У тебя пока нет активных проектов. Отправь идею!"
        )
        return
    
    project_id = USER_STATES[user_id].get("project_id")
    
    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            response = await client.get(f"{API_BASE_URL}/api/project/{project_id}")
            
            if response.status_code == 200:
                data = response.json()
                
                status_emoji = {
                    "pending": "⏳",
                    "generating": "🔧",
                    "deployed": "✅",
                    "failed": "❌",
                    "waiting_answers": "❓"
                }.get(data.get("status"), "📊")
                
                status_text = f"""
{status_emoji} Статус проекта #{project_id}

Идея: {data.get('idea', 'N/A')[:100]}
Статус: {data.get('status', 'unknown')}

Шаги:
                """
                
                for step in data.get("steps", []):
                    step_emoji = {
                        "completed": "✅",
                        "running": "🔄",
                        "failed": "❌",
                        "pending": "⏳"
                    }.get(step.get("status"), "•")
                    status_text += f"\n{step_emoji} {step.get('name')}: {step.get('message', '')}"
                
                if data.get("render_url"):
                    status_text += f"\n\n🌐 URL: {data['render_url']}"
                
                await update.message.reply_text(status_text)
            else:
                await update.message.reply_text("Не удалось получить статус проекта.")
    
    except Exception as e:
        await update.message.reply_text(f"Ошибка: {str(e)}")


async def help_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    """Команда /help"""
    help_text = """
🤖 AI Code Factory Bot - Помощь

Команды:
/start - Начать новый проект
/status - Проверить статус текущего проекта
/help - Эта справка

Как это работает:
1. Отправь идею приложения
2. Ответь на уточняющие вопросы (если будут)
3. Получи магическую ссылку на готовое приложение

Приложение будет доступно 24 часа!

Техподдержка: @admin
    """
    await update.message.reply_text(help_text)


def create_bot_application():
    """Создание и настройка приложения бота"""
    if not TELEGRAM_BOT_TOKEN:
        print("WARNING: TELEGRAM_BOT_TOKEN не настроен!")
        return None
    
    application = Application.builder().token(TELEGRAM_BOT_TOKEN).build()
    
    # Handlers
    application.add_handler(CommandHandler("start", start))
    application.add_handler(CommandHandler("setkey", set_key_command))
    application.add_handler(CommandHandler("status", status_command))
    application.add_handler(CommandHandler("help", help_command))
    
    # Обработка текста (идеи и ответы и API ключи)
    application.add_handler(MessageHandler(filters.TEXT & ~filters.COMMAND, handle_idea))
    
    return application


if __name__ == "__main__":
    app = create_bot_application()
    if app:
        print("🤖 Запуск Telegram бота...")
        app.run_polling(allowed_updates=Update.ALL_TYPES)