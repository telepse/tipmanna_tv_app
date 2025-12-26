package com.example.telepse.data.realtime

import com.example.telepse.data.model.AmountUpdatedEvent
import com.example.telepse.data.model.ScreenSettingsUpdatedEvent
import com.example.telepse.data.model.TipAnimationEvent
import com.example.telepse.data.realtime.PusherConfig
import com.google.gson.Gson
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
    private val gson = Gson()
    
    /**
     * Initializes and connects to Pusher.
     * Should be called before subscribing to channels.
     * 
     * @return true if initialization was successful, false otherwise
     */
    fun initialize(): Boolean {
        if (pusher != null) return true
        
        return try {
            val options = PusherOptions()
            options.setCluster(PusherConfig.CLUSTER)
            
            pusher = Pusher(PusherConfig.API_KEY, options)
        pusher?.connect(object : ConnectionEventListener {
            override fun onConnectionStateChange(change: ConnectionStateChange) {
                // Log connection state changes for debugging
                Log.d("PusherService", "Connection state: ${change.currentState}")
            }
            
            override fun onError(message: String, code: String, e: Exception?) {
                // Log errors for debugging
                Log.e("PusherService", "Error: $message, Code: $code", e)
            }
        }, ConnectionState.ALL)
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
                // Unsubscribe from previous channel if exists
                currentChannel?.let { channel ->
                    // Note: Pusher Java client doesn't have unbind_all, 
                    // we'll handle cleanup when disconnecting
                }
                currentChannel = pusherInstance.subscribe(channelName)
                Log.d("PusherService", "Subscribed to channel: $channelName")
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
                    Log.d("PusherService", "Received tip-animation event: ${event.data}")
                    val tipAnimation = gson.fromJson(event.data, TipAnimationEvent::class.java)
                    Log.d("PusherService", "Parsed tip-animation: ${tipAnimation.selectedTips.size} tips")
                    trySend(tipAnimation)
                } catch (e: Exception) {
                    // Log error but don't crash
                    Log.e("PusherService", "Error parsing tip-animation event", e)
                    e.printStackTrace()
                }
            }
        }
        
        finalChannel.bind("tip-animation", subscription)
        Log.d("PusherService", "Bound to tip-animation event")
        
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
                    Log.d("PusherService", "Received event-amount-updated: ${event.data}")
                    val amountUpdated = gson.fromJson(event.data, AmountUpdatedEvent::class.java)
                    trySend(amountUpdated)
                } catch (e: Exception) {
                    Log.e("PusherService", "Error parsing amount-updated event", e)
                    e.printStackTrace()
                }
            }
        }
        
        finalChannel.bind("event-amount-updated", subscription)
        Log.d("PusherService", "Bound to event-amount-updated event")
        
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
                    Log.d("PusherService", "Received screen-settings-updated: ${event.data}")
                    val settingsUpdated = gson.fromJson(event.data, ScreenSettingsUpdatedEvent::class.java)
                    trySend(settingsUpdated)
                } catch (e: Exception) {
                    Log.e("PusherService", "Error parsing settings-updated event", e)
                    e.printStackTrace()
                }
            }
        }
        
        finalChannel.bind("screen-settings-updated", subscription)
        Log.d("PusherService", "Bound to screen-settings-updated event")
        
        awaitClose {
            finalChannel.unbind("screen-settings-updated", subscription)
        }
    }
}

