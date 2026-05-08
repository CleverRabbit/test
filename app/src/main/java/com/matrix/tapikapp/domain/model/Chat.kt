package com.matrix.tapikapp.domain.model

/**
 * Модель чата в доменном слое.
 * 
 * @property id Уникальный идентификатор чата
 * @property name Название чата (для групп) или имя собеседника
 * @property avatarUrl URL аватара чата (опционально)
 * @property lastMessage Последнее сообщение в чате
 * @property lastMessageTime Время последнего сообщения
 * @property unreadCount Количество непрочитанных сообщений
 * @property isGroup Флаг группового чата
 * @property participantsCount Количество участников (для групп)
 */
data class Chat(
    val id: String,
    val name: String,
    val avatarUrl: String? = null,
    val lastMessage: String? = null,
    val lastMessageTime: Long = 0,
    val unreadCount: Int = 0,
    val isGroup: Boolean = false,
    val participantsCount: Int = 1
)
