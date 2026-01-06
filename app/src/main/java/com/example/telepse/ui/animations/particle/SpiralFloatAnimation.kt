package com.example.telepse.ui.animations.particle

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import kotlin.math.PI
import kotlin.math.sin

/**
 * Data class representing a spiral float icon.
 */
internal data class SpiralFloatIcon(
    val id: Int,
    val imageUrl: String,
    val phase: Float // Phase offset for spiral positioning
)

/**
 * Composable that renders the spiral float animation matching the web version exactly.
 * 
 * Web version parameters:
 * - 5 icons (ICON_COUNT = 5)
 * - Duration: 20000ms (default), minimum 20000ms
 * - Timing: 85% motion, 5% hold, 10% fade
 * - Lane: top = 35%, height = 25%
 * - Spiral: radius = 40, rotations = 2.5, scale = 1
 * - Spacing: 16vw (effectiveSpacingVw)
 * - VIEW_START = 120vw, VIEW_END = -20vw
 * 
 * @param icons List of icon image URLs
 * @param screenWidth Width of the screen in dp
 * @param screenHeight Height of the screen in dp
 * @param durationMillis Animation duration in milliseconds (default: 20000)
 * @param laneTop Top position of the lane in percentage (default: 35)
 * @param laneHeight Height of the lane in percentage (default: 25)
 * @param spiralRadius Spiral radius in percentage (default: 40)
 * @param rotations Number of spiral rotations (default: 2.5)
 * @param scale Scale factor for icons (default: 1.0)
 * @param spacing Spacing between icons in vw units (default: 16)
 */
@Composable
internal fun SpiralFloatAnimation(
    icons: List<String>,
    screenWidth: Dp,
    screenHeight: Dp,
    durationMillis: Int = 20000,
    laneTop: Float = 35f,
    laneHeight: Float = 25f,
    spiralRadius: Float = 40f,
    rotations: Float = 2.5f,
    scale: Float = 1.0f,
    spacing: Float = 16f
) {
    if (icons.isEmpty()) return
    
    val density = LocalDensity.current
    val widthPx = with(density) { screenWidth.toPx() }
    val heightPx = with(density) { screenHeight.toPx() }
    
    // Constants matching web version
    val ICON_COUNT = 5
    val SAFE_MIN_DURATION = 20000
    val effectiveDuration = maxOf(durationMillis, SAFE_MIN_DURATION)
    
    // Timing split: 85% motion, 5% hold, 10% fade
    val MOTION_RATIO = 0.85f
    val HOLD_RATIO = 0.05f
    val FADE_RATIO = 0.1f
    
    val motionDuration = effectiveDuration * MOTION_RATIO
    val holdDuration = effectiveDuration * HOLD_RATIO
    val fadeDuration = effectiveDuration * FADE_RATIO
    
    // View positions (in vw units, converted to pixels)
    val VIEW_START = 120f // 120vw
    val VIEW_END = -20f // -20vw
    // Prevent division by zero
    val vwToPx = if (widthPx > 0f) widthPx / 100f else 1f // 1vw = widthPx / 100
    
    // Lane positioning
    // Prevent division by zero and ensure valid values
    val laneTopPx = if (heightPx > 0f) heightPx * (laneTop / 100f).coerceIn(0f, 100f) else 0f
    val laneHeightPx = if (heightPx > 0f) heightPx * (laneHeight / 100f).coerceIn(0f, 100f) else 0f
    val laneCenterY = laneTopPx + (laneHeightPx / 2f)
    
    // Spacing (effectiveSpacingVw)
    // Check if mobile: width < 768dp converted to pixels
    val mobileThresholdPx = with(density) { 768.dp.toPx() }
    val isMobile = widthPx < mobileThresholdPx
    val effectiveSpacingVw = spacing * (if (isMobile) 0.6f else 1.0f)
    val effectiveSpacingPx = effectiveSpacingVw * vwToPx
    
    // Spiral radius in pixels (percentage of lane height)
    val spiralRadiusPx = laneHeightPx * (spiralRadius / 100f)
    
    // Use Animatable for time tracking
    val timeAnimatable = remember { Animatable(0f) }
    
    // Animate time from 0 to effectiveDuration
    LaunchedEffect(Unit) {
        timeAnimatable.animateTo(
            targetValue = effectiveDuration.toFloat(),
            animationSpec = tween(
                durationMillis = effectiveDuration,
                easing = LinearEasing
            )
        )
    }
    
    val elapsedTime = timeAnimatable.value
    
    // PHASE 1: MOTION (85% of duration)
    // Prevent division by zero
    val motionProgress = if (motionDuration > 0f) {
        (elapsedTime / motionDuration).coerceIn(0f, 1f)
    } else {
        0f
    }
    val thetaBase = motionProgress * PI.toFloat() * 2f * rotations
    
    // PHASE 3: FADE (last 10% of duration, after hold)
    val fadeProgress = if (fadeDuration > 0f && elapsedTime > motionDuration + holdDuration) {
        ((elapsedTime - motionDuration - holdDuration) / fadeDuration).coerceIn(0f, 1f)
    } else {
        0f
    }
    
    // Create icons (5 total)
    // Ensure we have at least one icon to avoid crashes
    val iconList = remember(icons) {
        if (icons.isEmpty()) {
            emptyList()
        } else {
            (0 until ICON_COUNT).map { i ->
                SpiralFloatIcon(
                    id = i,
                    imageUrl = icons[i % icons.size],
                    phase = (i * 2f * PI.toFloat()) / ICON_COUNT
                )
            }
        }
    }
    
    // Early return if no icons
    if (iconList.isEmpty()) return
    
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        iconList.forEach { icon ->
            // Calculate theta for this icon (thetaBase + phase)
            val theta = thetaBase + icon.phase
            
            // Spiral radius decreases during motion (from 100% to 80%)
            val currentRadius = spiralRadiusPx * (1f - motionProgress * 0.2f)
            
            // X position: VIEW_START + (VIEW_END - VIEW_START) * motionProgress + i * effectiveSpacingVw
            // Convert vw to pixels: 1vw = widthPx / 100
            val baseXVw = VIEW_START + (VIEW_END - VIEW_START) * motionProgress
            val xVw = baseXVw + icon.id * effectiveSpacingVw
            val xPx = (xVw * vwToPx).coerceIn(-widthPx * 2f, widthPx * 2f) // Clamp to reasonable range
            
            // Y position: Math.sin(theta) * radius + laneHeight * 0.5
            // Web version: y = Math.sin(theta) * radius + laneHeight * 0.5
            // This is relative to the lane, so we add it to laneCenterY
            val yOffset = sin(theta) * currentRadius
            val yPx = (laneCenterY + yOffset).coerceIn(0f, heightPx) // Clamp to screen bounds
            
            // Opacity: 1 - fadeProgress
            val opacity = 1f - fadeProgress
            
            // Render icon
            val context = LocalContext.current
            val imagePainter = rememberAsyncImagePainter(
                ImageRequest.Builder(context)
                    .data(icon.imageUrl)
                    .crossfade(true)
                    .build()
            )
            
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = xPx
                        translationY = yPx
                        scaleX = scale
                        scaleY = scale
                        alpha = opacity
                    }
                    .wrapContentSize()
            ) {
                Image(
                    painter = imagePainter,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp), // Base size, scaled by scale factor
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

