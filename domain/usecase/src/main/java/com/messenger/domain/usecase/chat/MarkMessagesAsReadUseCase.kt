package com.messenger.domain.usecase.chat

import com.messenger.domain.repository.ChatRepository

/**
 * UseCase для отметки сообщений как прочитанные.
 * 
 * Реализует бизнес-логику обновления статуса прочтения
 * с синхронизацией на сервере и локальным кэшированием.
 * 
 * @param chatRepository репозиторий чатов
 */
class MarkMessagesAsReadUseCase(
    private val chatRepository: ChatRepository
) {
    
    /**
     * Помечает все непрочитанные сообщения в чате как прочитанные.
     * 
     * @param chatId идентификатор чата
     */
    suspend operator fun invoke(chatId: String) {
        // Получаем чат для проверки существования
        val chat = chatRepository.getChatById(chatId) ?: return
        
        // В реальной реализации здесь будет получение списка непрочитанных сообщений
        // Для простоты передаем пустой список - репозиторий сам определит какие сообщения отметить
        chatRepository.markMessagesAsRead(
            chatId = chatId,
            messageIds = emptyList()
        )
    }
}
