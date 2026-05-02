package com.vibe.domain.repository

import com.vibe.domain.model.chat.Chat
import com.vibe.domain.model.message.Message
import kotlinx.coroutines.flow.Flow

/**
 * Репозиторий для операций с чатами и сообщениями.
 */
interface ChatRepository {
    
    /**
     * Поток списка всех чатов.
     * @return Flow со списком чатов
     */
    fun getChatsFlow(): Flow<List<Chat>>
    
    /**
     * Получение чата по ID.
     * @param chatId ID чата
     * @return Chat или null
     */
    suspend fun getChatById(chatId: String): Chat?
    
    /**
     * Поток сообщений в чате.
     * @param chatId ID чата
     * @return Flow со списком сообщений
     */
    fun getMessagesFlow(chatId: String): Flow<List<Message>>
    
    /**
     * Отправка сообщения.
     * @param chatId ID чата
     * @param content Содержание сообщения
     * @param type Тип сообщения
     * @return Result с отправленным сообщением или ошибкой
     */
    suspend fun sendMessage(
        chatId: String,
        content: String,
        type: Message.MessageType = Message.MessageType.TEXT
    ): Result<Message>
    
    /**
     * Отправка медиа-сообщения.
     * @param chatId ID чата
     * @param mediaUri URI медиафайла
     * @param type Тип медиа
     * @return Result с сообщением или ошибкой
     */
    suspend fun sendMediaMessage(
        chatId: String,
        mediaUri: String,
        type: Message.MessageType
    ): Result<Message>
    
    /**
     * Удаление сообщения.
     * @param chatId ID чата
     * @param messageId ID сообщения
     * @return true если успешно
     */
    suspend fun deleteMessage(chatId: String, messageId: String): Boolean
    
    /**
     * Отметка сообщений как прочитанные.
     * @param chatId ID чата
     * @param messageIds ID сообщений
     */
    suspend fun markAsRead(chatId: String, messageIds: List<String>)
    
    /**
     * Создание нового чата.
     * @param userId ID пользователя для личного чата
     * @return Result с созданным чатом или ошибкой
     */
    suspend fun createPrivateChat(userId: String): Result<Chat>
}
