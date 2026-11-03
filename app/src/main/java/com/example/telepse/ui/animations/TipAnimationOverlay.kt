package com.example.telepse.ui.animations

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import com.example.telepse.ui.viewmodel.AnimationTrigger

@Composable
fun TipAnimationOverlay(
    animations: List<AnimationTrigger>,
    onAnimationComplete: (AnimationTrigger) -> Unit,
) {
    if (animations.isEmpty()) return

    val currentAnimation = animations.first()
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .zIndex(1000f),
    ) {
        key(currentAnimation.id) {
            CharacterAnimation(
                animationType = currentAnimation.animationType,
                imageUrl = currentAnimation.imageUrl,
                modifier = Modifier.fillMaxSize(),
                onAnimationComplete = { onAnimationComplete(currentAnimation) },
            )
        }
    }
}
