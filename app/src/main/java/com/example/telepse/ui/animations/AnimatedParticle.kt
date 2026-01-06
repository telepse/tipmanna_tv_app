package com.example.telepse.ui.animations

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Data class representing a single animated particle.
 * 
 * @property id Unique identifier for the particle
 * @property imageUrl URL of the image/emoji to display
 * @property startPosition Initial position of the particle
 * @property endPosition Final position of the particle
 * @property rotationStart Initial rotation angle in degrees
 * @property rotationEnd Final rotation angle in degrees
 * @property scaleStart Initial scale factor
 * @property scaleEnd Final scale factor (for fade out)
 * @property scaleMid Optional peak scale factor at midpoint (for burst/pop effects)
 * @property delayMillis Delay before this particle starts animating
 * @property particleSizeRatio Optional size ratio (0.0-1.0) for variable particle sizes
 */
internal data class AnimatedParticle(
    val id: Int,
    val imageUrl: String,
    val startPosition: Offset,
    val endPosition: Offset,
    val rotationStart: Float = 0f,
    val rotationEnd: Float = 360f,
    val scaleStart: Float = 0f,
    val scaleEnd: Float = 1f,
    val scaleMid: Float? = null, // Optional peak scale at midpoint
    val delayMillis: Int = 0,
    val particleSizeRatio: Float? = null // Optional: 0.0-1.0 for variable sizing (e.g., 40-90dp)
)

/**
 * Composable that renders a single animated particle.
 * 
 * Uses Compose animations to smoothly transition the particle
 * from start to end position with rotation and scale effects.
 * 
 * @param particle The particle data to animate
 * @param size Base size of the particle (used if particleSizeRatio is null)
 * @param durationMillis Total animation duration
 * @param minSize Optional minimum size for variable sizing
 * @param maxSize Optional maximum size for variable sizing
 */
@Composable
internal fun AnimatedParticle(
    particle: AnimatedParticle,
    size: Dp,
    durationMillis: Int,
    minSize: Dp? = null,
    maxSize: Dp? = null
) {
    val context = LocalContext.current
    
    // Use Animatable for precise control over animation
    val positionAnimatable = remember { Animatable(0f) }
    val rotationAnimatable = remember { Animatable(particle.rotationStart) }
    val scaleAnimatable = remember { Animatable(particle.scaleStart) }
    
    // Start animations when composable is first created
    LaunchedEffect(Unit) {
        launch {
            delay(particle.delayMillis.toLong())
            positionAnimatable.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = durationMillis,
                    easing = LinearEasing
                )
            )
        }
        
        launch {
            delay(particle.delayMillis.toLong())
            rotationAnimatable.animateTo(
                targetValue = particle.rotationEnd,
                animationSpec = tween(
                    durationMillis = durationMillis,
                    easing = LinearEasing
                )
            )
        }
        
        launch {
            delay(particle.delayMillis.toLong())
            if (particle.scaleMid != null) {
                // Animate to peak scale at midpoint, then fade out
                scaleAnimatable.animateTo(
                    targetValue = particle.scaleMid,
                    animationSpec = tween(
                        durationMillis = durationMillis / 2,
                        easing = FastOutSlowInEasing
                    )
                )
                // Then fade out to final scale
                scaleAnimatable.animateTo(
                    targetValue = particle.scaleEnd,
                    animationSpec = tween(
                        durationMillis = durationMillis / 2,
                        easing = LinearEasing
                    )
                )
            } else {
                // Simple scale animation
                scaleAnimatable.animateTo(
                    targetValue = particle.scaleEnd,
                    animationSpec = tween(
                        durationMillis = durationMillis / 2,
                        easing = FastOutSlowInEasing
                    )
                )
            }
        }
    }
    
    val position = positionAnimatable.value
    val rotation = rotationAnimatable.value
    val scale = scaleAnimatable.value
    
    // Calculate variable size if particleSizeRatio is provided
    val actualSize = if (particle.particleSizeRatio != null && minSize != null && maxSize != null) {
        val sizeRatio = particle.particleSizeRatio
        val sizeRange = maxSize - minSize
        minSize + (sizeRange * sizeRatio)
    } else {
        size
    }
    
    // Calculate current position
    val currentX = particle.startPosition.x + (particle.endPosition.x - particle.startPosition.x) * position
    val currentY = particle.startPosition.y + (particle.endPosition.y - particle.startPosition.y) * position
    
    // Load image using Coil
    val imagePainter = rememberAsyncImagePainter(
        ImageRequest.Builder(context)
            .data(particle.imageUrl)
            .crossfade(true)
            .error(android.R.drawable.ic_menu_gallery) // Fallback if image fails to load
            .build()
    )
    
    // Always render the Box - use alpha to control visibility
    // This ensures particles are in the composition tree and can animate properly
    Box(
        modifier = Modifier
            .graphicsLayer {
                translationX = currentX
                translationY = currentY
                rotationZ = rotation
                // Use a minimum scale to ensure particles are always slightly visible when animating
                // This prevents them from being completely invisible during the animation
                val effectiveScale = if (scale <= 0.01f && position > 0f) {
                    0.01f // Minimum visibility when animating
                } else {
                    scale.coerceAtLeast(0f)
                }
                scaleX = effectiveScale
                scaleY = effectiveScale
                alpha = effectiveScale.coerceIn(0f, 1f) // Ensure alpha matches scale for complete fade
            }
            .size(actualSize)
    ) {
        Image(
            painter = imagePainter,
            contentDescription = null,
            modifier = Modifier.size(actualSize),
            contentScale = ContentScale.Fit
        )
    }
}
