package com.example.telepse.ui.animations.particle

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.example.telepse.ui.animations.AnimatedParticle
import com.example.telepse.ui.animations.AnimationConfig
import kotlin.random.Random

/**
 * Generates particles for the Confetti animation.
 * 
 * Confetti animation splashes outward from the bottom-left corner
 * in various directions, creating a celebratory effect.
 * 
 * @param imageUrl URL of the image/emoji to animate
 * @param config Animation configuration
 * @param screenWidth Width of the screen in dp
 * @param screenHeight Height of the screen in dp
 * @return List of animated particles
 */
@Composable
internal fun generateConfettiParticles(
    imageUrl: String,
    config: AnimationConfig,
    screenWidth: Dp,
    screenHeight: Dp
): List<AnimatedParticle> {
    val density = LocalDensity.current
    val widthPx = with(density) { screenWidth.toPx() }
    val heightPx = with(density) { screenHeight.toPx() }
    
    // Start from bottom-left corner
    val startX = 0f
    val startY = heightPx
    
    return (0 until config.particleCount).map { index ->
        val random = Random(index) // Use index as seed for reproducibility
        
        // Calculate end position - spread outward in various directions
        val angle = random.nextFloat() * 2f * kotlin.math.PI.toFloat() // Random angle
        val distance = random.nextFloat() * widthPx.coerceAtMost(heightPx) * 1.5f // Random distance
        
        val endX = startX + kotlin.math.cos(angle.toDouble()).toFloat() * distance
        val endY = startY + kotlin.math.sin(angle.toDouble()).toFloat() * distance
        
        AnimatedParticle(
            id = index,
            imageUrl = imageUrl,
            startPosition = Offset(startX, startY),
            endPosition = Offset(endX, endY),
            rotationStart = 0f,
            rotationEnd = random.nextFloat() * 720f - 360f, // Random rotation
            scaleStart = 0f,
            scaleEnd = 0f, // Fade out completely
            scaleMid = random.nextFloat() * 0.5f + 0.5f, // Peak scale between 0.5 and 1.0 at midpoint
            delayMillis = random.nextInt(200) // Stagger the start
        )
    }
}
