package com.tapik.messenger.presentation.ui.screens.chatManagement

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tapik.messenger.domain.model.Chat
import com.tapik.messenger.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatManagementUiState(
    val chat: Chat? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ChatManagementViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val chatId: String = savedStateHandle["chatId"] ?: ""

    private val _uiState = MutableStateFlow(ChatManagementUiState())
    val uiState: StateFlow<ChatManagementUiState> = _uiState.asStateFlow()

    init {
        loadChat()
    }

    private fun loadChat() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            chatRepository.getChatById(chatId).collect { chat ->
                _uiState.value = _uiState.value.copy(chat = chat, isLoading = false)
            }
        }
    }

    fun deleteChat(onDeleted: () -> Unit) {
        viewModelScope.launch {
            val result = chatRepository.deleteChat(chatId)
            if (result.isSuccess) {
                onDeleted()
            }
        }
    }
}
