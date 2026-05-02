package com.messenger.domain.usecase.chat

import com.messenger.domain.model.Chat
import com.messenger.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow

/**
 * UseCase для получения списка чатов.
 * 
 * Предоставляет реактивный поток чатов, обновляемый
 * при изменениях в базе данных или получении новых сообщений.
 * 
 * @param chatRepository репозиторий чатов
 */
class GetChatsUseCase(
    private val chatRepository: ChatRepository
) {
    
    /**
     * Возвращает поток списка чатов.
     * 
     * @return Flow со списком чатов, сортированных по времени последнего сообщения
     */
    operator fun invoke(): Flow<List<Chat>> {
        return chatRepository.getChats()
    }
}
