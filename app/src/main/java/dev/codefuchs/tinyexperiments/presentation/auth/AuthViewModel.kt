package dev.codefuchs.tinyexperiments.presentation.auth

import android.util.Patterns
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.codefuchs.tinyexperiments.data.repository.AuthRepository
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository()
): ViewModel() {
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var isSignedIn by mutableStateOf(false)

    init {
        isSignedIn = authRepository.currentUser != null
    }

    fun updateEmail(newEmail: String) {
        email = newEmail
    }

    fun updatePassword(newPassword: String) {
        password = newPassword
    }

    fun signIn() {
        viewModelScope.launch {
            errorMessage = null

            errorMessage = validateCredentials()
            if (errorMessage != null) return@launch

            isLoading = true
            val result = authRepository.signInWithEmail(email, password)
            result.onSuccess {
                user -> isSignedIn = true
            }.onFailure {
                exception -> errorMessage = exception.message
            }
            isLoading = false
        }
    }

    fun signUp() {
        viewModelScope.launch {
            errorMessage = null

            errorMessage = validateCredentials()
            if (errorMessage != null) return@launch

            isLoading = true
            val result = authRepository.signUpWithEmail(email, password)
            result.onSuccess {
                user -> isSignedIn = true
            }.onFailure {
                exception -> errorMessage = exception.message
            }
            isLoading = false
        }
    }

    fun signOut() {
        viewModelScope.launch {
            errorMessage = null
            authRepository.signOut()
            isSignedIn = false
            isLoading = false
        }
    }

    private fun validateCredentials(): String? {
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return "This is not a valid email format"
        }
        if (password.length< 6) {
            return "Password MUST be 6 characters or above"
        }
        return null
    }
}