package com.example.telepse.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.telepse.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TipperLoginUiState(
    val email: String = "",
    val code: String = "",
    val step: LoginStep = LoginStep.EMAIL,
    val loading: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val loginSucceeded: Boolean = false,
)

enum class LoginStep {
    EMAIL,
    CODE,
}

@HiltViewModel
class TipperLoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TipperLoginUiState())
    val uiState: StateFlow<TipperLoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, error = null) }
    }

    fun onCodeChange(value: String) {
        _uiState.update { it.copy(code = value, error = null) }
    }

    fun sendCode() {
        val email = _uiState.value.email.trim()
        if (email.isBlank()) {
            _uiState.update { it.copy(error = "Email is required") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null, message = null) }
            authRepository.requestLoginCode(email)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            loading = false,
                            step = LoginStep.CODE,
                            message = "Verification code sent to your email",
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(loading = false, error = e.message ?: "Login failed")
                    }
                }
        }
    }

    fun verifyCode() {
        val email = _uiState.value.email.trim()
        val code = _uiState.value.code.trim()
        if (code.isBlank()) {
            _uiState.update { it.copy(error = "Enter the verification code") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            authRepository.verifyCode(email, code)
                .onSuccess {
                    _uiState.update {
                        it.copy(loading = false, loginSucceeded = true, message = "Signed in")
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(loading = false, error = e.message ?: "Verification failed")
                    }
                }
        }
    }
}
