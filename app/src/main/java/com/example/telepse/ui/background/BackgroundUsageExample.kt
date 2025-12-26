package com.example.telepse.ui.background

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.telepse.data.model.Event
import android.net.Uri

/**
 * Example usage of the background configuration system.
 * 
 * This file demonstrates how to use getEventBackgroundResource() to get
 * the appropriate background resource based on an event's background information.
 */

/**
 * Example: Get background resource from an Event object
 */
@Composable
fun ExampleGetBackgroundFromEvent(event: Event) {
    val context = LocalContext.current
    
    // Get the background resource ID using the utility function
    val backgroundResourceId = getEventBackgroundResource(
        backgroundType = event.backgroundType,
        eventBackground = event.eventBackground
    )
    
    // Convert to URI for video player
    val backgroundUri = Uri.parse("android.resource://${context.packageName}/${backgroundResourceId}")
    
    // Now you can use backgroundUri with your video player
    // Example with ExoPlayer or other video player:
    // VideoPlayer(uri = backgroundUri)
}

/**
 * Example: Get background resource directly from backgroundType string
 */
fun ExampleGetBackgroundFromType(backgroundType: String?): Int {
    return getEventBackgroundResource(backgroundType = backgroundType)
}

/**
 * Example: Get background resource from eventBackground path
 */
fun ExampleGetBackgroundFromPath(eventBackground: String?): Int {
    return getEventBackgroundResource(eventBackground = eventBackground)
}

/**
 * Example: Manual lookup by ID
 */
fun ExampleManualLookup(backgroundId: String): Int? {
    return getBackgroundResource(backgroundId)
}
