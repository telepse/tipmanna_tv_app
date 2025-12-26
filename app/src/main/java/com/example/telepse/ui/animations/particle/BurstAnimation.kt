package com.example.telepse.ui.animations.particle

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.example.telepse.ui.animations.AnimatedParticle
import com.example.telepse.ui.animations.AnimationConfig
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Generates particles for the Burst animation.
 * 
 * Particles start at the center of the screen and scatter outward
 * in all directions, creating an explosion effect.
 * 
 * @param imageUrl URL of the image/emoji to animate
 * @param config Animation configuration
 * @param screenWidth Width of the screen in dp
 * @param screenHeight Height of the screen in dp
 * @return List of animated particles
 */
@Composable
internal fun generateBurstParticles(
    imageUrl: String,
    config: AnimationConfig,
    screenWidth: Dp,
    screenHeight: Dp
): List<AnimatedParticle> {
    val density = LocalDensity.current
    val widthPx = with(density) { screenWidth.toPx() }
    val heightPx = with(density) { screenHeight.toPx() }
    
    // Start from center
    val centerX = widthPx / 2f
    val centerY = heightPx / 2f
    
    return (0 until config.particleCount).map { index ->
        val random = Random(index)
        
        // Calculate angle for even distribution around circle
        val angle = (index.toFloat() / config.particleCount) * 2f * kotlin.math.PI.toFloat()
        
        // Add some randomness to the angle
        val angleVariation = (random.nextFloat() - 0.5f) * 0.3f
        val finalAngle = angle + angleVariation
        
        // Calculate distance - scatter outward
        val maxDistance = kotlin.math.sqrt(
            (widthPx * widthPx + heightPx * heightPx).toDouble()
        ).toFloat() / 2f
        val distance = maxDistance * (random.nextFloat() * 0.5f + 0.5f) // 50-100% of max distance
        
        val endX = centerX + cos(finalAngle.toDouble()).toFloat() * distance
        val endY = centerY + sin(finalAngle.toDouble()).toFloat() * distance
        
        AnimatedParticle(
            id = index,
            imageUrl = imageUrl,
            startPosition = Offset(centerX, centerY),
            endPosition = Offset(endX, endY),
            rotationStart = 0f,
            rotationEnd = random.nextFloat() * 720f - 360f, // Random rotation
            scaleStart = 0f,
            scaleEnd = 0f, // Fade out completely at the end
            scaleMid = random.nextFloat() * 0.4f + 0.6f, // Peak scale between 0.6 and 1.0 at midpoint
            delayMillis = random.nextInt(100) // Small delay for burst effect
        )
    }
}
