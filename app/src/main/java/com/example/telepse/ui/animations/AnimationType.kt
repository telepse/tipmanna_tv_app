package com.example.telepse.ui.animations

/**
 * Enumeration of available animation types for character/emoji animations.
 * 
 * Each animation type represents a distinct visual effect:
 * - [CONFETTI]: Splashes outward from bottom-left corner
 * - [POP_AND_SHOOT]: Shoots upward from bottom-left toward top-right
 * - [FALL]: Falls downward from various top positions
 * - [BURST]: Explodes from center, scattering in all directions
 */
enum class AnimationType {
    CONFETTI,
    POP_AND_SHOOT,
    FALL,
    BURST
}
