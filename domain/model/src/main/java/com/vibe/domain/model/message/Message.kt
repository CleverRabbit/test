package com.vibe.domain.model.message

import com.vibe.domain.model.user.User

/**
 * Модель сообщения в доменном слое.
 *
 * @param id Уникальный идентификатор сообщения
 * @param chatId ID чата
 * @param sender Отправитель
 * @param content Содержание сообщения
 * @param type Тип сообщения
 * @param status Статус доставки
 * @param timestamp Время отправки
 * @param isEdited Сообщение было отредактировано
 * @param replyToMessage ID сообщения, на которое это ответ (опционально)
 * @param mediaUrl URL медиафайла (для фото/видео/документов)
 */
data class Message(
    val id: String,
    val chatId: String,
    val sender: User,
    val content: String,
    val type: MessageType = MessageType.TEXT,
    val status: MessageStatus = MessageStatus.SENDING,
    val timestamp: Long = System.currentTimeMillis(),
    val isEdited: Boolean = false,
    val replyToMessage: String? = null,
    val mediaUrl: String? = null
)

/**
 * Тип сообщения
 */
enum class MessageType {
    /** Текстовое сообщение */
    TEXT,
    /** Изображение */
    IMAGE,
    /** Видео */
    VIDEO,
    /** Аудио/голосовое */
    AUDIO,
    /** Документ */
    DOCUMENT,
    /** Геолокация */
    LOCATION
}

/**
 * Статус доставки сообщения
 */
enum class MessageStatus {
    /** Отправка */
    SENDING,
    /** Отправлено на сервер */
    SENT,
    /** Доставлено получателю */
    DELIVERED,
    /** Прочитано */
    READ,
    /** Ошибка отправки */
    FAILED
}
