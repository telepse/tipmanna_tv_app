package com.example.telepse.ui.animations.particle

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.example.telepse.ui.animations.AnimatedParticle
import com.example.telepse.ui.animations.AnimationConfig
import kotlin.random.Random

/**
 * Generates particles for the Pop and Shoot animation.
 * 
 * Particles shoot upward from the bottom-left corner toward the top-right,
 * creating an upward trajectory effect.
 * 
 * @param imageUrl URL of the image/emoji to animate
 * @param config Animation configuration
 * @param screenWidth Width of the screen in dp
 * @param screenHeight Height of the screen in dp
 * @return List of animated particles
 */
@Composable
internal fun generatePopAndShootParticles(
    imageUrl: String,
    config: AnimationConfig,
    screenWidth: Dp,
    screenHeight: Dp
): List<AnimatedParticle> {
    val density = LocalDensity.current
    val widthPx = with(density) { screenWidth.toPx() }
    val heightPx = with(density) { screenHeight.toPx() }
    
    // Start from center of screen
    val centerX = widthPx / 2f
    val centerY = heightPx / 2f
    
    return (0 until config.particleCount).map { index ->
        val random = Random(index)
        
        // Move up randomly - random X position at top, but all moving upward
        val endX = random.nextFloat() * widthPx // Random X position across screen width
        val endY = random.nextFloat() * heightPx * 0.3f // Random position in top 30% of screen
        
        // Add some horizontal variation to the start position (small spread from center)
        val startXVariation = (random.nextFloat() - 0.5f) * widthPx * 0.2f // ±10% of screen width
        val startX = centerX + startXVariation
        val startYVariation = (random.nextFloat() - 0.5f) * heightPx * 0.1f // ±5% of screen height
        val startY = centerY + startYVariation
        
        AnimatedParticle(
            id = index,
            imageUrl = imageUrl,
            startPosition = Offset(startX, startY),
            endPosition = Offset(endX, endY),
            rotationStart = 0f,
            rotationEnd = random.nextFloat() * 360f, // Random rotation
            scaleStart = 0f,
            scaleEnd = 0f, // Fade out at the end
            scaleMid = random.nextFloat() * 0.4f + 0.6f, // Peak scale between 0.6 and 1.0 at midpoint
            delayMillis = random.nextInt(200) // Small stagger for pop effect
        )
    }
}
