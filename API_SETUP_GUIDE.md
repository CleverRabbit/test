# 📱 Android Messenger — Инструкция по подключению REST API

## Быстрый старт

Этот проект предоставляет готовую архитектуру для подключения вашего REST API бэкенда. Следуйте шагам ниже.

---

## 🔧 Шаг 1: Настройка сетевого модуля

### 1.1 Измените базовый URL

Откройте файл `core/network/src/main/kotlin/com/messenger/core/network/config/NetworkConfig.kt`:

```kotlin
object NetworkConfig {
    const val BASE_URL = "https://api.your-backend.com/"
    
    // Опционально: настройте таймауты
    const val CONNECT_TIMEOUT_MS = 30_000L
    const val READ_TIMEOUT_MS = 60_000L
    const val WRITE_TIMEOUT_MS = 60_000L
    
    // Опционально: настройте retry
    const val MAX_RETRY_COUNT = 3
}
```

**Альтернатива через BuildConfig:**

В `app/build.gradle.kts` уже настроены поля:
```kotlin
buildConfigField("String", "API_BASE_URL", "\"https://api.your-messenger.com/\"")
```

Измените URL для разных окружений (debug/staging/release).

---

## 🔐 Шаг 2: Реализация TokenProvider

Создайте файл в `data/src/main/kotlin/com/messenger/data/repository/AuthTokenProvider.kt`:

```kotlin
package com.messenger.data.repository

import com.messenger.core.network.interceptor.TokenProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthTokenProvider @Inject constructor(
    private val userDataStore: UserDataStore // Ваш DataStore
) : TokenProvider {
    
    override fun getAccessToken(): String? {
        return runBlocking { userDataStore.accessToken.first() }
    }
    
    override fun getRefreshToken(): String? {
        return runBlocking { userDataStore.refreshToken.first() }
    }
    
    override suspend fun refreshAccessToken(): String? {
        val refreshToken = getRefreshToken() ?: return null
        
        // Вызов API для обновления токена
        // val response = authApi.refreshToken(refreshToken)
        // val newToken = response.data?.accessToken
        
        // Сохранение новых токенов
        // userDataStore.saveTokens(newToken, response.data?.refreshToken)
        
        return null // Замените на реальную реализацию
    }
    
    override fun clearTokens() {
        runBlocking { userDataStore.clearTokens() }
    }
}
```

Зарегистрируйте в Hilt модуле (`data/src/main/kotlin/com/messenger/data/di/AuthModule.kt`):

```kotlin
package com.messenger.data.di

import com.messenger.core.network.interceptor.TokenProvider
import com.messenger.data.repository.AuthTokenProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {
    
    @Binds
    @Singleton
    abstract fun bindTokenProvider(impl: AuthTokenProvider): TokenProvider
}
```

---

## 🌐 Шаг 3: Создание API интерфейсов

### 3.1 Auth API

Создайте `data/src/main/kotlin/com/messenger/data/remote/AuthApi.kt`:

```kotlin
package com.messenger.data.remote

import com.messenger.core.network.model.ApiResponse
import retrofit2.http.Body
import retrofit2.http.POST

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
```

### 3.2 Chat API

Создайте `data/src/main/kotlin/com/messenger/data/remote/ChatApi.kt`:

```kotlin
package com.messenger.data.remote

import com.messenger.core.network.model.ApiResponse
import com.messenger.core.network.model.PagedResponse
import retrofit2.http.*

interface ChatApi {
    
    @GET("chats")
    suspend fun getChats(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50
    ): ApiResponse<PagedResponse<ChatDto>>
    
    @GET("chats/{chatId}")
    suspend fun getChat(@Path("chatId") chatId: String): ApiResponse<ChatDto>
    
    @GET("chats/{chatId}/messages")
    suspend fun getMessages(
        @Path("chatId") chatId: String,
        @Query("before") before: Long? = null,
        @Query("after") after: Long? = null,
        @Query("limit") limit: Int = 50
    ): ApiResponse<List<MessageDto>>
    
    @POST("chats/{chatId}/messages")
    suspend fun sendMessage(
        @Path("chatId") chatId: String,
        @Body request: SendMessageRequest
    ): ApiResponse<MessageDto>
    
    @DELETE("chats/{chatId}/messages/{messageId}")
    suspend fun deleteMessage(
        @Path("chatId") chatId: String,
        @Path("messageId") messageId: String
    ): ApiResponse<Unit>
}
```

### 3.3 Media API (для загрузки файлов)

Создайте `data/src/main/kotlin/com/messenger/data/remote/MediaApi.kt`:

```kotlin
package com.messenger.data.remote

import com.messenger.core.network.model.ApiResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface MediaApi {
    
    @Multipart
    @POST("media/upload")
    suspend fun uploadMedia(
        @Part file: MultipartBody.Part,
        @Part("type") type: RequestBody,
        @Part("chat_id") chatId: RequestBody? = null
    ): ApiResponse<MediaUploadResponse>
    
    @POST("media/upload/chunked/init")
    suspend fun initChunkedUpload(@Body request: ChunkedUploadInitRequest): ApiResponse<UploadSession>
    
    @Multipart
    @POST("media/upload/chunked/{sessionId}")
    suspend fun uploadChunk(
        @Path("sessionId") sessionId: String,
        @Part chunk: MultipartBody.Part,
        @Part("chunk_index") chunkIndex: RequestBody
    ): ApiResponse<ChunkUploadResponse>
    
    @POST("media/upload/chunked/{sessionId}/complete")
    suspend fun completeChunkedUpload(
        @Path("sessionId") sessionId: String,
        @Body request: CompleteUploadRequest
    ): ApiResponse<MediaUploadResponse>
}
```

---

## 💉 Шаг 4: Регистрация API в Hilt

Создайте `data/src/main/kotlin/com/messenger/data/di/ApiModule.kt`:

```kotlin
package com.messenger.data.di

import com.messenger.data.remote.AuthApi
import com.messenger.data.remote.ChatApi
import com.messenger.data.remote.MediaApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {
    
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

---

## 📦 Шаг 5: Создание DTO моделей

Создайте файлы DTO в `data/src/main/kotlin/com/messenger/data/model/`:

```kotlin
// LoginRequest.kt
package com.messenger.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    @SerialName("email") val email: String,
    @SerialName("password") val password: String
)

@Serializable
data class AuthResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("user") val user: UserDto
)

@Serializable
data class UserDto(
    @SerialName("id") val id: String,
    @SerialName("email") val email: String,
    @SerialName("name") val name: String,
    @SerialName("avatar_url") val avatarUrl: String?
)
```

---

## 🔄 Шаг 6: Реализация Repository

Создайте `data/src/main/kotlin/com/messenger/data/repository/AuthRepositoryImpl.kt`:

```kotlin
package com.messenger.data.repository

import com.messenger.core.common.result.Result
import com.messenger.data.mapper.toUser
import com.messenger.data.model.LoginRequest
import com.messenger.data.remote.AuthApi
import com.messenger.domain.auth.repository.AuthRepository
import com.messenger.domain.auth.model.User
import javax.inject.Inject
import javax.inject.Singleton

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
                tokenProvider.saveTokens(
                    response.data.accessToken,
                    response.data.refreshToken
                )
                
                Result.success(response.data.user.toUser())
            } else {
                Result.error(
                    exception = Exception(response.errorCode ?: "UNKNOWN_ERROR"),
                    code = response.errorCode
                )
            }
        } catch (e: Exception) {
            Result.error(e)
        }
    }
    
    override suspend fun logout(): Result<Unit> {
        return try {
            val response = authApi.logout()
            if (response.success) {
                tokenProvider.clearTokens()
                Result.success(Unit)
            } else {
                Result.error(Exception(response.errorCode ?: "LOGOUT_FAILED"))
            }
        } catch (e: Exception) {
            Result.error(e)
        }
    }
}
```

---

## ✅ Проверка подключения

1. **Запустите приложение**
2. **Проверьте логи** — должны появиться сообщения от `HttpLogger_Request` и `HttpLogger_Response`
3. **Проверьте обработку ошибок** — отключите сеть и убедитесь, что retry-механизм работает

---

## 📚 Дополнительные материалы

- [`core/network/README.md`](core/network/README.md) — подробная документация сетевого модуля
- [`core/common/src/main/kotlin/com/messenger/core/common/result/Result.kt`](core/common/src/main/kotlin/com/messenger/core/common/result/Result.kt) — Result wrapper
- [`core/common/src/main/kotlin/com/messenger/core/common/logging/AppLogger.kt`](core/common/src/main/kotlin/com/messenger/core/common/logging/AppLogger.kt) — система логирования

---

## 🆘 Troubleshooting

| Проблема | Решение |
|----------|---------|
| 401 Unauthorized | Проверьте реализацию TokenProvider |
| Таймауты | Увеличьте таймауты в NetworkConfig |
| Ошибки сериализации | Проверьте соответствие DTO структуре ответа API |
| Нет логов | Убедитесь, что `AppLogger.enabled = true` |

---

**Готово!** Ваше приложение теперь подключено к вашему REST API.
