package com.example.telepse.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.telepse.data.model.Event
import com.example.telepse.data.network.NetworkMonitor
import com.example.telepse.data.repository.EventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log
import javax.inject.Inject

data class CodeInputUiState(
    val code: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isValid: Boolean = false,
    val events: List<Event> = emptyList(),
    val currentEventIndex: Int = 0
)

@HiltViewModel
class CodeInputViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CodeInputUiState())
    val uiState: StateFlow<CodeInputUiState> = _uiState.asStateFlow()
    
    fun updateCode(code: String) {
        _uiState.value = _uiState.value.copy(
            code = code,
            error = null
        )
    }
    
    fun searchEvents() {
        val code = _uiState.value.code.trim()
        
        if (code.isBlank()) {
            _uiState.value = _uiState.value.copy(
                error = "Please enter a code"
            )
            return
        }
        
        // Check network status before attempting search
        if (!networkMonitor.isCurrentlyOnline()) {
            Log.w("CodeInputViewModel", "⚠️ No network connection, cannot search events")
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "No internet connection. Please check your network and try again."
            )
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val result = eventRepository.getEventsByCode(code)
                result.fold(
                    onSuccess = { events ->
                        if (events.isNotEmpty()) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                events = events,
                                currentEventIndex = 0,
                                isValid = true,
                                error = null
                            )
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "No events found for this code."
                            )
                        }
                    },
                    onFailure = { exception ->
                        // Check if error is due to network
                        val errorMessage = if (!networkMonitor.isCurrentlyOnline()) {
                            "No internet connection. Please check your network and try again."
                        } else {
                            exception.message ?: "Error fetching events. Please try again."
                        }
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = errorMessage
                        )
                    }
                )
            } catch (e: Exception) {
                val errorMessage = if (!networkMonitor.isCurrentlyOnline()) {
                    "No internet connection. Please check your network and try again."
                } else {
                    "Error validating code: ${e.message}"
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = errorMessage
                )
            }
        }
    }
    
    fun nextEvent() {
        val currentIndex = _uiState.value.currentEventIndex
        val events = _uiState.value.events
        if (events.isNotEmpty()) {
            val nextIndex = (currentIndex + 1) % events.size
            _uiState.value = _uiState.value.copy(currentEventIndex = nextIndex)
        }
    }
    
    fun previousEvent() {
        val currentIndex = _uiState.value.currentEventIndex
        val events = _uiState.value.events
        if (events.isNotEmpty()) {
            val previousIndex = if (currentIndex == 0) events.size - 1 else currentIndex - 1
            _uiState.value = _uiState.value.copy(currentEventIndex = previousIndex)
        }
    }
    
    fun getCurrentEvent(): Event? {
        val events = _uiState.value.events
        val index = _uiState.value.currentEventIndex
        return if (events.isNotEmpty() && index in events.indices) {
            events[index]
        } else {
            null
        }
    }
}
