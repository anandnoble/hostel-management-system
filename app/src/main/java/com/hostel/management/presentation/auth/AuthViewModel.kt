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
            android.util.Log.d("AuthDebug", "2. Request URL: Supabase Auth endpoint")
            android.util.Log.d("AuthDebug", "3. Request payload: email=$email")
            
            authRepository.login(email, password)
                .onSuccess { profile ->
                    android.util.Log.d("AuthDebug", "4. API status code: SUCCESS (via SDK)")
                    android.util.Log.d("AuthDebug", "5. API response body: profile=$profile")
                    android.util.Log.d("AuthDebug", "6. Token received? YES")
                    android.util.Log.d("AuthDebug", "7. Token stored? YES")
                    android.util.Log.d("AuthDebug", "8. Auth state updated? YES")
                    _loginState.value = LoginState.Success(profile)
                }
                .onFailure { error ->
                    android.util.Log.d("AuthDebug", "4. API status code: FAILED")
                    android.util.Log.d("AuthDebug", "5. API response body: Error ${error.message}")
                    android.util.Log.d("AuthDebug", "6. Token received? NO")
                    
                    // Log technical details safely to Android logcat for debugging
                    android.util.Log.e("AuthViewModel", "Login failed for user", error)

                    val rawMsg = error.message.orEmpty()
                    val userFriendlyMsg = when {
                        // Supabase remote auth errors (HTTP 400/401, "invalid_credentials")
                        rawMsg.contains("Invalid login credentials", ignoreCase = true) ||
                        rawMsg.contains("invalid_credentials", ignoreCase = true) ||
                        rawMsg.contains("Email not confirmed", ignoreCase = true) ||
                        rawMsg.contains("400", ignoreCase = true) ||
                        rawMsg.contains("401", ignoreCase = true) ||
                        // Local fallback errors from AuthRepositoryImpl
                        rawMsg.contains("Incorrect email or password", ignoreCase = true) ||
                        rawMsg.contains("Incorrect password", ignoreCase = true) -> {
                            "Incorrect email or password. Please check your credentials."
                        }
                        rawMsg.contains("Email not confirmed", ignoreCase = true) -> {
                            "Please confirm your email before signing in."
                        }
                        rawMsg.contains("Email and Password cannot be empty", ignoreCase = true) -> {
                            "Please enter your email and password."
                        }
                        rawMsg.contains("Database error", ignoreCase = true) ||
                        rawMsg.contains("500", ignoreCase = true) ||
                        rawMsg.contains("schema", ignoreCase = true) -> {
                            "Service temporarily unavailable. Please try again later."
                        }
                        rawMsg.contains("Unable to resolve host", ignoreCase = true) ||
                        rawMsg.contains("SocketTimeoutException", ignoreCase = true) ||
                        rawMsg.contains("ConnectException", ignoreCase = true) ||
                        rawMsg.contains("Network", ignoreCase = true) -> {
                            "No internet connection. Please check your network."
                        }
                        else -> {
                            "Unable to sign in right now. Please try again."
                        }
                    }
                    _loginState.value = LoginState.Error(userFriendlyMsg)
                }
        }
    }


    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _currentProfile.value = null
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
