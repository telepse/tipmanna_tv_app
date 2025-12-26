package com.example.telepse.ui.animations

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.example.telepse.ui.animations.particle.*

/**
 * Main composable for character/emoji animations.
 * 
 * This is the primary entry point for all animation types.
 * Simply call this function with the desired animation type and image URL.
 * 
 * Usage example:
 * ```
 * CharacterAnimation(
 *     animationType = AnimationType.CONFETTI,
 *     imageUrl = "https://example.com/emoji.png"
 * )
 * ```
 * 
 * @param animationType The type of animation to display
 * @param imageUrl URL of the image/emoji to animate
 * @param config Optional animation configuration (uses default if not provided)
 * @param modifier Modifier for the animation container
 * @param onAnimationComplete Optional callback when animation completes
 */
@Composable
fun CharacterAnimation(
    animationType: AnimationType,
    imageUrl: String,
    config: AnimationConfig = AnimationConfig.DEFAULT,
    modifier: Modifier = Modifier,
    onAnimationComplete: (() -> Unit)? = null
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    
    // Generate particles based on animation type
    val particles = when (animationType) {
        AnimationType.CONFETTI -> generateConfettiParticles(
            imageUrl = imageUrl,
            config = config,
            screenWidth = screenWidth,
            screenHeight = screenHeight
        )
        AnimationType.POP_AND_SHOOT -> generatePopAndShootParticles(
            imageUrl = imageUrl,
            config = config,
            screenWidth = screenWidth,
            screenHeight = screenHeight
        )
        AnimationType.FALL -> generateFallParticles(
            imageUrl = imageUrl,
            config = config,
            screenWidth = screenWidth,
            screenHeight = screenHeight
        )
        AnimationType.BURST -> generateBurstParticles(
            imageUrl = imageUrl,
            config = config,
            screenWidth = screenWidth,
            screenHeight = screenHeight
        )
    }
    
    // Trigger completion callback after animation duration
    // Add extra time to ensure particles have fully faded out and are removed from composition
    LaunchedEffect(Unit) {
        // Wait for full animation duration + fade out time
        val totalDuration = config.durationMillis + config.startDelayMillis + 300 // Extra 300ms buffer
        kotlinx.coroutines.delay(totalDuration.toLong())
        onAnimationComplete?.invoke()
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        particles.forEach { particle ->
            AnimatedParticle(
                particle = particle,
                size = config.particleSize,
                durationMillis = config.durationMillis
            )
        }
    }
}
