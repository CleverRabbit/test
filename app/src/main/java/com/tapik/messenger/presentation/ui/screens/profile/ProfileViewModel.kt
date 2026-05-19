package com.tapik.messenger.presentation.ui.screens.profile

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
    val error: String? = null,
    val isEditing: Boolean = false,
    val editedUsername: String = "",
    val editedBio: String = ""
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
            authRepository.currentUser.collect { user ->
                _uiState.value = _uiState.value.copy(
                    user = user,
                    editedUsername = user?.username ?: "",
                    editedBio = user?.bio ?: ""
                )
            }
        }
    }

    fun toggleEdit() {
        _uiState.value = _uiState.value.copy(isEditing = !_uiState.value.isEditing)
    }

    fun onUsernameChange(username: String) {
        _uiState.value = _uiState.value.copy(editedUsername = username)
    }

    fun onBioChange(bio: String) {
        _uiState.value = _uiState.value.copy(editedBio = bio)
    }

    fun saveProfile() {
        viewModelScope.launch {
            val currentUser = _uiState.value.user ?: return@launch
            val updatedUser = currentUser.copy(
                username = _uiState.value.editedUsername,
                bio = _uiState.value.editedBio
            )
            val result = authRepository.updateProfile(updatedUser)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(isEditing = false)
            } else {
                _uiState.value = _uiState.value.copy(
                    error = result.exceptionOrNull()?.message ?: "Failed to update profile"
                )
            }
        }
    }
}
