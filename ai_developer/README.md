# AI Developer

A lightweight, production-ready web application for AI-powered code generation and Docker project management.

## Features

- 🤖 **AI-Powered Code Generation** - Uses Google Gemini to analyze project ideas and generate code
- 🐳 **Docker Integration** - Automatic Dockerfile and container management for projects
- 💬 **Web Interface** - Clean, responsive UI built with Flask
- 📱 **Telegram Bot** - Create and manage projects via Telegram
- 🔒 **Security** - Password hashing (bcrypt), brute-force protection, API key redaction
- 📊 **Project Management** - Start, stop, delete, and monitor Docker containers
- 💾 **Lightweight Database** - SQLite with automatic migrations

## Architecture

```
ai_developer/
├── app/
│   ├── __init__.py          # Main application factory
│   ├── routes/              # Flask blueprints
│   │   ├── auth.py          # Authentication routes
│   │   ├── dashboard.py     # Dashboard and project routes
│   │   └── api.py           # API endpoints
│   ├── services/            # Business logic
│   │   ├── database.py      # SQLite database manager
│   │   ├── gemini_client.py # Google Gemini AI client
│   │   ├── docker_service.py# Docker management
│   │   ├── task_manager.py  # Async task handling
│   │   └── telegram_bot.py  # Telegram bot integration
│   ├── models/              # Data models
│   │   └── auth.py          # Authentication helpers
│   └── templates/           # HTML templates
├── nginx/                   # Nginx configuration
├── data/                    # SQLite database storage
├── logs/                    # Application logs
├── projects/                # Generated project files
├── Dockerfile
├── docker-compose.yml
└── requirements.txt
```

## Quick Start

### Prerequisites

- Docker and Docker Compose
- Google Gemini API key (optional, for AI features)
- Telegram Bot token (optional, for bot integration)

### Installation

1. **Clone the repository**
   ```bash
   cd ai_developer
   ```

2. **Configure environment variables**
   ```bash
   cp .env.example .env
   # Edit .env with your settings
   ```

3. **Start with Docker Compose**
   ```bash
   docker compose up --build
   ```

4. **Access the application**
   - Web interface: http://localhost
   - Health check: http://localhost/health

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SECRET_KEY` | Flask secret key | Random |
| `GEMINI_API_KEY` | Google Gemini API key | - |
| `TELEGRAM_BOT_TOKEN` | Telegram bot token | - |
| `NGINX_PORT` | External port for nginx | 80 |
| `DATABASE_PATH` | SQLite database path | ./data/ai_developer.db |

### Getting API Keys

1. **Google Gemini**: Visit [Google AI Studio](https://makersuite.google.com/app/apikey)
2. **Telegram Bot**: Message [@BotFather](https://t.me/botfather) on Telegram

## Usage

### Web Interface

1. Register a new account
2. Create a project by describing your idea
3. AI analyzes and generates the project structure
4. Start the project to run it in a Docker container
5. Chat with AI about your project

### Telegram Bot

```
/start - Welcome message
/newproject - Create a new project
/projects - List your projects
/help - Show help
```

Send your project idea after `/newproject` and the bot will:
1. Analyze the idea with AI
2. Generate code and Dockerfiles
3. Build and run the container
4. Send you the access link

## API Endpoints

- `GET /health` - Health check
- `GET /api/system/status` - System status (auth required)
- `POST /api/system/prune` - Prune Docker resources (auth required)
- `GET /api/task/<id>` - Get async task status

## Security Features

- **Password Hashing**: bcrypt with salt rounds
- **Brute-force Protection**: Login attempt tracking
- **API Key Redaction**: Keys hidden in logs as `[REDACTED]`
- **Session Management**: Secure Flask sessions
- **Rate Limiting**: Nginx rate limiting on API endpoints
- **Security Headers**: X-Frame-Options, CSP, etc.

## Resource Optimization

Designed for low-resource environments (2GB RAM, 1vCPU):

- Lightweight Flask instead of heavy frameworks
- SQLite instead of PostgreSQL/MySQL
- Alpine-based Docker images
- Efficient connection pooling
- Log rotation to prevent disk fill

## Development

### Running without Docker

```bash
pip install -r requirements.txt
export GEMINI_API_KEY=your_key
python app/__init__.py
```

### Testing

```bash
# Run tests (when implemented)
pytest
```

## Troubleshooting

### Docker Socket Access

If you get Docker permission errors:
```bash
sudo chmod 666 /var/run/docker.sock
```

### Port Conflicts

Change the nginx port in `.env`:
```
NGINX_PORT=8080
```

### API Key Issues

Check logs for `[REDACTED]` - keys are never exposed:
```bash
docker compose logs app | grep -i error
```

## License

MIT License

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## Support

For issues and feature requests, please open an issue on GitHub.
