package com.example.telepse.ui.animations.particle

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.telepse.ui.animations.AnimationConfig
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Data class representing a screen shock icon position.
 */
internal data class ScreenShockIcon(
    val id: Int,
    val imageUrl: String,
    val leftPercent: Float, // 0.0-1.0 (0% to 100%)
    val topPercent: Float, // 0.0-1.0 (0% to 100%)
    val scale: Float = 0.8f,
    val rotation: Float = 0f
)

/**
 * Generates screen shock icons arranged in rows.
 * 
 * Replicates the web version's screen shock effect:
 * - Icons arranged in rows (3-4 icons per row)
 * - Row spacing: 15%
 * - Start Y: 80% (glowOrigin = "bottom") or 10% (glowOrigin = "top")
 * - Uniform scale: 0.8
 * - Uniform rotation: 0
 * - Icons avoid overlapping with previous row
 * 
 * @param imageUrl URL of the image/emoji to animate
 * @param config Animation configuration
 * @param screenWidth Width of the screen in dp
 * @param screenHeight Height of the screen in dp
 * @param glowOrigin "top" or "bottom" - where the glow originates
 * @return List of screen shock icons
 */
@Composable
internal fun generateScreenShockIcons(
    imageUrl: String,
    config: AnimationConfig,
    screenWidth: Dp,
    screenHeight: Dp,
    glowOrigin: String = "bottom"
): List<ScreenShockIcon> {
    val density = LocalDensity.current
    val widthPx = with(density) { screenWidth.toPx() }
    val heightPx = with(density) { screenHeight.toPx() }
    
    // Web version parameters
    val maxIcons = 40
    val minIconsPerRow = 3
    val maxIconsPerRow = 4
    val uniformScale = 0.8f
    val uniformRotation = 0f
    val rowSpacing = 15f // 15% spacing between rows
    val startYPercent = if (glowOrigin == "bottom") 80f else 10f
    
    val icons = mutableListOf<ScreenShockIcon>()
    val previousRowPositions = mutableListOf<List<Float>>()
    
    var placedIcons = 0
    var currentRow = 0
    
    while (placedIcons < maxIcons) {
        val iconsThisRow = minOf(
            (Random.nextInt(maxIconsPerRow - minIconsPerRow + 1) + minIconsPerRow),
            maxIcons - placedIcons
        )
        
        val currentRowPositions = mutableListOf<Float>()
        
        for (i in 0 until iconsThisRow) {
            var leftPercent: Float
            var attempts = 0
            
            // Try to position icon without overlapping with previous row
            do {
                leftPercent = (Random.nextFloat() * 90f + 5f) / 100f // 5% to 95%
                attempts++
            } while (
                attempts < 50 &&
                previousRowPositions.isNotEmpty() &&
                previousRowPositions.lastOrNull()?.any { 
                    kotlin.math.abs(it - leftPercent) < 0.15f // 15% minimum spacing
                } == true
            )
            
            currentRowPositions.add(leftPercent)
            
            val topPercent = (startYPercent - currentRow * rowSpacing) / 100f
            
            icons.add(
                ScreenShockIcon(
                    id = placedIcons,
                    imageUrl = imageUrl,
                    leftPercent = leftPercent,
                    topPercent = topPercent,
                    scale = uniformScale,
                    rotation = uniformRotation
                )
            )
            
            placedIcons++
        }
        
        previousRowPositions.add(currentRowPositions)
        currentRow++
    }
    
    return icons
}

/**
 * Composable that renders the screen shock animation with camera shake effect.
 * 
 * @param icons List of screen shock icons to display
 * @param screenWidth Width of the screen in dp
 * @param screenHeight Height of the screen in dp
 * @param intensity Shake intensity in pixels (default: 32)
 * @param durationMillis Animation duration in milliseconds (default: 6000)
 */
@Composable
internal fun ScreenShockAnimation(
    icons: List<ScreenShockIcon>,
    screenWidth: Dp,
    screenHeight: Dp,
    intensity: Float = 32f,
    durationMillis: Int = 6000
) {
    val density = LocalDensity.current
    val widthPx = with(density) { screenWidth.toPx() }
    val heightPx = with(density) { screenHeight.toPx() }
    
    // Use Animatable for time tracking
    val timeAnimatable = remember { Animatable(0f) }
    
    // Animate time from 0 to durationMillis
    LaunchedEffect(Unit) {
        timeAnimatable.animateTo(
            targetValue = durationMillis.toFloat(),
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = LinearEasing
            )
        )
    }
    
    val elapsedTime = timeAnimatable.value
    val progress = (elapsedTime / durationMillis.toFloat()).coerceIn(0f, 1f)
    
    // Opacity: fade out starting at 85% progress
    val opacity = if (progress > 0.85f) {
        1f - (progress - 0.85f) / 0.15f
    } else {
        1f
    }
    
    // Decay: 0.85 until 75% progress, then decreases
    val decay = if (progress < 0.75f) {
        0.85f
    } else {
        0.85f * (1f - (progress - 0.75f) / 0.25f)
    }
    
    // Shake calculation: sin/cos waves with decay
    // Web uses elapsed * 0.02 and elapsed * 0.018 (elapsed is in milliseconds)
    val shakeX = sin(elapsedTime * 0.02f) * intensity * decay
    val shakeY = cos(elapsedTime * 0.018f) * intensity * decay
    
    // Create a backdrop blur/shadow effect by adding a semi-transparent dark overlay
    // This mimics the web version's blur/shadow effect during shake
    // The overlay creates a "shadow that blurs the background" effect
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationX = shakeX
                translationY = shakeY
                alpha = opacity
                // Add shadow/blur effect using a semi-transparent overlay
                // The shake motion combined with the overlay creates the blur effect
            }
    ) {
        // Semi-transparent dark overlay to create shadow/blur effect
        // This creates the "blurred background" effect similar to the web version
        // The overlay intensity varies with the shake intensity to enhance the blur effect
        // Higher intensity during active shake creates more visible blur
        val baseAlpha = 0.2f
        val overlayAlpha = (baseAlpha * (1f + decay * 0.5f)).coerceIn(0.1f, 0.3f) // Darker when shaking more
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = overlayAlpha), // Dark overlay that creates shadow/blur effect
                            Color.Black.copy(alpha = overlayAlpha * 0.6f),
                            Color.Black.copy(alpha = overlayAlpha * 0.3f),
                            Color.Transparent
                        ),
                        center = Offset(widthPx / 2, heightPx / 2),
                        radius = widthPx.coerceAtLeast(heightPx) * 1.5f
                    )
                )
        )
        
        // Icons layer on top of the blur overlay
        icons.forEach { icon ->
            // Position each icon using absolute positioning
            // Each icon is positioned at (leftPercent * width, topPercent * height)
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        // Position using translation (relative to parent Box at 0,0)
                        translationX = widthPx * icon.leftPercent
                        translationY = heightPx * icon.topPercent
                        scaleX = icon.scale
                        scaleY = icon.scale
                        rotationZ = icon.rotation
                    }
                    .wrapContentSize() // Don't fill parent, just wrap content
            ) {
                val context = LocalContext.current
                val imagePainter = rememberAsyncImagePainter(
                    ImageRequest.Builder(context)
                        .data(icon.imageUrl)
                        .crossfade(true)
                        .build()
                )
                
                Image(
                    painter = imagePainter,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp), // Base size, scaled by icon.scale
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

