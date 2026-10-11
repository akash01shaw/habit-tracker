package com.loopwise.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loopwise.app.data.repository.AuthRepository
import com.loopwise.app.data.repository.AuthUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AuthFormState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
)

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    val currentUser: StateFlow<AuthUser?> = authRepository.currentUser
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _formState = MutableStateFlow(AuthFormState())
    val formState: StateFlow<AuthFormState> = _formState

    fun onEmailChange(value: String) {
        _formState.value = _formState.value.copy(email = value, error = null)
    }

    fun onPasswordChange(value: String) {
        _formState.value = _formState.value.copy(password = value, error = null)
    }

    fun signIn() = runAuthAction {
        authRepository.signInWithEmail(_formState.value.email.trim(), _formState.value.password)
    }

    fun signUp() = runAuthAction {
        authRepository.signUpWithEmail(_formState.value.email.trim(), _formState.value.password)
    }

    fun signInWithGoogle(idToken: String, rawNonce: String?) = runAuthAction {
        authRepository.signInWithGoogleIdToken(idToken, rawNonce)
    }

    private fun runAuthAction(block: suspend () -> Unit) {
        viewModelScope.launch {
            _formState.value = _formState.value.copy(isLoading = true, error = null)
            runCatching { block() }
                .onSuccess { _formState.value = _formState.value.copy(isLoading = false) }
                .onFailure {
                    _formState.value = _formState.value.copy(
                        isLoading = false,
                        error = it.message ?: "Something went wrong. Please try again.",
                    )
                }
        }
    }
}
