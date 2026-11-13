package com.example.telepse.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.telepse.data.repository.BlockedHostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BlockedHostsUiState(
    val hostIds: List<String> = emptyList(),
    val loading: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

@HiltViewModel
class BlockedHostsViewModel @Inject constructor(
    private val blockedHostRepository: BlockedHostRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(BlockedHostsUiState())
    val state: StateFlow<BlockedHostsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            blockedHostRepository.blockedHostIds.collect { ids ->
                _state.update { it.copy(hostIds = ids.sorted()) }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            blockedHostRepository
                .refresh()
                .onSuccess { _state.update { it.copy(loading = false) } }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            loading = false,
                            error = error.message ?: "Could not load blocked hosts.",
                        )
                    }
                }
        }
    }

    fun unblock(hostId: String) {
        viewModelScope.launch {
            blockedHostRepository
                .unblockHost(hostId)
                .onSuccess {
                    _state.update {
                        it.copy(message = "Host unblocked.")
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(error = error.message ?: "Could not unblock host.")
                    }
                }
        }
    }

    fun clearMessages() {
        _state.update { it.copy(message = null, error = null) }
    }
}
