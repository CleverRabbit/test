# 🚀 БЫСТРЫЙ СТАРТ AI CODE FACTORY

## 1. Настройка окружения (5 минут)

```bash
# Копируем шаблон переменных окружения
cp .env.example .env

# Редактируем .env - заполняем ключи
nano .env  # или ваш любимый редактор
```

### Обязательные переменные для заполнения:

```bash
TELEGRAM_BOT_TOKEN=получить_у_BotFather
OPENROUTER_API_KEY=получить_na_openrouter.ai
GITHUB_USERNAME=ваш_login_github
GITHUB_TOKEN=создать_na_github_settings
RENDER_API_KEY=создать_na_render.com
POSTGRES_PASSWORD=придумать_сложный_пароль
SECRET_KEY=любая_случайная_строка
```

## 2. Локальный запуск (2 минуты)

```bash
docker-compose up --build
```

Готово! Откройте http://localhost

## 3. Первый проект (2 минуты)

### Через веб-интерфейс:
1. Откройте http://localhost
2. Введите: "Простой блог с постами и комментариями"
3. Нажмите "🚀 Создать приложение"
4. Следите за статусом генерации

### Через Telegram:
1. Найдите вашего бота
2. Отправьте: "Создай калькулятор с историей вычислений"
3. Получите магическую ссылку когда готово

## 4. Деплой на Render (10 минут)

### Вариант A: Автоматически через GitHub Actions

```bash
# Инициализируем Git
git init
git add .
git commit -m "Initial commit"
git branch -M main
git remote add origin https://github.com/USERNAME/ai-code-factory.git
git push -u origin main
```

Далее в GitHub:
1. Settings → Secrets and variables → Actions
2. Добавьте секреты: RENDER_API_KEY, TELEGRAM_BOT_TOKEN, OPENROUTER_API_KEY, GITHUB_TOKEN
3. Actions запустится автоматически

### Вариант B: Вручную на Render

1. Зарегистрируйтесь на render.com
2. New + → Web Service
3. Подключите репозиторий
4. Runtime: Docker
5. Instance Type: Starter (Free)
6. Добавьте переменные окружения из .env
7. Create Web Service

## 5. Проверка работы

```bash
# Health check
curl http://localhost/health

# Тест API
curl -X POST http://localhost/api/start-project \
  -H "Content-Type: application/json" \
  -d '{"idea": "Тест", "user_id": "test"}'

# Логи
docker-compose logs -f app
```

## 📚 Документация

- [README.md](README.md) - Полная документация
- [SETUP_CI_CD.md](SETUP_CI_CD.md) - Подробная настройка CI/CD
- [CHECKLIST.md](CHECKLIST.md) - Чек-лист проверки

## 🆘 Помощь

Если что-то не работает:
1. Проверьте логи: `docker-compose logs`
2. Убедитесь что все переменные в .env заполнены
3. Проверьте CHECKLIST.md

---

**Время до первого приложения: ~10 минут!** ⏱️