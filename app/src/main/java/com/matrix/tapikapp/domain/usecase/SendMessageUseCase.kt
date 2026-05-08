package com.matrix.tapikapp.domain.usecase

import com.matrix.tapikapp.domain.model.Message
import com.matrix.tapikapp.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case для отправки сообщения.
 * 
 * Инкапсулирует бизнес-логику отправки сообщения через репозиторий.
 */
class SendMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    /**
     * Отправляет сообщение в указанный чат.
     * 
     * @param chatId Идентификатор чата
     * @param content Текст сообщения
     * @param mediaUrl URL медиа-вложения (опционально)
     * @return Отправленное сообщение со статусом
     */
    suspend operator fun invoke(
        chatId: String,
        content: String,
        mediaUrl: String? = null
    ): Message {
        require(content.isNotBlank()) { "Текст сообщения не может быть пустым" }
        require(chatId.isNotBlank()) { "ID чата не может быть пустым" }
        
        return chatRepository.sendMessage(chatId, content, mediaUrl)
    }
}
