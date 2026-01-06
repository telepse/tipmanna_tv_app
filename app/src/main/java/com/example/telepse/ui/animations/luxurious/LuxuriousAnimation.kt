package com.example.telepse.ui.animations.luxurious

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import android.util.Log
import kotlinx.coroutines.delay

/**
 * Luxurious style animation that plays the crown video at the bottom center of the screen
 * with a stagger effect.
 * 
 * This matches the JavaScript luxuriousStyle function behavior:
 * - Video plays at bottom center (60px from bottom)
 * - Multiple videos can play simultaneously with stagger delay
 * - Each video is 350px x 350px
 * - Video loops while playing
 * - Video is removed after duration
 * 
 * Uses the local crown.webm resource from res/raw to avoid download time.
 * 
 * @param videoUri URI of the video to play (local resource)
 * @param count Number of videos to play simultaneously (default: 1)
 * @param stagger Delay between each video start in seconds (default: 0.2)
 * @param duration Duration each video plays in seconds (default: 8)
 * @param modifier Modifier for the container
 */
@Composable
fun LuxuriousAnimation(
    videoUri: Uri,
    count: Int = 1,
    stagger: Float = 0.2f,
    duration: Float = 8f,
    modifier: Modifier = Modifier
) {
    // Container: full width, wrap content height to allow video to show
    // In JS: position: absolute, bottom: 60px, left: 0, width: 100%, height: 0
    // The container is already positioned 60dp from bottom by parent
    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight() // Allow content to determine height
            .zIndex(999f), // Ensure it's above other content
        contentAlignment = Alignment.BottomCenter
    ) {
        // Create and manage video players
        // In JS: videos are positioned at bottom: 0, left: 50%, transform: translateX(-50%)
        repeat(count) { index ->
            val staggerDelay = index * stagger
            
            // Create video player for this video
            // Positioned at bottom center (bottom: 0, centered horizontally)
            LuxuriousVideoPlayer(
                videoUri = videoUri,
                staggerDelaySeconds = staggerDelay,
                durationSeconds = duration,
                modifier = Modifier
                    .size(350.dp) // 350px x 350px
                    .align(Alignment.BottomCenter) // Bottom center alignment
                    .zIndex(1000f) // Ensure video is on top
            )
        }
    }
}

/**
 * Individual video player for Luxurious animation.
 * 
 * @param videoUri URI of the video to play (local resource)
 * @param staggerDelaySeconds Delay before starting playback
 * @param durationSeconds How long to play before removing
 * @param modifier Modifier for the video player
 */
@Composable
private fun LuxuriousVideoPlayer(
    videoUri: Uri,
    staggerDelaySeconds: Float,
    durationSeconds: Float,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var shouldStartPlaying by remember { mutableStateOf(false) }
    var shouldRemove by remember { mutableStateOf(false) }
    var playerError by remember { mutableStateOf<String?>(null) }
    var playerReady by remember { mutableStateOf(false) }
    
    // Create ExoPlayer instance - make it nullable to handle errors gracefully
    val exoPlayer: ExoPlayer? = remember(videoUri) {
        try {
            Log.d("LuxuriousVideoPlayer", "Creating ExoPlayer for URI: $videoUri")
            ExoPlayer.Builder(context).build().apply {
                try {
                    val mediaItem = MediaItem.fromUri(videoUri)
                    setMediaItem(mediaItem)
                    repeatMode = Player.REPEAT_MODE_ONE // Loop the video
                    
                    // Add listener to track player state and errors
                    addListener(object : Player.Listener {
                        override fun onPlayerError(error: PlaybackException) {
                            playerError = error.message ?: "Unknown error"
                            Log.e("LuxuriousVideoPlayer", "Player error: ${error.message}", error)
                            Log.e("LuxuriousVideoPlayer", "Error cause: ${error.cause?.message}")
                        }
                        
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            Log.d("LuxuriousVideoPlayer", "Playback state changed: $playbackState")
                            when (playbackState) {
                                Player.STATE_READY -> {
                                    playerReady = true
                                    Log.d("LuxuriousVideoPlayer", "Player is ready")
                                }
                                Player.STATE_BUFFERING -> {
                                    Log.d("LuxuriousVideoPlayer", "Player is buffering")
                                }
                                Player.STATE_ENDED -> {
                                    Log.d("LuxuriousVideoPlayer", "Player ended")
                                }
                                Player.STATE_IDLE -> {
                                    Log.d("LuxuriousVideoPlayer", "Player idle")
                                }
                            }
                        }
                        
                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            Log.d("LuxuriousVideoPlayer", "Is playing: $isPlaying")
                        }
                    })
                    
                    prepare()
                    Log.d("LuxuriousVideoPlayer", "Player prepared successfully")
                } catch (e: Exception) {
                    playerError = e.message ?: "Unknown error"
                    Log.e("LuxuriousVideoPlayer", "Error setting up media item: $videoUri", e)
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            playerError = e.message ?: "Failed to create ExoPlayer"
            Log.e("LuxuriousVideoPlayer", "Error creating ExoPlayer instance", e)
            e.printStackTrace()
            null
        }
    }
    
    // Handle stagger delay and playback
    LaunchedEffect(videoUri) {
        if (exoPlayer == null) {
            Log.e("LuxuriousVideoPlayer", "ExoPlayer is null, cannot play video")
            shouldRemove = true
            return@LaunchedEffect
        }
        
        try {
            Log.d("LuxuriousVideoPlayer", "Starting playback sequence, stagger delay: ${staggerDelaySeconds}s")
            // Wait for stagger delay
            delay((staggerDelaySeconds * 1000).toLong())
            
            // Start playing
            Log.d("LuxuriousVideoPlayer", "Starting playback after stagger delay")
            shouldStartPlaying = true
            
            // Small delay to ensure view is ready
            delay(200)
            exoPlayer?.let { player ->
                player.playWhenReady = true
                Log.d("LuxuriousVideoPlayer", "Playback started, duration: ${durationSeconds}s")
            }
            
            // Wait for duration, then remove
            delay((durationSeconds * 1000).toLong())
            Log.d("LuxuriousVideoPlayer", "Duration complete, removing video")
            shouldRemove = true
            exoPlayer?.let { player ->
                player.pause()
                player.stop()
            }
        } catch (e: Exception) {
            playerError = e.message
            Log.e("LuxuriousVideoPlayer", "Error in video playback", e)
            e.printStackTrace()
            shouldRemove = true
        }
    }
    
    // Cleanup when component is removed
    DisposableEffect(videoUri) {
        onDispose {
            try {
                exoPlayer?.let { player ->
                    player.pause()
                    player.stop()
                    player.release()
                }
            } catch (e: Exception) {
                Log.e("LuxuriousVideoPlayer", "Error releasing video player", e)
            }
        }
    }
    
    // Show error message if there's an error
    if (playerError != null && !shouldRemove) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Log.e("LuxuriousVideoPlayer", "Displaying error: $playerError")
            // Error state - could show a placeholder or log
        }
    }
    
    // Only render PlayerView if player exists and not removed
    if (!shouldRemove && exoPlayer != null) {
        Log.d("LuxuriousVideoPlayer", "Rendering PlayerView (shouldStartPlaying: $shouldStartPlaying)")
        AndroidView(
            factory = { ctx ->
                try {
                Log.d("LuxuriousVideoPlayer", "Creating PlayerView for URI: $videoUri")
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    // Ensure the view is visible
                    visibility = android.view.View.VISIBLE
                    // Set background to transparent
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                }
            } catch (e: Exception) {
                Log.e("LuxuriousVideoPlayer", "Error in PlayerView factory", e)
                e.printStackTrace()
                // Return a simple view as fallback
                android.view.View(ctx)
            }
        },
        modifier = modifier,
        update = { view ->
            try {
                if (view is PlayerView && exoPlayer != null) {
                    // Update player reference if needed
                    if (view.player != exoPlayer) {
                        view.player = exoPlayer
                    }
                    // Update visibility based on playback state
                    if (shouldStartPlaying) {
                        view.visibility = android.view.View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                Log.e("LuxuriousVideoPlayer", "Error updating PlayerView", e)
            }
        }
        )
    } else if (playerError != null) {
        // Show error state if player failed to create
        Log.e("LuxuriousVideoPlayer", "Cannot render PlayerView - error: $playerError")
    }
}

