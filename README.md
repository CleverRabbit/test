# 🚀 AI-Powered Projects Repository

Репозиторий автономных AI-проектов полного цикла, созданных с использованием:
- **Python** (FastAPI, asyncio, telegram-bot)
- **PostgreSQL** (Supabase совместимый)
- **Frontend** (HTML/CSS/JS, адаптивный дизайн)
- **DevOps** (Docker, CI/CD, деплой на Render/Railway)
- **AI** (OpenRouter API, LLM-промпты, анонимизация)

---

## 📁 Проекты (ветки)

Каждый проект находится в отдельной ветке `feature/[название]` и представляет собой полностью независимое, готовое к деплою приложение.

### Активные проекты:

| Ветка | Описание | Статус |
|-------|----------|--------|
| [`feature/task-manager-eisenhower`](../../tree/feature/task-manager-eisenhower) | Система управления задачами с Telegram-ботом и AI-анализом приоритетов по методу Эйзенхауэра | ✅ Готов |

---

## 🏗️ Архитектура каждого проекта

Каждый проект следует единой архитектуре:

```
feature/[project-name]/
├── docker-compose.yml          # Оркестрация (nginx + app + db)
├── .env.example                # Шаблон переменных окружения
├── docker/
│   ├── nginx/                  # Reverse proxy + статика
│   └── app/                    # Python приложение
├── src/                        # Исходный код FastAPI + бот
├── frontend/                   # Веб-интерфейс
├── .github/workflows/          # CI/CD пайплайны
└── README.md                   # Инструкция по запуску
```

### Технологический стек (единый для всех проектов):

- **Backend:** FastAPI + SQLAlchemy + Pydantic
- **Database:** PostgreSQL 15 (в Docker)
- **Bot:** python-telegram-bot v20 / aiogram
- **AI:** OpenRouter API (Qwen 2.5 7B Instruct)
- **Frontend:** HTML5 + CSS3 + Vanilla JS
- **Server:** Nginx (reverse proxy, rate limiting, SSL-ready)
- **Container:** Docker + Docker Compose
- **Network:** Изолированная `internal_net`
- **CI/CD:** GitHub Actions (авто-деплой при push)

---

## 🚀 Быстрый старт любого проекта

### 1. Выберите проект и переключитесь на ветку:

```bash
git checkout feature/task-manager-eisenhower
cd feature/task-manager-eisenhower
```

### 2. Настройте переменные окружения:

```bash
cp .env.example .env
nano .env  # Заполните ключи API
```

### 3. Запустите через Docker:

```bash
docker-compose up --build
```

### 4. Откройте в браузере:

```
http://localhost
```

---

## 📋 Чек-лист готовности проекта

Проект считается завершённым, когда все пункты выполнены:

- [ ] Создана ветка `feature/[название-идеи]`
- [ ] Все Docker-файлы созданы и работают
- [ ] `docker-compose up --build` запускается без ошибок
- [ ] Nginx корректно проксирует на app и раздаёт статику
- [ ] `.env.example` содержит все необходимые переменные
- [ ] `README.md` с инструкцией по запуску и деплою
- [ ] GitHub Actions настроен на авто-деплой
- [ ] Бот отвечает на команды
- [ ] Веб-интерфейс отображает данные из базы
- [ ] `.env` НЕ закоммичен в Git

---

## 🔒 Безопасность

Все проекты следуют единым стандартам безопасности:

- ✅ Анонимизация данных перед отправкой в облачный LLM
- ✅ Docker-контейнеры запускаются от non-root пользователя
- ✅ Изолированная сеть для внутренних сервисов
- ✅ Rate limiting в Nginx
- ✅ CORS настроен корректно
- ✅ SQL-инъекции исключены (SQLAlchemy ORM)
- ✅ Все секреты только через переменные окружения

---

## 🌐 Деплой

### На VPS с Docker:

```bash
# 1. Скопируйте файлы на сервер
scp -r feature/task-manager-eisenhower user@server:/opt/

# 2. На сервере
cd /opt/task-manager-eisenhower
cp .env.example .env
nano .env  # заполните ключи

# 3. Запустите
docker-compose up -d
```

### На Render/Railway:

1. Подключите репозиторий к GitHub
2. Укажите путь к `docker-compose.yml`
3. Добавьте переменные окружения из `.env.example`
4. Deploy автоматически при push

---

## 🤝 Добавление нового проекта

Чтобы добавить новую идею:

1. Создайте ветку: `git checkout -b feature/new-project-name`
2. Следуйте структуре существующих проектов
3. Реализуйте MVP согласно требованиям
4. Пройдите чек-лист готовности
5. Создайте Pull Request в main (опционально)

---

## 📞 Поддержка

Вопросы и предложения приветствуются! Создавайте Issues в репозитории.

---

**Сделано с ❤️ для эффективной разработки**
