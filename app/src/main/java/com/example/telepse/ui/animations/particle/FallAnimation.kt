package com.example.telepse.ui.animations.particle

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.telepse.ui.animations.AnimatedParticle
import com.example.telepse.ui.animations.AnimationConfig
import kotlin.random.Random

/**
 * Generates particles for the Fall animation.
 * 
 * Replicates the web version's fall effect exactly:
 * - Icons fall from top with random horizontal drift
 * - Random sizes between 40-90dp
 * - Continuous rotation while falling
 * - Staggered creation (200-600ms delays)
 * - Opacity 0.95 (no fade out)
 * - Duration 6000ms
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
    
    // Web version parameters
    val baseSpeed = 1.6f // Base falling speed (pixels per frame at 60fps)
    val driftIntensity = 1.4f // Side-to-side drifting intensity
    val durationMs = 6000 // Duration in milliseconds (web version)
    val fps = 60f // Web uses requestAnimationFrame (~60fps)
    val frameTime = 1000f / fps // ~16.67ms per frame
    
    return (0 until config.particleCount).map { index ->
        val random = Random(index)
        
        // Random size between minSize and maxSize (matching web: 40-90px)
        val sizeRatio = random.nextFloat() // 0.0 to 1.0
        
        // Random start X position across container width
        val startX = random.nextFloat() * widthPx
        
        // Start from top (-100px equivalent)
        val startY: Float = with(density) { 
            val startDp: Dp = (-100f).dp
            startDp.toPx()
        }
        
        // Random drift direction (left or right)
        val driftDir = if (random.nextFloat() < 0.5f) -1f else 1f
        
        // Calculate drift speed: drift * (0.3 + random * 0.7)
        val driftSpeed = driftIntensity * (0.3f + random.nextFloat() * 0.7f)
        
        // Calculate falling speed: speed + random * 1.4
        val fallSpeed = baseSpeed + random.nextFloat() * 1.4f
        
        // Calculate total distance traveled
        // Web: y += fallSpeed per frame, so total distance = fallSpeed * frames
        val totalFrames = durationMs / frameTime
        val totalFallDistance = fallSpeed * totalFrames
        
        // Calculate total drift distance: driftSpeed * driftDir * frames
        val totalDriftX = driftSpeed * driftDir * totalFrames
        
        // End position: startX + drift, and startY + fall distance
        val endX: Float = startX + totalDriftX
        val endY: Float = startY + totalFallDistance
        
        // Ensure endY is at least below screen (height + 200px)
        val minEndY: Float = heightPx + with(density) {
            val offsetDp: Dp = 200f.dp
            offsetDp.toPx()
        }
        val finalEndY: Float = maxOf(endY, minEndY)
        
        // Initial rotation (random 0-360 degrees)
        val initialRotation = random.nextFloat() * 360f
        
        // Rotation speed: (random - 0.5) * 0.005 * 60 degrees per frame
        val rotateSpeed = (random.nextFloat() - 0.5f) * 0.005f * 60f
        val totalRotation = initialRotation + (rotateSpeed * totalFrames)
        
        // Staggered delay: 200-600ms between each icon
        val delayMillis = (200 + random.nextFloat() * 400).toInt()
        
        // Opacity = 0.95 (no fade out, maintain constant opacity)
        val opacity = 0.95f
        
        AnimatedParticle(
            id = index,
            imageUrl = imageUrl,
            startPosition = Offset(startX, startY),
            endPosition = Offset(endX, finalEndY),
            rotationStart = initialRotation,
            rotationEnd = totalRotation, // Continuous rotation while falling
            scaleStart = opacity, // Start at 0.95 opacity
            scaleEnd = opacity, // Maintain 0.95 opacity (no fade out)
            scaleMid = null, // No scale animation, just constant
            delayMillis = delayMillis, // Staggered creation
            particleSizeRatio = sizeRatio // Store size ratio for custom sizing
        )
    }
}
