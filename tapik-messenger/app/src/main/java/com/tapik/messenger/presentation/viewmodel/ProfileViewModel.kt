package com.tapik.messenger.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tapik.messenger.domain.model.User
import com.tapik.messenger.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val username: String = "",
    val bio: String = "",
    val isEditing: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            authRepository.currentUser.collect { user ->
                _uiState.value = _uiState.value.copy(
                    user = user,
                    isLoading = false,
                    username = user?.username ?: "",
                    bio = user?.bio ?: ""
                )
            }
        }
    }

    fun onUsernameChange(username: String) {
        _uiState.value = _uiState.value.copy(username = username)
    }

    fun onBioChange(bio: String) {
        _uiState.value = _uiState.value.copy(bio = bio)
    }

    fun saveProfile() {
        viewModelScope.launch {
            val currentUser = _uiState.value.user
            if (currentUser != null) {
                val updatedUser = currentUser.copy(
                    username = _uiState.value.username,
                    bio = _uiState.value.bio
                )
                authRepository.updateProfile(updatedUser)
                _uiState.value = _uiState.value.copy(isEditing = false)
            }
        }
    }

    fun startEditing() {
        _uiState.value = _uiState.value.copy(isEditing = true)
    }

    fun cancelEditing() {
        loadProfile()
        _uiState.value = _uiState.value.copy(isEditing = false)
    }
}
