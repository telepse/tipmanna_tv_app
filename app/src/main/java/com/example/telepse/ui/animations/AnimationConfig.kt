package com.example.telepse.ui.animations

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Configuration class for animation parameters.
 * 
 * Provides a centralized way to configure animation behavior,
 * making it easy to adjust timing, particle count, and other properties.
 * 
 * @property durationMillis Total duration of the animation in milliseconds
 * @property particleCount Number of particles/characters to animate
 * @property particleSize Size of each particle in dp
 * @property startDelayMillis Delay before animation starts in milliseconds
 */
data class AnimationConfig(
    val durationMillis: Int = 5000,
    val particleCount: Int = 40,
    val particleSize: Dp = 48.dp,
    val startDelayMillis: Int = 0
) {
    companion object {
        /**
         * Default configuration with standard values:
         * - 5 seconds duration
         * - 40 particles
         * - 48dp particle size
         */
        val DEFAULT = AnimationConfig()
    }
}
