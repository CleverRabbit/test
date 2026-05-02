package com.messenger.feature.chat.presentation.model

/**
 * Состояние UI экрана чата.
 * 
 * Используется для управления состоянием Compose UI
 * в рамках Unidirectional Data Flow.
 */
sealed class ChatUiState {
    
    /**
     * Начальное состояние загрузки.
     */
    object Loading : ChatUiState()
    
    /**
     * Состояние с данными для отображения.
     * 
     * @param chatInfo информация о чате
     * @param messages список сообщений
     * @param isLoadingMore true если идет загрузка старых сообщений
     */
    data class Success(
        val chatInfo: ChatUiModel,
        val messages: List<MessageUiModel>,
        val isLoadingMore: Boolean = false
    ) : ChatUiState()
    
    /**
     * Состояние ошибки.
     * 
     * @param message текст ошибки
     */
    data class Error(val message: String) : ChatUiState()
    
    /**
     * Чат не найден.
     */
    object NotFound : ChatUiState()
}

/**
 * События UI экрана чата.
 * 
 * Представляют действия пользователя или системные события.
 */
sealed class ChatUiEvent {
    
    /**
     * Отправка текстового сообщения.
     * 
     * @param content текст сообщения
     */
    data class SendMessage(val content: String) : ChatUiEvent()
    
    /**
     * Отправка медиа файла.
     * 
     * @param uri URI файла
     * @param caption подпись
     */
    data class SendMedia(val uri: String, val caption: String? = null) : ChatUiEvent()
    
    /**
     * Удаление сообщения.
     * 
     * @param messageId ID сообщения
     * @param forAll удалить для всех
     */
    data class DeleteMessage(val messageId: String, val forAll: Boolean = false) : ChatUiEvent()
    
    /**
     * Ответ на сообщение.
     * 
     * @param messageId ID сообщения для ответа
     */
    data class ReplyToMessage(val messageId: String) : ChatUiEvent()
    
    /**
     * Открытие профиля участника.
     * 
     * @param userId ID пользователя
     */
    data class OpenUserProfile(val userId: String) : ChatUiEvent()
    
    /**
     * Загрузка старых сообщений (пагинация).
     */
    object LoadMoreMessages : ChatUiEvent()
    
    /**
     * Пометить сообщения как прочитанные.
     */
    object MarkAsRead : ChatUiEvent()
    
    /**
     * Ошибка отображена, сбросить состояние ошибки.
     */
    object ErrorShown : ChatUiEvent()
}
