package com.example.telepse.data.realtime

import com.example.telepse.data.model.AmountUpdatedEvent
import com.example.telepse.data.model.LatestTipUpdatedEvent
import com.example.telepse.data.model.ScreenSettingsUpdatedEvent
import com.example.telepse.data.model.TipAnimationEvent
import com.example.telepse.data.realtime.PusherConfig
import com.pusher.client.Pusher
import com.pusher.client.PusherOptions
import com.pusher.client.Authorizer
import com.pusher.client.channel.Channel
import com.pusher.client.channel.PrivateChannel
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.net.URLEncoder
import com.pusher.client.channel.PusherEvent
import com.pusher.client.channel.SubscriptionEventListener
import com.pusher.client.channel.PrivateChannelEventListener
import com.pusher.client.connection.ConnectionEventListener
import com.pusher.client.connection.ConnectionState
import com.pusher.client.connection.ConnectionStateChange
import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
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
class PusherService @Inject constructor() {
    
    private var pusher: Pusher? = null
    private var publicChannel: Channel? = null
    private var privateChannel: Channel? = null
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
            
            val options = PusherOptions()
            options.setCluster(PusherConfig.CLUSTER)
            
            // Set up client-side authorization for private channels
            // This mimics pusher.authorizeChannel() from the backend
            // WARNING: This uses the SECRET on the client side, which is insecure!
            // This should only be used if you don't have a backend authorization endpoint.
            // For production, you should use HttpAuthorizer with a backend endpoint.
            val authorizer = object : Authorizer {
                override fun authorize(channelName: String, socketId: String): String {
                    try {
                        Log.d("PusherService", "🔐 Authorizing private channel: $channelName")
                        Log.d("PusherService", "   - Socket ID: $socketId")
                        Log.d("PusherService", "   - Channel: $channelName")
                        
                        // Generate HMAC-SHA256 signature (same as pusher.authorizeChannel())
                        // Format: HMAC-SHA256(socket_id:channel_name, SECRET)
                        val stringToSign = "$socketId:$channelName"
                        val secretKey = SecretKeySpec(PusherConfig.SECRET.toByteArray(), "HmacSHA256")
                        val mac = Mac.getInstance("HmacSHA256")
                        mac.init(secretKey)
                        val signature = mac.doFinal(stringToSign.toByteArray())
                        val authSignature = signature.joinToString("") { "%02x".format(it) }
                        
                        // Return authorization response in JSON format
                        // Format matches: {"auth": "API_KEY:SIGNATURE"}
                        val authResponse = """{"auth":"${PusherConfig.API_KEY}:$authSignature"}"""
                        Log.d("PusherService", "✅ Authorization signature generated")
                        Log.d("PusherService", "   - Auth response: $authResponse")
                        return authResponse
                    } catch (e: Exception) {
                        Log.e("PusherService", "❌ Error generating authorization", e)
                        e.printStackTrace()
                        throw e
                    }
                }
            }
            options.setAuthorizer(authorizer)
            
            Log.d("PusherService", "🔐 Client-side authorization configured for private channels")
            Log.w("PusherService", "⚠️ WARNING: Using SECRET on client side - not secure for production!")
            
            pusher = Pusher(PusherConfig.API_KEY, options)
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
                
                override fun onError(message: String, code: String, e: Exception?) {
                    // Log errors for debugging (matches React: pusherClient.connection.bind("error"))
                    Log.e("PusherService", "❌ PUSHER ERROR: $message")
                    Log.e("PusherService", "   - Error code: $code")
                    if (e != null) {
                        Log.e("PusherService", "   - Exception: ${e.message}", e)
                        e.printStackTrace()
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
     * Complete instance destruction for corrupted state recovery.
     * Use when Pusher instance needs to be completely recreated.
     */
    private fun forceDestroy() {
        try {
            Log.d("PusherService", "💥 Force destroying Pusher instance")
            
            // Clear channel references
            publicChannel = null
            privateChannel = null
            
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
            publicChannel = null
            privateChannel = null
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
     * Creates a Flow stream for tip-animation events.
     * These events come from the public channel.
     * 
     * @return Flow of TipAnimationEvent
     */
    fun observeTipAnimation(): Flow<TipAnimationEvent> = callbackFlow {
        // Wait for public channel to be available (with timeout)
        var channel: Channel? = null
        var attempts = 0
        while (channel == null && attempts < 20) {
            channel = publicChannel
            if (channel == null) {
                kotlinx.coroutines.delay(100)
                attempts++
            }
        }
        
        val finalChannel = channel ?: run {
            Log.e("PusherService", "Public channel not available after waiting")
            close()
            return@callbackFlow
        }
        
        val subscription = object : SubscriptionEventListener {
            override fun onEvent(event: PusherEvent) {
                try {
                    // Check if channel is still valid before processing
                    if (publicChannel == null) {
                        Log.d("PusherService", "⚠️ Public channel is null, ignoring tip-animation event")
                        return
                    }
                    Log.d("PusherService", "🔔 Received tip-animation event")
                    Log.d("PusherService", "Raw event data: ${event.data}")
                    Log.d("PusherService", "Event name: ${event.eventName}")
                    
                    // Try to parse the event
                    val tipAnimation = json.decodeFromString<TipAnimationEvent>(event.data)
                    Log.d("PusherService", "✅ Successfully parsed tip-animation: ${tipAnimation.selectedTips.size} tips")
                    Log.d("PusherService", "Tip names: ${tipAnimation.selectedTips}")
                    Log.d("PusherService", "Amount: ${tipAnimation.amount}, Sender: ${tipAnimation.sender}")
                    
                    // Create a formatted JSON string for display
                    val jsonDisplay = """
                        {
                          "selectedTips": [${tipAnimation.selectedTips.joinToString(", ") { "\"$it\"" }}],
                          "amount": ${tipAnimation.amount},
                          "sender": "${tipAnimation.sender}"
                        }
                    """.trimIndent()
                    
                    // Store raw JSON in a way that can be accessed by ViewModel
                    // We'll pass it through the event or use a different approach
                    Log.d("PusherService", "📋 JSON Payload:\n$jsonDisplay")
                    
                    // Send to flow
                    val sent = trySend(tipAnimation)
                    if (sent.isSuccess) {
                        Log.d("PusherService", "✅ Sent tip-animation to flow")
                    } else {
                        Log.e("PusherService", "❌ Failed to send tip-animation to flow: ${sent.exceptionOrNull()}")
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    // Cancellation is expected when disconnecting - don't log as error
                    throw e
                } catch (e: Exception) {
                    // Log error but don't crash
                    Log.e("PusherService", "❌ Error parsing tip-animation event", e)
                    Log.e("PusherService", "Error message: ${e.message}")
                    Log.e("PusherService", "Error type: ${e.javaClass.simpleName}")
                    Log.e("PusherService", "Raw data that failed: ${event.data}")
                    e.printStackTrace()
                }
            }
        }
        
        finalChannel.bind("tip-animation", subscription)
        Log.d("PusherService", "✅ Bound to tip-animation event - ready to receive events")
        
        awaitClose {
            try {
                // Only unbind if channel still exists and is valid
                val currentChannel = publicChannel
                if (finalChannel != null && currentChannel != null && finalChannel == currentChannel) {
                    finalChannel.unbind("tip-animation", subscription)
                    Log.d("PusherService", "✅ Unbound tip-animation listener")
                } else {
                    Log.d("PusherService", "⚠️ Skipping tip-animation unbind - channel already disposed")
                }
            } catch (e: Exception) {
                Log.w("PusherService", "Error unbinding tip-animation listener", e)
            }
        }
    }
    
    /**
     * Creates a Flow stream for event-amount-updated events.
     * These events come from the public channel.
     * 
     * @return Flow of AmountUpdatedEvent
     */
    fun observeAmountUpdated(): Flow<AmountUpdatedEvent> = callbackFlow {
        // Wait for public channel to be available (with timeout)
        var channel: Channel? = null
        var attempts = 0
        while (channel == null && attempts < 20) {
            channel = publicChannel
            if (channel == null) {
                kotlinx.coroutines.delay(100)
                attempts++
            }
        }
        
        val finalChannel = channel ?: run {
            Log.e("PusherService", "Public channel not available after waiting")
            close()
            return@callbackFlow
        }
        
        val subscription = object : SubscriptionEventListener {
            override fun onEvent(event: PusherEvent) {
                try {
                    // Check if channel is still valid before processing
                    if (publicChannel == null) {
                        Log.d("PusherService", "⚠️ Public channel is null, ignoring amount-updated event")
                        return
                    }
                    Log.d("PusherService", "🔔 Received event-amount-updated: ${event.data}")
                    val amountUpdated = json.decodeFromString<AmountUpdatedEvent>(event.data)
                    Log.d("PusherService", "✅ Parsed amount-updated: £${amountUpdated.amountRaised}, ${amountUpdated.numOfTips} tips")
                    trySend(amountUpdated)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    // Cancellation is expected when disconnecting - don't log as error
                    throw e
                } catch (e: Exception) {
                    Log.e("PusherService", "❌ Error parsing amount-updated event", e)
                    e.printStackTrace()
                }
            }
        }
        
        finalChannel.bind("event-amount-updated", subscription)
        Log.d("PusherService", "✅ Bound to event-amount-updated event - ready to receive events")
        
        awaitClose {
            try {
                // Only unbind if channel still exists and is valid
                val currentChannel = publicChannel
                if (finalChannel != null && currentChannel != null && finalChannel == currentChannel) {
                    finalChannel.unbind("event-amount-updated", subscription)
                    Log.d("PusherService", "✅ Unbound event-amount-updated listener")
                } else {
                    Log.d("PusherService", "⚠️ Skipping event-amount-updated unbind - channel already disposed")
                }
            } catch (e: Exception) {
                Log.w("PusherService", "Error unbinding event-amount-updated listener", e)
            }
        }
    }
    
    /**
     * Creates a Flow stream for screen-settings-updated events.
     * These events come from the private channel.
     * 
     * @return Flow of ScreenSettingsUpdatedEvent
     */
    fun observeScreenSettingsUpdated(): Flow<ScreenSettingsUpdatedEvent> = callbackFlow {
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
                    Log.d("PusherService", "🔔 Received screen-settings-updated event from private channel")
                    Log.d("PusherService", "   - Event name: ${event.eventName}")
                    Log.d("PusherService", "   - Raw event data: ${event.data}")
                    val settingsUpdated = json.decodeFromString<ScreenSettingsUpdatedEvent>(event.data)
                    Log.d("PusherService", "✅ Parsed settings-updated successfully")
                    Log.d("PusherService", "   - eventBackground: ${settingsUpdated.eventBackground}")
                    Log.d("PusherService", "   - backgroundType: ${settingsUpdated.backgroundType}")
                    Log.d("PusherService", "   - hideProgressBarAmount: ${settingsUpdated.hideProgressBarAmount}")
                    Log.d("PusherService", "   - showLatestAmount: ${settingsUpdated.showLatestAmount}")
                    Log.d("PusherService", "   - moneyFor: ${settingsUpdated.moneyFor}")
                    trySend(settingsUpdated)
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
            
            override fun onAuthenticationFailure(message: String, e: Exception?) {
                Log.e("PusherService", "❌ Private channel authentication failed: $message", e)
            }
        }
        
        finalChannel.bind("screen-settings-updated", subscription)
        Log.d("PusherService", "✅ Bound to screen-settings-updated event on private channel - ready to receive events")
        
        awaitClose {
            try {
                // Only unbind if channel still exists and is valid
                val currentChannel = privateChannel
                if (finalChannel != null && currentChannel != null && finalChannel == currentChannel) {
                    finalChannel.unbind("screen-settings-updated", subscription)
                    Log.d("PusherService", "✅ Unbound screen-settings-updated listener")
                } else {
                    Log.d("PusherService", "⚠️ Skipping screen-settings-updated unbind - channel already disposed")
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
    fun observeLatestTipUpdated(): Flow<LatestTipUpdatedEvent> = callbackFlow {
        // Wait for public channel to be available (with timeout)
        var channel: Channel? = null
        var attempts = 0
        while (channel == null && attempts < 20) {
            channel = publicChannel
            if (channel == null) {
                kotlinx.coroutines.delay(100)
                attempts++
            }
        }
        
        val finalChannel = channel ?: run {
            Log.e("PusherService", "Public channel not available after waiting")
            close()
            return@callbackFlow
        }
        
        val subscription = object : SubscriptionEventListener {
            override fun onEvent(event: PusherEvent) {
                try {
                    // Check if channel is still valid before processing
                    if (publicChannel == null) {
                        Log.d("PusherService", "⚠️ Public channel is null, ignoring latest-tip-updated event")
                        return
                    }
                    Log.d("PusherService", "🔔 Received latest-tip-updated: ${event.data}")
                    val latestTipUpdated = json.decodeFromString<LatestTipUpdatedEvent>(event.data)
                    Log.d("PusherService", "✅ Parsed latest-tip-updated: £${latestTipUpdated.latestTipAmount}")
                    trySend(latestTipUpdated)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    // Cancellation is expected when disconnecting - don't log as error
                    throw e
                } catch (e: Exception) {
                    Log.e("PusherService", "❌ Error parsing latest-tip-updated event", e)
                    e.printStackTrace()
                }
            }
        }
        
        finalChannel.bind("latest-tip-updated", subscription)
        Log.d("PusherService", "✅ Bound to latest-tip-updated event - ready to receive events")
        
        awaitClose {
            try {
                // Only unbind if channel still exists and is valid
                val currentChannel = publicChannel
                if (finalChannel != null && currentChannel != null && finalChannel == currentChannel) {
                    finalChannel.unbind("latest-tip-updated", subscription)
                    Log.d("PusherService", "✅ Unbound latest-tip-updated listener")
                } else {
                    Log.d("PusherService", "⚠️ Skipping latest-tip-updated unbind - channel already disposed")
                }
            } catch (e: Exception) {
                Log.w("PusherService", "Error unbinding latest-tip-updated listener", e)
            }
        }
    }
}
