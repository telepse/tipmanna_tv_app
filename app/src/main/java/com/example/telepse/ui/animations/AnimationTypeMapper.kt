package com.example.telepse.ui.animations

/**
 * Utility class for mapping style names from the backend to AnimationType enum.
 * 
 * This mapper handles various style name formats and ensures proper
 * conversion to the corresponding animation type.
 * 
 * Usage:
 * ```
 * val animationType = AnimationTypeMapper.mapStyleName("Confetti")
 * ```
 */
object AnimationTypeMapper {
    
    /**
     * Maps a style name string to the corresponding AnimationType.
     * 
     * Handles case-insensitive matching and various format variations:
     * - "Confetti" -> AnimationType.CONFETTI
     * - "Pop and Shoot" or "Shoot and Pop" -> AnimationType.POP_AND_SHOOT
     * - "Fall" -> AnimationType.FALL
     * - "Burst" -> AnimationType.BURST
     * 
     * @param styleName The style name from the backend (e.g., "Confetti", "Pop and Shoot")
     * @return The corresponding AnimationType, defaults to CONFETTI if not recognized
     */
    fun mapStyleName(styleName: String?): AnimationType {
        if (styleName == null) return AnimationType.CONFETTI
        
        return when (styleName.trim().lowercase()) {
            "confetti" -> AnimationType.CONFETTI
            "pop and shoot", "shoot and pop", "popandshoot", "shootandpop" -> AnimationType.POP_AND_SHOOT
            "fall" -> AnimationType.FALL
            "burst" -> AnimationType.BURST
            else -> {
                // Default to confetti for unknown styles
                AnimationType.CONFETTI
            }
        }
    }
}

