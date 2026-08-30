package com.hostel.management.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hostel.management.domain.model.Profile
import com.hostel.management.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    private val _currentProfile = MutableStateFlow<Profile?>(null)
    val currentProfile: StateFlow<Profile?> = _currentProfile.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.getSessionFlow().collect { profile ->
                _currentProfile.value = profile
            }
        }
        checkSession()
    }

    fun checkSession() {
        viewModelScope.launch {
            authRepository.getCurrentProfile()
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _loginState.value = LoginState.Error("Email and Password cannot be empty")
            return
        }
        _loginState.value = LoginState.Loading
        viewModelScope.launch {
            authRepository.login(email, password)
                .onSuccess { profile ->
                    _loginState.value = LoginState.Success(profile)
                }
                .onFailure { error ->
                    _loginState.value = LoginState.Error(error.localizedMessage ?: "Login failed")
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _loginState.value = LoginState.Idle
        }
    }

    fun resetPassword(email: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (email.isBlank()) {
            onError("Email cannot be empty")
            return
        }
        viewModelScope.launch {
            // Simulated / Mock reset link
            onSuccess()
        }
    }

    fun clearError() {
        _loginState.value = LoginState.Idle
    }
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val profile: Profile) : LoginState()
    data class Error(val message: String) : LoginState()
}
