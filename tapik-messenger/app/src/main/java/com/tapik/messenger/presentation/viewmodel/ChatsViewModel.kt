package com.tapik.messenger.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tapik.messenger.domain.model.Chat
import com.tapik.messenger.domain.repository.AuthRepository
import com.tapik.messenger.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatsUiState(
    val chats: List<Chat> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val isSearching: Boolean = false
)

@HiltViewModel
class ChatsViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatsUiState())
    val uiState: StateFlow<ChatsUiState> = _uiState.asStateFlow()

    init {
        loadChats()
    }

    private fun loadChats() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            chatRepository.chats.collect { chats ->
                _uiState.value = _uiState.value.copy(chats = chats, isLoading = false)
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query, isSearching = query.isNotEmpty())
        if (query.isNotEmpty()) {
            performSearch(query)
        } else {
            loadChats()
        }
    }

    private fun performSearch(query: String) {
        viewModelScope.launch {
            chatRepository.searchChats(query).collect { results ->
                _uiState.value = _uiState.value.copy(chats = results)
            }
        }
    }

    fun closeSearch() {
        _uiState.value = _uiState.value.copy(searchQuery = "", isSearching = false)
        loadChats()
    }
}
