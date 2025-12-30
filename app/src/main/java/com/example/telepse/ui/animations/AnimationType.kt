package com.example.telepse.ui.animations

/**
 * Enumeration of available animation types for character/emoji animations.
 * 
 * Each animation type represents a distinct visual effect:
 * - [CONFETTI]: Splashes outward from bottom-left corner
 * - [POP_AND_SHOOT]: Shoots upward from bottom-left toward top-right
 * - [FALL]: Falls downward from various top positions
 * - [BURST]: Explodes from center, scattering in all directions
 * - [SCREEN_SHOCK]: Icons arranged in rows with camera shake effect
 * - [FLOAT]: Characters float across the middle of the screen in a W-shaped pattern
 */
enum class AnimationType {
    CONFETTI,
    POP_AND_SHOOT,
    FALL,
    BURST,
    SCREEN_SHOCK,
    FLOAT
}
