package com.lockpact.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

class AuthViewModel : ViewModel() {
    private val repository = AuthRepository()

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            val result = repository.signIn(email.trim(), password)
            _uiState.value = if (result.isSuccess) {
                AuthUiState(isSuccess = true)
            } else {
                AuthUiState(error = friendlyAuthError(result.exceptionOrNull(), isSignIn = true))
            }
        }
    }

    fun signUp(email: String, password: String, username: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            val result = repository.signUp(email.trim(), password, username.trim())
            _uiState.value = if (result.isSuccess) {
                AuthUiState(isSuccess = true)
            } else {
                AuthUiState(error = friendlyAuthError(result.exceptionOrNull(), isSignIn = false))
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun friendlyAuthError(error: Throwable?, isSignIn: Boolean): String {
        val raw = error?.message.orEmpty().lowercase()
        return when {
            raw.contains("invalid_credentials") || raw.contains("invalid login credentials") ->
                "Email or password is incorrect."
            raw.contains("email_not_confirmed") || raw.contains("email not confirmed") ->
                "Please confirm your email before logging in."
            raw.contains("user already registered") || raw.contains("already registered") ->
                "An account with this email already exists."
            raw.contains("password") && raw.contains("weak") ->
                "Please use a stronger password."
            raw.contains("network") || raw.contains("timeout") ->
                "Network problem. Check your internet and try again."
            isSignIn -> "Could not log in. Check your details and try again."
            else -> "Could not create account. Please try again."
        }
    }
}
