# Network Module - Сетевой слой приложения

## Назначение
Модуль `core:network` предоставляет инфраструктуру для работы с REST API бэкендом.

## Архитектура

```
com.messenger.core.network/
├── config/
│   └── NetworkConfig.kt          # Конфигурация (URL, таймауты, retry)
├── client/
│   └── NetworkClientFactory.kt   # Фабрика Retrofit/OkHttpClient
├── interceptor/
│   ├── RetryInterceptor.kt       # Повторные попытки с exponential backoff
│   ├── LoggingInterceptor.kt     # Логирование с фильтрацией чувствительных данных
│   └── AuthInterceptor.kt        # Добавление токенов авторизации
├── adapter/
│   └── ApiCallAdapterFactory.kt  # CallAdapter для обработки ошибок
├── model/
│   └── ApiResponse.kt            # Базовые модели ответов API
└── di/
    └── NetworkModule.kt          # Hilt модуль для DI
```

## Как подключить свой REST API

### Шаг 1: Настройка базового URL

Откройте `config/NetworkConfig.kt` и измените `BASE_URL`:

```kotlin
object NetworkConfig {
    const val BASE_URL = "https://api.your-backend.com/"
    // Остальные настройки...
}
```

Или используйте разные URL для разных окружений через BuildConfig:

```kotlin
object NetworkConfig {
    const val BASE_URL = BuildConfig.API_BASE_URL
}
```

### Шаг 2: Реализация TokenProvider

Создайте реализацию интерфейса `TokenProvider` в вашем проекте:

```kotlin
@Singleton
class AuthTokenProvider @Inject constructor(
    private val dataStore: UserDataStore
) : TokenProvider {
    
    override fun getAccessToken(): String? {
        return runBlocking { dataStore.accessToken.first() }
    }
    
    override fun getRefreshToken(): String? {
        return runBlocking { dataStore.refreshToken.first() }
    }
    
    override suspend fun refreshAccessToken(): String? {
        val refreshToken = getRefreshToken() ?: return null
        // Вызов API для обновления токена
        val response = authApi.refreshToken(RefreshTokenRequest(refreshToken))
        return response.data?.accessToken.also { 
            dataStore.saveTokens(it, response.data?.refreshToken)
        }
    }
    
    override fun clearTokens() {
        runBlocking { dataStore.clearTokens() }
    }
}
```

Зарегистрируйте в Hilt модуле:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {
    
    @Binds
    @Singleton
    abstract fun bindTokenProvider(impl: AuthTokenProvider): TokenProvider
}
```

### Шаг 3: Создание API интерфейсов

Создайте интерфейсы для вашего API в модуле `data`:

```kotlin
interface AuthApi {
    
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<AuthResponse>
    
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiResponse<UserResponse>
    
    @POST("auth/logout")
    suspend fun logout(): ApiResponse<Unit>
    
    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): ApiResponse<AuthResponse>
}

interface ChatApi {
    
    @GET("chats")
    suspend fun getChats(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50
    ): ApiResponse<PagedResponse<Chat>>
    
    @GET("chats/{chatId}")
    suspend fun getChat(@Path("chatId") chatId: String): ApiResponse<Chat>
    
    @GET("chats/{chatId}/messages")
    suspend fun getMessages(
        @Path("chatId") chatId: String,
        @Query("before") before: Long? = null,
        @Query("after") after: Long? = null,
        @Query("limit") limit: Int = 50
    ): ApiResponse<List<Message>>
    
    @POST("chats/{chatId}/messages")
    suspend fun sendMessage(
        @Path("chatId") chatId: String,
        @Body request: SendMessageRequest
    ): ApiResponse<Message>
}

interface MediaApi {
    
    @Multipart
    @POST("media/upload")
    suspend fun uploadMedia(
        @Part file: MultipartBody.Part,
        @Part("type") type: RequestBody,
        @Part("chat_id") chatId: RequestBody?
    ): ApiResponse<MediaUploadResponse>
    
    @POST("media/upload/chunked")
    suspend fun uploadChunked(
        @Body request: ChunkedUploadRequest
    ): ApiResponse<MediaUploadResponse>
}
```

### Шаг 4: Регистрация API сервисов в Hilt

Добавьте методы предоставления в `NetworkModule` или создайте отдельный модуль:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object ApiServicesModule {
    
    @Provides
    @Singleton
    fun provideAuthApi(@Named("default") retrofit: Retrofit): AuthApi {
        return retrofit.create(AuthApi::class.java)
    }
    
    @Provides
    @Singleton
    fun provideChatApi(@Named("default") retrofit: Retrofit): ChatApi {
        return retrofit.create(ChatApi::class.java)
    }
    
    @Provides
    @Singleton
    fun provideMediaApi(@Named("default") retrofit: Retrofit): MediaApi {
        return retrofit.create(MediaApi::class.java)
    }
}
```

### Шаг 5: Использование в Repository

```kotlin
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val tokenProvider: TokenProvider
) : AuthRepository {
    
    override suspend fun login(email: String, password: String): Result<User> {
        return try {
            val response = authApi.login(LoginRequest(email, password))
            if (response.success && response.data != null) {
                // Сохраняем токены
                tokenProvider.saveTokens(response.data.accessToken, response.data.refreshToken)
                Result.success(response.data.toUser())
            } else {
                Result.failure(ApiException(response.errorCode ?: "UNKNOWN_ERROR"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

## Особенности реализации

### Обработка ошибок сети
- Автоматические повторные попытки при временных ошибках (5xx, 429, таймауты)
- Экспоненциальная задержка между попытками
- Специальная обработка 401 (разлогинивание) и 403 (доступ запрещён)

### Безопасность
- Фильтрация чувствительных данных в логах (токены, пароли)
- Автоматическое добавление Bearer токена
- Поддержка refresh token flow

### Отказоустойчивость
- Graceful degradation при потере связи
- Кэширование ответов (настраивается)
- Idempotency ключи для критичных операций

## Зависимости

Основные зависимости указаны в `build.gradle.kts`:
- Retrofit 2.9.0
- OkHttp 4.12.0
- Kotlinx Serialization 1.6.0
- Hilt 2.48.1

## Тестирование

Для тестирования используйте MockWebServer:

```kotlin
@Test
fun `login returns user on success`() = runTest {
    val mockWebServer = MockWebServer()
    mockWebServer.enqueue(MockResponse().setBody("""{"success": true, "data": {...}}"""))
    
    val api = createTestApi(mockWebServer.url("/").toString())
    val result = api.login(LoginRequest("test@test.com", "password"))
    
    assertTrue(result.success)
}
```
