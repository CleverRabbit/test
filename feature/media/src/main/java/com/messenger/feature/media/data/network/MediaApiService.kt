package com.messenger.feature.media.data.network

import com.messenger.feature.media.data.model.ChunkUploadResponseDto
import com.messenger.feature.media.data.model.MediaFileDto
import com.messenger.feature.media.data.model.UploadMediaRequestDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * API интерфейс для работы с медиафайлами.
 * 
 * Определяет все сетевые эндпоинты для загрузки, скачивания и управления медиа.
 * 
 * Для подключения своего REST API:
 * 1. Замените @Path/@Query параметры на соответствующие вашему API
 * 2. Измените типы данных в @Body при необходимости
 * 3. Добавьте дополнительные методы для специфичных эндпоинтов вашего бэкенда
 * 4. Настройте baseUrl в модуле core:network
 */
interface MediaApiService {
    
    /**
     * Инициирование загрузки медиафайла.
     * Возвращает URL для прямой загрузки или токен доступа.
     */
    @POST("media/upload/init")
    suspend fun initUpload(
        @Body request: UploadMediaRequestDto
    ): Response<MediaFileDto>
    
    /**
     * Загрузка файла целиком (для небольших файлов).
     * Multipart загрузка с метаданными.
     */
    @Multipart
    @POST("media/upload")
    suspend fun uploadFile(
        @Part file: MultipartBody.Part,
        @Part("chat_id") chatId: RequestBody? = null,
        @Part("message_id") messageId: RequestBody? = null
    ): Response<MediaFileDto>
    
    /**
     * Получение URL для chunked загрузки следующей части файла.
     * Используется для больших файлов (>10MB).
     */
    @POST("media/upload/chunk/{uploadId}")
    suspend fun getChunkUploadUrl(
        @Path("uploadId") uploadId: String,
        @Query("part_number") partNumber: Int,
        @Query("total_parts") totalParts: Int
    ): Response<ChunkUploadResponseDto>
    
    /**
     * Завершение chunked загрузки.
     * Сообщает серверу о готовности собрать файл из частей.
     */
    @POST("media/upload/complete/{uploadId}")
    suspend fun completeChunkUpload(
        @Path("uploadId") uploadId: String,
        @Body parts: List<Map<String, String>>
    ): Response<MediaFileDto>
    
    /**
     * Получение информации о медиафайле по ID.
     */
    @GET("media/{mediaId}")
    suspend fun getMediaInfo(
        @Path("mediaId") mediaId: String
    ): Response<MediaFileDto>
    
    /**
     * Удаление медиафайла.
     */
    @POST("media/{mediaId}/delete")
    suspend fun deleteMedia(
        @Path("mediaId") mediaId: String
    ): Response<Unit>
    
    /**
     * Получение списка загруженных медиа для чата.
     */
    @GET("media/chat/{chatId}")
    suspend fun getChatMedia(
        @Path("chatId") chatId: String,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): Response<List<MediaFileDto>>
}
