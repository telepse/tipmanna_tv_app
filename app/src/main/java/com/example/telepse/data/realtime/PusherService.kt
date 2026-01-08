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
    
    /**
     * Initializes and connects to Pusher.
     * Should be called before subscribing to channels.
     * 
     * @param forceReconnect If true, disconnects existing connection before reinitializing
     * @return true if initialization was successful, false otherwise
     */
    fun initialize(forceReconnect: Boolean = false): Boolean {
        // If already initialized and not forcing reconnect, return true
        if (pusher != null && !forceReconnect) {
            // Check if pusher is still connected
            val state = pusher?.connection?.state
            if (state == ConnectionState.CONNECTED || state == ConnectionState.CONNECTING) {
                Log.d("PusherService", "Pusher already initialized and connected")
                return true
            } else {
                Log.d("PusherService", "Pusher exists but not connected (state: $state), reinitializing...")
                disconnect()
            }
        }
        
        // If forcing reconnect, disconnect first
        if (forceReconnect && pusher != null) {
            Log.d("PusherService", "Force reconnecting Pusher...")
            disconnect()
        }
        
        return try {
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
    }
    
    /**
     * Subscribes to a public event channel (e.g., "event-{eventId}").
     * 
     * @param channelName Name of the channel (e.g., "event-{eventId}")
     * @return true if subscription was successful, false otherwise
     */
    fun subscribeToPublicChannel(channelName: String): Boolean {
        return try {
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
                publicChannel = pusherInstance.subscribe(channelName)
                Log.d("PusherService", "✅ Subscribed to public channel: $channelName")
                true
            } ?: run {
                Log.e("PusherService", "Pusher not initialized. Call initialize() first.")
                false
            }
        } catch (e: Exception) {
            Log.e("PusherService", "Failed to subscribe to channel: $channelName", e)
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
    fun subscribeToPrivateChannel(channelName: String): Boolean {
        return try {
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
                // IMPORTANT: Use subscribePrivate for private channels
                privateChannel = pusherInstance.subscribePrivate(channelName)
                
                // Set up subscription event listeners
                privateChannel?.bind("pusher:subscription_succeeded") { event ->
                    Log.d("PusherService", "✅ Private channel subscription succeeded: $channelName")
                    Log.d("PusherService", "   - Event data: ${event.data}")
                }
                
                privateChannel?.bind("pusher:subscription_error") { event ->
                    Log.e("PusherService", "❌ Private channel subscription error: $channelName")
                    Log.e("PusherService", "   - Event data: ${event.data}")
                    Log.e("PusherService", "   - Event name: ${event.eventName}")
                }
                
                // Log all events received from private channel for debugging
                privateChannel?.bindGlobal { event ->
                    Log.d("PusherService", "📨 Event received on private channel $channelName:")
                    Log.d("PusherService", "   - Event name: ${event.eventName}")
                    Log.d("PusherService", "   - Event data: ${event.data}")
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
     * Unsubscribes from all channels and disconnects.
     */
    fun disconnect() {
        pusher?.let { pusherInstance ->
            publicChannel?.let { channel ->
                try {
                    pusherInstance.unsubscribe(channel.name)
                } catch (e: Exception) {
                    Log.w("PusherService", "Error unsubscribing from public channel", e)
                }
            }
            privateChannel?.let { channel ->
                try {
                    pusherInstance.unsubscribe(channel.name)
                } catch (e: Exception) {
                    Log.w("PusherService", "Error unsubscribing from private channel", e)
                }
            }
        }
        publicChannel = null
        privateChannel = null
        pusher?.disconnect()
        pusher = null
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
            finalChannel.unbind("tip-animation", subscription)
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
                    Log.d("PusherService", "🔔 Received event-amount-updated: ${event.data}")
                    val amountUpdated = json.decodeFromString<AmountUpdatedEvent>(event.data)
                    Log.d("PusherService", "✅ Parsed amount-updated: £${amountUpdated.amountRaised}, ${amountUpdated.numOfTips} tips")
                    trySend(amountUpdated)
                } catch (e: Exception) {
                    Log.e("PusherService", "❌ Error parsing amount-updated event", e)
                    e.printStackTrace()
                }
            }
        }
        
        finalChannel.bind("event-amount-updated", subscription)
        Log.d("PusherService", "✅ Bound to event-amount-updated event - ready to receive events")
        
        awaitClose {
            finalChannel.unbind("event-amount-updated", subscription)
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
            finalChannel.unbind("screen-settings-updated", subscription)
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
                    Log.d("PusherService", "🔔 Received latest-tip-updated: ${event.data}")
                    val latestTipUpdated = json.decodeFromString<LatestTipUpdatedEvent>(event.data)
                    Log.d("PusherService", "✅ Parsed latest-tip-updated: £${latestTipUpdated.latestTipAmount}")
                    trySend(latestTipUpdated)
                } catch (e: Exception) {
                    Log.e("PusherService", "❌ Error parsing latest-tip-updated event", e)
                    e.printStackTrace()
                }
            }
        }
        
        finalChannel.bind("latest-tip-updated", subscription)
        Log.d("PusherService", "✅ Bound to latest-tip-updated event - ready to receive events")
        
        awaitClose {
            finalChannel.unbind("latest-tip-updated", subscription)
        }
    }
}

