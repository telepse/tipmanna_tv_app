package com.example.telepse.ui.network

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.telepse.data.network.NetworkMonitor
import com.example.telepse.ui.screen.NetworkMonitorEntryPoint
import dagger.hilt.android.EntryPointAccessors
import androidx.compose.runtime.collectAsState

/**
 * Composable that displays a network status indicator at the top of the screen.
 * Shows a banner when offline.
 * 
 * @param networkMonitor The network monitor instance
 * @param modifier Modifier for the container
 */
@Composable
fun NetworkStatusIndicator(
    networkMonitor: NetworkMonitor,
    modifier: Modifier = Modifier
) {
    val isOnline by networkMonitor.isOnline.collectAsState(initial = networkMonitor.isCurrentlyOnline())
    
    if (!isOnline) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(Color(0xFFFF4444)) // Red background
                .zIndex(1000f), // Ensure it's on top
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.WifiOff,
                    contentDescription = "Offline",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                androidx.tv.material3.Text(
                    text = "No Internet Connection",
                    color = Color.White,
                    style = androidx.tv.material3.MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/**
 * Composable hook to get NetworkMonitor instance from Hilt.
 */
@Composable
fun rememberNetworkMonitor(): NetworkMonitor {
    val context = LocalContext.current
    return remember {
        val entryPoint = EntryPointAccessors.fromActivity(
            context as Activity,
            NetworkMonitorEntryPoint::class.java
        )
        entryPoint.networkMonitor()
    }
}

/**
 * Composable hook to get current network status.
 * 
 * @param networkMonitor The network monitor instance
 * @return true if online, false if offline
 */
@Composable
fun rememberNetworkStatus(networkMonitor: NetworkMonitor): Boolean {
    val isOnline by networkMonitor.isOnline.collectAsState(initial = networkMonitor.isCurrentlyOnline())
    return isOnline
}

