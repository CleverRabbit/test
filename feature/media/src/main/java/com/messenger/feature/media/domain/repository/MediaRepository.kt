package com.messenger.feature.media.domain.repository

import com.messenger.feature.media.domain.model.MediaFile
import com.messenger.feature.media.domain.model.UploadProgress
import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс репозитория для работы с медиафайлами.
 * 
 * Определяет контракт для доступа к данным о медиафайлах.
 * Реализация находится в data слое.
 * 
 * Для подключения своего REST API:
 * 1. Реализуйте методы в DataRepositoryImpl
 * 2. Настройте маппинг DTO -> Domain модели
 * 3. Обработайте специфичные ошибки вашего API
 */
interface MediaRepository {
    
    /**
     * Загрузка медиафайла на сервер.
     * 
     * @param localPath Локальный путь к файлу
     * @param chatId ID чата (опционально)
     * @return Flow с прогрессом загрузки и результатом
     */
    suspend fun uploadMedia(
        localPath: String,
        chatId: String? = null
    ): Flow<UploadProgress>
    
    /**
     * Получение информации о медиафайле по ID.
     * 
     * @param mediaId ID медиафайла
     * @return MediaFile или null если не найден
     */
    suspend fun getMediaById(mediaId: String): MediaFile?
    
    /**
     * Получение списка медиафайлов для чата.
     * 
     * @param chatId ID чата
     * @param limit Максимальное количество файлов
     * @param offset Смещение для пагинации
     * @return Список медиафайлов
     */
    suspend fun getChatMedia(
        chatId: String,
        limit: Int = 50,
        offset: Int = 0
    ): List<MediaFile>
    
    /**
     * Удаление медиафайла с сервера.
     * 
     * @param mediaId ID медиафайла
     * @return true если успешно удален
     */
    suspend fun deleteMedia(mediaId: String): Boolean
    
    /**
     * Скачивание медиафайла локально.
     * 
     * @param mediaUrl URL файла
     * @param destinationPath Путь для сохранения
     * @return Flow с прогрессом скачивания
     */
    suspend fun downloadMedia(
        mediaUrl: String,
        destinationPath: String
    ): Flow<UploadProgress>
    
    /**
     * Отмена загрузки/скачивания файла.
     * 
     * @param fileId ID файла
     */
    suspend fun cancelOperation(fileId: String)
    
    /**
     * Получение локального кэшированного файла.
     * 
     * @param mediaId ID медиафайла
     * @return Локальный путь к файлу или null
     */
    suspend fun getCachedMediaPath(mediaId: String): String?
    
    /**
     * Очистка кэша медиафайлов.
     * 
     * @param olderThanDays Удалять файлы старше указанного количества дней
     */
    suspend fun clearCache(olderThanDays: Int = 7)
}
