package com.example.telepse.ui.animations

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
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
    
    // Screen Shock uses a different rendering approach
    if (animationType == AnimationType.SCREEN_SHOCK) {
        val icons = generateScreenShockIcons(
            imageUrl = imageUrl,
            config = config,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            glowOrigin = "bottom" // Default to bottom, can be made configurable
        )
        
        // Trigger completion callback after animation duration (6000ms for Screen Shock)
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(6000L + 300L) // 6000ms + 300ms buffer
            onAnimationComplete?.invoke()
        }
        
        Box(modifier = modifier.fillMaxSize()) {
            ScreenShockAnimation(
                icons = icons,
                screenWidth = screenWidth,
                screenHeight = screenHeight,
                intensity = 32f, // Default intensity
                durationMillis = 6000 // Web version uses 6000ms
            )
        }
        return
    }
    
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
        AnimationType.FLOAT -> generateFloatParticles(
            imageUrl = imageUrl,
            config = config,
            screenWidth = screenWidth,
            screenHeight = screenHeight
        )
        AnimationType.SCREEN_SHOCK -> emptyList() // Handled above
    }
    
    // Trigger completion callback after animation duration
    // Add extra time to ensure particles have fully faded out and are removed from composition
    LaunchedEffect(Unit) {
        // For Fall animation, use 6000ms duration (web version)
        val animationDuration = when (animationType) {
            AnimationType.FALL -> 6000
            else -> config.durationMillis
        }
        val totalDuration = animationDuration + config.startDelayMillis + 300 // Extra 300ms buffer
        kotlinx.coroutines.delay(totalDuration.toLong())
        onAnimationComplete?.invoke()
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        particles.forEach { particle ->
            // For Fall animation, use variable sizes (40-90dp) and 6000ms duration
            val particleSize: Dp
            val duration: Int
            val minSize: Dp?
            val maxSize: Dp?
            
            when (animationType) {
                AnimationType.FALL -> {
                    minSize = 40.dp
                    maxSize = 90.dp
                    particleSize = config.particleSize
                    duration = 6000 // Web version uses 6000ms
                }
                else -> {
                    minSize = null
                    maxSize = null
                    particleSize = config.particleSize
                    duration = config.durationMillis
                }
            }
            
            AnimatedParticle(
                particle = particle,
                size = particleSize,
                durationMillis = duration,
                minSize = minSize,
                maxSize = maxSize
            )
        }
    }
}
