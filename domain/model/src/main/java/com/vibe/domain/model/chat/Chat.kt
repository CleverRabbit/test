package com.vibe.domain.model.chat

import com.vibe.domain.model.message.Message
import com.vibe.domain.model.user.User

/**
 * Модель чата в доменном слое.
 *
 * @param id Уникальный идентификатор чата
 * @param type Тип чата (личный, групповой, канал)
 * @param name Название чата (для групп и каналов)
 * @param participants Участники чата
 * @param lastMessage Последнее сообщение
 * @param unreadCount Количество непрочитанных сообщений
 * @param isMuted Чат заглушен
 * @param isPinned Чат закреплен
 * @param createdAt Дата создания
 */
data class Chat(
    val id: String,
    val type: ChatType,
    val name: String? = null,
    val participants: List<User> = emptyList(),
    val lastMessage: Message? = null,
    val unreadCount: Int = 0,
    val isMuted: Boolean = false,
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Отображаемое имя чата
     */
    val displayName: String
        get() = when (type) {
            ChatType.PRIVATE -> participants.firstOrNull()?.fullName ?: "Неизвестный"
            ChatType.GROUP, ChatType.CHANNEL -> name ?: "Без названия"
        }
    
    /**
     * URL аватара чата
     */
    val avatarUrl: String?
        get() = when (type) {
            ChatType.PRIVATE -> participants.firstOrNull()?.avatarUrl
            ChatType.GROUP, ChatType.CHANNEL -> null // Для групп можно добавить отдельное поле
        }
}

/**
 * Тип чата
 */
enum class ChatType {
    /** Личный чат */
    PRIVATE,
    /** Групповой чат */
    GROUP,
    /** Канал */
    CHANNEL
}
