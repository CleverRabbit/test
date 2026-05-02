package com.messenger.feature.media.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO модель для представления медиафайла при загрузке на сервер.
 * 
 * Используется для сетевых запросов и сериализации.
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
 */
@Serializable
data class MediaFileDto(
    @SerialName("id")
    val id: String? = null,
    
    @SerialName("url")
    val url: String? = null,
    
    @SerialName("type")
    val type: String,
    
    @SerialName("mime_type")
    val mimeType: String,
    
    @SerialName("size")
    val size: Long,
    
    @SerialName("width")
    val width: Int? = null,
    
    @SerialName("height")
    val height: Int? = null,
    
    @SerialName("duration")
    val duration: Long? = null,
    
    @SerialName("thumbnail_url")
    val thumbnailUrl: String? = null,
    
    @SerialName("upload_status")
    val uploadStatus: String = "pending",
    
    @SerialName("created_at")
    val createdAt: String? = null
) {
    companion object {
        const val TYPE_IMAGE = "image"
        const val TYPE_VIDEO = "video"
        const val TYPE_AUDIO = "audio"
        const val TYPE_DOCUMENT = "document"
        
        const val STATUS_PENDING = "pending"
        const val STATUS_UPLOADING = "uploading"
        const val STATUS_COMPLETED = "completed"
        const val STATUS_FAILED = "failed"
    }
}
