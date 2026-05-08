package com.matrix.tapikapp.domain.usecase

import com.matrix.tapikapp.domain.model.Chat
import com.matrix.tapikapp.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case для получения списка чатов.
 * 
 * Следует принципу Single Responsibility - отвечает только за получение чатов.
 * Использует Repository абстракцию, не зная о деталях реализации.
 */
class GetChatsUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    /**
     * Выполняет use case и возвращает поток чатов.
     * 
     * @return Flow списка чатов для наблюдения в ViewModel
     */
    operator fun invoke(): Flow<List<Chat>> {
        return chatRepository.getChatsFlow()
    }
}
