package com.example.telepse.data.realtime

import com.example.telepse.data.model.AmountUpdatedEvent
import com.example.telepse.data.model.LatestTipUpdatedEvent
import com.example.telepse.data.model.ScreenSettingsUpdatedEvent
import com.example.telepse.data.model.TipAnimationEvent
import com.example.telepse.data.realtime.PusherConfig
import com.pusher.client.Pusher
import com.pusher.client.PusherOptions
import com.pusher.client.channel.Channel
import com.pusher.client.channel.PusherEvent
import com.pusher.client.channel.SubscriptionEventListener
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
    private var currentChannel: Channel? = null
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
            
            pusher = Pusher(PusherConfig.API_KEY, options)
            pusher?.connect(object : ConnectionEventListener {
                override fun onConnectionStateChange(change: ConnectionStateChange) {
                    // Log connection state changes for debugging
                    Log.d("PusherService", "🔌 Connection state changed: ${change.previousState} -> ${change.currentState}")
                }
                
                override fun onError(message: String, code: String, e: Exception?) {
                    // Log errors for debugging
                    Log.e("PusherService", "❌ Pusher Error: $message, Code: $code", e)
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
     * Subscribes to an event channel.
     * 
     * @param channelName Name of the channel (e.g., "event-{eventId}")
     * @return true if subscription was successful, false otherwise
     */
    fun subscribeToChannel(channelName: String): Boolean {
        return try {
            pusher?.let { pusherInstance ->
                // Unsubscribe from previous channel if exists and it's a different channel
                currentChannel?.let { channel ->
                    if (channel.name != channelName) {
                        Log.d("PusherService", "🔄 Unsubscribing from previous channel: ${channel.name}")
                        try {
                            pusherInstance.unsubscribe(channel.name)
                        } catch (e: Exception) {
                            Log.w("PusherService", "Error unsubscribing from channel ${channel.name}", e)
                        }
                        currentChannel = null
                    }
                }
                currentChannel = pusherInstance.subscribe(channelName)
                Log.d("PusherService", "✅ Subscribed to channel: $channelName")
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
     * Unsubscribes from the current channel and disconnects.
     */
    fun disconnect() {
        currentChannel = null
        pusher?.disconnect()
        pusher = null
    }
    
    /**
     * Creates a Flow stream for tip-animation events.
     * 
     * @return Flow of TipAnimationEvent
     */
    fun observeTipAnimation(): Flow<TipAnimationEvent> = callbackFlow {
        // Wait for channel to be available (with timeout)
        var channel: Channel? = null
        var attempts = 0
        while (channel == null && attempts < 20) {
            channel = currentChannel
            if (channel == null) {
                kotlinx.coroutines.delay(100)
                attempts++
            }
        }
        
        val finalChannel = channel ?: run {
            Log.e("PusherService", "Channel not available after waiting")
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
     * 
     * @return Flow of AmountUpdatedEvent
     */
    fun observeAmountUpdated(): Flow<AmountUpdatedEvent> = callbackFlow {
        // Wait for channel to be available (with timeout)
        var channel: Channel? = null
        var attempts = 0
        while (channel == null && attempts < 20) {
            channel = currentChannel
            if (channel == null) {
                kotlinx.coroutines.delay(100)
                attempts++
            }
        }
        
        val finalChannel = channel ?: run {
            Log.e("PusherService", "Channel not available after waiting")
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
     * 
     * @return Flow of ScreenSettingsUpdatedEvent
     */
    fun observeScreenSettingsUpdated(): Flow<ScreenSettingsUpdatedEvent> = callbackFlow {
        // Wait for channel to be available (with timeout)
        var channel: Channel? = null
        var attempts = 0
        while (channel == null && attempts < 20) {
            channel = currentChannel
            if (channel == null) {
                kotlinx.coroutines.delay(100)
                attempts++
            }
        }
        
        val finalChannel = channel ?: run {
            Log.e("PusherService", "Channel not available after waiting")
            close()
            return@callbackFlow
        }
        
        val subscription = object : SubscriptionEventListener {
            override fun onEvent(event: PusherEvent) {
                try {
                    Log.d("PusherService", "🔔 Received screen-settings-updated: ${event.data}")
                    val settingsUpdated = json.decodeFromString<ScreenSettingsUpdatedEvent>(event.data)
                    Log.d("PusherService", "✅ Parsed settings-updated successfully")
                    trySend(settingsUpdated)
                } catch (e: Exception) {
                    Log.e("PusherService", "❌ Error parsing settings-updated event", e)
                    e.printStackTrace()
                }
            }
        }
        
        finalChannel.bind("screen-settings-updated", subscription)
        Log.d("PusherService", "✅ Bound to screen-settings-updated event - ready to receive events")
        
        awaitClose {
            finalChannel.unbind("screen-settings-updated", subscription)
        }
    }
    
    /**
     * Creates a Flow stream for latest-tip-updated events.
     * 
     * This event is triggered when a tip is made and updates the latest tip amount
     * displayed on the screen.
     * 
     * @return Flow of LatestTipUpdatedEvent
     */
    fun observeLatestTipUpdated(): Flow<LatestTipUpdatedEvent> = callbackFlow {
        // Wait for channel to be available (with timeout)
        var channel: Channel? = null
        var attempts = 0
        while (channel == null && attempts < 20) {
            channel = currentChannel
            if (channel == null) {
                kotlinx.coroutines.delay(100)
                attempts++
            }
        }
        
        val finalChannel = channel ?: run {
            Log.e("PusherService", "Channel not available after waiting")
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

