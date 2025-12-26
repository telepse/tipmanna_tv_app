package com.example.telepse.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.telepse.data.model.Character
import com.example.telepse.data.model.Event
import com.example.telepse.data.realtime.PusherService
import com.example.telepse.data.repository.EventRepository
import com.example.telepse.ui.animations.AnimationType
import com.example.telepse.ui.animations.AnimationTypeMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import android.util.Log
import javax.inject.Inject

/**
 * Data class representing an animation trigger request.
 * 
 * @property animationType The type of animation to display
 * @property imageUrl URL of the character/emoji image
 */
data class AnimationTrigger(
    val animationType: AnimationType,
    val imageUrl: String
)

data class FundraisingMilestoneUiState(
    val event: Event? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val pendingAnimations: List<AnimationTrigger> = emptyList()
)

@HiltViewModel
class FundraisingMilestoneViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val pusherService: PusherService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(FundraisingMilestoneUiState())
    val uiState: StateFlow<FundraisingMilestoneUiState> = _uiState.asStateFlow()
    
    /**
     * Loads event details and initializes Pusher subscription.
     * 
     * This method:
     * 1. Fetches the event from the API
     * 2. Initializes Pusher connection
     * 3. Subscribes to the event channel
     * 4. Sets up event listeners for real-time updates
     * 
     * @param eventId The ID of the event to load
     */
    fun loadEvent(eventId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )
            
            try {
                // Fetch event details first
                val result = eventRepository.getEventById(eventId)
                result.fold(
                    onSuccess = { event ->
                        _uiState.value = _uiState.value.copy(
                            event = event,
                            isLoading = false,
                            error = null
                        )
                        
                        // Initialize Pusher and subscribe after fetching event
                        initializePusher(eventId, event)
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to load event"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error loading event: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Initializes Pusher connection and sets up event listeners.
     */
    private fun initializePusher(eventId: String, event: Event) {
        viewModelScope.launch {
            try {
                // Initialize Pusher first
                if (!pusherService.initialize()) {
                    Log.e("FundraisingMilestoneViewModel", "Failed to initialize Pusher")
                    return@launch
                }
                
                // Wait a bit for connection to establish
                kotlinx.coroutines.delay(500)
                
                // Subscribe to channel
                val channelName = "event-$eventId"
                if (!pusherService.subscribeToChannel(channelName)) {
                    Log.e("FundraisingMilestoneViewModel", "Failed to subscribe to channel: $channelName")
                    return@launch
                }
                
                // Wait a bit for channel subscription to be ready
                kotlinx.coroutines.delay(300)
                
                Log.d("FundraisingMilestoneViewModel", "Pusher initialized and subscribed to: $channelName")
            } catch (e: Exception) {
                // Handle Pusher initialization errors
                Log.e("FundraisingMilestoneViewModel", "Error initializing Pusher", e)
            }
        }
        
        // Observe tip-animation events in separate coroutine
        viewModelScope.launch {
            try {
                pusherService.observeTipAnimation()
                    .catch { e -> 
                        Log.e("FundraisingMilestoneViewModel", "Error in tip-animation flow", e)
                    }
                    .collect { tipAnimationEvent ->
                        Log.d("FundraisingMilestoneViewModel", "Received tip-animation event: ${tipAnimationEvent.selectedTips.size} tips")
                        // Use current event from state (may have been updated)
                        val currentEvent = _uiState.value.event
                        if (currentEvent != null) {
                            handleTipAnimationEvent(tipAnimationEvent, currentEvent)
                        } else {
                            Log.w("FundraisingMilestoneViewModel", "Event is null, cannot handle tip animation")
                        }
                    }
            } catch (e: Exception) {
                Log.e("FundraisingMilestoneViewModel", "Error collecting tip-animation", e)
            }
        }
        
        // Observe amount updates in separate coroutine
        viewModelScope.launch {
            try {
                pusherService.observeAmountUpdated()
                    .catch { e -> 
                        Log.e("FundraisingMilestoneViewModel", "Error in amount-updated flow", e)
                    }
                    .collect { amountUpdated ->
                        Log.d("FundraisingMilestoneViewModel", "Received amount-updated: ${amountUpdated.amountRaised}, ${amountUpdated.numOfTips}")
                        updateAmountRaised(amountUpdated.amountRaised, amountUpdated.numOfTips)
                    }
            } catch (e: Exception) {
                Log.e("FundraisingMilestoneViewModel", "Error collecting amount-updated", e)
            }
        }
        
        // Observe screen settings updates in separate coroutine
        viewModelScope.launch {
            try {
                pusherService.observeScreenSettingsUpdated()
                    .catch { e -> 
                        Log.e("FundraisingMilestoneViewModel", "Error in settings-updated flow", e)
                    }
                    .collect { settingsUpdated ->
                        Log.d("FundraisingMilestoneViewModel", "Received settings-updated event")
                        updateScreenSettings(settingsUpdated)
                    }
            } catch (e: Exception) {
                Log.e("FundraisingMilestoneViewModel", "Error collecting settings-updated", e)
            }
        }
    }
    
    /**
     * Handles tip-animation events by finding characters and triggering animations.
     */
    private fun handleTipAnimationEvent(
        tipAnimationEvent: com.example.telepse.data.model.TipAnimationEvent,
        event: Event
    ) {
        Log.d("FundraisingMilestoneViewModel", "Handling tip-animation event with ${tipAnimationEvent.selectedTips.size} tips")
        Log.d("FundraisingMilestoneViewModel", "Event has ${event.characterSets?.size ?: 0} character sets")
        
        // Log all character IDs in the event for debugging
        val allCharacterIds = event.characterSets
            ?.flatMap { it.characters ?: emptyList() }
            ?.map { it.id } ?: emptyList()
        Log.d("FundraisingMilestoneViewModel", "Available character IDs: $allCharacterIds")
        
        val animations = tipAnimationEvent.selectedTips.mapNotNull { selectedTip ->
            Log.d("FundraisingMilestoneViewModel", "Looking for character ID: ${selectedTip.characterId}")
            
            // Find character by characterId in characterSets
            val character = findCharacterById(selectedTip.characterId, event)
            
            if (character == null) {
                Log.w("FundraisingMilestoneViewModel", "Character not found for ID: ${selectedTip.characterId}")
                Log.w("FundraisingMilestoneViewModel", "Available IDs: $allCharacterIds")
            } else {
                Log.d("FundraisingMilestoneViewModel", "Found character: ${character.name}, style: ${selectedTip.styleName}, iconUrl: ${character.iconUrl}")
            }
            
            character?.let {
                val animationType = AnimationTypeMapper.mapStyleName(selectedTip.styleName)
                Log.d("FundraisingMilestoneViewModel", "Mapped style '${selectedTip.styleName}' to $animationType")
                AnimationTrigger(
                    animationType = animationType,
                    imageUrl = it.iconUrl
                )
            }
        }
        
        if (animations.isNotEmpty()) {
            Log.d("FundraisingMilestoneViewModel", "Adding ${animations.size} animations to pending list (current: ${_uiState.value.pendingAnimations.size})")
            _uiState.value = _uiState.value.copy(
                pendingAnimations = _uiState.value.pendingAnimations + animations
            )
            Log.d("FundraisingMilestoneViewModel", "New pending animations count: ${_uiState.value.pendingAnimations.size}")
        } else {
            Log.w("FundraisingMilestoneViewModel", "No animations created from tip event - all characters were null")
        }
    }
    
    /**
     * Finds a character by ID in the event's character sets.
     */
    private fun findCharacterById(characterId: String, event: Event): Character? {
        return event.characterSets
            ?.flatMap { it.characters ?: emptyList() }
            ?.firstOrNull { it.id == characterId }
    }
    
    /**
     * Updates the amount raised and number of tips from Pusher event.
     */
    private fun updateAmountRaised(amountRaised: Double, numOfTips: Int) {
        val currentEvent = _uiState.value.event
        if (currentEvent != null) {
            _uiState.value = _uiState.value.copy(
                event = currentEvent.copy(
                    amountRaised = amountRaised,
                    numOfTips = numOfTips
                )
            )
        }
    }
    
    /**
     * Updates screen settings from Pusher event.
     */
    private fun updateScreenSettings(settingsUpdated: com.example.telepse.data.model.ScreenSettingsUpdatedEvent) {
        val currentEvent = _uiState.value.event
        if (currentEvent != null) {
            _uiState.value = _uiState.value.copy(
                event = currentEvent.copy(
                    amountRaised = settingsUpdated.amountRaised ?: currentEvent.amountRaised,
                    numOfTips = settingsUpdated.numOfTips ?: currentEvent.numOfTips,
                    hideProgressBarAmount = settingsUpdated.hideProgressBarAmount ?: currentEvent.hideProgressBarAmount,
                    showLatestAmount = settingsUpdated.showLatestAmount ?: currentEvent.showLatestAmount,
                    tipUsageDisclosure = settingsUpdated.tipUsageDisclosure ?: currentEvent.tipUsageDisclosure,
                    backgroundType = settingsUpdated.backgroundType ?: currentEvent.backgroundType
                )
            )
        }
    }
    
    /**
     * Removes a completed animation from the pending list.
     */
    fun removeAnimation(animation: AnimationTrigger) {
        Log.d("FundraisingMilestoneViewModel", "Removing animation: ${animation.animationType}, current count: ${_uiState.value.pendingAnimations.size}")
        _uiState.value = _uiState.value.copy(
            pendingAnimations = _uiState.value.pendingAnimations.filter { it != animation }
        )
        Log.d("FundraisingMilestoneViewModel", "Animation removed, new count: ${_uiState.value.pendingAnimations.size}")
    }
    
    override fun onCleared() {
        super.onCleared()
        pusherService.disconnect()
    }
}
