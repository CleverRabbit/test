package com.messenger.feature.chat.presentation.model

import com.messenger.domain.model.Message
import com.messenger.domain.model.MessageStatus

/**
 * UI модель сообщения для отображения в чате.
 * 
 * Содержит данные необходимые для отображения сообщения в UI,
 * включая статусы доставки и форматированное время.
 */
data class MessageUiModel(
    val id: String,
    val chatId: String,
    val senderId: String,
    val content: String,
    val timestamp: Long,
    val formattedTime: String,
    val isOutgoing: Boolean,
    val status: MessageStatus,
    val replyToMessageId: String? = null,
    val mediaUrl: String? = null,
    val mediaType: MediaType? = null
) {
    /**
     * Тип медиа контента.
     */
    enum class MediaType {
        IMAGE,
        VIDEO,
        AUDIO,
        DOCUMENT
    }
}

/**
 * Расширение для конвертации доменной модели в UI модель.
 */
fun Message.toUiModel(isCurrentUserSender: Boolean): MessageUiModel {
    return MessageUiModel(
        id = this.id,
        chatId = this.chatId,
        senderId = this.senderId,
        content = this.content,
        timestamp = this.timestamp,
        formattedTime = formatTime(this.timestamp),
        isOutgoing = isCurrentUserSender,
        status = this.status,
        replyToMessageId = this.replyToMessageId,
        mediaUrl = this.mediaUrl,
        mediaType = this.mediaType?.toUiMediaType()
    )
}

private fun formatTime(timestamp: Long): String {
    // Простая реализация форматирования времени
    // В продакшене использовать DateTimeFormatter
    val minutes = (timestamp / 1000 / 60) % 60
    val hours = (timestamp / 1000 / 3600) % 24
    return "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}"
}

private fun Message.MediaType.toUiMediaType(): MessageUiModel.MediaType {
    return when (this) {
        Message.MediaType.IMAGE -> MessageUiModel.MediaType.IMAGE
        Message.MediaType.VIDEO -> MessageUiModel.MediaType.VIDEO
        Message.MediaType.AUDIO -> MessageUiModel.MediaType.AUDIO
        Message.MediaType.DOCUMENT -> MessageUiModel.MediaType.DOCUMENT
    }
}
