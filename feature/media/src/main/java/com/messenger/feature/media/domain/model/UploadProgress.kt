package com.messenger.feature.media.domain.model

/**
 * Модель состояния загрузки медиафайла.
 * 
 * Используется для отслеживания прогресса загрузки в UI.
 * 
 * @property fileId ID загружаемого файла
 * @property progress Прогресс загрузки (0-100)
 * @property bytesLoaded Загружено байт
 * @property bytesTotal Всего байт
 * @property status Статус загрузки
 * @property error Сообщение об ошибке (если есть)
 */
data class UploadProgress(
    val fileId: String,
    val progress: Int = 0,
    val bytesLoaded: Long = 0L,
    val bytesTotal: Long = 0L,
    val status: UploadStatus = UploadStatus.PENDING,
    val error: String? = null
) {
    enum class UploadStatus {
        PENDING,
        UPLOADING,
        PAUSED,
        COMPLETED,
        FAILED
    }
}
