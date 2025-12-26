# Character Animation System

A professional, modular animation system for displaying character/emoji animations in Jetpack Compose.

## Overview

This animation system provides four distinct animation types:
- **CONFETTI**: Splashes outward from bottom-left corner
- **POP_AND_SHOOT**: Shoots upward from bottom-left toward top-right
- **FALL**: Falls downward from various top positions
- **BURST**: Explodes from center, scattering in all directions

## Architecture

The system is organized into modular components:

```
ui/animations/
├── AnimationType.kt          # Enum defining animation types
├── AnimationConfig.kt        # Configuration data class
├── AnimatedParticle.kt       # Core particle animation logic
├── CharacterAnimation.kt     # Main entry point composable
├── particle/
│   ├── ConfettiAnimation.kt      # Confetti particle generator
│   ├── PopAndShootAnimation.kt    # Pop and shoot particle generator
│   ├── FallAnimation.kt           # Fall particle generator
│   └── BurstAnimation.kt          # Burst particle generator
└── AnimationUsageExample.kt  # Usage examples
```

## Usage

### Basic Usage

```kotlin
import com.example.telepse.ui.animations.CharacterAnimation
import com.example.telepse.ui.animations.AnimationType

CharacterAnimation(
    animationType = AnimationType.CONFETTI,
    imageUrl = "https://example.com/emoji.png"
)
```

### With Custom Configuration

```kotlin
CharacterAnimation(
    animationType = AnimationType.BURST,
    imageUrl = characterUrl,
    config = AnimationConfig(
        durationMillis = 5000,
        particleCount = 40,
        particleSize = 48.dp
    )
)
```

### With Completion Callback

```kotlin
var showAnimation by remember { mutableStateOf(false) }

if (showAnimation) {
    CharacterAnimation(
        animationType = AnimationType.POP_AND_SHOOT,
        imageUrl = characterUrl,
        onAnimationComplete = {
            showAnimation = false
        }
    )
}
```

## Default Configuration

- **Duration**: 5000ms (5 seconds)
- **Particle Count**: 40
- **Particle Size**: 48dp
- **Start Delay**: 0ms

## Animation Details

### CONFETTI
- Origin: Bottom-left corner
- Pattern: Outward splash in all directions
- Rotation: Random rotation for each particle
- Scale: Random scale between 0.5 and 1.0

### POP_AND_SHOOT
- Origin: Bottom-left corner
- Pattern: Upward trajectory toward top-right
- Rotation: Starts at -45° with slight variation
- Scale: Random scale between 0.6 and 1.0

### FALL
- Origin: Various positions at top of screen
- Pattern: Falls downward with slight horizontal drift
- Rotation: Continuous rotation while falling
- Scale: Maintains scale (70-100% of original)

### BURST
- Origin: Center of screen
- Pattern: Scatters outward in all directions
- Rotation: Random rotation for each particle
- Scale: Random scale between 0.6 and 1.0

## Technical Implementation

- Uses Jetpack Compose animations (`Animatable`, `LaunchedEffect`)
- Image loading via Coil (`rememberAsyncImagePainter`)
- Particle system with 40 particles per animation
- Smooth animations with proper easing functions
- Modular design for easy extension

## Performance Considerations

- Particles are generated once and cached
- Images are loaded asynchronously with Coil
- Animations use efficient Compose animation APIs
- Each particle is independently animated
