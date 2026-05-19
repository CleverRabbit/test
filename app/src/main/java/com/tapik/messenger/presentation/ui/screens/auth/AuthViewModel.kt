package com.tapik.messenger.presentation.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tapik.messenger.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val phoneNumber: String = "",
    val code: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAuthenticated: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onPhoneNumberChange(phoneNumber: String) {
        _uiState.value = _uiState.value.copy(phoneNumber = phoneNumber, error = null)
    }

    fun onCodeChange(code: String) {
        _uiState.value = _uiState.value.copy(code = code, error = null)
    }

    fun login() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = authRepository.login(
                _uiState.value.phoneNumber,
                _uiState.value.code
            )
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(isLoading = false, isAuthenticated = true)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Login failed"
                )
            }
        }
    }
}
