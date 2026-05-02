package com.messenger.feature.media.domain.model

/**
 * Доменная модель медиафайла.
 * 
 * Представляет медиафайл в бизнес-логике приложения.
 * Используется для передачи данных между слоями Domain и Presentation.
 * 
 * @property id Уникальный идентификатор медиафайла
 * @property url URL для доступа к файлу
 * @property type Тип медиа (IMAGE, VIDEO, AUDIO, DOCUMENT)
 * @property mimeType MIME-тип файла
 * @property size Размер файла в байтах
 * @property width Ширина для изображений/видео (опционально)
 * @property height Высота для изображений/видео (опционально)
 * @property duration Длительность для аудио/видео в мс (опционально)
 * @property thumbnailUrl URL миниатюры (опционально)
 * @property uploadStatus Статус загрузки
 * @property createdAt Дата создания
 * @property localPath Локальный путь к файлу (для офлайн работы)
 */
data class MediaFile(
    val id: String? = null,
    val url: String? = null,
    val type: MediaType,
    val mimeType: String,
    val size: Long,
    val width: Int? = null,
    val height: Int? = null,
    val duration: Long? = null,
    val thumbnailUrl: String? = null,
    val uploadStatus: UploadStatus = UploadStatus.PENDING,
    val createdAt: Long? = null,
    val localPath: String? = null
) {
    /**
     * Типы медиафайлов, поддерживаемые мессенджером.
     */
    enum class MediaType {
        IMAGE,
        VIDEO,
        AUDIO,
        DOCUMENT,
        UNKNOWN
    }
    
    /**
     * Статусы загрузки файла.
     */
    enum class UploadStatus {
        PENDING,      // Ожидает загрузки
        UPLOADING,    // В процессе загрузки
        COMPLETED,    // Загрузка завершена
        FAILED,       // Ошибка загрузки
        PAUSED        // Приостановлено
    }
    
    /**
     * Проверка, является ли файл изображением.
     */
    fun isImage(): Boolean = type == MediaType.IMAGE
    
    /**
     * Проверка, является ли файл видео.
     */
    fun isVideo(): Boolean = type == MediaType.VIDEO
    
    /**
     * Проверка, является ли файл аудио.
     */
    fun isAudio(): Boolean = type == MediaType.AUDIO
    
    /**
     * Проверка, загружен ли файл на сервер.
     */
    fun isUploaded(): Boolean = uploadStatus == UploadStatus.COMPLETED && url != null
    
    /**
     * Форматированный размер файла для отображения в UI.
     */
    fun getFormattedSize(): String {
        return when {
            size < 1024 -> "$size Б"
            size < 1024 * 1024 -> "${size / 1024} КБ"
            size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} МБ"
            else -> "${size / (1024 * 1024 * 1024)} ГБ"
        }
    }
}
