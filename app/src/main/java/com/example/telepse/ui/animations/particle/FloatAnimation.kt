package com.example.telepse.ui.animations.particle

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.example.telepse.ui.animations.AnimatedParticle
import com.example.telepse.ui.animations.AnimationConfig
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Generates particles for the Float animation.
 * 
 * Characters float across the middle of the screen in a W-shaped pattern.
 * - Characters start from the left side
 * - Move horizontally across the screen
 * - Follow a W pattern (wave-like: up, down, up, down)
 * - Small spaces between characters
 * - Positioned at middle vertical position
 * 
 * @param imageUrl URL of the image/emoji to animate
 * @param config Animation configuration
 * @param screenWidth Width of the screen in dp
 * @param screenHeight Height of the screen in dp
 * @return List of animated particles
 */
@Composable
internal fun generateFloatParticles(
    imageUrl: String,
    config: AnimationConfig,
    screenWidth: Dp,
    screenHeight: Dp
): List<AnimatedParticle> {
    val density = LocalDensity.current
    val widthPx = with(density) { screenWidth.toPx() }
    val heightPx = with(density) { screenHeight.toPx() }
    
    // Middle of the screen vertically
    val middleY = heightPx / 2f
    
    // W pattern parameters
    val waveAmplitude = heightPx * 0.12f // 12% of screen height for wave amplitude
    val waveFrequency = 2.5f // Number of complete W cycles across the screen
    val spacing = widthPx / (config.particleCount + 1f) // Even spacing between particles
    
    return (0 until config.particleCount).map { index ->
        val random = Random(index) // Use index as seed for reproducibility
        
        // Calculate horizontal position with spacing
        // Space them out evenly with small gaps
        val progress = (index + 1) / (config.particleCount + 1f) // 0.0 to 1.0
        
        // Calculate W pattern offset
        // W pattern: sin wave that creates up-down-up-down movement
        // Use 2 * PI * frequency * progress to create multiple waves
        val wavePhase = 2f * PI.toFloat() * waveFrequency * progress
        val waveOffset = sin(wavePhase) * waveAmplitude
        
        // Start position: left side, middle Y with W offset
        val startX = -50f // Start slightly off-screen left
        val startY = middleY + waveOffset
        
        // End position: right side, middle Y with W offset
        // The W pattern continues across, so we calculate the end wave offset
        // Each particle follows the W pattern as it moves across
        val endX = widthPx + 50f // End slightly off-screen right
        
        // Calculate end Y based on the W pattern at the end position
        // The wave continues, so we need to calculate where the wave is at the end
        val endProgress = 1f // End at right side
        val endWavePhase = 2f * PI.toFloat() * waveFrequency * endProgress
        val endWaveOffset = sin(endWavePhase) * waveAmplitude
        val endY = middleY + endWaveOffset
        
        // Add slight random variation to Y for more natural look
        val randomYVariation = (random.nextFloat() - 0.5f) * waveAmplitude * 0.2f // ±10% variation
        
        // Slight rotation as they float (gentle rotation)
        val initialRotation = (random.nextFloat() - 0.5f) * 30f // ±15 degrees
        val endRotation = initialRotation + (random.nextFloat() - 0.5f) * 20f // Slight additional rotation
        
        // Staggered delay for smooth flow
        val delayMillis = (index * 100).coerceAtMost(2000) // Max 2 second stagger
        
        AnimatedParticle(
            id = index,
            imageUrl = imageUrl,
            startPosition = Offset(startX, startY + randomYVariation),
            endPosition = Offset(endX, endY + randomYVariation),
            rotationStart = initialRotation,
            rotationEnd = endRotation,
            scaleStart = 0.8f, // Start slightly smaller
            scaleEnd = 0.8f, // Maintain size (no fade)
            scaleMid = null,
            delayMillis = delayMillis
        )
    }
}

