package com.example.telepse.ui.viewmodel

import com.example.telepse.ui.animations.AnimationType

/**
 * One queued character animation on top of the LIVE TV / venue player surface.
 */
data class AnimationTrigger(
    val animationType: AnimationType,
    val imageUrl: String,
    val id: String = java.util.UUID.randomUUID().toString(),
)
