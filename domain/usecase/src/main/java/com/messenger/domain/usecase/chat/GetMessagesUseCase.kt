package com.messenger.domain.usecase.chat

import com.messenger.domain.model.Message
import com.messenger.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow

/**
 * UseCase для получения сообщений чата.
 * 
 * Предоставляет реактивный поток сообщений, обновляемый
 * при получении новых сообщений или изменении существующих.
 * 
 * @param chatRepository репозиторий чатов
 */
class GetMessagesUseCase(
    private val chatRepository: ChatRepository
) {
    
    /**
     * Возвращает поток сообщений чата.
     * 
     * @param chatId идентификатор чата
     * @return Flow со списком сообщений, сортированных по времени
     */
    operator fun invoke(chatId: String): Flow<List<Message>> {
        return chatRepository.getMessages(chatId)
    }
}
