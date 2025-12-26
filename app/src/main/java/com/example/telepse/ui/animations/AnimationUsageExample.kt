package com.example.telepse.ui.animations

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

/**
 * Example usage of the CharacterAnimation system.
 * 
 * This file demonstrates how to use the animation system in your screens.
 * 
 * Example 1: Basic usage with default configuration
 * ```
 * CharacterAnimation(
 *     animationType = AnimationType.CONFETTI,
 *     imageUrl = "https://example.com/emoji.png"
 * )
 * ```
 * 
 * Example 2: Custom configuration
 * ```
 * CharacterAnimation(
 *     animationType = AnimationType.BURST,
 *     imageUrl = "https://example.com/emoji.png",
 *     config = AnimationConfig(
 *         durationMillis = 5000,
 *         particleCount = 40,
 *         particleSize = 48.dp
 *     )
 * )
 * ```
 * 
 * Example 3: With completion callback
 * ```
 * var showAnimation by remember { mutableStateOf(false) }
 * 
 * if (showAnimation) {
 *     CharacterAnimation(
 *         animationType = AnimationType.POP_AND_SHOOT,
 *         imageUrl = characterUrl,
 *         onAnimationComplete = {
 *             showAnimation = false
 *         }
 *     )
 * }
 * ```
 */

/**
 * Example composable showing how to trigger animations conditionally.
 */
@Composable
fun AnimationExampleScreen() {
    var showConfetti by remember { mutableStateOf(false) }
    var showBurst by remember { mutableStateOf(false) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Your screen content here
        
        // Trigger confetti animation
        if (showConfetti) {
            CharacterAnimation(
                animationType = AnimationType.CONFETTI,
                imageUrl = "https://res.cloudinary.com/tipmanna/image/upload/v1764696817/vdtu6skbr0exj7jsg3gu.png",
                onAnimationComplete = {
                    showConfetti = false
                }
            )
        }
        
        // Trigger burst animation
        if (showBurst) {
            CharacterAnimation(
                animationType = AnimationType.BURST,
                imageUrl = "https://res.cloudinary.com/tipmanna/image/upload/v1764696861/highxzdg2ih0hqayvh9d.png",
                onAnimationComplete = {
                    showBurst = false
                }
            )
        }
    }
}
