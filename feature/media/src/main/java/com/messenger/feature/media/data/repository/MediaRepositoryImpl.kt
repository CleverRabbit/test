package com.messenger.feature.media.data.repository

import android.content.Context
import com.messenger.core.common.Logger
import com.messenger.feature.media.data.model.MediaFileDto
import com.messenger.feature.media.data.network.MediaApiService
import com.messenger.feature.media.domain.model.MediaFile
import com.messenger.feature.media.domain.model.UploadProgress
import com.messenger.feature.media.domain.repository.MediaRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import java.io.File
import javax.inject.Inject

/**
 * Реализация репозитория для работы с медиафайлами.
 * 
 * Обрабатывает загрузку, скачивание и кэширование медиафайлов.
 * Использует Retrofit для сетевых запросов и локальное хранилище для кэша.
 * 
 * Для подключения своего REST API:
 * 1. Настройте маппинг DTO -> Domain в приватных методах
 * 2. Обработайте специфичные ошибки вашего API в try-catch блоках
 * 3. Добавьте логирование через Logger для отладки
 */
class MediaRepositoryImpl @Inject constructor(
    private val apiService: MediaApiService,
    private val context: Context,
    private val logger: Logger
) : MediaRepository {
    
    companion object {
        private const val TAG = "MediaRepository"
        private const val CHUNK_SIZE = 5 * 1024 * 1024 // 5 MB для chunked загрузки
    }
    
    override suspend fun uploadMedia(
        localPath: String,
        chatId: String?
    ): Flow<UploadProgress> = callbackFlow {
        val fileId = System.currentTimeMillis().toString()
        val file = File(localPath)
        
        if (!file.exists()) {
            trySend(
                UploadProgress(
                    fileId = fileId,
                    status = UploadProgress.UploadStatus.FAILED,
                    error = "Файл не найден"
                )
            )
            close()
            return@callbackFlow
        }
        
        try {
            logger.d(TAG, "Начало загрузки файла: ${file.name}, размер: ${file.length()}")
            
            // Определение типа файла
            val mimeType = context.contentResolver.getType(file.toURI().toURL().toURI()) 
                ?: "application/octet-stream"
            val mediaType = when {
                mimeType.startsWith("image/") -> MediaFile.MediaType.IMAGE
                mimeType.startsWith("video/") -> MediaFile.MediaType.VIDEO
                mimeType.startsWith("audio/") -> MediaFile.MediaType.AUDIO
                else -> MediaFile.MediaType.DOCUMENT
            }
            
            // Для небольших файлов загружаем целиком
            if (file.length() < CHUNK_SIZE) {
                trySend(
                    UploadProgress(
                        fileId = fileId,
                        status = UploadProgress.UploadStatus.UPLOADING,
                        bytesTotal = file.length()
                    )
                )
                
                val requestFile = okhttp3.RequestBody.create(
                    okhttp3.MediaType.parse(mimeType),
                    file
                )
                
                val body = okhttp3.MultipartBody.Part.createFormData(
                    "file",
                    file.name,
                    requestFile
                )
                
                val response = apiService.uploadFile(
                    file = body,
                    chatId = chatId?.let { okhttp3.RequestBody.create(null, it) },
                    messageId = null
                )
                
                if (response.isSuccessful && response.body() != null) {
                    logger.d(TAG, "Загрузка завершена успешно")
                    trySend(
                        UploadProgress(
                            fileId = fileId,
                            progress = 100,
                            bytesLoaded = file.length(),
                            bytesTotal = file.length(),
                            status = UploadProgress.UploadStatus.COMPLETED
                        )
                    )
                } else {
                    logger.e(TAG, "Ошибка загрузки: ${response.errorBody()?.string()}")
                    trySend(
                        UploadProgress(
                            fileId = fileId,
                            status = UploadProgress.UploadStatus.FAILED,
                            error = "Ошибка сервера: ${response.code()}"
                        )
                    )
                }
            } else {
                // TODO: Реализовать chunked загрузку для больших файлов
                logger.w(TAG, "Chunked загрузка еще не реализована")
                trySend(
                    UploadProgress(
                        fileId = fileId,
                        status = UploadProgress.UploadStatus.FAILED,
                        error = "Большие файлы пока не поддерживаются"
                    )
                )
            }
        } catch (e: Exception) {
            logger.e(TAG, "Исключение при загрузке: ${e.message}", e)
            trySend(
                UploadProgress(
                    fileId = fileId,
                    status = UploadProgress.UploadStatus.FAILED,
                    error = e.message ?: "Неизвестная ошибка"
                )
            )
        }
        
        close()
    }
    
    override suspend fun getMediaById(mediaId: String): MediaFile? {
        return try {
            logger.d(TAG, "Получение информации о медиафайле: $mediaId")
            val response = apiService.getMediaInfo(mediaId)
            if (response.isSuccessful && response.body() != null) {
                response.body()!!.toDomainModel()
            } else {
                logger.w(TAG, "Файл не найден или ошибка: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            logger.e(TAG, "Ошибка получения информации о файле: ${e.message}", e)
            null
        }
    }
    
    override suspend fun getChatMedia(
        chatId: String,
        limit: Int,
        offset: Int
    ): List<MediaFile> {
        return try {
            logger.d(TAG, "Получение списка медиа для чата: $chatId, limit: $limit, offset: $offset")
            val response = apiService.getChatMedia(chatId, limit, offset)
            if (response.isSuccessful && response.body() != null) {
                response.body()!!.map { it.toDomainModel() }
            } else {
                logger.w(TAG, "Ошибка получения списка медиа: ${response.code()}")
                emptyList()
            }
        } catch (e: Exception) {
            logger.e(TAG, "Исключение при получении списка медиа: ${e.message}", e)
            emptyList()
        }
    }
    
    override suspend fun deleteMedia(mediaId: String): Boolean {
        return try {
            logger.d(TAG, "Удаление медиафайла: $mediaId")
            val response = apiService.deleteMedia(mediaId)
            response.isSuccessful
        } catch (e: Exception) {
            logger.e(TAG, "Ошибка удаления файла: ${e.message}", e)
            false
        }
    }
    
    override suspend fun downloadMedia(
        mediaUrl: String,
        destinationPath: String
    ): Flow<UploadProgress> = flow {
        // TODO: Реализовать скачивание файла
        logger.w(TAG, "Скачивание файлов еще не реализовано")
    }
    
    override suspend fun cancelOperation(fileId: String) {
        logger.d(TAG, "Отмена операции для файла: $fileId")
        // TODO: Реализовать отмену загрузки/скачивания
    }
    
    override suspend fun getCachedMediaPath(mediaId: String): String? {
        val cacheDir = File(context.cacheDir, "media/$mediaId")
        return if (cacheDir.exists()) cacheDir.absolutePath else null
    }
    
    override suspend fun clearCache(olderThanDays: Int) {
        logger.d(TAG, "Очистка кэша старше $olderThanDays дней")
        val cacheDir = File(context.cacheDir, "media")
        if (cacheDir.exists()) {
            val threshold = System.currentTimeMillis() - (olderThanDays.toLong() * 24 * 60 * 60 * 1000)
            cacheDir.listFiles()?.forEach { file ->
                if (file.lastModified() < threshold) {
                    file.deleteRecursively()
                    logger.d(TAG, "Удален устаревший файл: ${file.name}")
                }
            }
        }
    }
    
    /**
     * Маппинг DTO в доменную модель.
     */
    private fun MediaFileDto.toDomainModel(): MediaFile {
        return MediaFile(
            id = this.id,
            url = this.url,
            type = when (this.type.lowercase()) {
                MediaFileDto.TYPE_IMAGE -> MediaFile.MediaType.IMAGE
                MediaFileDto.TYPE_VIDEO -> MediaFile.MediaType.VIDEO
                MediaFileDto.TYPE_AUDIO -> MediaFile.MediaType.AUDIO
                else -> MediaFile.MediaType.DOCUMENT
            },
            mimeType = this.mimeType,
            size = this.size,
            width = this.width,
            height = this.height,
            duration = this.duration,
            thumbnailUrl = this.thumbnailUrl,
            uploadStatus = when (this.uploadStatus.lowercase()) {
                MediaFileDto.STATUS_PENDING -> MediaFile.UploadStatus.PENDING
                MediaFileDto.STATUS_UPLOADING -> MediaFile.UploadStatus.UPLOADING
                MediaFileDto.STATUS_COMPLETED -> MediaFile.UploadStatus.COMPLETED
                MediaFileDto.STATUS_FAILED -> MediaFile.UploadStatus.FAILED
                else -> MediaFile.UploadStatus.PENDING
            },
            createdAt = this.createdAt?.let { 
                try { 
                    java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", java.util.Locale.getDefault())
                        .parse(it)?.time 
                } catch (e: Exception) { 
                    null 
                }
            }
        )
    }
}
