#!/bin/bash
# AI Developer - Скрипт установки и запуска

set -e

echo "=========================================="
echo "AI Developer - Установка"
echo "=========================================="

# Проверка прав root
if [ "$EUID" -ne 0 ]; then 
    echo "Пожалуйста, запустите от root (sudo ./install.sh)"
    exit 1
fi

# Обновление пакетов
echo "[1/6] Обновление пакетов..."
apt update -qq

# Установка системных зависимостей
echo "[2/6] Установка системных зависимостей..."
apt install -y -qq python3-pip python3-venv docker.io nginx git curl > /dev/null 2>&1

# Установка Python зависимостей
echo "[3/6] Установка Python зависимостей..."
pip3 install --quiet flask requests python-dotenv

# Создание директорий
echo "[4/6] Создание директорий..."
mkdir -p /workspace/projects
mkdir -p /workspace/templates
mkdir -p /workspace/static

# Копирование .env если не существует
if [ ! -f /workspace/.env ]; then
    echo "[5/6] Создание файла конфигурации..."
    cp /workspace/.env.example /workspace/.env
    echo ""
    echo "⚠️  ВАЖНО: Отредактируйте /workspace/.env и укажите ваш GEMINI_API_KEY"
    echo ""
fi

# Настройка NGINX
echo "[6/6] Настройка NGINX..."
cat > /etc/nginx/sites-available/ai-developer << 'NGINX_EOF'
server {
    listen 80;
    server_name _;
    
    location / {
        proxy_pass http://127.0.0.1:5000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
NGINX_EOF

ln -sf /etc/nginx/sites-available/ai-developer /etc/nginx/sites-enabled/
rm -f /etc/nginx/sites-enabled/default

# Перезапуск NGINX
systemctl restart nginx 2>/dev/null || service nginx restart 2>/dev/null || true

echo ""
echo "=========================================="
echo "✅ Установка завершена!"
echo "=========================================="
echo ""
echo "Следующие шаги:"
echo "1. Отредактируйте /workspace/.env и укажите GEMINI_API_KEY"
echo "2. Запустите приложение: cd /workspace && python3 app.py"
echo "3. Откройте в браузере: http://localhost:5000"
echo ""
echo "Или через NGINX (порт 80): http://localhost/"
echo ""
