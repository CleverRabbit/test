package com.messenger.feature.media.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO модель для ответа chunked загрузки.
 * 
 * Используется при загрузке больших файлов по частям.
 * 
 * @property uploadUrl URL для загрузки следующей части
 * @property partNumber Номер части
 * @property totalParts Общее количество частей
 * @property uploadedParts Количество загруженных частей
 * @property isComplete Флаг завершения загрузки
 * @property mediaFileId ID медиафайла после завершения загрузки
 */
@Serializable
data class ChunkUploadResponseDto(
    @SerialName("upload_url")
    val uploadUrl: String,
    
    @SerialName("part_number")
    val partNumber: Int,
    
    @SerialName("total_parts")
    val totalParts: Int,
    
    @SerialName("uploaded_parts")
    val uploadedParts: Int = 0,
    
    @SerialName("is_complete")
    val isComplete: Boolean = false,
    
    @SerialName("media_file_id")
    val mediaFileId: String? = null,
    
    @SerialName("etag")
    val etag: String? = null
)
