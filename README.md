# 🤖 AI Code Factory

**Автономная система генерации и деплоя приложений по идее**

Превращает текстовое описание идеи в работающее приложение с веб-интерфейсом, Telegram-ботом и автоматическим деплоем на Render.

## 🚀 Возможности

- **Генерация по идее**: Опишите идею → получите готовое приложение
- **AI-анализ**: Автоматический анализ требований и уточняющие вопросы
- **Полный стек**: FastAPI + PostgreSQL + HTML/JS + Docker
- **Авто-деплой**: GitHub + Render автоматически
- **Магические ссылки**: Доступ к приложению на 24 часа по уникальной ссылке
- **Telegram-бот**: Управление через Telegram
- **Веб-интерфейс**: Создание проектов через браузер
- **Google Gemini**: Интеграция с Google Gemini для генерации кода
- **Настройка ключа через бота**: API ключ вводится пользователем командой /setkey

## 📋 Требования

- Docker и Docker Compose
- Аккаунт на GitHub
- Аккаунт на Render (бесплатный тариф подходит)
- Telegram Bot Token (от @BotFather)
- Google Gemini API Key (бесплатно на https://aistudio.google.com/app/apikey)

## 🔧 Настройка

### 1. Клонирование репозитория

```bash
git clone https://github.com/YOUR_USERNAME/ai-code-factory.git
cd ai-code-factory
```

### 2. Настройка переменных окружения

Скопируйте `.env.example` в `.env` и заполните значения:

```bash
cp .env.example .env
```

#### Обязательные переменные:

| Переменная | Описание | Где получить |
|------------|----------|--------------|
| `TELEGRAM_BOT_TOKEN` | Токен Telegram бота | [@BotFather](https://t.me/BotFather) |
| `GEMINI_API_KEY` | API ключ Google Gemini (опционально, можно задать через бота) | [Google AI Studio](https://aistudio.google.com/app/apikey) |
| `GITHUB_USERNAME` | Ваш логин на GitHub | GitHub профиль |
| `GITHUB_TOKEN` | Personal Access Token | GitHub Settings → Developer settings → Personal access tokens |
| `RENDER_API_KEY` | API ключ Render | Render Dashboard → API Keys |
| `SECRET_KEY` | Секретный ключ для сессий | Любая случайная строка |
| `POSTGRES_PASSWORD` | Пароль базы данных | Любая сложная строка |

### 3. Создание Telegram бота

1. Откройте [@BotFather](https://t.me/BotFather)
2. Отправьте `/newbot`
3. Следуйте инструкциям
4. Скопируйте полученный токен в `.env`

### 4. Получение Google Gemini API Key (опционально)

API ключ можно ввести через Telegram бота командой `/setkey` при первом запуске, или задать по умолчанию в `.env`:

1. Перейдите на [Google AI Studio](https://aistudio.google.com/app/apikey)
2. Нажмите "Create API Key"
3. Скопируйте ключ в `.env` (или используйте команду `/setkey` в боте)

**Важно:** Пользователи могут настроить свой личный API ключ через Telegram бота командой `/setkey`. Это позволяет использовать собственные квоты Gemini.

### 5. Настройка GitHub Token

1. GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)
2. Generate new token (classic)
3. Выберите права: `repo`, `workflow`
4. Скопируйте токен в `.env`

### 6. Настройка Render API Key

1. Render Dashboard → Settings → API Keys
2. Create API Key
3. Скопируйте в `.env`

## 🏃 Запуск

### Локальный запуск (Docker)

```bash
docker-compose up --build
```

Сервисы будут доступны:
- Веб-интерфейс: http://localhost
- API: http://localhost/api
- Health check: http://localhost/health

### Режим разработки

```bash
docker-compose up -d  # Запуск в фоне
docker-compose logs -f app  # Логи приложения
docker-compose down  # Остановка
```

## 📱 Использование

### Через веб-интерфейс

1. Откройте http://localhost
2. Введите идею приложения
3. Нажмите "Создать приложение"
4. Следите за статусом генерации
5. Получите магическую ссылку на готовое приложение

### Через Telegram бота

1. Откройте вашего бота в Telegram
2. Отправьте команду `/start`
3. **Настройте API ключ Gemini**: отправьте команду `/setkey` и следуйте инструкциям
4. Напишите идею приложения
5. Бот будет уведомлять о статусе
6. Получите магическую ссылку когда готово

### Команды бота

- `/start` - Начать новый проект
- `/setkey` - Установить API ключ Gemini
- `/status` - Проверить статус текущего проекта
- `/help` - Справка

## 🏗 Архитектура

```
┌─────────────────┐
│   Nginx (80)    │
│  Reverse Proxy  │
└────────┬────────┘
         │
    ┌────┴────┐
    │         │
┌───▼───┐ ┌──▼────────┐
│ App   │ │ Frontend  │
│FastAPI│ │  (static) │
└───┬───┘ └───────────┘
    │
┌───▼───┐
│  DB   │
│Postgres│
└────────┘
```

### Компоненты

- **Nginx**: Reverse proxy, раздача статики, rate limiting
- **App (FastAPI)**: API, генерация кода, интеграции
- **Database (PostgreSQL)**: Хранение проектов и пользователей
- **LLM Client**: Интеграция с Google Gemini API
- **GitHub Client**: Создание репозиториев, пуш кода
- **Render Client**: Автоматический деплой
- **Telegram Bot**: Интерфейс для пользователей

## 🔐 Безопасность

- Все секреты через переменные окружения
- Docker контейнеры от non-root пользователя
- Изолированная Docker сеть
- Rate limiting в Nginx
- Магические ссылки с истекающим сроком (24 часа)
- Basic Auth для админских endpoints

## 📊 API Endpoints

| Endpoint | Method | Описание |
|----------|--------|----------|
| `/health` | GET | Health check |
| `/api/start-project` | POST | Запуск генерации проекта |
| `/api/project/{id}` | GET | Статус проекта |
| `/app/{magic_hash}` | GET | Доступ по магической ссылке |
| `/api/projects` | GET | Список проектов (admin only) |

## 🎛 Админ панель

Доступна по `/api/projects` с Basic Auth.

Логин/пароль по умолчанию: `admin` / `admin`

**Измените в `.env`:**
```
ADMIN_LOGIN=your_login
ADMIN_PASSWORD=your_strong_password
```

## 🔄 CI/CD

GitHub Actions настроен на:
- Сборку Docker образов при push
- Валидацию docker-compose
- Авто-деплой на Render при push в main
- Уведомления в Telegram

### Настройка secrets в GitHub

В репозитории GitHub перейдите в Settings → Secrets and variables → Actions:

- `RENDER_API_KEY` - API ключ Render
- `TELEGRAM_BOT_TOKEN` - Токен бота для уведомлений
- `TELEGRAM_CHAT_ID` - ID чата для уведомлений

## 🛠 Troubleshooting

### Ошибка "GitHub токен не настроен"

Проверьте переменные `GITHUB_TOKEN` и `GITHUB_USERNAME` в `.env`

### Ошибка "Render API ключ не настроен"

Проверьте `RENDER_API_KEY` в `.env`

### Бот не отвечает

1. Проверьте `TELEGRAM_BOT_TOKEN`
2. Убедитесь что бот запущен: `docker-compose ps`
3. Проверьте логи: `docker-compose logs bot`

### Ошибка подключения к базе

Убедитесь что PostgreSQL запустился:
```bash
docker-compose ps db
docker-compose logs db
```

## 📝 Лицензия

MIT License

## 🤝 Поддержка

Создайте issue в репозитории или обратитесь к разработчику.

---

**Сделано с ❤️ используя AI Code Factory**