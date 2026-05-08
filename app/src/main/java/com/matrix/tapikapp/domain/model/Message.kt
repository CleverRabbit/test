package com.matrix.tapikapp.domain.model

/**
 * Модель сообщения в доменном слое.
 * 
 * @property id Уникальный идентификатор сообщения
 * @property chatId Идентификатор чата
 * @property senderId Идентификатор отправителя
 * @property content Текст сообщения
 * @property timestamp Время отправки
 * @property status Статус доставки сообщения
 * @property isIncoming Флаг входящего сообщения
 * @property mediaUrl URL медиа-вложения (опционально)
 * @property mediaType Тип медиа (опционально)
 */
data class Message(
    val id: String,
    val chatId: String,
    val senderId: String,
    val content: String,
    val timestamp: Long,
    val status: MessageStatus = MessageStatus.SENDING,
    val isIncoming: Boolean,
    val mediaUrl: String? = null,
    val mediaType: MediaType? = null
)

/**
 * Статусы доставки сообщения
 */
enum class MessageStatus {
    /** Отправка в процессе */
    SENDING,
    /** Ошибка отправки */
    FAILED,
    /** Доставлено серверу */
    SENT,
    /** Доставлено получателю */
    DELIVERED,
    /** Прочитано получателем */
    READ
}

/**
 * Типы медиа-контента
 */
enum class MediaType {
    IMAGE,
    VIDEO,
    AUDIO,
    DOCUMENT,
    VOICE
}
