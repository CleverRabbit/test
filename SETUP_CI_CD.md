# 📋 ЧЕК-ЛИСТ НАСТРОЙКИ CI/CD ДЛЯ AI CODE FACTORY

## Шаг 1: Подготовка GitHub репозитория

### 1.1 Создайте репозиторий на GitHub
```bash
cd /workspace
git init
git add .
git commit -m "Initial commit: AI Code Factory"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/ai-code-factory.git
git push -u origin main
```

### 1.2 Создайте Personal Access Token
1. GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)
2. Нажмите "Generate new token (classic)"
3. Выберите права:
   - ✅ `repo` (Full control of private repositories)
   - ✅ `workflow` (Update GitHub Action workflows)
   - ✅ `admin:org` (если нужно для организации)
4. Скопируйте токен (показывается один раз!)
5. Добавьте в `.env`:
   ```
   GITHUB_TOKEN=ghp_xxxxxxxxxxxxxxxxxxxx
   GITHUB_USERNAME=your_github_username
   ```

---

## Шаг 2: Настройка Render

### 2.1 Зарегистрируйтесь на Render
- Перейдите на https://render.com
- Войдите через GitHub

### 2.2 Получите API Key
1. Dashboard → Settings → API Keys
2. Нажмите "Create API Key"
3. Скопируйте ключ
4. Добавьте в `.env`:
   ```
   RENDER_API_KEY=rnu_xxxxxxxxxxxxxxxxxxxx
   ```

### 2.3 Подготовьте сервис на Render
1. В Render Dashboard нажмите "New +" → "Web Service"
2. Connect your repository (выберите ваш репозиторий ai-code-factory)
3. Настройте:
   - **Name**: ai-code-factory
   - **Region**: Frankfurt (или ближайший)
   - **Branch**: main
   - **Root Directory**: (оставьте пустым)
   - **Runtime**: Docker
   - **Docker Context**: .
   - **Dockerfile**: ./docker/app/Dockerfile
   - **Instance Type**: Starter (Free)
   - **Auto-Deploy**: Enabled
   
4. Добавьте переменные окружения в Render:
   ```
   DATABASE_URL=postgresql://postgres:postgres_password@db:5432/factory_db
   TELEGRAM_BOT_TOKEN=your_bot_token
   OPENROUTER_API_KEY=your_openrouter_key
   GITHUB_TOKEN=your_github_token
   GITHUB_USERNAME=your_github_username
   RENDER_API_KEY=your_render_api_key
   SECRET_KEY=your_secret_key
   ADMIN_LOGIN=admin
   ADMIN_PASSWORD=your_strong_password
   ```

5. Нажмите "Create Web Service"

⚠️ **ВАЖНО**: Для работы с базой данных в production используйте Render PostgreSQL или внешний Supabase!

---

## Шаг 3: Настройка GitHub Secrets

### 3.1 Добавьте секреты в GitHub
1. В репозитории перейдите в **Settings** → **Secrets and variables** → **Actions**
2. Нажмите "New repository secret" для каждого:

| Name | Value |
|------|-------|
| `RENDER_API_KEY` | Ваш API ключ Render |
| `TELEGRAM_BOT_TOKEN` | Токен Telegram бота |
| `TELEGRAM_CHAT_ID` | ID чата для уведомлений (узнайте через @userinfobot) |
| `GITHUB_TOKEN` | Тот же токен что в .env |
| `OPENROUTER_API_KEY` | API ключ OpenRouter |

### 3.2 Узнайте Telegram Chat ID
1. Откройте @userinfobot в Telegram
2. Нажмите Start
3. Бот покажет ваш Chat ID
4. Скопируйте и добавьте в GitHub Secrets

---

## Шаг 4: Проверка GitHub Actions

### 4.1 Включите Actions
1. В репозитории перейдите на вкладку **Actions**
2. Если видите предупреждение, нажмите "I understand my workflows, go ahead and enable them"

### 4.2 Запустите workflow вручную для теста
1. Actions → "Auto Deploy AI Code Factory"
2. Нажмите "Run workflow"
3. Выберите ветку main
4. Нажмите "Run workflow"

### 4.3 Проверьте логи
- Убедитесь что все шаги прошли успешно
- Проверьте что Docker образы собрались

---

## Шаг 5: Настройка Telegram бота

### 5.1 Создайте бота
1. Откройте @BotFather в Telegram
2. Отправьте `/newbot`
3. Введите имя бота (например: AI Code Factory Bot)
4. Введите username бота (должен заканчиваться на `bot`, например: ai_code_factory_bot)
5. Скопируйте полученный токен
6. Добавьте в `.env` и GitHub Secrets:
   ```
   TELEGRAM_BOT_TOKEN=1234567890:AABBccDDeeFFggHHiiJJkkLLmmNNooP
   ```

### 5.2 Протестируйте бота
1. Найдите вашего бота в Telegram по username
2. Нажмите Start
3. Отправьте `/start`
4. Бот должен ответить приветствием

---

## Шаг 6: Настройка OpenRouter

### 6.1 Получите API ключ
1. Зарегистрируйтесь на https://openrouter.ai/
2. Перейдите в раздел "Keys"
3. Создайте новый ключ
4. Добавьте в `.env` и GitHub Secrets:
   ```
   OPENROUTER_API_KEY=sk-or-v1-xxxxxxxxxxxxxxxxxxxx
   ```

### 6.2 Проверьте баланс
- Убедитесь что на счету есть средства (минимум $1 для тестов)
- Или используйте бесплатные модели если доступны

---

## Шаг 7: Финальная проверка

### 7.1 Локальный тест (если есть Docker)
```bash
# Копируем .env.example в .env
cp .env.example .env

# Заполняем .env своими значениями

# Запускаем
docker-compose up --build

# Проверяем
curl http://localhost/health
```

### 7.2 Проверка веб-интерфейса
- Откройте http://localhost (или ваш домен)
- Введите тестовую идею
- Нажмите "Создать приложение"
- Проверьте что появился статус генерации

### 7.3 Проверка бота
- Отправьте идею боту
- Проверьте уведомления о статусе

---

## Шаг 8: Деплой на VPS (альтернатива Render)

Если хотите развернуть на своём сервере:

### 8.1 Требования
- VPS с Ubuntu 20.04+
- Docker и Docker Compose
- Домен (опционально)

### 8.2 Установка Docker
```bash
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER
```

### 8.3 Клонирование и запуск
```bash
git clone https://github.com/YOUR_USERNAME/ai-code-factory.git
cd ai-code-factory
cp .env.example .env
# Редактируем .env
nano .env

docker-compose up -d --build
```

### 8.4 Настройка Nginx (если нужен SSL)
```bash
# Установите Certbot
sudo apt install certbot python3-certbot-nginx

# Получите сертификат
sudo certbot --nginx -d yourdomain.com

# Откомментируйте SSL секции в docker/nginx/nginx.conf
```

---

## 🔍 Troubleshooting

### Ошибка: "GitHub API rate limit exceeded"
- Убедитесь что используете Personal Access Token а не пароль
- Проверьте что токен имеет права `repo` и `workflow`

### Ошибка: "Render service creation failed"
- Проверьте API ключ Render
- Убедитесь что аккаунт верифицирован
- Проверьте лимиты бесплатного тарифа

### Ошибка: "Database connection failed"
- Для Render: добавьте PostgreSQL сервис и обновите DATABASE_URL
- Для локального запуска: убедитесь что контейнер db запустился

### Бот не получает сообщения
- Проверьте TELEGRAM_BOT_TOKEN
- Убедитесь что бот не заблокирован
- Проверьте webhook (если используется) вместо polling

---

## 📞 Полезные ссылки

- [GitHub Actions Docs](https://docs.github.com/en/actions)
- [Render API Docs](https://api-docs.render.com/)
- [Telegram Bot API](https://core.telegram.org/bots/api)
- [OpenRouter API](https://openrouter.ai/docs)
- [FastAPI Documentation](https://fastapi.tiangolo.com/)

---

**Готово!** 🎉 Ваша AI Code Factory настроена и готова создавать приложения!