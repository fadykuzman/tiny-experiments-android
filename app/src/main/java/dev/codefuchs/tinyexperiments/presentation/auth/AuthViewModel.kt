package dev.codefuchs.tinyexperiments.presentation.auth

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

    fun updateEmail(newEmail: String) {
        email = newEmail
    }

    fun updatePassword(newPassword: String) {
        password = newPassword
    }

    fun signIn() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
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
            isLoading = true
            errorMessage = null
            val result = authRepository.signUpWithEmail(email, password)
            result.onSuccess {
                user -> isSignedIn = true
            }.onFailure {
                exception -> errorMessage = exception.message
            }
            isLoading = false
        }
    }
}