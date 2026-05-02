package com.messenger.domain.repository

import com.messenger.domain.model.Chat
import com.messenger.domain.model.Message
import kotlinx.coroutines.flow.Flow

/**
 * Репозиторий для работы с чатами и сообщениями.
 * 
 * Определяет контракт для получения и отправки сообщений,
 * управления чатами. Реализация находится в data слое.
 */
interface ChatRepository {
    
    /**
     * Поток списка всех чатов пользователя.
     * Чаты сортируются по времени последнего сообщения.
     */
    fun getChats(): Flow<List<Chat>>
    
    /**
     * Получает чат по ID.
     * 
     * @param chatId идентификатор чата
     * @return данные чата или null если не найден
     */
    suspend fun getChatById(chatId: String): Chat?
    
    /**
     * Поток сообщений в чате.
     * 
     * @param chatId идентификатор чата
     * @return поток со списком сообщений
     */
    fun getMessages(chatId: String): Flow<List<Message>>
    
    /**
     * Отправляет сообщение в чат.
     * 
     * @param chatId идентификатор чата
     * @param content текст сообщения
     * @param replyToMessageId ID сообщения для ответа (опционально)
     * @return Result с отправленным сообщением или ошибкой
     */
    suspend fun sendMessage(
        chatId: String,
        content: String,
        replyToMessageId: String? = null
    ): Result<Message>
    
    /**
     * Отправляет медиафайл в чат.
     * 
     * @param chatId идентификатор чата
     * @param mediaUri URI медиафайла
     * @param caption подпись к медиа (опционально)
     * @return Result с отправленным сообщением или ошибкой
     */
    suspend fun sendMediaMessage(
        chatId: String,
        mediaUri: String,
        caption: String? = null
    ): Result<Message>
    
    /**
     * Помечает сообщения как прочитанные.
     * 
     * @param chatId идентификатор чата
     * @param messageIds список ID сообщений
     */
    suspend fun markMessagesAsRead(
        chatId: String,
        messageIds: List<String>
    )
    
    /**
     * Удаляет сообщение из чата.
     * 
     * @param chatId идентификатор чата
     * @param messageId ID сообщения
     * @param forAll true чтобы удалить для всех участников
     */
    suspend fun deleteMessage(
        chatId: String,
        messageId: String,
        forAll: Boolean = false
    ): Result<Unit>
    
    /**
     * Создает новый чат с пользователем.
     * 
     * @param userId ID пользователя
     * @return созданный чат
     */
    suspend fun createChat(userId: String): Result<Chat>
    
    /**
     * Ищет сообщения в чате по запросу.
     * 
     * @param chatId идентификатор чата
     * @param query поисковый запрос
     * @return список найденных сообщений
     */
    suspend fun searchMessages(chatId: String, query: String): List<Message>
}
