# Android Messenger - Модульное приложение мессенджера

Production-ready модульное Android-приложение мессенджера (аналог Telegram) на Clean Architecture + MVVM.

## 📋 Технические требования

- **Архитектура**: Clean Architecture (Presentation/Domain/Data), MVVM, Unidirectional Data Flow
- **DI**: Hilt
- **UI**: Jetpack Compose Material 3
- **Сеть**: Retrofit + kotlinx.serialization
- **Хранение**: Room + DataStore
- **Минимальная версия**: Android 8.0 (API 26)

## 🏗️ Структура проекта

```
Messenger/
├── app/                          # Главный модуль приложения
│   ├── src/main/kotlin/com/messenger/
│   │   ├── di/                   # Hilt компоненты приложения
│   │   ├── ui/                   # MainActivity, навигация
│   │   └── navigation/           # Graph навигации
│   └── build.gradle.kts
│
├── core/                         # Core модули (общая инфраструктура)
│   ├── network/                  # Сетевой слой (Retrofit, OkHttp)
│   │   ├── config/               # Конфигурация сети
│   │   ├── client/               # Фабрики клиентов
│   │   ├── interceptor/          # Интерцепторы (retry, logging, auth)
│   │   ├── adapter/              # CallAdapter для обработки ошибок
│   │   ├── model/                # Базовые модели API
│   │   └── di/                   # Hilt модуль сети
│   │
│   ├── datastore/                # DataStore для настроек и сессий
│   ├── security/                 # Биометрия, криптография
│   ├── ui/                       # Общие UI компоненты, темы
│   └── common/                   # Утилиты, Result wrapper, логирование
│
├── domain/                       # Domain слой (бизнес-логика)
│   ├── auth/                     # Домен аутентификации
│   │   ├── model/                # Entities: User, Session
│   │   ├── repository/           # Interfaces репозиториев
│   │   ├── usecase/              # Use cases
│   │   └── di/                   # DI домена
│   │
│   ├── chat/                     # Домен чатов
│   ├── contacts/                 # Домен контактов
│   └── media/                    # Домен медиа
│
├── data/                         # Data слой (реализация репозиториев)
│   ├── remote/                   # API сервисы, DTO
│   ├── local/                    # Room DAO, DataStore
│   ├── repository/               # Реализации репозиториев
│   ├── model/                    # DTO, Entity мапперы
│   └── queue/                    # Offline очередь отправки
│
└── feature/                      # Feature модули (UI + логика)
    ├── auth/                     # Аутентификация, регистрация
    ├── chat/                     # Список чатов, экран чата
    ├── contacts/                 # Контакты, поиск
    ├── settings/                 # Настройки, профиль
    └── media/                    # Просмотр медиа, загрузка
```

## 🔌 Подключение своего REST API

### Быстрый старт

1. **Настройте базовый URL** в `core/network/src/main/kotlin/com/messenger/core/network/config/NetworkConfig.kt`:

```kotlin
object NetworkConfig {
    const val BASE_URL = "https://api.your-backend.com/"
    // Остальные настройки...
}
```

2. **Реализуйте `TokenProvider`** для управления токенами:

```kotlin
@Singleton
class AuthTokenProvider @Inject constructor(
    private val dataStore: UserDataStore
) : TokenProvider {
    override fun getAccessToken(): String? = /* ... */
    override fun getRefreshToken(): String? = /* ... */
    override suspend fun refreshAccessToken(): String? = /* ... */
    override fun clearTokens() = /* ... */
}
```

3. **Создайте API интерфейсы** в модуле `data`:

```kotlin
interface AuthApi {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<AuthResponse>
}

interface ChatApi {
    @GET("chats/{chatId}/messages")
    suspend fun getMessages(
        @Path("chatId") chatId: String,
        @Query("limit") limit: Int = 50
    ): ApiResponse<List<Message>>
}
```

4. **Зарегистрируйте API в Hilt**:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object ApiModule {
    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = 
        retrofit.create(AuthApi::class.java)
}
```

📖 **Подробная инструкция** см. в [`core/network/README.md`](core/network/README.md)

## 📦 Модули

| Модуль | Описание |
|--------|----------|
| `app` | Точка входа, навигация, DI граф приложения |
| `core:network` | HTTP клиент, интерцепторы, адаптеры |
| `core:datastore` | Настройки, сессии, тема |
| `core:security` | Биометрия, криптография (готово к E2EE) |
| `core:ui` | Темы, компоненты, навигация |
| `core:common` | Утилиты, Result wrapper, логирование |
| `domain:*` | Бизнес-логика (use cases, entities, repository interfaces) |
| `data` | Реализация репозиториев, Room, offline очередь |
| `feature:*` | UI экраны, ViewModel, навигация |

## 🚀 Ключевые особенности

### Сеть
- ✅ Retrofit + kotlinx.serialization
- ✅ Автоматические retry с exponential backoff
- ✅ Обработка HTTP статусов (401, 403, 5xx)
- ✅ Логирование с фильтрацией чувствительных данных
- ✅ Chunked upload для больших файлов
- ✅ Idempotency ключи

### Хранение
- ✅ Room с реактивными Flow
- ✅ DataStore для настроек
- ✅ Offline очередь отправки сообщений
- ✅ Гарантия доставки при восстановлении связи

### UI
- ✅ Jetpack Compose Material 3
- ✅ Одно Activity + Navigation Compose
- ✅ Deep links для чатов
- ✅ Системная тема + ручной переключатель
- ✅ Кастомная палитра цветов

### Безопасность
- ✅ BiometricPrompt с CryptoObject
- ✅ Готовность к E2EE
- ✅ Безопасное хранение токенов

### Отказоустойчивость
- ✅ Graceful degradation
- ✅ Кэширование ответов
- ✅ Работа при разрывах сети
- ✅ Готовность к блокировкам

## 🛠️ Сборка

```bash
# Debug сборка
./gradlew assembleDebug

# Release сборка
./gradlew assembleRelease

# Запуск тестов
./gradlew test

# Проверка кода
./gradlew detekt
```

## 📝 Лицензия

MIT License
