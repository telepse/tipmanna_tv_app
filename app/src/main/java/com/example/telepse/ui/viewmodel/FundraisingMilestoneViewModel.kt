package com.example.telepse.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.telepse.data.model.Character
import com.example.telepse.data.model.Event
import com.example.telepse.data.model.TipAnimationEvent
import com.example.telepse.data.network.NetworkMonitor
import com.example.telepse.data.realtime.PusherService
import com.example.telepse.data.repository.AnimationContractRepository
import com.example.telepse.data.repository.EventRepository
import com.example.telepse.player.TipAnimationGuard
import com.example.telepse.ui.animations.AnimationType
import com.example.telepse.ui.animations.AnimationTypeMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
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
    val pendingAnimations: List<AnimationTrigger> = emptyList()
)

@HiltViewModel
class FundraisingMilestoneViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val pusherService: PusherService,
    private val networkMonitor: NetworkMonitor,
    private val animationContractRepository: AnimationContractRepository,
) : ViewModel() {
    
    private val tipAnimationGuard = TipAnimationGuard(AnimationContractRepository.DEFAULT_STALE_MS)
    
    private val _uiState = MutableStateFlow(FundraisingMilestoneUiState())
    val uiState: StateFlow<FundraisingMilestoneUiState> = _uiState.asStateFlow()
    
    private var currentEventId: String? = null
    private var isPusherInitialized = false
    private var flowJobs: MutableList<kotlinx.coroutines.Job> = mutableListOf()
    
    // CRITICAL: Mutex ensures only one event switch at a time
    // This prevents race conditions when switching events rapidly
    private val eventSwitchMutex = Mutex()
    
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
            // CRITICAL: Serialize all event switches with mutex
            // This ensures only one event switch happens at a time
            eventSwitchMutex.withLock {
                try {
                    Log.d("FundraisingMilestoneViewModel", "📥 [LOAD_EVENT] Starting to load event (mutex acquired)")
                    Log.d("FundraisingMilestoneViewModel", "   - Requested event ID: $eventId")
                    Log.d("FundraisingMilestoneViewModel", "   - Current event ID: $currentEventId")
                    Log.d("FundraisingMilestoneViewModel", "   - Is Pusher initialized: $isPusherInitialized")
                    
                    // DON'T clear event immediately - keep showing old event until new one loads
                    // This prevents UI from showing null state and crashing
                    // Only set loading if we don't have an event yet
                    if (_uiState.value.event == null) {
                        Log.d("FundraisingMilestoneViewModel", "   [STEP 1] No event loaded, setting loading state...")
                        _uiState.value = _uiState.value.copy(
                            isLoading = true,
                            error = null
                        )
                        Log.d("FundraisingMilestoneViewModel", "   [STEP 1] ✅ Loading state set")
                    } else {
                        Log.d("FundraisingMilestoneViewModel", "   [STEP 1] Event exists, keeping it visible during switch")
                    }
                    
                    performEventLoad(eventId)
                } catch (e: Exception) {
                    Log.e("FundraisingMilestoneViewModel", "❌ Critical error in loadEvent", e)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load event: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Performs the actual event loading logic within the mutex lock.
     */
    private suspend fun performEventLoad(eventId: String) {
        // Check network status before attempting to load
        Log.d("FundraisingMilestoneViewModel", "   [STEP 2] Checking network connectivity...")
        if (!networkMonitor.isCurrentlyOnline()) {
            Log.w("FundraisingMilestoneViewModel", "   [STEP 2] ❌ No network connection")
            Log.w("FundraisingMilestoneViewModel", "⚠️ No network connection, cannot load event")
            _uiState.value = FundraisingMilestoneUiState(
                event = null,
                isLoading = false,
                error = "No internet connection. Please check your network and try again.",
                pendingAnimations = emptyList()
            )
            return
        }
        
        // If switching to a different event, disconnect from the old one first
        if (currentEventId != null && currentEventId != eventId) {
            Log.d("FundraisingMilestoneViewModel", "   [STEP 2] 🔄 Switching events: $currentEventId -> $eventId")
            Log.d("FundraisingMilestoneViewModel", "      - Direction: ${if (eventId > currentEventId!!) "FORWARD" else "BACKWARD"}")
            
            // CRITICAL: Clear UI state FIRST to show loading screen
            // This prevents the screen from trying to render old event data with new player manager
            Log.d("FundraisingMilestoneViewModel", "   [STEP 2a] Clearing UI state to show loading...")
            _uiState.value = FundraisingMilestoneUiState(
                event = null,
                isLoading = true,
                error = null,
                pendingAnimations = emptyList()
            )
            Log.d("FundraisingMilestoneViewModel", "   [STEP 2a] ✅ UI state cleared")
            
            // Give Compose time to fully recompose with the loading screen
            // This prevents race conditions with lingering effects from previous renders
            // Longer delay needed after background changes or complex UI updates
            Log.d("FundraisingMilestoneViewModel", "   [STEP 2b] Waiting for Compose to recompose...")
            delay(200)
            Log.d("FundraisingMilestoneViewModel", "   [STEP 2b] ✅ Recomposition wait complete")
            
            try {
                disconnectFromCurrentEvent()
                Log.d("FundraisingMilestoneViewModel", "   [STEP 2] ✅ Disconnected from previous event")
            } catch (e: Exception) {
                Log.e("FundraisingMilestoneViewModel", "   [STEP 2] ❌ Error disconnecting from previous event", e)
                // Force cleanup even on error
                flowJobs.forEach { job ->
                    try {
                        job.cancelAndJoin()
                    } catch (e: Exception) {
                        Log.e("FundraisingMilestoneViewModel", "Error canceling job", e)
                    }
                }
                flowJobs.clear()
                try {
                    pusherService.disconnect()
                } catch (e: Exception) {
                    Log.e("FundraisingMilestoneViewModel", "Error disconnecting Pusher", e)
                }
                isPusherInitialized = false
                currentEventId = null
                
                // Wait for cleanup to settle
                delay(300)
            }
        }
        
        // Also disconnect if same event but Pusher needs reinitialization
        if (currentEventId == eventId && !isPusherInitialized) {
            Log.d("FundraisingMilestoneViewModel", "🔄 Reinitializing Pusher for event: $eventId")
            try {
                disconnectFromCurrentEvent()
                delay(300)
            } catch (e: Exception) {
                Log.e("FundraisingMilestoneViewModel", "Error disconnecting for reinitialization", e)
                // Force cleanup
                flowJobs.forEach { job ->
                    try {
                        job.cancelAndJoin()
                    } catch (e: Exception) {
                        Log.e("FundraisingMilestoneViewModel", "Error canceling job", e)
                    }
                }
                flowJobs.clear()
            }
        }
        
        try {
            Log.d("FundraisingMilestoneViewModel", "   [STEP 3] Fetching event details from repository...")
            // Fetch event details first
            val result = eventRepository.getEventById(eventId)
            result.fold(
                onSuccess = { event ->
                    Log.d("FundraisingMilestoneViewModel", "   [STEP 4] ✅ Event fetched successfully")
                    Log.d("FundraisingMilestoneViewModel", "      - Event name: ${event.eventName}")
                    Log.d("FundraisingMilestoneViewModel", "      - Background type: ${event.backgroundType}")
                    
                    currentEventId = eventId
                    Log.d("FundraisingMilestoneViewModel", "   [STEP 5] Updating UI state with event...")
                    _uiState.value = _uiState.value.copy(
                        event = event,
                        isLoading = false,
                        error = null
                    )
                    Log.d("FundraisingMilestoneViewModel", "   [STEP 5] ✅ UI state updated")
                    
                    // Initialize Pusher and subscribe after fetching event (only if online)
                    Log.d("FundraisingMilestoneViewModel", "   [STEP 6] Checking network before initializing Pusher...")
                    if (networkMonitor.isCurrentlyOnline()) {
                        Log.d("FundraisingMilestoneViewModel", "   [STEP 6] ✅ Network online, initializing Pusher...")
                        initializePusher(eventId, event)
                    } else {
                        Log.w("FundraisingMilestoneViewModel", "⚠️ Network lost during load, skipping Pusher initialization")
                    }
                },
                onFailure = { exception ->
                    Log.e("FundraisingMilestoneViewModel", "   [STEP 4] ❌ Failed to fetch event", exception)
                    // Check if error is due to network
                    val errorMessage = if (!networkMonitor.isCurrentlyOnline()) {
                        "No internet connection. Please check your network and try again."
                    } else {
                        exception.message ?: "Failed to load event"
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = errorMessage
                    )
                }
            )
        } catch (e: Exception) {
            Log.e("FundraisingMilestoneViewModel", "   [STEP 3] ❌ Error loading event", e)
            val errorMessage = if (!networkMonitor.isCurrentlyOnline()) {
                "No internet connection. Please check your network and try again."
            } else {
                "Error loading event: ${e.message}"
            }
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = errorMessage
            )
        }
    }
    
    /**
     * Disconnects from the current event's Pusher channel.
     * Called when switching to a different event.
     */
    private suspend fun disconnectFromCurrentEvent() {
        Log.d("FundraisingMilestoneViewModel", "🔌 [DISCONNECT] Starting disconnect from current event")
        Log.d("FundraisingMilestoneViewModel", "   - Current event ID: $currentEventId")
        Log.d("FundraisingMilestoneViewModel", "   - Is Pusher initialized: $isPusherInitialized")
        Log.d("FundraisingMilestoneViewModel", "   - Flow jobs count: ${flowJobs.size}")
        
        // Cancel all flow collection jobs
        Log.d("FundraisingMilestoneViewModel", "   [STEP 1] Cancelling flow collection jobs...")
        flowJobs.forEachIndexed { index, job ->
            try {
                Log.d("FundraisingMilestoneViewModel", "      - Cancelling job $index/${flowJobs.size}")
                job.cancel()
                Log.d("FundraisingMilestoneViewModel", "      - ✅ Job $index cancelled")
            } catch (e: Exception) {
                Log.e("FundraisingMilestoneViewModel", "      - ❌ Error cancelling job $index", e)
            }
        }
        flowJobs.clear()
        Log.d("FundraisingMilestoneViewModel", "   [STEP 1] ✅ All flow jobs cancelled and cleared")
        
        // Disconnect from Pusher
        Log.d("FundraisingMilestoneViewModel", "   [STEP 2] Disconnecting from Pusher service...")
        try {
            pusherService.disconnect()
            Log.d("FundraisingMilestoneViewModel", "   [STEP 2] ✅ Pusher disconnect called")
        } catch (e: Exception) {
            Log.e("FundraisingMilestoneViewModel", "   [STEP 2] ❌ Error calling Pusher disconnect", e)
        }
        
        // Reset flags
        Log.d("FundraisingMilestoneViewModel", "   [STEP 3] Resetting flags...")
        isPusherInitialized = false
        currentEventId = null
        Log.d("FundraisingMilestoneViewModel", "   [STEP 3] ✅ Flags reset")
        
        // Brief pause to ensure channel unsubscribe completes
        // We don't call pusher.disconnect() anymore (it causes crashes), so this is minimal
        Log.d("FundraisingMilestoneViewModel", "   [STEP 4] Brief pause (100ms) for channel unsubscribe to complete...")
        kotlinx.coroutines.delay(100)
        Log.d("FundraisingMilestoneViewModel", "   [STEP 4] ✅ Pause complete")
        
        // Reset isDisconnecting flag so next initialize can proceed immediately
        Log.d("FundraisingMilestoneViewModel", "   [STEP 5] Resetting PusherService.isDisconnecting flag...")
        pusherService.resetDisconnectingFlag()
        Log.d("FundraisingMilestoneViewModel", "   [STEP 5] ✅ Flag reset")
        
        Log.d("FundraisingMilestoneViewModel", "✅ [DISCONNECT] Disconnect from current event complete")
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
        
        // Disconnection should already be handled in loadEvent before calling this
        // But if somehow we get here with a different event, log a warning
        if (isPusherInitialized && currentEventId != eventId) {
            Log.w("FundraisingMilestoneViewModel", "⚠️ Warning: Initializing Pusher for different event without disconnecting first")
        }
        
        isPusherInitialized = true
        currentEventId = eventId
        tipAnimationGuard.reset()
        pusherService.setEventCodeForAuth(event.eventCode)
        
        viewModelScope.launch {
            try {
                val staleMs = animationContractRepository.fetchStaleThresholdMs()
                tipAnimationGuard.updateStaleThresholdMs(staleMs)
                Log.d("FundraisingMilestoneViewModel", "Animation guard staleThresholdMs=$staleMs")

                val forceReconnect = isPusherInitialized && currentEventId != eventId
                if (!pusherService.initialize(forceReconnect = forceReconnect)) {
                    Log.e("FundraisingMilestoneViewModel", "Failed to initialize Pusher")
                    return@launch
                }
                
                kotlinx.coroutines.delay(500)
                
                val publicChannelName = "event-$eventId"
                if (!pusherService.subscribeToPublicChannel(publicChannelName)) {
                    Log.e("FundraisingMilestoneViewModel", "Failed to subscribe to public channel: $publicChannelName")
                    return@launch
                }

                if (!pusherService.subscribeToGlobalLiveTv()) {
                    Log.e("FundraisingMilestoneViewModel", "Failed to subscribe to global-live-tv")
                    return@launch
                }
                
                val privateChannelName = "private-event-$eventId"
                if (!pusherService.subscribeToPrivateChannel(privateChannelName)) {
                    Log.e("FundraisingMilestoneViewModel", "Failed to subscribe to private channel: $privateChannelName")
                    return@launch
                }
                
                kotlinx.coroutines.delay(500)
                
                Log.d("FundraisingMilestoneViewModel", "✅ Pusher initialized and subscribed to:")
                Log.d("FundraisingMilestoneViewModel", "   - Public channel: $publicChannelName")
                Log.d("FundraisingMilestoneViewModel", "   - global-live-tv (tip-animation)")
                Log.d("FundraisingMilestoneViewModel", "   - Private channel: $privateChannelName")
                
                // Log that we're about to start observing
                Log.d("FundraisingMilestoneViewModel", "   - About to start observing flows...")
            } catch (e: Exception) {
                // Handle Pusher initialization errors
                Log.e("FundraisingMilestoneViewModel", "Error initializing Pusher", e)
            }
        }
        
        // Observe tip-animation events in separate coroutine
        val tipAnimationJob = viewModelScope.launch {
            try {
                Log.d("FundraisingMilestoneViewModel", "🔍 Starting to observe tip-animation flow...")
                
                // Wait for event to be loaded before processing animations
                // This prevents animations from failing when they arrive before event data is ready
                var eventReady = false
                var waitAttempts = 0
                while (!eventReady && waitAttempts < 50) { // Wait up to 5 seconds for event
                    val currentEvent = _uiState.value.event
                    if (currentEvent != null && currentEvent.characterSets != null) {
                        eventReady = true
                        Log.d("FundraisingMilestoneViewModel", "✅ Event loaded, ready to process animations")
                    } else {
                        kotlinx.coroutines.delay(100)
                        waitAttempts++
                    }
                }
                
                if (!eventReady) {
                    Log.w("FundraisingMilestoneViewModel", "⚠️ Event not loaded after waiting, but will continue to observe")
                }
                
                // Queue for events that arrive before event is ready
                val pendingEvents = mutableListOf<TipAnimationEvent>()
                
                pusherService.observeTipAnimation()
                    .catch { e -> 
                        // Ignore cancellation exceptions - they're expected when switching events
                        if (e !is kotlinx.coroutines.CancellationException) {
                            Log.e("FundraisingMilestoneViewModel", "❌ Error in tip-animation flow", e)
                            e.printStackTrace()
                        }
                    }
                    .collect { tipAnimationEvent ->
                        val activeId = currentEventId ?: return@collect
                        if (!tipAnimationGuard.shouldAccept(tipAnimationEvent, activeId)) {
                            Log.d(
                                "FundraisingMilestoneViewModel",
                                "tip-animation rejected by guard (eventId=${tipAnimationEvent.eventId})",
                            )
                            return@collect
                        }
                        Log.d("FundraisingMilestoneViewModel", "🎬 Accepted tip-animation for event $activeId")
                        Log.d("FundraisingMilestoneViewModel", "   - Tips count: ${tipAnimationEvent.selectedTips.size}")
                        Log.d("FundraisingMilestoneViewModel", "   - Amount: ${tipAnimationEvent.amount}")
                        Log.d("FundraisingMilestoneViewModel", "   - Sender: ${tipAnimationEvent.sender}")
                        
                        val currentEvent = _uiState.value.event
                        if (currentEvent != null && currentEvent.characterSets != null) {
                            Log.d("FundraisingMilestoneViewModel", "   - Event found, processing animations...")
                            handleTipAnimationEvent(tipAnimationEvent, currentEvent)
                            
                            // Process any pending events that arrived before event was ready
                            if (pendingEvents.isNotEmpty()) {
                                Log.d("FundraisingMilestoneViewModel", "   - Processing ${pendingEvents.size} pending animation events")
                                val eventsToProcess = pendingEvents.toList() // Create a copy to avoid concurrent modification
                                pendingEvents.clear()
                                eventsToProcess.forEach { pendingEvent: TipAnimationEvent ->
                                    handleTipAnimationEvent(pendingEvent, currentEvent)
                                }
                            }
                        } else {
                            Log.w("FundraisingMilestoneViewModel", "⚠️ Event is null or not ready, queuing animation event")
                            // Queue the event to process later when event is ready
                            pendingEvents.add(tipAnimationEvent)
                            
                            // Try to wait a bit more for event to load
                            var retryAttempts = 0
                            while (retryAttempts < 20 && (_uiState.value.event == null || _uiState.value.event?.characterSets == null)) {
                                kotlinx.coroutines.delay(100)
                                retryAttempts++
                            }
                            
                            // Try again with current state
                            val retryEvent = _uiState.value.event
                            if (retryEvent != null && retryEvent.characterSets != null) {
                                Log.d("FundraisingMilestoneViewModel", "   - Event loaded after retry, processing queued event")
                                handleTipAnimationEvent(tipAnimationEvent, retryEvent)
                                pendingEvents.remove(tipAnimationEvent)
                            }
                        }
                    }
            } catch (e: Exception) {
                // Ignore cancellation exceptions - they're expected when switching events
                if (e !is kotlinx.coroutines.CancellationException) {
                    Log.e("FundraisingMilestoneViewModel", "❌ Error collecting tip-animation", e)
                    e.printStackTrace()
                }
            }
        }
        flowJobs.add(tipAnimationJob)
        
        // Observe amount updates in separate coroutine
        val amountUpdatedJob = viewModelScope.launch {
            try {
                pusherService.observeAmountUpdated()
                    .catch { e -> 
                        // Ignore cancellation exceptions - they're expected when switching events
                        if (e !is kotlinx.coroutines.CancellationException) {
                            Log.e("FundraisingMilestoneViewModel", "Error in amount-updated flow", e)
                        }
                    }
                    .collect { amountUpdated ->
                        Log.d("FundraisingMilestoneViewModel", "Received amount-updated: ${amountUpdated.amountRaised}, ${amountUpdated.numOfTips}")
                        updateAmountRaised(amountUpdated.amountRaised, amountUpdated.numOfTips)
                    }
            } catch (e: Exception) {
                // Ignore cancellation exceptions - they're expected when switching events
                if (e !is kotlinx.coroutines.CancellationException) {
                    Log.e("FundraisingMilestoneViewModel", "Error collecting amount-updated", e)
                }
            }
        }
        flowJobs.add(amountUpdatedJob)
        
        // Observe screen settings updates in separate coroutine
        val settingsUpdatedJob = viewModelScope.launch {
            try {
                pusherService.observeScreenSettingsUpdated()
                    .catch { e -> 
                        // Ignore cancellation exceptions - they're expected when switching events
                        if (e !is kotlinx.coroutines.CancellationException) {
                            Log.e("FundraisingMilestoneViewModel", "Error in settings-updated flow", e)
                        }
                    }
                    .collect { settingsUpdated ->
                        Log.d("FundraisingMilestoneViewModel", "Received settings-updated event")
                        updateScreenSettings(settingsUpdated)
                    }
            } catch (e: Exception) {
                // Ignore cancellation exceptions - they're expected when switching events
                if (e !is kotlinx.coroutines.CancellationException) {
                    Log.e("FundraisingMilestoneViewModel", "Error collecting settings-updated", e)
                }
            }
        }
        flowJobs.add(settingsUpdatedJob)
        
        // Observe latest tip updates in separate coroutine
        val latestTipUpdatedJob = viewModelScope.launch {
            try {
                pusherService.observeLatestTipUpdated()
                    .catch { e -> 
                        // Ignore cancellation exceptions - they're expected when switching events
                        if (e !is kotlinx.coroutines.CancellationException) {
                            Log.e("FundraisingMilestoneViewModel", "Error in latest-tip-updated flow", e)
                        }
                    }
                    .collect { latestTipUpdated ->
                        Log.d("FundraisingMilestoneViewModel", "Received latest-tip-updated: £${latestTipUpdated.latestTipAmount}")
                        updateLatestTipAmount(latestTipUpdated.latestTipAmount)
                    }
            } catch (e: Exception) {
                // Ignore cancellation exceptions - they're expected when switching events
                if (e !is kotlinx.coroutines.CancellationException) {
                    Log.e("FundraisingMilestoneViewModel", "Error collecting latest-tip-updated", e)
                }
            }
        }
        flowJobs.add(latestTipUpdatedJob)
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
            _uiState.value = _uiState.value.copy(
                pendingAnimations = _uiState.value.pendingAnimations + animations
            )
        } else {
            Log.w("FundraisingMilestoneViewModel", "   ❌ No animations created - all characters were null or not found")
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
            Log.d("FundraisingMilestoneViewModel", "🔄 Updating screen settings from Pusher event")
            Log.d("FundraisingMilestoneViewModel", "   - eventBackground: ${settingsUpdated.eventBackground}")
            Log.d("FundraisingMilestoneViewModel", "   - backgroundType: ${settingsUpdated.backgroundType}")
            Log.d("FundraisingMilestoneViewModel", "   - hideProgressBarAmount: ${settingsUpdated.hideProgressBarAmount}")
            Log.d("FundraisingMilestoneViewModel", "   - showLatestAmount: ${settingsUpdated.showLatestAmount}")
            Log.d("FundraisingMilestoneViewModel", "   - moneyFor: ${settingsUpdated.moneyFor}")
            Log.d("FundraisingMilestoneViewModel", "   - tipUsageDisclosure: ${settingsUpdated.tipUsageDisclosure?.size} items")
            
            _uiState.value = _uiState.value.copy(
                event = currentEvent.copy(
                    eventBackground = settingsUpdated.eventBackground ?: currentEvent.eventBackground,
                    backgroundType = settingsUpdated.backgroundType ?: currentEvent.backgroundType,
                    hideProgressBarAmount = settingsUpdated.hideProgressBarAmount ?: currentEvent.hideProgressBarAmount,
                    showLatestAmount = settingsUpdated.showLatestAmount ?: currentEvent.showLatestAmount,
                    moneyFor = settingsUpdated.moneyFor ?: currentEvent.moneyFor,
                    tipUsageDisclosure = settingsUpdated.tipUsageDisclosure ?: currentEvent.tipUsageDisclosure,
                    amountRaised = settingsUpdated.amountRaised ?: currentEvent.amountRaised,
                    numOfTips = settingsUpdated.numOfTips ?: currentEvent.numOfTips,
                    latestTipAmount = settingsUpdated.latestTipAmount ?: currentEvent.latestTipAmount
                )
            )
            Log.d("FundraisingMilestoneViewModel", "✅ Screen settings updated successfully")
        } else {
            Log.w("FundraisingMilestoneViewModel", "⚠️ Cannot update screen settings: event is null")
        }
    }
    
    /**
     * Updates the latest tip amount from Pusher event.
     * This is called when a tip is made and the latest-tip-updated event is received.
     */
    private fun updateLatestTipAmount(latestTipAmount: Double) {
        val currentEvent = _uiState.value.event
        if (currentEvent != null) {
            _uiState.value = _uiState.value.copy(
                event = currentEvent.copy(
                    latestTipAmount = latestTipAmount
                )
            )
            Log.d("FundraisingMilestoneViewModel", "✅ Updated latest tip amount to: £$latestTipAmount")
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
    
    /**
     * Immediately cleans up and clears state when leaving the screen.
     * Called from the composable's DisposableEffect.
     * This must be synchronous and immediate - no coroutines.
     */
    fun cleanup() {
        Log.d("FundraisingMilestoneViewModel", "🧹 IMMEDIATE CLEANUP - clearing state and disconnecting")
        
        // Immediately clear state - no delay
        _uiState.value = FundraisingMilestoneUiState(
            event = null,
            isLoading = false,
            error = null,
            pendingAnimations = emptyList()
        )
        
        // Immediately cancel all flow collection jobs
        // This will trigger awaitClose in flows, allowing them to clean up properly
        flowJobs.forEach { job ->
            try {
                job.cancel()
            } catch (e: Exception) {
                Log.e("FundraisingMilestoneViewModel", "Error canceling job during cleanup", e)
            }
        }
        flowJobs.clear()
        
        // Reset flags BEFORE disconnecting to prevent race conditions
        isPusherInitialized = false
        currentEventId = null
        
        // Disconnect from Pusher AFTER canceling jobs (launch coroutine for suspend function)
        // This ensures flows have a chance to clean up in awaitClose
        try {
            viewModelScope.launch {
                try {
                    pusherService.disconnect()
                    Log.d("FundraisingMilestoneViewModel", "✅ Pusher disconnected during cleanup")
                } catch (e: Exception) {
                    Log.e("FundraisingMilestoneViewModel", "Error disconnecting Pusher during cleanup", e)
                }
            }
        } catch (e: Exception) {
            Log.e("FundraisingMilestoneViewModel", "Error launching disconnect during cleanup", e)
        }
        
        Log.d("FundraisingMilestoneViewModel", "✅ IMMEDIATE CLEANUP COMPLETE - clean slate ready")
    }
    
    override fun onCleared() {
        super.onCleared()
        Log.d("FundraisingMilestoneViewModel", "🧹 [ON_CLEARED] ViewModel onCleared() called")
        Log.d("FundraisingMilestoneViewModel", "   - Current event ID: $currentEventId")
        Log.d("FundraisingMilestoneViewModel", "   - Is Pusher initialized: $isPusherInitialized")
        Log.d("FundraisingMilestoneViewModel", "   - Flow jobs count: ${flowJobs.size}")
        
        // Cancel all flow collection jobs
        Log.d("FundraisingMilestoneViewModel", "   [STEP 1] Cancelling flow collection jobs...")
        flowJobs.forEachIndexed { index, job ->
            try {
                Log.d("FundraisingMilestoneViewModel", "      - Cancelling job $index/${flowJobs.size}")
                job.cancel()
                Log.d("FundraisingMilestoneViewModel", "      - ✅ Job $index cancelled")
            } catch (e: Exception) {
                Log.e("FundraisingMilestoneViewModel", "      - ❌ Error cancelling job $index", e)
            }
        }
        flowJobs.clear()
        Log.d("FundraisingMilestoneViewModel", "   [STEP 1] ✅ All flow jobs cancelled and cleared")

        // Disconnect from Pusher (launch coroutine for suspend function)
        Log.d("FundraisingMilestoneViewModel", "   [STEP 2] Disconnecting from Pusher service...")
        try {
            // Launch in viewModelScope to call suspend function
            viewModelScope.launch {
                try {
                    pusherService.disconnect()
                    Log.d("FundraisingMilestoneViewModel", "   [STEP 2] ✅ Pusher disconnect called")
                } catch (e: Exception) {
                    Log.e("FundraisingMilestoneViewModel", "   [STEP 2] ❌ Error calling Pusher disconnect", e)
                }
            }
        } catch (e: Exception) {
            Log.e("FundraisingMilestoneViewModel", "   [STEP 2] ❌ Error launching disconnect", e)
        }
        
        // Reset flags
        Log.d("FundraisingMilestoneViewModel", "   [STEP 3] Resetting flags...")
        isPusherInitialized = false
        currentEventId = null
        Log.d("FundraisingMilestoneViewModel", "   [STEP 3] ✅ Flags reset")
        
        Log.d("FundraisingMilestoneViewModel", "✅ [ON_CLEARED] ViewModel cleanup complete")
    }
}
