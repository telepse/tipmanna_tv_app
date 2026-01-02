package com.example.telepse.ui.background

import com.example.telepse.R

/**
 * Background configuration for event backgrounds.
 * 
 * To add a new background:
 * 1. Add the video file to app/src/main/res/raw/ (e.g., new_background.mp4)
 * 2. Add a new entry to the BACKGROUND_CONFIG list below with:
 *    - id: The identifier that matches the backend's backgroundType or eventBackground
 *    - backgroundAsset: The R.raw resource ID
 * 
 * Example:
 * BackgroundEntry(
 *     id = "new_background",
 *     backgroundAsset = R.raw.new_background
 * )
 */
data class BackgroundEntry(
    val id: String,
    val backgroundAsset: Int
)

/**
 * Centralized background configuration.
 * Add new backgrounds here when backend adds new background types.
 */
val BACKGROUND_CONFIG = listOf(
    BackgroundEntry(
        id = "default",
        backgroundAsset = R.raw.fallback_background
    ),
    BackgroundEntry(
        id = "gaming",
        backgroundAsset = R.raw.gaming_background
    ),
    BackgroundEntry(
        id = "islamic",
        backgroundAsset = R.raw.religious_background
    )
    // Add more backgrounds here as needed
    // Example:
    // BackgroundEntry(
    //     id = "sports",
    //     backgroundAsset = R.raw.sports_background
    // )
)

/**
 * Gets the background resource ID based on the background identifier.
 * 
 * @param backgroundId The background identifier from the backend (from backgroundType or eventBackground).
 *                     Can be "default", "gaming", "religious", or extracted from eventBackground path like "/default.png"
 * @return The R.raw resource ID for the background, or null if not found
 */
fun getBackgroundResource(backgroundId: String?): Int? {
    if (backgroundId == null) return null
    
    // Extract the identifier from eventBackground path if it's a path (e.g., "/default.png" -> "default")
    val normalizedId = backgroundId
        .removePrefix("/")
        .removeSuffix(".png")
        .removeSuffix(".mp4")
        .lowercase()
    
    return BACKGROUND_CONFIG
        .firstOrNull { it.id.lowercase() == normalizedId }
        ?.backgroundAsset
}

/**
 * Gets the background resource ID from an event's background information.
 * 
 * This function checks both backgroundType and eventBackground fields,
 * with backgroundType taking priority.
 * 
 * @param backgroundType The backgroundType field from the event (e.g., "default", "gaming")
 * @param eventBackground The eventBackground field from the event (e.g., "/default.png")
 * @return The R.raw resource ID for the background, or the default fallback if not found
 */
fun getEventBackgroundResource(
    backgroundType: String? = null,
    eventBackground: String? = null
): Int {
    // Try backgroundType first (higher priority)
    backgroundType?.let {
        getBackgroundResource(it)?.let { resourceId ->
            return resourceId
        }
    }
    
    // Fall back to eventBackground
    eventBackground?.let {
        getBackgroundResource(it)?.let { resourceId ->
            return resourceId
        }
    }
    
    // Default fallback if nothing matches
    return R.raw.fallback_background
}
