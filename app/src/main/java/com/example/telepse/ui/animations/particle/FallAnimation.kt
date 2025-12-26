package com.example.telepse.ui.animations.particle

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.example.telepse.ui.animations.AnimatedParticle
import com.example.telepse.ui.animations.AnimationConfig
import kotlin.random.Random

/**
 * Generates particles for the Fall animation.
 * 
 * Particles fall downward from various positions at the top of the screen,
 * creating a rain-like effect.
 * 
 * @param imageUrl URL of the image/emoji to animate
 * @param config Animation configuration
 * @param screenWidth Width of the screen in dp
 * @param screenHeight Height of the screen in dp
 * @return List of animated particles
 */
@Composable
internal fun generateFallParticles(
    imageUrl: String,
    config: AnimationConfig,
    screenWidth: Dp,
    screenHeight: Dp
): List<AnimatedParticle> {
    val density = LocalDensity.current
    val widthPx = with(density) { screenWidth.toPx() }
    val heightPx = with(density) { screenHeight.toPx() }
    
    return (0 until config.particleCount).map { index ->
        val random = Random(index)
        
        // Start from various positions at the top
        val startX = random.nextFloat() * widthPx
        val startY = -50f // Start slightly above screen
        
        // End at various positions at the bottom
        val endX = startX + (random.nextFloat() * widthPx * 0.3f - widthPx * 0.15f) // Slight horizontal drift
        val endY = heightPx + 50f // End slightly below screen
        
        val initialRotation = random.nextFloat() * 360f
        val peakScale = random.nextFloat() * 0.3f + 0.7f // Peak scale between 70-100%
        
        AnimatedParticle(
            id = index,
            imageUrl = imageUrl,
            startPosition = Offset(startX, startY),
            endPosition = Offset(endX, endY),
            rotationStart = initialRotation,
            rotationEnd = initialRotation + random.nextFloat() * 180f, // Continue rotating while falling
            scaleStart = 0f,
            scaleEnd = 0f, // Fade out completely at the end
            scaleMid = peakScale, // Peak scale at midpoint, then fade out
            delayMillis = random.nextInt(500) // Stagger the start for natural effect
        )
    }
}
