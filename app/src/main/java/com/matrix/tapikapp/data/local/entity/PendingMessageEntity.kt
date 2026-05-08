package com.matrix.tapikapp.data.local.entity

/**
 * Entity для offline-очереди отправки сообщений.
 * 
 * Хранит сообщения, которые не удалось отправить из-за отсутствия сети.
 * Использует exponential backoff для повторных попыток.
 */
@androidx.room.Entity(tableName = "pending_messages")
data class PendingMessageEntity(
    @androidx.room.PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Уникальный ключ идемпотентности для гарантии однократной отправки
    val idempotencyKey: String,
    
    val chatId: String,
    
    val content: String,
    
    val mediaUrl: String? = null,
    
    val mediaType: String? = null,
    
    // Количество попыток отправки
    val retryCount: Int = 0,
    
    // Время следующей попытки (exponential backoff)
    val nextRetryAt: Long = 0,
    
    // Время создания
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        /**
         * Генерация уникального ключа идемпотентности.
         */
        fun generateIdempotencyKey(chatId: String, content: String, timestamp: Long): String {
            return "${chatId}_${content.hashCode()}_$timestamp"
        }
    }
}
