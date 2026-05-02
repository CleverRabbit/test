package com.messenger.domain.usecase.chat

import com.messenger.core.common.result.Result
import com.messenger.domain.model.Message
import com.messenger.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow

/**
 * UseCase для отправки сообщения в чат.
 * 
 * Реализует бизнес-логику отправки сообщений с обработкой ошибок
 * и гарантией доставки через offline очередь.
 * 
 * @param chatRepository репозиторий чатов
 */
class SendMessageUseCase(
    private val chatRepository: ChatRepository
) {
    
    /**
     * Отправляет текстовое сообщение в чат.
     * 
     * @param chatId идентификатор чата
     * @param content текст сообщения
     * @param replyToMessageId ID сообщения для ответа (опционально)
     * @return Result с отправленным сообщением или ошибкой
     */
    suspend operator fun invoke(
        chatId: String,
        content: String,
        replyToMessageId: String? = null
    ): Result<Message> {
        // Валидация входных данных
        if (content.isBlank()) {
            return Result.Error(Exception("Сообщение не может быть пустым"))
        }
        
        if (chatId.isBlank()) {
            return Result.Error(Exception("Некорректный ID чата"))
        }
        
        // Отправка сообщения через репозиторий
        return chatRepository.sendMessage(
            chatId = chatId,
            content = content.trim(),
            replyToMessageId = replyToMessageId
        )
    }
}
