# 📋 ПОШАГОВЫЙ ПЛАН НАСТРОЙКИ CI/CD

## ЧАСТЬ 1: GitHub (5 минут)

### Шаг 1.1: Создайте репозиторий
```bash
cd /workspace
git init
git add .
git commit -m "AI Code Factory initial commit"
git branch -M main
```

### Шаг 1.2: Создайте репозиторий на GitHub
1. Откройте https://github.com/new
2. Repository name: `ai-code-factory`
3. Public или Private (на ваш выбор)
4. НЕ нажимайте "Add README" (у нас уже есть код)
5. Create repository

### Шаг 1.3: Свяжите локальный и удалённый репозиторий
```bash
# Замените YOUR_USERNAME на ваш логин GitHub
git remote add origin https://github.com/YOUR_USERNAME/ai-code-factory.git
git push -u origin main
```

---

## ЧАСТЬ 2: GitHub Token (3 минуты)

### Шаг 2.1: Создайте Personal Access Token
1. GitHub → Settings (шестерёнка справа сверху)
2. Developer settings (внизу слева)
3. Personal access tokens → Tokens (classic)
4. Generate new token → Generate new token (classic)
5. Note: `AI Code Factory`
6. Expiration: `No expiration` (или 90 дней)
7. ✅ Выберите права:
   - `repo` (Full control of private repositories)
   - `workflow` (Update GitHub Action workflows)
8. Generate token
9. **Скопируйте токен** (показывается один раз!)

### Шаг 2.2: Добавьте токен в .env
```bash
nano .env
# Найдите строки:
GITHUB_USERNAME=ваш_логин_github
GITHUB_TOKEN=ghp_xxxxxxxxxxxx
```

---

## ЧАСТЬ 3: Telegram Bot (3 минуты)

### Шаг 3.1: Создайте бота
1. Откройте @BotFather в Telegram
2. `/newbot`
3. Введите имя: `AI Code Factory Bot`
4. Введите username: `ai_code_factory_bot` (должен заканчиваться на `bot`)
5. **Скопируйте токен** (выглядит как `1234567890:AABBccDDeeFFggHHiiJJkkLLmmNNooP`)

### Шаг 3.2: Добавьте токен в .env
```bash
nano .env
# Найдите строку:
TELEGRAM_BOT_TOKEN=1234567890:AABBccDDeeFFggHHiiJJkkLLmmNNooP
```

### Шаг 3.3: Узнайте ваш Chat ID
1. Откройте @userinfobot в Telegram
2. Нажмите Start
3. Скопируйте Chat ID (число, например `123456789`)

---

## ЧАСТЬ 4: OpenRouter (3 минуты)

### Шаг 4.1: Зарегистрируйтесь и получите ключ
1. Откройте https://openrouter.ai/
2. Sign in (через GitHub)
3. Keys → Create Key
4. Name: `AI Code Factory`
5. **Скопируйте ключ** (начинается с `sk-or-v1-`)

### Шаг 4.2: Добавьте ключ в .env
```bash
nano .env
# Найдите строку:
OPENROUTER_API_KEY=sk-or-v1-xxxxxxxxxxxx
```

---

## ЧАСТЬ 5: Render (5 минут)

### Шаг 5.1: Зарегистрируйтесь на Render
1. Откройте https://render.com/
2. Sign up (через GitHub)
3. Подтвердите email

### Шаг 5.2: Получите API Key
1. Dashboard → Settings (слева)
2. API Keys → Create API Key
3. Name: `AI Code Factory`
4. **Скопируйте ключ** (начинается с `rnu-`)

### Шаг 5.3: Добавьте ключ в .env
```bash
nano .env
# Найдите строку:
RENDER_API_KEY=rnu-xxxxxxxxxxxx
```

---

## ЧАСТЬ 6: GitHub Secrets (3 минуты)

### Шаг 6.1: Откройте Secrets
1. Откройте ваш репозиторий на GitHub
2. Settings → Secrets and variables → Actions

### Шаг 6.2: Добавьте секреты
Нажмите "New repository secret" для каждого:

| Name | Value |
|------|-------|
| `GITHUB_TOKEN` | Токен из Части 2 |
| `GITHUB_USERNAME` | Ваш логин GitHub |
| `TELEGRAM_BOT_TOKEN` | Токен из Части 3 |
| `TELEGRAM_CHAT_ID` | Chat ID из Части 3 |
| `OPENROUTER_API_KEY` | Ключ из Части 4 |
| `RENDER_API_KEY` | Ключ из Части 5 |

---

## ЧАСТЬ 7: Финальный пуш (2 минуты)

### Шаг 7.1: Проверьте .env.example
Убедитесь что `.env` НЕ закоммичен:
```bash
cat .gitignore | grep ".env"
# Должно быть: .env
```

### Шаг 7.2: Запушьте изменения
```bash
git add .
git commit -m "Complete AI Code Factory setup"
git push origin main
```

### Шаг 7.3: Проверьте GitHub Actions
1. Откройте репозиторий на GitHub
2. Перейдите на вкладку Actions
3. Должен запуститься workflow "Auto Deploy AI Code Factory"
4. Подождите завершения (зелёная галочка)

---

## ЧАСТЬ 8: Настройка сервиса на Render (5 минут)

### Шаг 8.1: Создайте Web Service
1. Render Dashboard → New + → Web Service
2. Connect your repository → выберите `ai-code-factory`
3. Configure service:

```
Name: ai-code-factory
Region: Frankfurt (или ближайший)
Branch: main
Root Directory: (оставьте пустым)
Runtime: Docker
Docker Context: .
Dockerfile: ./docker/app/Dockerfile
Instance Type: Starter (Free)
Auto-Deploy: Yes
Health Check Path: /health
```

### Шаг 8.2: Добавьте переменные окружения
Нажмите "Advanced" → "Add Environment Variable":

```
DATABASE_URL=postgresql://postgres:postgres_password@db:5432/factory_db
TELEGRAM_BOT_TOKEN=из_Части_3
OPENROUTER_API_KEY=из_Части_4
GITHUB_TOKEN=из_Части_2
GITHUB_USERNAME=ваш_логин
RENDER_API_KEY=из_Части_5
SECRET_KEY=любая_случайная_строка_32_символа
ADMIN_LOGIN=admin
ADMIN_PASSWORD=придумайте_сложный_пароль
```

### Шаг 8.3: Создайте сервис
1. Нажмите "Create Web Service"
2. Подождите деплой (5-10 минут)
3. Скопируйте URL (выглядит как `https://ai-code-factory-xxxx.onrender.com`)

---

## ЧАСТЬ 9: Проверка (3 минуты)

### Шаг 9.1: Проверьте веб-интерфейс
Откройте URL из Шага 8.3 в браузере
- Должна загрузиться страница AI Code Factory

### Шаг 9.2: Проверьте бота
1. Найдите бота в Telegram по username
2. Нажмите Start
3. Должно появиться приветствие

### Шаг 9.3: Создайте тестовый проект
1. Введите идею: "Простой TODO лист"
2. Нажмите создать
3. Следите за статусом

---

## ИТОГО

**Время настройки: ~30 минут**

**Что получилось:**
✅ GitHub репозиторий с кодом
✅ GitHub Actions для авто-деплоя
✅ Telegram бот для приёма идей
✅ Render сервис для хостинга
✅ Магические ссылки на 24 часа

**Следующие шаги:**
1. Поделитесь ботом с друзьями
2. Создавайте приложения по идее
3. Мониторьте использование API (OpenRouter платный)

---

## 🔍 Troubleshooting

### GitHub Actions не запускается
- Проверьте что воркфлоу включён: Actions → "I understand my workflows"

### Render деплой неудачен
- Проверьте логи в Render Dashboard
- Убедитесь что все переменные окружения добавлены

### Бот не отвечает
- Проверьте TELEGRAM_BOT_TOKEN
- Убедитесь что бот не заблокирован

### Ошибка LLM
- Проверьте баланс на OpenRouter
- Убедитесь что OPENROUTER_API_KEY правильный

---

**Готово!** 🎉 Ваша AI Code Factory полностью настроена!
