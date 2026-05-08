package com.matrix.tapikapp.domain.repository

import com.matrix.tapikapp.domain.model.Chat
import com.matrix.tapikapp.domain.model.Message
import com.matrix.tapikapp.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс репозитория для работы с пользователями и чатами.
 * 
 * Определяет контракты для получения данных из локальных и удаленных источников.
 * Реализует паттерн Repository из Clean Architecture.
 */
interface ChatRepository {

    /**
     * Поток списка всех чатов пользователя.
     * 
     * @return Flow списка чатов
     */
    fun getChatsFlow(): Flow<List<Chat>>

    /**
     * Получение чата по ID.
     * 
     * @param chatId Идентификатор чата
     * @return Чат или null если не найден
     */
    suspend fun getChatById(chatId: String): Chat?

    /**
     * Поток сообщений в чате.
     * 
     * @param chatId Идентификатор чата
     * @param limit Максимальное количество сообщений
     * @return Flow списка сообщений
     */
    fun getMessagesFlow(chatId: String, limit: Int = 50): Flow<List<Message>>

    /**
     * Отправка сообщения.
     * 
     * @param chatId Идентификатор чата
     * @param content Текст сообщения
     * @param mediaUrl URL медиа (опционально)
     * @return Отправленное сообщение
     */
    suspend fun sendMessage(
        chatId: String,
        content: String,
        mediaUrl: String? = null
    ): Message

    /**
     * Пометка сообщения как прочитанного.
     * 
     * @param messageId Идентификатор сообщения
     */
    suspend fun markMessageAsRead(messageId: String)

    /**
     * Получение текущего пользователя.
     * 
     * @return Текущий пользователь или null
     */
    suspend fun getCurrentUser(): User?

    /**
     * Поиск чатов по запросу.
     * 
     * @param query Поисковый запрос
     * @return Список найденных чатов
     */
    suspend fun searchChats(query: String): List<Chat>
}
