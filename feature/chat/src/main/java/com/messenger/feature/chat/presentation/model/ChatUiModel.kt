package com.messenger.feature.chat.presentation.model

/**
 * UI модель чата для отображения в списке.
 * 
 * Содержит данные необходимые для отображения чата в UI,
 * включая информацию о последнем сообщении и непрочитанных.
 */
data class ChatUiModel(
    val id: String,
    val name: String,
    val avatarUrl: String?,
    val lastMessage: String?,
    val lastMessageTime: Long,
    val formattedLastMessageTime: String,
    val unreadCount: Int,
    val isOnline: Boolean,
    val isGroup: Boolean = false,
    val participantsCount: Int = 1
)

/**
 * Расширение для конвертации доменной модели в UI модель.
 */
fun com.messenger.domain.model.Chat.toUiModel(): ChatUiModel {
    return ChatUiModel(
        id = this.id,
        name = this.name,
        avatarUrl = this.avatarUrl,
        lastMessage = this.lastMessage?.content,
        lastMessageTime = this.lastMessage?.timestamp ?: 0L,
        formattedLastMessageTime = formatTime(this.lastMessage?.timestamp ?: 0L),
        unreadCount = this.unreadCount,
        isOnline = this.isOnline,
        isGroup = this.isGroup,
        participantsCount = this.participantsCount
    )
}

private fun formatTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        // Сегодня - показываем время
        diff < 24 * 60 * 60 * 1000 -> {
            val minutes = (timestamp / 1000 / 60) % 60
            val hours = (timestamp / 1000 / 3600) % 24
            "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}"
        }
        // Вчера
        diff < 48 * 60 * 60 * 1000 -> "Вчера"
        // В этом году - показываем дату
        diff < 365 * 24 * 60 * 60 * 1000 -> {
            val day = (timestamp / 1000 / 3600 / 24) % 31 + 1
            val month = (timestamp / 1000 / 3600 / 24 / 30) % 12 + 1
            "$day.$month"
        }
        // Старые сообщения - год
        else -> {
            val year = (timestamp / 1000 / 3600 / 24 / 365) + 1970
            year.toString()
        }
    }
}
