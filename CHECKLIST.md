# ✅ ЧЕК-ЛИСТ ПРОВЕРКИ AI CODE FACTORY

## Шаг 1: Проверка структуры проекта

- [ ] Все файлы созданы в правильных директориях
- [ ] `.env.example` содержит все необходимые переменные
- [ ] `.gitignore` исключает `.env` и временные файлы
- [ ] `docker-compose.yml` корректно настроен
- [ ] Dockerfile для app и nginx созданы
- [ ] Frontend (index.html) существует
- [ ] Исходный код Python в src/

## Шаг 2: Docker проверка

### 2.1 Валидация docker-compose
```bash
docker-compose config
```
- [ ] Конфигурация валидна, ошибок нет

### 2.2 Сборка образов
```bash
docker-compose build
```
- [ ] Образ nginx собрался без ошибок
- [ ] Образ app собрался без ошибок
- [ ] Образ postgres скачан

### 2.3 Запуск контейнеров
```bash
docker-compose up -d
```
- [ ] Все контейнеры запустились
- [ ] Контейнер db в статусе "healthy"
- [ ] Контейнер app в статусе "Up"
- [ ] Контейнер nginx в статусе "Up"

```bash
docker-compose ps
```

## Шаг 3: Проверка API

### 3.1 Health check
```bash
curl http://localhost/health
```
Ожидаемый ответ:
```json
{"status": "ok", "timestamp": "..."}
```
- [ ] Health check возвращает 200 OK

### 3.2 Создание тестового проекта
```bash
curl -X POST http://localhost/api/start-project \
  -H "Content-Type: application/json" \
  -d '{"idea": "Тестовое приложение", "user_id": "test123"}'
```
- [ ] Возвращается project_id
- [ ] Возвращается magic_link
- [ ] Статус "generating" или "pending"

### 3.3 Проверка статуса проекта
```bash
curl http://localhost/api/project/{project_id}
```
- [ ] Возвращается информация о проекте
- [ ] Видны шаги генерации

## Шаг 4: Проверка веб-интерфейса

### 4.1 Доступ к сайту
```bash
curl http://localhost/
```
- [ ] Возвращается HTML страница
- [ ] Заголовок "AI Code Factory" присутствует

### 4.2 Визуальная проверка
Откройте http://localhost в браузере:
- [ ] Страница отображается корректно
- [ ] Форма ввода идеи видна
- [ ] Кнопка "Создать приложение" активна
- [ ] При вводе идеи и нажатии кнопки начинается генерация
- [ ] Отображаются шаги генерации
- [ ] При успехе показываются ссылки на Render и GitHub

## Шаг 5: Проверка Telegram бота

### 5.1 Запуск бота
```bash
docker-compose logs bot
```
- [ ] Бот запустился без ошибок
- [ ] Видно "Starting polling..."

### 5.2 Тест команд
В Telegram:
- [ ] `/start` - отвечает приветствием
- [ ] `/help` - отвечает справкой
- [ ] `/status` - отвечает статусом (или что нет проектов)

### 5.3 Тест создания проекта
- [ ] Отправка идеи боту создаёт проект
- [ ] Бот присылает confirmation с project_id
- [ ] Бот уведомляет о завершении генерации

## Шаг 6: Проверка базы данных

### 6.1 Подключение к PostgreSQL
```bash
docker-compose exec db psql -U postgres -d factory_db
```

### 6.2 Проверка таблиц
```sql
\dt
```
- [ ] Таблица projects существует
- [ ] Таблица generation_steps существует
- [ ] Таблица users существует

### 6.3 Проверка данных
```sql
SELECT id, idea, status, created_at FROM projects ORDER BY created_at DESC LIMIT 5;
```
- [ ] Проекты сохраняются в БД

## Шаг 7: Проверка безопасности

### 7.1 Non-root пользователь
```bash
docker-compose exec app whoami
```
- [ ] Возвращает "appuser" а не "root"

### 7.2 Изолированная сеть
```bash
docker network inspect workspace_internal_net
```
- [ ] Только nginx имеет доступ наружу
- [ ] App и db внутри сети

### 7.3 .env не закоммичен
```bash
git ls-files | grep ".env"
```
- [ ] В выводе только `.env.example`, `.env` отсутствует

## Шаг 8: Проверка GitHub Actions

### 8.1 Валидация workflow
```bash
cd /workspace
cat .github/workflows/deploy.yml
```
- [ ] Workflow файл существует
- [ ] Синтаксис YAML корректен

### 8.2 Push в GitHub
- [ ] Репозиторий создан на GitHub
- [ ] Код запушен в ветку main
- [ ] GitHub Actions запустился автоматически

### 8.3 Проверка Secrets
- [ ] RENDER_API_KEY добавлен в Secrets
- [ ] TELEGRAM_BOT_TOKEN добавлен в Secrets
- [ ] GITHUB_TOKEN добавлен в Secrets
- [ ] OPENROUTER_API_KEY добавлен в Secrets

## Шаг 9: Проверка интеграций

### 9.1 OpenRouter (LLM)
- [ ] API ключ настроен
- [ ] Запросы к LLM работают (проверить по логам)

### 9.2 GitHub API
- [ ] Token имеет права repo и workflow
- [ ] Создание репозиториев работает

### 9.3 Render API
- [ ] API ключ настроен
- [ ] Создание сервисов работает

## Шаг 10: Финальные тесты

### 10.1 End-to-End тест
1. Откройте веб-интерфейс
2. Введите идею: "Простой TODO лист с базой данных"
3. Нажмите "Создать приложение"
4. Дождитесь завершения генерации
5. Проверьте что получили ссылку на Render

- [ ] Проект создался
- [ ] Код сгенерировался
- [ ] Репозиторий создан на GitHub
- [ ] Сервис создан на Render
- [ ] Магическая ссылка работает

### 10.2 Проверка магической ссылки
- [ ] Ссылка вида `/app/{hash}` открывается
- [ ] Показывает статус проекта
- [ ] После деплоя показывает Render URL

### 10.3 Проверка админки
```bash
curl -u admin:admin http://localhost/api/projects
```
- [ ] Возвращается список проектов
- [ ] Без авторизации возвращает 401

## 🐛 Troubleshooting

### Контейнер app не запускается
```bash
docker-compose logs app
```
Частые ошибки:
- Нет подключения к БД → проверьте DATABASE_URL
- Ошибка импорта → проверьте requirements.txt
- Порт занят → измените APP_PORT

### Nginx не проксирует на app
```bash
docker-compose logs nginx
curl -v http://localhost/health
```
Проверьте:
- App слушает порт 8000
- nginx.conf корректен

### Бот не отвечает
```bash
docker-compose logs -f bot
```
Проверьте:
- TELEGRAM_BOT_TOKEN правильный
- Бот не заблокирован

### Ошибки LLM
```bash
docker-compose logs app | grep -i "llm\|openrouter"
```
Проверьте:
- OPENROUTER_API_KEY настроен
- Есть баланс на счету

---

## 📊 Критерии готовности

Проект считается полностью готовым когда:

✅ Все пункты чек-листа отмечены
✅ Docker-compose up запускается без ошибок
✅ Веб-интерфейс доступен и работает
✅ Бот отвечает на команды
✅ API возвращает корректные данные
✅ GitHub Actions деплоит при push
✅ .env не закоммичен в Git
✅ Магические ссылки работают 24 часа

---

**Поздравляем!** 🎉 Ваша AI Code Factory полностью готова к работе!