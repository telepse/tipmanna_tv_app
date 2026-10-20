package com.example.telepse.data.realtime

import com.example.telepse.data.model.AmountUpdatedEvent
import com.example.telepse.data.model.LatestTipUpdatedEvent
import com.example.telepse.data.model.ScreenDisplayStatePayload
import com.example.telepse.data.model.TipAnimationEvent
import com.example.telepse.data.realtime.PusherConfig
import com.pusher.client.Pusher
import com.pusher.client.PusherOptions
import com.pusher.client.channel.Channel
import com.pusher.client.channel.PrivateChannel
import io.ktor.client.HttpClient
import com.pusher.client.channel.PusherEvent
import com.pusher.client.channel.SubscriptionEventListener
import com.pusher.client.channel.PrivateChannelEventListener
import com.pusher.client.connection.ConnectionEventListener
import com.pusher.client.connection.ConnectionState
import com.pusher.client.connection.ConnectionStateChange
import android.util.Log
import com.example.telepse.data.community.DEFAULT_COMMUNITY_DISPLAY_UPDATED_EVENT
import com.example.telepse.data.community.parseCommunityDisplayUpdatedPayload
import com.example.telepse.data.model.CommunityDisplayUpdatedPayload
import com.example.telepse.data.model.CommunityRoomRealtimeHints
import com.example.telepse.data.model.TvEventRealtimeEventNames
import com.example.telepse.data.model.TvEventRealtimeHints
import com.example.telepse.data.model.TvScheduleUpdatedPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import com.example.telepse.data.auth.SessionExpiryHandler
import com.example.telepse.data.auth.SessionStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for handling Pusher real-time events.
 * 
 * This service manages Pusher connection, subscription to event channels,
 * and provides Flow-based event streams for reactive UI updates.
 * 
 * @property pusherApiKey Pusher API key for authentication
 * @property pusherCluster Pusher cluster (e.g., "mt1", "eu")
 */
@Singleton
class PusherService @Inject constructor(
    private val httpClient: HttpClient,
    private val sessionStore: SessionStore,
    private val sessionExpiryHandler: SessionExpiryHandler,
) {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        serviceScope.launch {
            var wasLoggedIn = sessionStore.snapshot.value.loggedIn
            sessionStore.snapshot.collect { snap ->
                if (wasLoggedIn && !snap.loggedIn) {
                    dropAuthenticatedPrivateChannelSubscription()
                }
                wasLoggedIn = snap.loggedIn
            }
        }
    }

    private var pusher: Pusher? = null
    private var publicChannel: Channel? = null
    private var privateChannel: Channel? = null
    private var globalLiveTvChannel: Channel? = null
    @Volatile
    private var eventCodeForAuth: String? = null
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = false
    }
    
    // CRITICAL: Track if Pusher instance is corrupted and needs recreation
    private var isPusherCorrupted = false
    
    // CRITICAL: Track if we're disconnecting to prevent listener callbacks from processing
    @Volatile
    private var isDisconnecting = false
    
    // Mutex to prevent concurrent modifications to Pusher state
    private val pusherMutex = Mutex()

    private val _connectionEstablished =
        MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Emits when Pusher reaches CONNECTED (initial connect or reconnect). */
    fun observeConnectionEstablished(): Flow<Unit> = _connectionEstablished.asSharedFlow()

    private val _screenDisplayStatePayloads =
        MutableSharedFlow<ScreenDisplayStatePayload>(extraBufferCapacity = 16)

    private var screenDisplayPusherEventName: String = DEFAULT_SCREEN_DISPLAY_STATE_EVENT
    private var screenDisplaySubscription: SubscriptionEventListener? = null
    private var boundScreenDisplayEventName: String? = null

    private var configuredCredentials: ResolvedPusherCredentials? = null
    private var activeCredentialsKey: String? = null
    private var activeCredentialsCluster: String? = null

    private var amountUpdatedEventName: String = "event-amount-updated"
    private var latestTipUpdatedEventName: String = "latest-tip-updated"
    private var tipAnimationEventName: String = PusherConfig.TIP_ANIMATION_EVENT

    private val _amountUpdatedEvents =
        MutableSharedFlow<AmountUpdatedEvent>(extraBufferCapacity = 16)
    private val _latestTipUpdatedEvents =
        MutableSharedFlow<LatestTipUpdatedEvent>(extraBufferCapacity = 16)
    private val _tipAnimationEvents =
        MutableSharedFlow<TipAnimationEvent>(extraBufferCapacity = 8)
    private val _tvScheduleUpdated =
        MutableSharedFlow<TvScheduleUpdatedPayload>(extraBufferCapacity = 4)

    private var amountUpdatedSubscription: SubscriptionEventListener? = null
    private var boundAmountUpdatedEventName: String? = null
    private var latestTipUpdatedSubscription: SubscriptionEventListener? = null
    private var boundLatestTipUpdatedEventName: String? = null
    private var tipAnimationSubscription: SubscriptionEventListener? = null
    private var boundTipAnimationEventName: String? = null
    private var tvScheduleSubscription: SubscriptionEventListener? = null

    private var communityDisplayChannel: Channel? = null
    private var communityDisplayUpdatedSubscription: SubscriptionEventListener? = null
    private var communityDisplayUpdatedEventName: String = DEFAULT_COMMUNITY_DISPLAY_UPDATED_EVENT
    private val _communityDisplayUpdatedPayloads =
        MutableSharedFlow<CommunityDisplayUpdatedPayload>(extraBufferCapacity = 8)

    /** Venue HUD event name from tv-app bootstrap (defaults to [DEFAULT_SCREEN_DISPLAY_STATE_EVENT]). */
    fun setScreenDisplayStateEventName(eventName: String) {
        val normalized = eventName.trim().takeIf { it.isNotEmpty() }
            ?: DEFAULT_SCREEN_DISPLAY_STATE_EVENT
        if (screenDisplayPusherEventName == normalized) return
        screenDisplayPusherEventName = normalized
        publicChannel?.let { channel ->
            bindScreenDisplayStateListener(channel)
        }
    }

    fun setEventCodeForAuth(eventCode: String?) {
        eventCodeForAuth = eventCode
    }

    /**
     * Apply tv-app bootstrap realtime hints (Pusher key/cluster + documented event names).
     */
    fun configureFromBootstrap(hints: TvEventRealtimeHints) {
        configuredCredentials =
            resolvePusherCredentials(hints.pusherKey, hints.pusherCluster)
        hints.events?.let { setVenueEventNames(it) }
        Log.d(
            "PusherService",
            "🔧 Bootstrap realtime config: ${formatPusherCredentialDiagnostic(configuredCredentials!!)}",
        )
    }

    fun configureFromCommunityRealtime(hints: CommunityRoomRealtimeHints) {
        configuredCredentials =
            resolvePusherCredentials(hints.pusherKey, hints.pusherCluster)
        hints.events?.displayUpdated?.trim()?.takeIf { it.isNotEmpty() }?.let {
            communityDisplayUpdatedEventName = it
        }
        communityDisplayChannel?.let { bindCommunityDisplayUpdatedListener(it) }
        Log.d(
            "PusherService",
            "🔧 Community realtime config: ${formatPusherCredentialDiagnostic(configuredCredentials!!)}",
        )
    }

    fun setVenueEventNames(events: TvEventRealtimeEventNames) {
        events.eventAmountUpdated?.trim()?.takeIf { it.isNotEmpty() }?.let {
            amountUpdatedEventName = it
        }
        events.latestTipUpdated?.trim()?.takeIf { it.isNotEmpty() }?.let {
            latestTipUpdatedEventName = it
        }
        events.tipAnimation?.trim()?.takeIf { it.isNotEmpty() }?.let {
            tipAnimationEventName = it
        }
        publicChannel?.let { bindPublicVenueListeners(it) }
        globalLiveTvChannel?.let { bindGlobalLiveTvListeners(it) }
    }

    private fun unbindScreenDisplayStateListener(channel: Channel?) {
        val listener = screenDisplaySubscription ?: return
        val eventName = boundScreenDisplayEventName ?: return
        channel ?: return
        try {
            channel.unbind(eventName, listener)
            Log.d("PusherService", "✅ Unbound $eventName (screen display)")
        } catch (e: Exception) {
            Log.w("PusherService", "Error unbinding screen display listener", e)
        } finally {
            screenDisplaySubscription = null
            boundScreenDisplayEventName = null
        }
    }

    private fun bindScreenDisplayStateListener(channel: Channel) {
        unbindScreenDisplayStateListener(channel)
        val eventName = screenDisplayPusherEventName
        val subscription = object : SubscriptionEventListener {
            override fun onEvent(event: PusherEvent) {
                if (isDisconnecting) {
                    Log.d("PusherService", "⏭️ Ignoring $eventName — disconnecting")
                    return
                }
                try {
                    Log.d("PusherService", "🔔 Received $eventName (bound at subscribe)")
                    val payload = parseScreenDisplayStatePayload(event.data)
                    if (payload == null) {
                        Log.w("PusherService", "⚠️ Malformed $eventName payload ignored")
                        return
                    }
                    val emitted = _screenDisplayStatePayloads.tryEmit(payload)
                    Log.d(
                        "PusherService",
                        "📤 Screen display payload emitted=$emitted eventId=${payload.eventId}",
                    )
                } catch (e: Exception) {
                    Log.e("PusherService", "❌ Error handling $eventName", e)
                }
            }
        }
        channel.bind(eventName, subscription)
        screenDisplaySubscription = subscription
        boundScreenDisplayEventName = eventName
        Log.d(
            "PusherService",
            "✅ Bound $eventName on ${channel.name} (subscription-confirmed channel object)",
        )
    }

    private fun unbindAmountUpdatedListener(channel: Channel?) {
        val listener = amountUpdatedSubscription ?: return
        val eventName = boundAmountUpdatedEventName ?: return
        channel ?: return
        try {
            channel.unbind(eventName, listener)
        } catch (e: Exception) {
            Log.w("PusherService", "Error unbinding $eventName", e)
        } finally {
            amountUpdatedSubscription = null
            boundAmountUpdatedEventName = null
        }
    }

    private fun unbindLatestTipUpdatedListener(channel: Channel?) {
        val listener = latestTipUpdatedSubscription ?: return
        val eventName = boundLatestTipUpdatedEventName ?: return
        channel ?: return
        try {
            channel.unbind(eventName, listener)
        } catch (e: Exception) {
            Log.w("PusherService", "Error unbinding $eventName", e)
        } finally {
            latestTipUpdatedSubscription = null
            boundLatestTipUpdatedEventName = null
        }
    }

    private fun unbindTipAnimationListener(channel: Channel?) {
        val listener = tipAnimationSubscription ?: return
        val eventName = boundTipAnimationEventName ?: return
        channel ?: return
        try {
            channel.unbind(eventName, listener)
        } catch (e: Exception) {
            Log.w("PusherService", "Error unbinding $eventName", e)
        } finally {
            tipAnimationSubscription = null
            boundTipAnimationEventName = null
        }
    }

    private fun unbindTvScheduleListener(channel: Channel?) {
        val listener = tvScheduleSubscription ?: return
        channel ?: return
        try {
            channel.unbind(PusherConfig.TV_SCHEDULE_UPDATED_EVENT, listener)
        } catch (e: Exception) {
            Log.w("PusherService", "Error unbinding tv-schedule-updated", e)
        } finally {
            tvScheduleSubscription = null
        }
    }

    private fun unbindPublicVenueListeners(channel: Channel?) {
        unbindScreenDisplayStateListener(channel)
        unbindAmountUpdatedListener(channel)
        unbindLatestTipUpdatedListener(channel)
    }

    private fun unbindGlobalLiveTvListeners(channel: Channel?) {
        unbindTipAnimationListener(channel)
        unbindTvScheduleListener(channel)
    }

    private fun bindAmountUpdatedListener(channel: Channel) {
        unbindAmountUpdatedListener(channel)
        val eventName = amountUpdatedEventName
        val subscription =
            object : SubscriptionEventListener {
                override fun onEvent(event: PusherEvent) {
                    if (isDisconnecting || publicChannel == null) return
                    try {
                        val amountUpdated = json.decodeFromString<AmountUpdatedEvent>(event.data)
                        val emitted = _amountUpdatedEvents.tryEmit(amountUpdated)
                        Log.d(
                            "PusherService",
                            "📤 amount-updated emitted=$emitted raised=${amountUpdated.amountRaised}",
                        )
                    } catch (e: Exception) {
                        Log.w("PusherService", "⚠️ Malformed $eventName payload ignored", e)
                    }
                }
            }
        channel.bind(eventName, subscription)
        amountUpdatedSubscription = subscription
        boundAmountUpdatedEventName = eventName
        Log.d("PusherService", "✅ Bound $eventName on ${channel.name}")
    }

    private fun bindLatestTipUpdatedListener(channel: Channel) {
        unbindLatestTipUpdatedListener(channel)
        val eventName = latestTipUpdatedEventName
        val subscription =
            object : SubscriptionEventListener {
                override fun onEvent(event: PusherEvent) {
                    if (isDisconnecting || publicChannel == null) return
                    try {
                        val latest = json.decodeFromString<LatestTipUpdatedEvent>(event.data)
                        val emitted = _latestTipUpdatedEvents.tryEmit(latest)
                        Log.d(
                            "PusherService",
                            "📤 latest-tip-updated emitted=$emitted amount=${latest.latestTipAmount}",
                        )
                    } catch (e: Exception) {
                        Log.w("PusherService", "⚠️ Malformed $eventName payload ignored", e)
                    }
                }
            }
        channel.bind(eventName, subscription)
        latestTipUpdatedSubscription = subscription
        boundLatestTipUpdatedEventName = eventName
        Log.d("PusherService", "✅ Bound $eventName on ${channel.name}")
    }

    private fun bindTipAnimationListener(channel: Channel) {
        unbindTipAnimationListener(channel)
        val eventName = tipAnimationEventName
        val subscription =
            object : SubscriptionEventListener {
                override fun onEvent(event: PusherEvent) {
                    if (isDisconnecting || globalLiveTvChannel == null) return
                    try {
                        Log.d("PusherService", "🔔 Received $eventName on ${channel.name}")
                        val tipAnimation = json.decodeFromString<TipAnimationEvent>(event.data)
                        val emitted = _tipAnimationEvents.tryEmit(tipAnimation)
                        Log.d(
                            "PusherService",
                            "📤 tip-animation emitted=$emitted tips=${tipAnimation.selectedTips.size} eventId=${tipAnimation.eventId}",
                        )
                    } catch (e: Exception) {
                        Log.w("PusherService", "⚠️ Malformed $eventName payload ignored", e)
                    }
                }
            }
        channel.bind(eventName, subscription)
        tipAnimationSubscription = subscription
        boundTipAnimationEventName = eventName
        Log.d("PusherService", "✅ Bound $eventName on ${channel.name}")
    }

    private fun bindPublicVenueListeners(channel: Channel) {
        bindScreenDisplayStateListener(channel)
        bindAmountUpdatedListener(channel)
        bindLatestTipUpdatedListener(channel)
    }

    private fun bindGlobalLiveTvListeners(channel: Channel) {
        bindTipAnimationListener(channel)
        unbindTvScheduleListener(channel)
        val scheduleSubscription =
            object : SubscriptionEventListener {
                override fun onEvent(event: PusherEvent) {
                    if (globalLiveTvChannel == null) return
                    try {
                        val payload =
                            try {
                                json.decodeFromString<TvScheduleUpdatedPayload>(event.data)
                            } catch (_: Exception) {
                                TvScheduleUpdatedPayload()
                            }
                        _tvScheduleUpdated.tryEmit(payload)
                    } catch (e: Exception) {
                        Log.w("PusherService", "tv-schedule-updated handler error", e)
                    }
                }
            }
        channel.bind(PusherConfig.TV_SCHEDULE_UPDATED_EVENT, scheduleSubscription)
        tvScheduleSubscription = scheduleSubscription
    }
    
    /**
     * Initializes and connects to Pusher.
     * Should be called before subscribing to channels.
     * 
     * @param forceReconnect If true, disconnects existing connection before reinitializing
     * @return true if initialization was successful, false otherwise
     */
    suspend fun initialize(forceReconnect: Boolean = false): Boolean {
        // Brief check if disconnect is in progress
        var waitAttempts = 0
        while (isDisconnecting && waitAttempts < 10) {  // Max 1 second (should be very quick now)
            Log.d("PusherService", "⏳ Waiting for disconnect flag reset (attempt $waitAttempts)")
            delay(100)
            waitAttempts++
        }
        
        if (isDisconnecting) {
            Log.e("PusherService", "❌ Disconnect flag still true after waiting - aborting initialize")
            return false
        }
        
        return pusherMutex.withLock {
        // CRITICAL: If Pusher instance is corrupted, force complete recreation
        if (isPusherCorrupted) {
            Log.w("PusherService", "⚠️ Pusher instance corrupted - forcing complete recreation")
            forceDestroy()
            isPusherCorrupted = false
        }
        
        // If already initialized and not forcing reconnect, return true
        if (pusher != null && !forceReconnect) {
            // Check if pusher is still connected
            val state = pusher?.connection?.state
            if (state == ConnectionState.CONNECTED || state == ConnectionState.CONNECTING) {
                Log.d("PusherService", "Pusher already initialized and connected")
                return@withLock true
            } else {
                Log.d("PusherService", "Pusher exists but not connected (state: $state), reinitializing...")
                forceDestroy()
            }
        }
        
        // If forcing reconnect, disconnect first
        if (forceReconnect && pusher != null) {
            Log.d("PusherService", "Force reconnecting Pusher...")
            forceDestroy()
        }
        
        try {
            // Disconnecting flag should already be false, but ensure it
            isDisconnecting = false
            Log.d("PusherService", "   - Confirmed isDisconnecting = false")
            
            val credentials =
                configuredCredentials
                    ?: resolvePusherCredentials(null, null).also { configuredCredentials = it }
            if (
                activeCredentialsKey != null &&
                    (activeCredentialsKey != credentials.apiKey ||
                        activeCredentialsCluster != credentials.cluster)
            ) {
                Log.w("PusherService", "⚠️ Pusher credentials changed — recreating client")
                forceDestroy()
            }
            Log.d(
                "PusherService",
                "🔑 Using Pusher credentials: ${formatPusherCredentialDiagnostic(credentials)}",
            )

            val options = PusherOptions()
            options.setCluster(credentials.cluster)
            
            options.setAuthorizer(
                PusherHttpAuthorizer(
                    httpClient = httpClient,
                    sessionStore = sessionStore,
                    sessionExpiryHandler = sessionExpiryHandler,
                    eventCodeProvider = { eventCodeForAuth },
                ),
            )
            Log.d("PusherService", "🔐 Server-side Pusher auth via /api/v1/tv-app/pusher/auth")
            
            pusher = Pusher(credentials.apiKey, options)
            activeCredentialsKey = credentials.apiKey
            activeCredentialsCluster = credentials.cluster
            pusher?.connect(object : ConnectionEventListener {
                override fun onConnectionStateChange(change: ConnectionStateChange) {
                    // CRITICAL: Don't process callbacks if we're disconnecting
                    if (isDisconnecting) {
                        Log.d("PusherService", "⏭️ Skipping connection state callback - disconnecting in progress")
                        return
                    }
                    
                    // Log connection state changes for debugging
                    Log.d("PusherService", "🔌 Connection state changed: ${change.previousState} -> ${change.currentState}")
                    when (change.currentState) {
                        ConnectionState.CONNECTED -> {
                            Log.d("PusherService", "✅ Pusher connection established")
                            if (change.previousState != ConnectionState.CONNECTED) {
                                _connectionEstablished.tryEmit(Unit)
                            }
                        }
                        ConnectionState.DISCONNECTED -> {
                            Log.d("PusherService", "❌ Pusher disconnected")
                        }
                        ConnectionState.CONNECTING -> {
                            Log.d("PusherService", "🔄 Connecting to Pusher...")
                        }
                        ConnectionState.DISCONNECTING -> {
                            Log.d("PusherService", "🔄 Disconnecting from Pusher...")
                        }
                        ConnectionState.RECONNECTING -> {
                            Log.d("PusherService", "🔄 Reconnecting to Pusher...")
                        }
                        else -> {
                            Log.d("PusherService", "🔌 Connection state: ${change.currentState}")
                        }
                    }
                }
                
                override fun onError(message: String?, code: String?, e: Exception?) {
                    // Pusher Java client may invoke with null code/message on websocket faults.
                    Log.e(
                        "PusherService",
                        "❌ PUSHER connection error: ${formatPusherConnectionError(message, code, e)}",
                    )
                    if (e != null) {
                        Log.e("PusherService", "   - Exception detail", e)
                    }
                }
            }, ConnectionState.ALL)
            Log.d("PusherService", "✅ Pusher initialized")
            true
        } catch (e: Exception) {
            Log.e("PusherService", "Failed to initialize Pusher", e)
            false
        }
        } // End of pusherMutex.withLock
    }
    
    /**
     * Subscribes to a public event channel (e.g., "event-{eventId}").
     * 
     * @param channelName Name of the channel (e.g., "event-{eventId}")
     * @return true if subscription was successful, false otherwise
     */
    suspend fun subscribeToPublicChannel(channelName: String): Boolean = pusherMutex.withLock {
        return try {
            if (isPusherCorrupted) {
                Log.e("PusherService", "❌ Cannot subscribe - Pusher instance corrupted")
                return false
            }
            
            pusher?.let { pusherInstance ->
                // Unsubscribe from previous public channel if exists and it's a different channel
                publicChannel?.let { channel ->
                    if (channel.name != channelName) {
                        Log.d("PusherService", "🔄 Unsubscribing from previous public channel: ${channel.name}")
                        try {
                            pusherInstance.unsubscribe(channel.name)
                        } catch (e: Exception) {
                            Log.w("PusherService", "Error unsubscribing from channel ${channel.name}", e)
                        }
                        publicChannel = null
                    }
                }
                
                try {
                    publicChannel = pusherInstance.subscribe(channelName)
                    bindPublicVenueListeners(publicChannel!!)
                    Log.d("PusherService", "✅ Subscribed to public channel: $channelName")
                    true
                } catch (e: IllegalArgumentException) {
                    Log.e("PusherService", "❌ IllegalArgumentException - Pusher instance corrupted", e)
                    isPusherCorrupted = true
                    false
                } catch (e: Exception) {
                    Log.e("PusherService", "Failed to subscribe to channel: $channelName", e)
                    false
                }
            } ?: run {
                Log.e("PusherService", "Pusher not initialized. Call initialize() first.")
                false
            }
        } catch (e: Exception) {
            Log.e("PusherService", "Critical error in subscribeToPublicChannel", e)
            false
        }
    }

    /**
     * Subscribes to canonical payment animation channel (public).
     */
    suspend fun subscribeToGlobalLiveTv(): Boolean = pusherMutex.withLock {
        return try {
            pusher?.let { pusherInstance ->
                if (globalLiveTvChannel?.name == PusherConfig.GLOBAL_LIVE_TV_CHANNEL) {
                    return true
                }
                globalLiveTvChannel?.let { existing ->
                    try {
                        pusherInstance.unsubscribe(existing.name)
                    } catch (_: Exception) {
                    }
                }
                globalLiveTvChannel =
                    pusherInstance.subscribe(PusherConfig.GLOBAL_LIVE_TV_CHANNEL)
                bindGlobalLiveTvListeners(globalLiveTvChannel!!)
                Log.d(
                    "PusherService",
                    "✅ Subscribed to ${PusherConfig.GLOBAL_LIVE_TV_CHANNEL}",
                )
                true
            } ?: false
        } catch (e: Exception) {
            Log.e("PusherService", "Failed to subscribe to global-live-tv", e)
            false
        }
    }
    
    /**
     * Subscribes to a private event channel (e.g., "private-event-{eventId}").
     * Private channels require authorization.
     * 
     * @param channelName Name of the private channel (e.g., "private-event-{eventId}")
     * @return true if subscription was successful, false otherwise
     */
    suspend fun subscribeToPrivateChannel(channelName: String): Boolean = pusherMutex.withLock {
        return try {
            if (isPusherCorrupted) {
                Log.e("PusherService", "❌ Cannot subscribe - Pusher instance corrupted")
                return false
            }
            
            pusher?.let { pusherInstance ->
                // Unsubscribe from previous private channel if exists and it's a different channel
                privateChannel?.let { channel ->
                    if (channel.name != channelName) {
                        Log.d("PusherService", "🔄 Unsubscribing from previous private channel: ${channel.name}")
                        try {
                            pusherInstance.unsubscribe(channel.name)
                        } catch (e: Exception) {
                            Log.w("PusherService", "Error unsubscribing from channel ${channel.name}", e)
                        }
                        privateChannel = null
                    }
                }
                
                try {
                    // IMPORTANT: Use subscribePrivate for private channels
                    // THIS IS THE CRITICAL POINT where IllegalArgumentException may occur
                    privateChannel = pusherInstance.subscribePrivate(channelName)
                    
                    // NOTE: Do NOT manually bind pusher:subscription_succeeded and pusher:subscription_error
                    // These are internal events that the Pusher client handles automatically
                    // Binding them manually causes IllegalArgumentException
                    
                    Log.d("PusherService", "   - Private channel object created: $channelName")
                    Log.d("PusherService", "   - Waiting for Pusher to complete subscription...")
                    
                    // Give Pusher time to complete the subscription handshake
                    // The authorization happens asynchronously
                    
                } catch (e: IllegalArgumentException) {
                    // CRITICAL: IllegalArgumentException indicates Pusher instance is corrupted
                    Log.e("PusherService", "❌❌❌ IllegalArgumentException - PUSHER INSTANCE CORRUPTED", e)
                    Log.e("PusherService", "   Message: ${e.message}")
                    Log.e("PusherService", "   Channel: $channelName")
                    isPusherCorrupted = true
                    return false
                } catch (e: Exception) {
                    Log.e("PusherService", "❌ Unexpected error during private channel subscription", e)
                    return false
                }
                
                Log.d("PusherService", "✅ Subscribed to private channel: $channelName")
                true
            } ?: run {
                Log.e("PusherService", "Pusher not initialized. Call initialize() first.")
                false
            }
        } catch (e: Exception) {
            Log.e("PusherService", "Failed to subscribe to private channel: $channelName", e)
            false
        }
    }
    
    /**
     * After local session invalidation, drop venue private-channel subscription only.
     * Public venue channels and [PusherConfig.GLOBAL_LIVE_TV_CHANNEL] stay subscribed so
     * anonymous/global LIVE TV realtime hints can still flow until the next reconnect cycle.
     */
    private suspend fun dropAuthenticatedPrivateChannelSubscription() {
        pusherMutex.withLock {
            val pusherInstance = pusher ?: return@withLock
            privateChannel?.let { channel ->
                try {
                    pusherInstance.unsubscribe(channel.name)
                    Log.d(
                        "PusherService",
                        "🔐 Dropped private channel after session cleared: ${channel.name}",
                    )
                } catch (e: Exception) {
                    Log.w("PusherService", "Error dropping private channel after session clear", e)
                }
                privateChannel = null
            }
            eventCodeForAuth = null
        }
    }

    /**
     * Complete instance destruction for corrupted state recovery.
     * Use when Pusher instance needs to be completely recreated.
     */
    private fun forceDestroy() {
        try {
            Log.d("PusherService", "💥 Force destroying Pusher instance")
            
            unbindPublicVenueListeners(publicChannel)
            unbindGlobalLiveTvListeners(globalLiveTvChannel)
            unbindCommunityDisplayUpdatedListener(communityDisplayChannel)
            publicChannel = null
            privateChannel = null
            communityDisplayChannel = null
            
            // Don't call disconnect() - just null out the instance
            // Garbage collection will handle cleanup
            Log.d("PusherService", "   - Skipping disconnect(), nulling out instance for GC")
            pusher = null
            isPusherCorrupted = false
            
            Log.d("PusherService", "✅ Force destroy complete - clean slate")
        } catch (e: Exception) {
            Log.e("PusherService", "❌ Error in force destroy", e)
        }
    }
    
    /**
     * Resets the disconnecting flag. Called after disconnect has completed its async operations.
     * This allows the next initialize() call to proceed.
     */
    fun resetDisconnectingFlag() {
        Log.d("PusherService", "🔓 Resetting isDisconnecting flag to false")
        isDisconnecting = false
    }
    
    /**
     * Unsubscribes from all channels and disconnects.
     */
    suspend fun disconnect() {
        Log.d("PusherService", "🔌 [DISCONNECT] Starting Pusher disconnect")
        
        // CRITICAL: Set flag FIRST to prevent connection listener callbacks
        isDisconnecting = true
        Log.d("PusherService", "   - Set isDisconnecting = true")
        
        // Store pusher reference before entering mutex
        val pusherInstance = pusher
        
        pusherMutex.withLock {
            Log.d("PusherService", "   - Public channel: ${publicChannel?.name ?: "null"}")
            Log.d("PusherService", "   - Private channel: ${privateChannel?.name ?: "null"}")
            Log.d("PusherService", "   - Pusher instance: ${if (pusher != null) "exists" else "null"}")
        
        try {
            // Store channel references before clearing
            Log.d("PusherService", "   [STEP 1] Storing channel references...")
            val pubChannel = publicChannel
            val privChannel = privateChannel
            val pusherInstance = pusher
            Log.d("PusherService", "   [STEP 1] ✅ References stored")
            
            // Clear channel references FIRST to prevent flows from accessing them
            Log.d("PusherService", "   [STEP 2] Clearing channel references...")
            unbindPublicVenueListeners(pubChannel)
            unbindGlobalLiveTvListeners(globalLiveTvChannel)
            unbindCommunityDisplayUpdatedListener(communityDisplayChannel)
            publicChannel = null
            privateChannel = null
            communityDisplayChannel = null
            Log.d("PusherService", "   [STEP 2] ✅ Channel references cleared")
            
            pusherInstance?.let {
                Log.d("PusherService", "   [STEP 3] Unsubscribing from channels...")
                
                // Unsubscribe from channels (using stored references)
                pubChannel?.let { channel ->
                    try {
                        Log.d("PusherService", "      - Unsubscribing from public channel: ${channel.name}")
                        it.unsubscribe(channel.name)
                        Log.d("PusherService", "      - ✅ Unsubscribed from public channel: ${channel.name}")
                    } catch (e: Exception) {
                        Log.w("PusherService", "      - ❌ Error unsubscribing from public channel", e)
                    }
                } ?: run {
                    Log.d("PusherService", "      - ⚠️ No public channel to unsubscribe")
                }
                
                privChannel?.let { channel ->
                    try {
                        Log.d("PusherService", "      - Unsubscribing from private channel: ${channel.name}")
                        it.unsubscribe(channel.name)
                        Log.d("PusherService", "      - ✅ Unsubscribed from private channel: ${channel.name}")
                    } catch (e: Exception) {
                        Log.w("PusherService", "      - ❌ Error unsubscribing from private channel", e)
                    }
                } ?: run {
                    Log.d("PusherService", "      - ⚠️ No private channel to unsubscribe")
                }
                
                Log.d("PusherService", "   [STEP 3] ✅ Channel unsubscription complete")
                
                // DON'T call disconnect() - it triggers Pusher's internal cleanup threads which can crash
                // Instead, just unsubscribe (already done above) and null out the reference (done in finally)
                // Garbage collection will handle cleanup
                Log.d("PusherService", "   [STEP 4] Skipping pusher.disconnect() to avoid internal thread crashes")
                Log.d("PusherService", "   [STEP 4] ✅ Will rely on garbage collection for cleanup")
            } ?: run {
                Log.d("PusherService", "   [STEP 3] ⚠️ No Pusher instance to disconnect")
            }
        } catch (e: Exception) {
            Log.e("PusherService", "❌ [DISCONNECT] Error during disconnect", e)
        } finally {
            // Ensure all references are cleared
            Log.d("PusherService", "   [STEP 5] Final cleanup - clearing all references...")
            publicChannel = null
            privateChannel = null
            globalLiveTvChannel = null
            communityDisplayChannel = null
            
            // CRITICAL: Always null out the Pusher instance to force fresh creation on next initialize
            // This prevents issues with trying to reuse a Pusher instance that's still disconnecting
            Log.d("PusherService", "   - Nulling out Pusher instance for fresh creation on next initialize")
            pusher = null
            isPusherCorrupted = false // Reset corruption flag
            
            Log.d("PusherService", "   [STEP 5] ✅ All references cleared")
        }
        } // End of pusherMutex.withLock
        
        Log.d("PusherService", "✅ [DISCONNECT] Pusher disconnect complete (async disconnection continuing in background)")
    }
    
    /**
     * Invalidation hints for global TV schedule — refresh via GET /api/v1/tv-app/schedule.
     */
    fun observeTvScheduleUpdated(): Flow<TvScheduleUpdatedPayload> =
        _tvScheduleUpdated.asSharedFlow()

    fun observeTipAnimation(): Flow<TipAnimationEvent> = _tipAnimationEvents.asSharedFlow()

    fun observeAmountUpdated(): Flow<AmountUpdatedEvent> = _amountUpdatedEvents.asSharedFlow()
    
    /**
     * Creates a Flow stream for `screen-display-state` on the public event channel.
     * Event name comes from tv-app bootstrap (`realtime.events.screenDisplayState`).
     */
    fun observeScreenDisplayState(
        pusherEventName: String = DEFAULT_SCREEN_DISPLAY_STATE_EVENT,
    ): Flow<ScreenDisplayStatePayload> {
        setScreenDisplayStateEventName(pusherEventName)
        return _screenDisplayStatePayloads.asSharedFlow()
    }

    /**
     * Creates a Flow stream for screen-settings-updated events.
     * These events come from the private channel (same payload as screen-display-state).
     *
     * @return Flow of ScreenDisplayStatePayload
     */
    fun observeScreenSettingsUpdated(
        pusherEventName: String = DEFAULT_SCREEN_SETTINGS_UPDATED_EVENT,
    ): Flow<ScreenDisplayStatePayload> = callbackFlow {
        // Wait for private channel to be available (with timeout)
        var channel: Channel? = null
        var attempts = 0
        while (channel == null && attempts < 20) {
            channel = privateChannel
            if (channel == null) {
                kotlinx.coroutines.delay(100)
                attempts++
            }
        }
        
        val finalChannel = channel as? PrivateChannel ?: run {
            Log.e("PusherService", "Private channel not available after waiting")
            close()
            return@callbackFlow
        }
        
        val subscription = object : PrivateChannelEventListener {
            override fun onEvent(event: PusherEvent) {
                try {
                    // Check if channel is still valid before processing
                    if (privateChannel == null) {
                        Log.d("PusherService", "⚠️ Private channel is null, ignoring event")
                        return
                    }
                    Log.d("PusherService", "🔔 Received $pusherEventName from private channel")
                    val payload = parseScreenDisplayStatePayload(event.data)
                    if (payload == null) {
                        Log.w("PusherService", "⚠️ Ignoring malformed $pusherEventName payload")
                        return
                    }
                    trySend(payload)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    // Cancellation is expected when disconnecting - don't log as error
                    throw e
                } catch (e: Exception) {
                    Log.e("PusherService", "❌ Error parsing settings-updated event", e)
                    Log.e("PusherService", "   - Raw data that failed: ${event.data}")
                    e.printStackTrace()
                }
            }
            
            override fun onSubscriptionSucceeded(channelName: String) {
                Log.d("PusherService", "✅ Private channel subscription succeeded: $channelName")
            }
            
            override fun onAuthenticationFailure(message: String?, e: Exception?) {
                Log.e(
                    "PusherService",
                    "❌ Private channel authentication failed: ${message ?: "(null)"}",
                    e,
                )
            }
        }
        
        finalChannel.bind(pusherEventName, subscription)
        Log.d("PusherService", "✅ Bound to $pusherEventName on private channel")
        
        awaitClose {
            try {
                // Only unbind if channel still exists and is valid
                val currentChannel = privateChannel
                if (finalChannel != null && currentChannel != null && finalChannel == currentChannel) {
                    finalChannel.unbind(pusherEventName, subscription)
                    Log.d("PusherService", "✅ Unbound $pusherEventName listener")
                } else {
                    Log.d("PusherService", "⚠️ Skipping $pusherEventName unbind - channel already disposed")
                }
            } catch (e: Exception) {
                Log.w("PusherService", "Error unbinding screen-settings-updated listener", e)
            }
        }
    }
    
    /**
     * Creates a Flow stream for latest-tip-updated events.
     * These events come from the public channel.
     * 
     * This event is triggered when a tip is made and updates the latest tip amount
     * displayed on the screen.
     * 
     * @return Flow of LatestTipUpdatedEvent
     */
    fun observeLatestTipUpdated(): Flow<LatestTipUpdatedEvent> =
        _latestTipUpdatedEvents.asSharedFlow()

    suspend fun subscribeToCommunityDisplayChannel(channelName: String): Boolean =
        pusherMutex.withLock {
            try {
                if (isPusherCorrupted) return false
                pusher?.let { pusherInstance ->
                    communityDisplayChannel?.let { existing ->
                        if (existing.name != channelName) {
                            unbindCommunityDisplayUpdatedListener(existing)
                            try {
                                pusherInstance.unsubscribe(existing.name)
                            } catch (_: Exception) {
                            }
                            communityDisplayChannel = null
                        }
                    }
                    communityDisplayChannel = pusherInstance.subscribe(channelName)
                    bindCommunityDisplayUpdatedListener(communityDisplayChannel!!)
                    Log.d("PusherService", "✅ Subscribed to community display channel: $channelName")
                    true
                } ?: false
            } catch (e: Exception) {
                Log.e("PusherService", "Failed to subscribe to community channel", e)
                false
            }
        }

    fun observeCommunityDisplayUpdated(): Flow<CommunityDisplayUpdatedPayload> =
        _communityDisplayUpdatedPayloads.asSharedFlow()

    private fun unbindCommunityDisplayUpdatedListener(channel: Channel?) {
        val listener = communityDisplayUpdatedSubscription ?: return
        val eventName = communityDisplayUpdatedEventName
        channel ?: return
        try {
            channel.unbind(eventName, listener)
        } catch (e: Exception) {
            Log.w("PusherService", "Error unbinding $eventName on community channel", e)
        } finally {
            communityDisplayUpdatedSubscription = null
        }
    }

    private fun bindCommunityDisplayUpdatedListener(channel: Channel) {
        unbindCommunityDisplayUpdatedListener(channel)
        val eventName = communityDisplayUpdatedEventName
        val subscription =
            object : SubscriptionEventListener {
                override fun onEvent(event: PusherEvent) {
                    if (isDisconnecting || communityDisplayChannel == null) return
                    val payload = parseCommunityDisplayUpdatedPayload(event.data)
                    if (payload == null) {
                        Log.w("PusherService", "⚠️ Malformed $eventName on ${channel.name}")
                        return
                    }
                    val emitted = _communityDisplayUpdatedPayloads.tryEmit(payload)
                    Log.d(
                        "PusherService",
                        "📤 community $eventName emitted=$emitted communityId=${payload.communityId}",
                    )
                }
            }
        channel.bind(eventName, subscription)
        communityDisplayUpdatedSubscription = subscription
        Log.d("PusherService", "✅ Bound $eventName on ${channel.name}")
    }
}
