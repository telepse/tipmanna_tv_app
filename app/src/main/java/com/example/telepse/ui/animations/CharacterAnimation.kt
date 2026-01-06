package com.example.telepse.ui.animations

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.telepse.R
import com.example.telepse.ui.animations.particle.*
import com.example.telepse.ui.animations.luxurious.LuxuriousAnimation

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
    
    // Spiral Float uses a different rendering approach (matching web version)
    if (animationType == AnimationType.FLOAT) {
        // Validate imageUrl before proceeding
        if (imageUrl.isBlank()) {
            android.util.Log.w("CharacterAnimation", "FLOAT animation: imageUrl is blank, skipping")
            return
        }
        
        // Web version uses 20000ms duration, minimum 20000ms
        val floatDuration = maxOf(config.durationMillis, 20000)
        
        // Trigger completion callback after animation duration
        LaunchedEffect(Unit) {
            try {
                kotlinx.coroutines.delay(floatDuration.toLong() + 300L) // Duration + 300ms buffer
                onAnimationComplete?.invoke()
            } catch (e: Exception) {
                android.util.Log.e("CharacterAnimation", "Error in FLOAT animation completion", e)
            }
        }
        
        Box(modifier = modifier.fillMaxSize()) {
            SpiralFloatAnimation(
                icons = listOf(imageUrl), // Web version accepts array of icons
                screenWidth = screenWidth,
                screenHeight = screenHeight,
                durationMillis = floatDuration,
                laneTop = 35f, // Default: 35%
                laneHeight = 25f, // Default: 25%
                spiralRadius = 40f, // Default: 40
                rotations = 2.5f, // Default: 2.5
                scale = 1.0f, // Default: 1
                spacing = 16f // Default: 16vw
            )
        }
        return
    }
    
    // Luxurious uses local crown video from res/raw (not from Cloudinary)
    if (animationType == AnimationType.LUXURIOUS) {
        val context = LocalContext.current
        
        // Use local crown.webm resource instead of loading from Cloudinary
        val crownResourceId = R.raw.crown
        val crownVideoUri = Uri.parse("android.resource://${context.packageName}/$crownResourceId")
        
        android.util.Log.d("CharacterAnimation", "🎬 LUXURIOUS animation triggered")
        android.util.Log.d("CharacterAnimation", "   - Crown resource ID: $crownResourceId")
        android.util.Log.d("CharacterAnimation", "   - Crown video URI: $crownVideoUri")
        
        // Default values matching JavaScript: count=1, stagger=0.2s, duration=8s
        val count = 1
        val stagger = 0.2f
        val duration = 8f
        
        // Trigger completion callback after animation duration
        LaunchedEffect(Unit) {
            try {
                // Calculate total duration: stagger delay + duration
                val totalDuration = ((count - 1) * stagger + duration) * 1000
                kotlinx.coroutines.delay(totalDuration.toLong() + 300L) // Duration + 300ms buffer
                onAnimationComplete?.invoke()
            } catch (e: Exception) {
                android.util.Log.e("CharacterAnimation", "Error in LUXURIOUS animation completion", e)
                onAnimationComplete?.invoke() // Still call completion to avoid blocking
            }
        }
        
        Box(modifier = modifier.fillMaxSize()) {
            // Position container at bottom center, 60dp from bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 60.dp) // 60dp from bottom
            ) {
                LuxuriousAnimation(
                    videoUri = crownVideoUri, // Use local crown video resource
                    count = count,
                    stagger = stagger,
                    duration = duration
                )
            }
        }
        return
    }
    
    // Validate imageUrl for particle animations
    if (imageUrl.isBlank()) {
        android.util.Log.w("CharacterAnimation", "${animationType} animation: imageUrl is blank, skipping")
        // Still call completion to avoid blocking the queue
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(100)
            onAnimationComplete?.invoke()
        }
        return
    }
    
    android.util.Log.d("CharacterAnimation", "🎬 ${animationType} animation triggered")
    android.util.Log.d("CharacterAnimation", "   - Image URL: $imageUrl")
    android.util.Log.d("CharacterAnimation", "   - Screen size: ${screenWidth.value}dp x ${screenHeight.value}dp")
    
    // Generate particles based on animation type
    val particles = when (animationType) {
        AnimationType.CONFETTI -> {
            android.util.Log.d("CharacterAnimation", "   - Generating CONFETTI particles...")
            generateConfettiParticles(
                imageUrl = imageUrl,
                config = config,
                screenWidth = screenWidth,
                screenHeight = screenHeight
            )
        }
        AnimationType.POP_AND_SHOOT -> {
            android.util.Log.d("CharacterAnimation", "   - Generating POP_AND_SHOOT particles...")
            generatePopAndShootParticles(
                imageUrl = imageUrl,
                config = config,
                screenWidth = screenWidth,
                screenHeight = screenHeight
            )
        }
        AnimationType.FALL -> {
            android.util.Log.d("CharacterAnimation", "   - Generating FALL particles...")
            generateFallParticles(
                imageUrl = imageUrl,
                config = config,
                screenWidth = screenWidth,
                screenHeight = screenHeight
            )
        }
        AnimationType.BURST -> {
            android.util.Log.d("CharacterAnimation", "   - Generating BURST particles...")
            generateBurstParticles(
                imageUrl = imageUrl,
                config = config,
                screenWidth = screenWidth,
                screenHeight = screenHeight
            )
        }
        AnimationType.FLOAT -> {
            // Spiral Float uses a different rendering approach (like Screen Shock)
            emptyList() // Will be handled separately below
        }
        AnimationType.SCREEN_SHOCK -> emptyList() // Handled above
        AnimationType.LUXURIOUS -> emptyList() // Handled above
    }
    
    android.util.Log.d("CharacterAnimation", "   - Generated ${particles.size} particles")
    
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
    
    if (particles.isEmpty()) {
        android.util.Log.w("CharacterAnimation", "⚠️ No particles generated for ${animationType} animation")
        // Still call completion to avoid blocking the queue
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(100)
            onAnimationComplete?.invoke()
        }
        return
    }
    
    android.util.Log.d("CharacterAnimation", "   - Rendering ${particles.size} particles")
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(1500f) // Ensure particles are above content but below network indicator
    ) {
        particles.forEachIndexed { index, particle ->
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
            
            if (index < 5) { // Log first 5 particles for debugging
                android.util.Log.d("CharacterAnimation", "   - Particle $index: ${particle.imageUrl}, start: ${particle.startPosition}, end: ${particle.endPosition}")
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
