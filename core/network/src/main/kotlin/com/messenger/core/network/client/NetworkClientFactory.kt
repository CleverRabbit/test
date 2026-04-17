package com.messenger.core.network.client

import android.content.Context
import com.messenger.core.network.config.NetworkConfig
import com.messenger.core.network.interceptor.AuthInterceptor
import com.messenger.core.network.interceptor.LoggingInterceptor
import com.messenger.core.network.interceptor.RetryInterceptor
import com.messenger.core.network.interceptor.TokenProvider
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory as serializationConverterFactory

/**
 * Фабрика для создания Retrofit инстансов.
 * 
 * Предоставляет настроенный OkHttpClient и Retrofit с:
 * - Таймаутами подключения/чтения/записи
 * - Интерцепторами для логирования, авторизации и retry-логики
 * - Сериализацией Kotlinx Serialization
 * - Обработкой ошибок сети
 *
 * @param context Контекст приложения.
 * @param tokenProvider Поставщик токенов для авторизации.
 * @param baseUrl Базовый URL API (по умолчанию из NetworkConfig).
 * @param enableLogging Включить подробное логирование.
 */
class NetworkClientFactory(
    private val context: Context,
    private val tokenProvider: TokenProvider? = null,
    private val baseUrl: String = NetworkConfig.BASE_URL,
    private val enableLogging: Boolean = true
) {

    /**
     * Создаёт настроенный OkHttpClient.
     */
    fun createOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(NetworkConfig.CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .readTimeout(NetworkConfig.READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .writeTimeout(NetworkConfig.WRITE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        
        // Добавляем интерцептор для retry-логики
        builder.addInterceptor(
            RetryInterceptor(
                maxRetryCount = NetworkConfig.MAX_RETRY_COUNT,
                initialDelayMs = NetworkConfig.INITIAL_RETRY_DELAY_MS,
                maxDelayMs = NetworkConfig.MAX_RETRY_DELAY_MS
            )
        )
        
        // Добавляем интерцептор авторизации (если предоставлен токен-провайдер)
        tokenProvider?.let { provider ->
            builder.addInterceptor(AuthInterceptor(provider))
        }
        
        // Добавляем интерцептор логирования (только для отладочных сборок)
        if (enableLogging) {
            builder.addInterceptor(
                LoggingInterceptor(LoggingInterceptor.LogLevel.BODY)
            )
        }
        
        return builder.build()
    }

    /**
     * Создаёт Retrofit инстанс с настроенным OkHttpClient.
     */
    fun createRetrofit(): Retrofit {
        val okHttpClient = createOkHttpClient()
        
        // Настраиваем JSON сериализатор
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
            explicitNulls = false
        }
        
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
    }

    /**
     * Создаёт API сервис указанного типа.
     *
     * @param T Тип API интерфейса.
     * @return Экземпляр API сервиса.
     *
     * Пример использования:
     * ```kotlin
     * val authApi = factory.createApi<AuthApi>()
     * val chatApi = factory.createApi<ChatApi>()
     * ```
     */
    inline fun <reified T> createApi(): T {
        return createRetrofit().create(T::class.java)
    }

    companion object {
        /**
         * Создает фабрику с дефолтными настройками.
         * Используйте для быстрой инициализации.
         */
        fun createDefault(context: Context, tokenProvider: TokenProvider? = null): NetworkClientFactory {
            return NetworkClientFactory(
                context = context,
                tokenProvider = tokenProvider,
                enableLogging = BuildConfig.DEBUG
            )
        }
    }
}

// Временный класс для доступа к BuildConfig.DEBUG
// В реальном проекте замените на ваш BuildConfig или используйте другой механизм
private object BuildConfig {
    val DEBUG: Boolean = true // Замените на проверку реальной сборки
}
