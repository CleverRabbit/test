package com.messenger.core.network.di

import android.content.Context
import com.messenger.core.network.client.NetworkClientFactory
import com.messenger.core.network.config.NetworkConfig
import com.messenger.core.network.interceptor.TokenProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Named
import javax.inject.Singleton

/**
 * Hilt модуль для предоставления сетевых зависимостей.
 * 
 * Предоставляет:
 * - [NetworkClientFactory] - фабрика для создания Retrofit инстансов
 * - [OkHttpClient] - настроенный HTTP клиент
 * - [Retrofit] - основной Retrofit инстанс
 * 
 * Для подключения своего API:
 * 1. Измените [NetworkConfig.BASE_URL] на адрес вашего сервера
 * 2. Реализуйте интерфейс [TokenProvider] в вашем проекте
 * 3. При необходимости добавьте дополнительные API сервисы через [@Provides] методы
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * Предоставляет фабрику для создания сетевых клиентов.
     * 
     * @param context Контекст приложения.
     * @param tokenProvider Поставщик токенов (опционально, может быть null).
     * @return Настроенная фабрика NetworkClientFactory.
     */
    @Provides
    @Singleton
    fun provideNetworkClientFactory(
        @ApplicationContext context: Context,
        tokenProvider: TokenProvider? = null
    ): NetworkClientFactory {
        return NetworkClientFactory.createDefault(context, tokenProvider)
    }

    /**
     * Предоставляет настроенный OkHttpClient.
     * 
     * Используется как основной HTTP клиент для всех запросов.
     * Включает интерцепторы для логирования, авторизации и retry-логики.
     */
    @Provides
    @Singleton
    @Named("default")
    fun provideOkHttpClient(
        factory: NetworkClientFactory
    ): OkHttpClient {
        return factory.createOkHttpClient()
    }

    /**
     * Предоставляет основной Retrofit инстанс.
     * 
     * Настроен с:
     * - Базовым URL из NetworkConfig
     * - Kotlinx Serialization для JSON
     * - Scalars Converter для простых типов
     */
    @Provides
    @Singleton
    @Named("default")
    fun provideRetrofit(
        factory: NetworkClientFactory
    ): Retrofit {
        return factory.createRetrofit()
    }

    /**
     * Предоставляет базовый URL API.
     * Может быть переопределён для разных окружений (dev/staging/prod).
     */
    @Provides
    @Singleton
    @Named("baseUrl")
    fun provideBaseUrl(): String {
        return NetworkConfig.BASE_URL
    }

    /**
     * Пример предоставления конкретного API сервиса.
     * 
     * Раскомментируйте и адаптируйте под ваш API:
     * 
     * ```kotlin
     * @Provides
     * @Singleton
     * fun provideAuthApi(@Named("default") retrofit: Retrofit): AuthApi {
     *     return retrofit.create(AuthApi::class.java)
     * }
     * 
     * @Provides
     * @Singleton
     * fun provideChatApi(@Named("default") retrofit: Retrofit): ChatApi {
     *     return retrofit.create(ChatApi::class.java)
     * }
     * ```
     * 
     * Где AuthApi и ChatApi - ваши интерфейсы с аннотациями Retrofit:
     * 
     * ```kotlin
     * interface AuthApi {
     *     @POST("auth/login")
     *     suspend fun login(@Body request: LoginRequest): ApiResponse<AuthResponse>
     *     
     *     @POST("auth/register")
     *     suspend fun register(@Body request: RegisterRequest): ApiResponse<UserResponse>
     * }
     * 
     * interface ChatApi {
     *     @GET("chats")
     *     suspend fun getChats(
     *         @Query("page") page: Int,
     *         @Query("limit") limit: Int
     *     ): ApiResponse<PagedResponse<Chat>>
     *     
     *     @GET("chats/{chatId}/messages")
     *     suspend fun getMessages(
     *         @Path("chatId") chatId: String,
     *         @Query("before") before: Long?
     *     ): ApiResponse<List<Message>>
     * }
     * ```
     */
}
