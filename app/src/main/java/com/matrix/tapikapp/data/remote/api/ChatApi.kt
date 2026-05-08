package com.matrix.tapikapp.data.remote.api

import com.matrix.tapikapp.data.remote.model.ApiResponse
import com.matrix.tapikapp.data.remote.model.ChatDto
import com.matrix.tapikapp.data.remote.model.MessageDto
import com.matrix.tapikapp.data.remote.model.UserDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit API интерфейс для работы с чатами и сообщениями.
 * 
 * Для подключения своего REST API:
 * 1. Измените baseUrl в NetworkModule
 * 2. Адаптируйте пути эндпоинтов под вашу структуру
 * 3. При необходимости измените модели DTO
 */
interface ChatApi {

    /**
     * Получение списка чатов текущего пользователя.
     * GET /api/v1/chats
     */
    @GET("api/v1/chats")
    suspend fun getChats(): ApiResponse<List<ChatDto>>

    /**
     * Получение конкретного чата по ID.
     * GET /api/v1/chats/{chatId}
     */
    @GET("api/v1/chats/{chatId}")
    suspend fun getChatById(@Path("chatId") chatId: String): ApiResponse<ChatDto>

    /**
     * Получение сообщений чата с пагинацией.
     * GET /api/v1/chats/{chatId}/messages?limit=50&offset=0
     */
    @GET("api/v1/chats/{chatId}/messages")
    suspend fun getMessages(
        @Path("chatId") chatId: String,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): ApiResponse<List<MessageDto>>

    /**
     * Отправка сообщения.
     * POST /api/v1/chats/{chatId}/messages
     */
    @POST("api/v1/chats/{chatId}/messages")
    suspend fun sendMessage(
        @Path("chatId") chatId: String,
        @Body request: SendMessageRequest
    ): ApiResponse<MessageDto>

    /**
     * Пометка сообщения как прочитанного.
     * POST /api/v1/messages/{messageId}/read
     */
    @POST("api/v1/messages/{messageId}/read")
    suspend fun markMessageAsRead(@Path("messageId") messageId: String): ApiResponse<Unit>

    /**
     * Поиск чатов.
     * GET /api/v1/chats/search?q=query
     */
    @GET("api/v1/chats/search")
    suspend fun searchChats(@Query("q") query: String): ApiResponse<List<ChatDto>>
}

/**
 * Запрос на отправку сообщения.
 */
@kotlinx.serialization.Serializable
data class SendMessageRequest(
    val content: String,
    val mediaUrl: String? = null,
    val mediaType: String? = null
)
