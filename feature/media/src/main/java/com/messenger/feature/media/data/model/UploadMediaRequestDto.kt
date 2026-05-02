package com.messenger.feature.media.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO модель для запроса загрузки медиафайла.
 * 
 * Используется при отправке метаданных файла на сервер перед загрузкой.
 * 
 * @property fileName Имя файла
 * @property fileSize Размер файла в байтах
 * @property mimeType MIME-тип файла
 * @property mediaType Тип медиа (IMAGE, VIDEO, AUDIO, DOCUMENT)
 * @property chatId ID чата, к которому относится файл (опционально)
 * @property messageId ID сообщения, к которому прикрепляется файл (опционально)
 */
@Serializable
data class UploadMediaRequestDto(
    @SerialName("file_name")
    val fileName: String,
    
    @SerialName("file_size")
    val fileSize: Long,
    
    @SerialName("mime_type")
    val mimeType: String,
    
    @SerialName("media_type")
    val mediaType: String,
    
    @SerialName("chat_id")
    val chatId: String? = null,
    
    @SerialName("message_id")
    val messageId: String? = null
)
