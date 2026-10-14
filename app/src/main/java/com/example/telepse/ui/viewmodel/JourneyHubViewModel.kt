package com.example.telepse.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.telepse.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class JourneyHubUiState(
    val loggedIn: Boolean = false,
    val tipperEmail: String? = null,
)

@HiltViewModel
class JourneyHubViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(
            JourneyHubUiState(
                loggedIn = authRepository.isLoggedIn(),
                tipperEmail = authRepository.currentEmail(),
            ),
        )
    val uiState: StateFlow<JourneyHubUiState> = _uiState.asStateFlow()

    fun refresh() {
        _uiState.value =
            JourneyHubUiState(
                loggedIn = authRepository.isLoggedIn(),
                tipperEmail = authRepository.currentEmail(),
            )
    }

    fun signOut() {
        authRepository.signOut()
        refresh()
    }
}
