package com.messenger.core.network.api

import com.messenger.core.common.result.Result
import com.messenger.core.network.interceptor.AuthInterceptor
import com.messenger.core.network.interceptor.HttpInterceptor
import com.messenger.core.network.interceptor.NetworkAvailabilityInterceptor
import com.messenger.core.network.adapter.ResultCallAdapterFactory
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Конфигурация сетевого клиента.
 * 
 * ИНСТРУКЦИЯ ПО ПОДКЛЮЧЕНИЮ СВОЕГО REST API:
 * 
 * 1. Измените BASE_URL на адрес вашего сервера:
 *    const val BASE_URL = "https://api.yourserver.com/"
 * 
 * 2. Добавьте свои API интерфейсы в Retrofit.Builder:
 *    .addApi<YourAuthService>()
 *    .addApi<YourChatService>()
 * 
 * 3. Настройте AuthInterceptor для работы с вашей схемой авторизации:
 *    - Измените путь к эндпоинтам авторизации
 *    - Настройте формат токена (Bearer, Basic, etc.)
 * 
 * 4. При необходимости добавьте дополнительные Interceptor:
 *    - LoggingInterceptor для отладки
 *    - CustomHeaderInterceptor для специфичных заголовков
 * 
 * 5. Для работы с offline-очередью используйте WorkManager или Service
 */
object NetworkConfig {
    
    /**
     * Базовый URL вашего API сервера.
     * ЗАМЕНИТЕ НА СВОЙ АДРЕС!
     */
    const val BASE_URL = "https://api.messenger.com/"
    
    /**
     * Таймауты подключения.
     */
    const val CONNECT_TIMEOUT_MS = 15_000L
    const val READ_TIMEOUT_MS = 30_000L
    const val WRITE_TIMEOUT_MS = 30_000L
    
    /**
     * Настройки JSON сериализации.
     */
    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }
}

/**
 * Builder для создания Retrofit экземпляра.
 */
class RetrofitBuilder private constructor() {
    
    private var baseUrl: String = NetworkConfig.BASE_URL
    private var okHttpClient: OkHttpClient? = null
    private val apiServices = mutableMapOf<Class<*>, Any>()
    
    companion object {
        fun create(): RetrofitBuilder = RetrofitBuilder()
    }
    
    /**
     * Установка базового URL.
     */
    fun baseUrl(url: String): RetrofitBuilder {
        this.baseUrl = url
        return this
    }
    
    /**
     * Настройка OkHttpClient.
     */
    fun client(client: OkHttpClient): RetrofitBuilder {
        this.okHttpClient = client
        return this
    }
    
    /**
     * Создание OkHttpClient с интерсепторами по умолчанию.
     * 
     * @param tokenProvider функция для получения токена авторизации
     * @param isNetworkAvailable функция проверки доступности сети
     * @param enableLogging включение логирования HTTP запросов
     */
    fun defaultClient(
        tokenProvider: () -> String? = { null },
        isNetworkAvailable: () -> Boolean = { true },
        enableLogging: Boolean = BuildConfig.DEBUG
    ): RetrofitBuilder {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (enableLogging) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        
        this.okHttpClient = OkHttpClient.Builder()
            .connectTimeout(NetworkConfig.CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .readTimeout(NetworkConfig.READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .writeTimeout(NetworkConfig.WRITE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .addInterceptor(NetworkAvailabilityInterceptor(isNetworkAvailable))
            .addInterceptor(HttpInterceptor())
            .addInterceptor(AuthInterceptor(tokenProvider))
            .addInterceptor(loggingInterceptor)
            .build()
        
        return this
    }
    
    /**
     * Построение Retrofit и создание API сервиса.
     */
    inline fun <reified T> build(): T {
        val client = okHttpClient ?: defaultClient().okHttpClient!!
        
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(NetworkConfig.json.asConverterFactory("application/json".toMediaType()))
            .addCallAdapterFactory(ResultCallAdapterFactory.create())
            .build()
        
        return retrofit.create(T::class.java)
    }
    
    /**
     * Создание нескольких API сервисов одновременно.
     */
    fun buildMultiple(vararg services: Class<*>): Map<Class<*>, Any> {
        val client = okHttpClient ?: defaultClient().okHttpClient!!
        
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(NetworkConfig.json.asConverterFactory("application/json".toMediaType()))
            .addCallAdapterFactory(ResultCallAdapterFactory.create())
            .build()
        
        return services.associateWith { service ->
            retrofit.create(service)
        }
    }
}

/**
 * Extension функция для удобного создания API.
 */
inline fun <reified T> createApi(
    baseUrl: String = NetworkConfig.BASE_URL,
    tokenProvider: () -> String? = { null },
    isNetworkAvailable: () -> Boolean = { true }
): T {
    return RetrofitBuilder.create()
        .baseUrl(baseUrl)
        .defaultClient(tokenProvider, isNetworkAvailable)
        .build()
}

/**
 * Пример API интерфейса для аутентификации.
 * Скопируйте и адаптируйте под свой backend.
 */
interface AuthService {
    // @POST("auth/login")
    // suspend fun login(@Body request: LoginRequest): Result<ApiResponse<LoginResponse>>
    
    // @POST("auth/register")
    // suspend fun register(@Body request: RegisterRequest): Result<ApiResponse<User>>
    
    // @POST("auth/refresh")
    // suspend fun refreshToken(@Body request: RefreshTokenRequest): Result<ApiResponse<TokenResponse>>
    
    // @POST("auth/logout")
    // suspend fun logout(): Result<ApiResponse<Unit>>
}

/**
 * Пример API интерфейса для чатов.
 */
interface ChatService {
    // @GET("chats")
    // suspend fun getChats(
    //     @Query("page") page: Int,
    //     @Query("limit") limit: Int
    // ): Result<ApiResponse<List<Chat>>>
    
    // @GET("chats/{chatId}/messages")
    // suspend fun getMessages(
    //     @Path("chatId") chatId: String,
    //     @Query("before") before: Long?
    // ): Result<ApiResponse<List<Message>>>
    
    // @POST("chats/{chatId}/messages")
    // suspend fun sendMessage(
    //     @Path("chatId") chatId: String,
    //     @Body request: SendMessageRequest
    // ): Result<ApiResponse<Message>>
}

/**
 * Пример API интерфейса для медиа.
 */
interface MediaService {
    // @Multipart
    // @POST("media/upload")
    // suspend fun uploadMedia(
    //     @Part file: MultipartBody.Part,
    //     @Part("type") type: RequestBody
    // ): Result<ApiResponse<MediaUploadResponse>>
    
    // @Streaming
    // @GET("media/{mediaId}")
    // suspend fun downloadMedia(@Path("mediaId") mediaId: String): Result<ResponseBody>
}
