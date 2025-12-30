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
 * @property id Unique identifier for this animation instance
 */
data class AnimationTrigger(
    val animationType: AnimationType,
    val imageUrl: String,
    val id: String = java.util.UUID.randomUUID().toString()
)

data class FundraisingMilestoneUiState(
    val event: Event? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val pendingAnimations: List<AnimationTrigger> = emptyList(),
    val toastMessage: String? = null
)

@HiltViewModel
class FundraisingMilestoneViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val pusherService: PusherService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(FundraisingMilestoneUiState())
    val uiState: StateFlow<FundraisingMilestoneUiState> = _uiState.asStateFlow()
    
    private var currentEventId: String? = null
    private var isPusherInitialized = false
    private var flowJobs: MutableList<kotlinx.coroutines.Job> = mutableListOf()
    
    private fun showToast(message: String) {
        // Only update if message is different to avoid rapid state updates
        if (_uiState.value.toastMessage != message) {
            _uiState.value = _uiState.value.copy(toastMessage = message)
            // Clear toast after a short delay
            viewModelScope.launch {
                kotlinx.coroutines.delay(2000) // Show for 2 seconds
                if (_uiState.value.toastMessage == message) {
                    _uiState.value = _uiState.value.copy(toastMessage = null)
                }
            }
        }
    }
    
    /**
     * Loads event details and initializes Pusher subscription.
     * 
     * This method:
     * 1. Disconnects from previous event if switching events
     * 2. Fetches the event from the API
     * 3. Initializes Pusher connection
     * 4. Subscribes to the event channel
     * 5. Sets up event listeners for real-time updates
     * 
     * @param eventId The ID of the event to load
     */
    fun loadEvent(eventId: String) {
        viewModelScope.launch {
            // If switching to a different event, disconnect from the old one first
            if (currentEventId != null && currentEventId != eventId) {
                Log.d("FundraisingMilestoneViewModel", "🔄 Switching events: $currentEventId -> $eventId")
                disconnectFromCurrentEvent()
            }
            
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                pendingAnimations = emptyList() // Clear animations when loading new event
            )
            
            try {
                // Fetch event details first
                val result = eventRepository.getEventById(eventId)
                result.fold(
                    onSuccess = { event ->
                        currentEventId = eventId
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
     * Disconnects from the current event's Pusher channel.
     * Called when switching to a different event.
     */
    private fun disconnectFromCurrentEvent() {
        Log.d("FundraisingMilestoneViewModel", "🔌 Disconnecting from current event")
        
        // Cancel all flow collection jobs
        flowJobs.forEach { job ->
            job.cancel()
        }
        flowJobs.clear()
        
        // Disconnect from Pusher
        pusherService.disconnect()
        isPusherInitialized = false
        currentEventId = null
        
        Log.d("FundraisingMilestoneViewModel", "✅ Disconnected and cleaned up")
    }
    
    /**
     * Initializes Pusher connection and sets up event listeners.
     */
    private fun initializePusher(eventId: String, event: Event) {
        // Only initialize if not already initialized for this event
        if (isPusherInitialized && currentEventId == eventId) {
            Log.d("FundraisingMilestoneViewModel", "Pusher already initialized for event $eventId, skipping")
            return
        }
        
        // If already initialized for a different event, disconnect first
        if (isPusherInitialized && currentEventId != eventId) {
            Log.d("FundraisingMilestoneViewModel", "Reinitializing Pusher for new event: $eventId")
            disconnectFromCurrentEvent()
        }
        
        isPusherInitialized = true
        currentEventId = eventId
        
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
                
                Log.d("FundraisingMilestoneViewModel", "✅ Pusher initialized and subscribed to: $channelName")
                Log.d("FundraisingMilestoneViewModel", "   - Waiting for tip-animation events...")
                // Delay toast to ensure UI is ready
                kotlinx.coroutines.delay(500)
                showToast("✅ Pusher connected")
                
                // Log that we're about to start observing
                Log.d("FundraisingMilestoneViewModel", "   - About to start observing tip-animation flow...")
            } catch (e: Exception) {
                // Handle Pusher initialization errors
                Log.e("FundraisingMilestoneViewModel", "Error initializing Pusher", e)
            }
        }
        
        // Observe tip-animation events in separate coroutine
        val tipAnimationJob = viewModelScope.launch {
            try {
                Log.d("FundraisingMilestoneViewModel", "🔍 Starting to observe tip-animation flow...")
                // Don't show toast during initialization to avoid crashes
                pusherService.observeTipAnimation()
                    .catch { e -> 
                        Log.e("FundraisingMilestoneViewModel", "❌ Error in tip-animation flow", e)
                        showToast("❌ Tip flow error: ${e.message}")
                        e.printStackTrace()
                    }
                    .collect { tipAnimationEvent ->
                        Log.d("FundraisingMilestoneViewModel", "🎬 Received tip-animation event in ViewModel")
                        Log.d("FundraisingMilestoneViewModel", "   - Tips count: ${tipAnimationEvent.selectedTips.size}")
                        Log.d("FundraisingMilestoneViewModel", "   - Amount: ${tipAnimationEvent.amount}")
                        Log.d("FundraisingMilestoneViewModel", "   - Sender: ${tipAnimationEvent.sender}")
                        
                        // Create JSON display for toast - show full JSON structure
                        try {
                            // Escape special characters in tip names and sender
                            val tipsArray = tipAnimationEvent.selectedTips.joinToString(",") { 
                                val escaped = it.replace("\"", "\\\"").replace("\n", "\\n")
                                "\"$escaped\""
                            }
                            val escapedSender = tipAnimationEvent.sender.replace("\"", "\\\"").replace("\n", "\\n")
                            val jsonDisplay = """{"selectedTips":[$tipsArray],"amount":${tipAnimationEvent.amount},"sender":"$escapedSender"}"""
                            
                            // Show JSON in toast (limit length to prevent crashes - Toast has character limits)
                            val displayText = if (jsonDisplay.length > 200) {
                                jsonDisplay.take(197) + "..."
                            } else {
                                jsonDisplay
                            }
                            showToast("📋 $displayText")
                        } catch (e: Exception) {
                            Log.e("FundraisingMilestoneViewModel", "Error creating JSON display", e)
                            // Fallback to simple message
                            showToast("📋 ${tipAnimationEvent.selectedTips.size} tips: ${tipAnimationEvent.selectedTips.joinToString(",")}")
                        }
                        
                        // Use current event from state (may have been updated)
                        val currentEvent = _uiState.value.event
                        if (currentEvent != null) {
                            Log.d("FundraisingMilestoneViewModel", "   - Event found, processing animations...")
                            handleTipAnimationEvent(tipAnimationEvent, currentEvent)
                        } else {
                            Log.w("FundraisingMilestoneViewModel", "⚠️ Event is null, cannot handle tip animation")
                            showToast("⚠️ Event is null")
                        }
                    }
            } catch (e: Exception) {
                Log.e("FundraisingMilestoneViewModel", "❌ Error collecting tip-animation", e)
                showToast("❌ Collect error: ${e.message}")
                e.printStackTrace()
            }
        }
        flowJobs.add(tipAnimationJob)
        
        // Observe amount updates in separate coroutine
        val amountUpdatedJob = viewModelScope.launch {
            try {
                pusherService.observeAmountUpdated()
                    .catch { e -> 
                        Log.e("FundraisingMilestoneViewModel", "Error in amount-updated flow", e)
                    }
                    .collect { amountUpdated ->
                        Log.d("FundraisingMilestoneViewModel", "Received amount-updated: ${amountUpdated.amountRaised}, ${amountUpdated.numOfTips}")
                        showToast("💰 Amount: £${amountUpdated.amountRaised}, Tips: ${amountUpdated.numOfTips}")
                        updateAmountRaised(amountUpdated.amountRaised, amountUpdated.numOfTips)
                    }
            } catch (e: Exception) {
                Log.e("FundraisingMilestoneViewModel", "Error collecting amount-updated", e)
            }
        }
        flowJobs.add(amountUpdatedJob)
        
        // Observe screen settings updates in separate coroutine
        val settingsUpdatedJob = viewModelScope.launch {
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
        flowJobs.add(settingsUpdatedJob)
    }
    
    /**
     * Handles tip-animation events by finding characters and triggering animations.
     * 
     * The selectedTips array contains character names (strings), not IDs.
     * We match these names against the event's character list.
     */
    private fun handleTipAnimationEvent(
        tipAnimationEvent: com.example.telepse.data.model.TipAnimationEvent,
        event: Event
    ) {
        Log.d("FundraisingMilestoneViewModel", "🎯 Handling tip-animation event with ${tipAnimationEvent.selectedTips.size} tips")
        Log.d("FundraisingMilestoneViewModel", "   - Event has ${event.characterSets?.size ?: 0} character sets")
        Log.d("FundraisingMilestoneViewModel", "   - Selected tip names: ${tipAnimationEvent.selectedTips}")
        
        // Log all character names in the event for debugging
        val allCharacterNames = event.characterSets
            ?.flatMap { it.characters ?: emptyList() }
            ?.map { it.name } ?: emptyList()
        Log.d("FundraisingMilestoneViewModel", "   - Available character names (${allCharacterNames.size}): $allCharacterNames")
        
        val animations = tipAnimationEvent.selectedTips.mapNotNull { tipName ->
            Log.d("FundraisingMilestoneViewModel", "   🔍 Looking for character by name: '$tipName'")
            
            // Find character by name in characterSets (case-insensitive match)
            val character = findCharacterByName(tipName, event)
            
            if (character == null) {
                Log.w("FundraisingMilestoneViewModel", "   ⚠️ Character NOT FOUND for name: '$tipName'")
                Log.w("FundraisingMilestoneViewModel", "      - Available names: ${allCharacterNames.take(5)}${if (allCharacterNames.size > 5) "..." else ""}")
                showToast("⚠️ Char not found: $tipName")
                null
            } else {
                Log.d("FundraisingMilestoneViewModel", "   ✅ Found character: ${character.name}")
                Log.d("FundraisingMilestoneViewModel", "      - Style: ${character.styleName}")
                Log.d("FundraisingMilestoneViewModel", "      - Icon URL: ${character.iconUrl}")
                
                // Map the character's styleName to AnimationType
                val animationType = AnimationTypeMapper.mapStyleName(character.styleName)
                Log.d("FundraisingMilestoneViewModel", "      - Mapped style '${character.styleName}' -> $animationType")
                
                AnimationTrigger(
                    animationType = animationType,
                    imageUrl = character.iconUrl,
                    id = "${character.id}_${System.currentTimeMillis()}_${tipName}" // Unique ID for each animation
                )
            }
        }
        
        if (animations.isNotEmpty()) {
            val currentCount = _uiState.value.pendingAnimations.size
            Log.d("FundraisingMilestoneViewModel", "   ✅ Adding ${animations.size} animations to queue")
            Log.d("FundraisingMilestoneViewModel", "      - Current queue size: $currentCount")
            Log.d("FundraisingMilestoneViewModel", "      - New queue size: ${currentCount + animations.size}")
            showToast("✅ Added ${animations.size} animations (queue: ${currentCount + animations.size})")
            _uiState.value = _uiState.value.copy(
                pendingAnimations = _uiState.value.pendingAnimations + animations
            )
        } else {
            Log.w("FundraisingMilestoneViewModel", "   ❌ No animations created - all characters were null or not found")
            showToast("❌ No animations created")
        }
    }
    
    /**
     * Finds a character by name in the event's character sets.
     * Performs case-insensitive matching.
     */
    private fun findCharacterByName(characterName: String, event: Event): Character? {
        return event.characterSets
            ?.flatMap { it.characters ?: emptyList() }
            ?.firstOrNull { it.name.equals(characterName, ignoreCase = true) }
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
     * Uses the unique ID to ensure correct removal even if there are duplicates.
     */
    fun removeAnimation(animation: AnimationTrigger) {
        Log.d("FundraisingMilestoneViewModel", "🗑️ Removing animation: ${animation.animationType} (id: ${animation.id}), current count: ${_uiState.value.pendingAnimations.size}")
        _uiState.value = _uiState.value.copy(
            pendingAnimations = _uiState.value.pendingAnimations.filter { it.id != animation.id }
        )
        Log.d("FundraisingMilestoneViewModel", "✅ Animation removed, new count: ${_uiState.value.pendingAnimations.size}")
    }
    
    override fun onCleared() {
        super.onCleared()
        disconnectFromCurrentEvent()
    }
}
