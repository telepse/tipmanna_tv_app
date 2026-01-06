package com.example.telepse.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Network connectivity status
 */
enum class NetworkStatus {
    AVAILABLE,
    UNAVAILABLE,
    LOSING,
    LOST
}

/**
 * Service that monitors network connectivity and provides real-time updates.
 * 
 * Uses ConnectivityManager to detect network state changes and provides
 * a Flow that emits network status updates.
 */
@Singleton
class NetworkMonitor @Inject constructor(
    private val context: Context
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    /**
     * Flow that emits the current network connectivity status.
     * Emits true when network is available, false when unavailable.
     */
    val isOnline: Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d("NetworkMonitor", "✅ Network available")
                trySend(true)
            }
            
            override fun onLost(network: Network) {
                Log.d("NetworkMonitor", "❌ Network lost")
                trySend(false)
            }
            
            override fun onUnavailable() {
                Log.d("NetworkMonitor", "❌ Network unavailable")
                trySend(false)
            }
            
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val hasInternet = networkCapabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_INTERNET
                ) && networkCapabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_VALIDATED
                )
                Log.d("NetworkMonitor", "Network capabilities changed - hasInternet: $hasInternet")
                trySend(hasInternet)
            }
        }
        
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()
        
        // Register callback
        connectivityManager.registerNetworkCallback(networkRequest, callback)
        
        // Check initial state
        val initialStatus = checkNetworkStatus()
        trySend(initialStatus)
        
        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()
    
    /**
     * Flow that emits detailed network status.
     */
    val networkStatus: Flow<NetworkStatus> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d("NetworkMonitor", "Network status: AVAILABLE")
                trySend(NetworkStatus.AVAILABLE)
            }
            
            override fun onLost(network: Network) {
                Log.d("NetworkMonitor", "Network status: LOST")
                trySend(NetworkStatus.LOST)
            }
            
            override fun onUnavailable() {
                Log.d("NetworkMonitor", "Network status: UNAVAILABLE")
                trySend(NetworkStatus.UNAVAILABLE)
            }
            
            override fun onLosing(network: Network, maxMsToLive: Int) {
                Log.d("NetworkMonitor", "Network status: LOSING (maxMsToLive: $maxMsToLive)")
                trySend(NetworkStatus.LOSING)
            }
        }
        
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()
        
        connectivityManager.registerNetworkCallback(networkRequest, callback)
        
        // Check initial state
        val initialStatus = getCurrentNetworkStatus()
        trySend(initialStatus)
        
        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()
    
    /**
     * Checks the current network status synchronously.
     * @return true if network is available and validated, false otherwise
     */
    fun checkNetworkStatus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.isConnected == true
        }
    }
    
    /**
     * Gets the current detailed network status.
     */
    private fun getCurrentNetworkStatus(): NetworkStatus {
        return if (checkNetworkStatus()) {
            NetworkStatus.AVAILABLE
        } else {
            NetworkStatus.UNAVAILABLE
        }
    }
    
    /**
     * Checks if device is currently online (synchronous check).
     * Use this for one-time checks, use the Flow for continuous monitoring.
     */
    fun isCurrentlyOnline(): Boolean {
        return checkNetworkStatus()
    }
}

