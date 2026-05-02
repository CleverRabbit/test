package com.messenger.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Пользователь мессенджера.
 * Domain модель - используется во всех слоях приложения.
 */
@Serializable
data class User(
    @SerialName("id")
    val id: String,
    
    @SerialName("username")
    val username: String,
    
    @SerialName("displayName")
    val displayName: String? = null,
    
    @SerialName("avatarUrl")
    val avatarUrl: String? = null,
    
    @SerialName("isOnline")
    val isOnline: Boolean = false,
    
    @SerialName("lastSeenAt")
    val lastSeenAt: Long? = null,
    
    @SerialName("phone")
    val phone: String? = null,
    
    @SerialName("bio")
    val bio: String? = null,
    
    @SerialName("isBlocked")
    val isBlocked: Boolean = false,
    
    @SerialName("createdAt")
    val createdAt: Long? = null
) {
    /**
     * Отображаемое имя (display name или username).
     */
    fun getDisplayNameOrUsername(): String = displayName ?: username
    
    /**
     * Статус "в сети" с учётом lastSeen.
     */
    fun getStatusText(): String {
        return when {
            isOnline -> "В сети"
            lastSeenAt == null -> "Был(а) недавно"
            else -> formatLastSeen(lastSeenAt)
        }
    }
    
    private fun formatLastSeen(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < 60_000 -> "Был(а) только что"
            diff < 3600_000 -> "Был(а) ${diff / 60_000} мин назад"
            diff < 86400_000 -> "Был(а) ${diff / 3600_000} ч назад"
            else -> "Был(а) ${diff / 86400_000} дн назад"
        }
    }
}

/**
 * Чат (диалог или группа).
 */
@Serializable
data class Chat(
    @SerialName("id")
    val id: String,
    
    @SerialName("type")
    val type: ChatType = ChatType.PRIVATE,
    
    @SerialName("name")
    val name: String? = null,
    
    @SerialName("avatarUrl")
    val avatarUrl: String? = null,
    
    @SerialName("participants")
    val participants: List<User> = emptyList(),
    
    @SerialName("lastMessage")
    val lastMessage: Message? = null,
    
    @SerialName("unreadCount")
    val unreadCount: Int = 0,
    
    @SerialName("isMuted")
    val isMuted: Boolean = false,
    
    @SerialName("isPinned")
    val isPinned: Boolean = false,
    
    @SerialName("updatedAt")
    val updatedAt: Long? = null,
    
    @SerialName("createdAt")
    val createdAt: Long? = null
) {
    /**
     * Получение отображаемого имени чата.
     */
    fun getDisplayName(): String {
        return when (type) {
            ChatType.PRIVATE -> participants.firstOrNull()?.getDisplayNameOrUsername() ?: "Неизвестный"
            ChatType.GROUP -> name ?: "Группа"
            ChatType.CHANNEL -> name ?: "Канал"
        }
    }
    
    /**
     * Получение аватара чата.
     */
    fun getAvatarUrl(): String? {
        return when (type) {
            ChatType.PRIVATE -> participants.firstOrNull()?.avatarUrl
            ChatType.GROUP, ChatType.CHANNEL -> avatarUrl
        }
    }
}

/**
 * Тип чата.
 */
enum class ChatType {
    PRIVATE,    // Личный диалог
    GROUP,      // Группа
    CHANNEL     // Канал
}

/**
 * Сообщение в чате.
 */
@Serializable
data class Message(
    @SerialName("id")
    val id: String,
    
    @SerialName("chatId")
    val chatId: String,
    
    @SerialName("senderId")
    val senderId: String,
    
    @SerialName("sender")
    val sender: User? = null,
    
    @SerialName("content")
    val content: String? = null,
    
    @SerialName("media")
    val media: List<MediaAttachment>? = null,
    
    @SerialName("replyTo")
    val replyTo: Message? = null,
    
    @SerialName("status")
    val status: MessageStatus = MessageStatus.PENDING,
    
    @SerialName("isEdited")
    val isEdited: Boolean = false,
    
    @SerialName("isDeleted")
    val isDeleted: Boolean = false,
    
    @SerialName("createdAt")
    val createdAt: Long,
    
    @SerialName("updatedAt")
    val updatedAt: Long? = null,
    
    @SerialName("readAt")
    val readAt: Long? = null
) {
    /**
     * Проверка, является ли сообщение исходящим.
     */
    fun isOutgoing(currentUserId: String): Boolean = senderId == currentUserId
    
    /**
     * Текст статуса сообщения.
     */
    fun getStatusText(): String {
        return when (status) {
            MessageStatus.PENDING -> "Отправка..."
            MessageStatus.SENT -> "✓"
            MessageStatus.DELIVERED -> "✓✓"
            MessageStatus.READ -> "✓✓ Прочитано"
            MessageStatus.FAILED -> "Ошибка"
        }
    }
}

/**
 * Статус сообщения.
 */
enum class MessageStatus {
    PENDING,    // Ожидает отправки
    SENT,       // Отправлено на сервер
    DELIVERED,  // Доставлено получателю
    READ,       // Прочитано
    FAILED      // Ошибка отправки
}

/**
 * Медиа-вложение в сообщении.
 */
@Serializable
data class MediaAttachment(
    @SerialName("id")
    val id: String,
    
    @SerialName("type")
    val type: MediaType,
    
    @SerialName("url")
    val url: String,
    
    @SerialName("thumbnailUrl")
    val thumbnailUrl: String? = null,
    
    @SerialName("fileName")
    val fileName: String? = null,
    
    @SerialName("fileSize")
    val fileSize: Long? = null,
    
    @SerialName("width")
    val width: Int? = null,
    
    @SerialName("height")
    val height: Int? = null,
    
    @SerialName("duration")
    val duration: Long? = null  // Для видео/аудио в мс
)

/**
 * Тип медиа-вложения.
 */
enum class MediaType {
    IMAGE,
    VIDEO,
    AUDIO,
    DOCUMENT,
    VOICE,
    GIF,
    STICKER
}

/**
 * Сессия аутентификации.
 */
@Serializable
data class AuthSession(
    @SerialName("userId")
    val userId: String,
    
    @SerialName("accessToken")
    val accessToken: String,
    
    @SerialName("refreshToken")
    val refreshToken: String,
    
    @SerialName("expiresIn")
    val expiresIn: Long,
    
    @SerialName("tokenType")
    val tokenType: String = "Bearer"
) {
    /**
     * Проверка истечения токена.
     * @param bufferMs буфер времени в мс для предупреждения об истечении
     */
    fun isTokenExpired(bufferMs: Long = 60_000): Boolean {
        return System.currentTimeMillis() >= (expiresIn - bufferMs)
    }
}

/**
 * Результат входа/регистрации.
 */
@Serializable
data class AuthResult(
    @SerialName("session")
    val session: AuthSession,
    
    @SerialName("user")
    val user: User,
    
    @SerialName("requiresTwoFactor")
    val requiresTwoFactor: Boolean = false
)
