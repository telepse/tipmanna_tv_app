package com.example.telepse.ui.screen

import android.net.Uri
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.example.telepse.R
import com.example.telepse.data.network.NetworkMonitor
import com.example.telepse.ui.network.NetworkStatusIndicator
import com.example.telepse.ui.network.rememberNetworkMonitor
import com.example.telepse.ui.animations.CharacterAnimation
import com.example.telepse.ui.background.getEventBackgroundResource
import com.example.telepse.ui.theme.*
import com.example.telepse.ui.viewmodel.AnimationTrigger
import com.example.telepse.ui.viewmodel.FundraisingMilestoneViewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.zIndex
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.text.style.TextAlign
import com.example.telepse.data.model.TipUsageDisclosure
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield
import androidx.compose.runtime.Stable
import android.content.Context

/**
 * ExoPlayer Manager with Mutex synchronization to prevent concurrent modifications.
 * This wrapper ensures thread-safe operations on ExoPlayer instance.
 */
@Stable
internal class ExoPlayerManager(context: Context) {
    private val mutex = Mutex()
    private var _player: ExoPlayer? = null
    private var isReleased = false
    private var currentUri: Uri? = null
    
    val player: ExoPlayer = ExoPlayer.Builder(context)
        .build()
        .apply {
            repeatMode = Player.REPEAT_MODE_ONE
            _player = this
        }
    
    /**
     * Thread-safe media update with idempotency check.
     * Prevents redundant updates and ensures only one update at a time.
     */
    suspend fun updateMedia(uri: Uri): Unit {
        // CRITICAL: Check if released BEFORE acquiring mutex
        if (isReleased) {
            android.util.Log.w("ExoPlayerManager", "⚠️ Cannot update - player released (pre-check)")
            return
        }
        
        mutex.withLock {
            // CRITICAL: Check for cancellation at the start
            yield() // Checks for cancellation
            
            // Double-check released state after acquiring mutex
            if (isReleased) {
                android.util.Log.w("ExoPlayerManager", "⚠️ Cannot update - player released (post-check)")
                return@withLock
            }
            
            // Skip if same URI (prevents redundant updates)
            if (currentUri == uri) {
                android.util.Log.d("ExoPlayerManager", "⏭️ Skipping - same URI already loaded: $uri")
                return@withLock
            }
            
            _player?.let { exoPlayer ->
                try {
                    android.util.Log.d("ExoPlayerManager", "🎬 Updating media (mutex locked): $uri")
                
                // Check if player is still valid before any operation
                try {
                    val state = exoPlayer.playbackState
                    android.util.Log.d("ExoPlayerManager", "   - Current player state: $state")
                } catch (e: IllegalStateException) {
                    android.util.Log.e("ExoPlayerManager", "❌ Player already released, aborting update")
                    isReleased = true
                    currentUri = null
                    return@withLock
                }
                
                // Check for cancellation before stopping
                yield() // Checks for cancellation
                
                // Stop current playback safely
                try {
                    when (exoPlayer.playbackState) {
                        Player.STATE_IDLE -> {
                            android.util.Log.d("ExoPlayerManager", "   - Player already idle")
                        }
                        Player.STATE_BUFFERING,
                        Player.STATE_READY,
                        Player.STATE_ENDED -> {
                            exoPlayer.stop()
                            // Clear any pending media items
                            try {
                                exoPlayer.clearMediaItems()
                            } catch (e: Exception) {
                                android.util.Log.w("ExoPlayerManager", "Could not clear media items", e)
                            }
                            android.util.Log.d("ExoPlayerManager", "   - Player stopped and cleared")
                        }
                    }
                } catch (e: IllegalStateException) {
                    android.util.Log.e("ExoPlayerManager", "Player released during stop", e)
                    isReleased = true
                    currentUri = null
                    return@withLock
                }
                
                // Check for cancellation before setting new media
                yield() // Checks for cancellation
                
                // Set new media with comprehensive error handling
                try {
                    val mediaItem = MediaItem.fromUri(uri)
                    exoPlayer.setMediaItem(mediaItem)
                    exoPlayer.prepare()
                    exoPlayer.playWhenReady = true
                    
                    currentUri = uri
                    
                    android.util.Log.d("ExoPlayerManager", "✅ Media updated successfully to: $uri")
                } catch (e: IllegalStateException) {
                    android.util.Log.e("ExoPlayerManager", "❌ IllegalStateException setting media", e)
                    isReleased = true
                    currentUri = null
                    return@withLock
                }
                
                } catch (e: kotlinx.coroutines.CancellationException) {
                    android.util.Log.d("ExoPlayerManager", "⚠️ Update cancelled (event switching)")
                    // Don't update currentUri - leave it as is for next valid update
                    throw e // Re-throw to properly cancel
                } catch (e: IllegalStateException) {
                    android.util.Log.e("ExoPlayerManager", "❌ IllegalStateException updating media", e)
                    if (e.message?.contains("released", ignoreCase = true) == true) {
                        isReleased = true
                        currentUri = null
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ExoPlayerManager", "❌ Error updating media", e)
                }
            } ?: run {
                android.util.Log.w("ExoPlayerManager", "⚠️ Player is null")
                isReleased = true
            }
        }
    }
    
    /**
     * Stop playback safely.
     */
    suspend fun stop(): Unit = mutex.withLock {
        if (isReleased) return@withLock
        
        _player?.let { exoPlayer ->
            try {
                if (exoPlayer.playbackState != Player.STATE_IDLE) {
                    exoPlayer.stop()
                    android.util.Log.d("ExoPlayerManager", "✅ Player stopped")
                }
            } catch (e: Exception) {
                android.util.Log.e("ExoPlayerManager", "Error stopping player", e)
            }
        }
    }
    
    /**
     * Reset player state without releasing the instance.
     * Use when switching events to ensure clean state.
     */
    fun reset() {
        android.util.Log.d("ExoPlayerManager", "🔄 Resetting player state")
        _player?.let { exoPlayer ->
            try {
                if (exoPlayer.playbackState != Player.STATE_IDLE) {
                    exoPlayer.stop()
                }
                exoPlayer.clearMediaItems()
                android.util.Log.d("ExoPlayerManager", "✅ Player reset (stopped and cleared)")
            } catch (e: Exception) {
                android.util.Log.e("ExoPlayerManager", "Error resetting player", e)
            }
        }
        currentUri = null // Clear URI so next event will load
    }
    
    /**
     * Release player resources.
     * This is a regular function, not suspend, for DisposableEffect compatibility.
     */
    fun release() {
        android.util.Log.d("ExoPlayerManager", "🗑️ Releasing player")
        
        // CRITICAL: Set isReleased FIRST to stop any pending updateMedia calls
        isReleased = true
        
        _player?.let { exoPlayer ->
            try {
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                exoPlayer.release()
                android.util.Log.d("ExoPlayerManager", "✅ Player released")
            } catch (e: Exception) {
                android.util.Log.e("ExoPlayerManager", "Error releasing player", e)
            }
        }
        _player = null
        currentUri = null
    }
}

/**
 * Helper function to determine text color based on background type
 * Default background: white text
 * Islamic or gaming background: black text
 */
private fun getTextColorForBackground(backgroundType: String?, eventBackground: String?): Color {
    // Normalize the background identifier
    val normalizedId = when {
        backgroundType != null -> backgroundType.lowercase()
        eventBackground != null -> {
            eventBackground
                .removePrefix("/")
                .removeSuffix(".png")
                .removeSuffix(".mp4")
                .lowercase()
        }
        else -> "default"
    }
    
    return when (normalizedId) {
        "islamic", "gaming" -> Color.Black
        else -> Color.White // default background
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FundraisingMilestoneScreen(
    eventId: String,
    onBackClick: () -> Unit,
    viewModel: FundraisingMilestoneViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val networkMonitor = rememberNetworkMonitor()
    
    // Fetch event details
    LaunchedEffect(eventId) {
        android.util.Log.d("FundraisingMilestoneScreen", "📥 [LOAD] Loading event: $eventId")
        viewModel.loadEvent(eventId)
    }
    
    val event = uiState.event
    
    // CRITICAL: Only render if event matches eventId to prevent stale data during transitions
    // If event data doesn't match eventId, treat it as loading state
    if (event != null && event.id == eventId) {
        // Create ExoPlayerManager ONLY when we have the correct event data
        // This prevents creating a player manager when UI state is null/mismatched
        val playerManager = remember(eventId) {
            android.util.Log.d("FundraisingMilestoneScreen", "🎬 Creating ExoPlayerManager for event: $eventId")
            ExoPlayerManager(context)
        }
        
        // Release ExoPlayerManager when eventId changes or screen leaves
        DisposableEffect(eventId) {
            android.util.Log.d("FundraisingMilestoneScreen", "🔧 [SCREEN] Event entered: $eventId")
            
            onDispose {
                android.util.Log.d("FundraisingMilestoneScreen", "🧹 [SCREEN] Event leaving: $eventId - releasing ExoPlayerManager")
                playerManager.release()
                android.util.Log.d("FundraisingMilestoneScreen", "✅ ExoPlayerManager released for event: $eventId")
            }
        }
        // Get background resource based on event background type
        val backgroundResourceId = getEventBackgroundResource(
            backgroundType = event.backgroundType,
            eventBackground = event.eventBackground
        )
        val backgroundUri = Uri.parse("android.resource://${context.packageName}/${backgroundResourceId}")
        
        // Determine text color based on background type
        val textColor = getTextColorForBackground(event.backgroundType, event.eventBackground)
        
        Box(modifier = Modifier.fillMaxSize()) {
            // Network Status Indicator - shows at top when offline
            NetworkStatusIndicator(
                networkMonitor = networkMonitor,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(2000f) // Above everything
            )
            
            // Video Background - pass playerManager for synchronized operations
            // CRITICAL: Use key() to ensure VideoBackground is completely recreated per event
            // This cancels any pending LaunchedEffects from previous events
            key(eventId) {
                VideoBackground(
                    uri = backgroundUri,
                    resourceId = backgroundResourceId,
                    playerManager = playerManager,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // Content Overlay - Full height container
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 48.dp, vertical = 16.dp) // Reduced vertical padding to move content up
            ) {
                // Top Navigation Row - Positioned absolutely at top
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp), // Reduced top padding
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back Arrow (Top Left)
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    // Star Icon (Top Right)
                    IconButton(
                        onClick = { /* Handle favorite */ },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Favorite",
                            tint = Color(0xFFFFD700), // Gold color
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                // Center Content - Reordered according to requirements, full height
                // Use SpaceBetween to push bottom content (QR code + title) to the bottom
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                        .padding(top = 50.dp, bottom = 8.dp) // Reduced top padding significantly to move content up
                        .align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween // SpaceBetween to distribute space
                ) {
                    // Top group - stays at the top (reduced spacing to move content up)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                ) {
                        // 1. "Together you've sent" - First (bold like amount)
                    Text(
                            text = if (event.showLatestAmount == true) "Latest amount received" else "Together you've sent",
                        fontSize = 20.sp,
                            fontWeight = FontWeight.Bold, // Bold like the amount
                            color = textColor,
                            modifier = Modifier.padding(bottom = 8.dp) // Reduced from 12.dp
                    )
                    
                        // 2. Amount - Second (bold)
                        val displayAmount = if (event.showLatestAmount == true) {
                            event.latestTipAmount?.toInt() ?: 0
                        } else {
                            event.amountRaised?.toInt() ?: 0
                        }
                    Text(
                            text = "£$displayAmount",
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Bold,
                            color = textColor,
                            modifier = Modifier.padding(bottom = 6.dp) // Reduced from 8.dp
                    )
                    
                        // 3. Money for - Auto-scrolling text
                    event.tipUsageDisclosure?.let { disclosures ->
                        if (disclosures.isNotEmpty()) {
                            ScrollingTipUsageText(
                                disclosures = disclosures,
                                    textColor = textColor,
                                modifier = Modifier
                                        .width(200.dp)
                                        .padding(bottom = 6.dp) // Reduced from 8.dp
                            )
                        }
                    }
                    
                        // 4. Progress Bar Section - Always show progress bar, but hide amount text when hideProgressBarAmount is true
                        val amountRaised = event.amountRaised?.toInt() ?: 0
                        // Track when tips are received to trigger glow animation
                        var previousAnimationCount by remember { mutableStateOf(uiState.pendingAnimations.size) }
                        var previousAmountRaised by remember { mutableStateOf(amountRaised) }
                        var shouldGlow by remember { mutableStateOf(false) }
                        
                        LaunchedEffect(uiState.pendingAnimations.size, amountRaised) {
                            // Trigger glow when new animations are added (tip received)
                            if (uiState.pendingAnimations.size > previousAnimationCount) {
                                shouldGlow = true
                                kotlinx.coroutines.delay(2000) // Glow for 2 seconds
                                shouldGlow = false
                            }
                            // Trigger glow when amount increases (tip processed)
                            if (amountRaised > previousAmountRaised) {
                                shouldGlow = true
                                kotlinx.coroutines.delay(2000) // Glow for 2 seconds
                                shouldGlow = false
                            }
                            previousAnimationCount = uiState.pendingAnimations.size
                            previousAmountRaised = amountRaised
                        }
                        
                        ProgressBarSection(
                            amountRaised = amountRaised,
                            amountToRaise = event.amountToRaise?.toInt() ?: 0,
                            textColor = textColor,
                            hideAmountText = event.hideProgressBarAmount != true, // Reversed: backend has logic inverted
                            shouldGlow = shouldGlow,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    // Bottom group - pushed to the bottom by SpaceBetween
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 5. QR Code - Close to bottom (perfect square)
                        event.scanLink?.let { scanLink ->
                            QRCodeImage(
                                content = scanLink,
                                modifier = Modifier.size(150.dp) // Perfect square
                            )
                        } ?: run {
                            // Fallback if no scanLink
                            Box(
                                modifier = Modifier
                                    .size(150.dp) // Perfect square
                                    .background(Color.White, RoundedCornerShape(8.dp))
                                    .border(2.dp, Color.White, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No QR Code",
                                    fontSize = 12.sp,
                                    color = Color.Black
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // 6. Title - Last (Event Name with background #212121 and border #667185)
                    Box(
                        modifier = Modifier
                                .widthIn(max = 200.dp)
                            .background(
                                color = Color(0xFF212121), // bg-[#212121]
                                shape = RoundedCornerShape(6.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = Color(0xFF667185), // border-[#667185]
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = event.eventName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            
            // Megaphone icon - Positioned absolutely at bottom left of screen
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 16.dp)
                    .zIndex(100f) // Ensure it's above other content but below animations
            ) {
                ShakingSpeakerIcon(
                    modifier = Modifier.size(96.dp), // Increased size for better visibility
                    contentDescription = "Megaphone"
                )
            }
            
            // Animation Overlay - MUST be last to appear on top of everything
            AnimationOverlay(
                animations = uiState.pendingAnimations,
                onAnimationComplete = { animation ->
                    viewModel.removeAnimation(animation)
                }
            )
        }
    } else if (uiState.isLoading || (event != null && event.id != eventId)) {
        // Loading state OR event data mismatch (switching events)
        // Show loading screen instead of rendering mismatched data
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black), // Add black background for stability
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (event != null && event.id != eventId) {
                        "Switching events..."
                    } else {
                        "Loading..."
                    },
                    color = Color.White,
                    fontSize = 18.sp
                )
            }
        }
    } else {
        // Error state
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black), // Add black background
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = uiState.error ?: "Failed to load event",
                    color = Color.White, // Changed to white for visibility on black background
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                IconButton(onClick = { viewModel.loadEvent(eventId) }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retry",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoBackground(
    uri: Uri,
    resourceId: Int,
    playerManager: ExoPlayerManager,
    modifier: Modifier = Modifier
) {
    // Create a stable key that includes both resourceId and uri for proper cancellation
    val backgroundKey = remember(uri, resourceId) { 
        "${resourceId}_${uri.toString()}"
    }
    
    android.util.Log.d("VideoBackground", "🔧 VideoBackground composable entered")
    android.util.Log.d("VideoBackground", "   - Resource ID: $resourceId")
    android.util.Log.d("VideoBackground", "   - URI: $uri")
    android.util.Log.d("VideoBackground", "   - Background Key: $backgroundKey")
    
    // Update media when URI changes - playerManager handles synchronization
    // Using backgroundKey ensures LaunchedEffect cancels when event changes
    LaunchedEffect(backgroundKey) {
        try {
            android.util.Log.d("VideoBackground", "🔄 LaunchedEffect triggered")
            android.util.Log.d("VideoBackground", "   - Key: $backgroundKey")
            android.util.Log.d("VideoBackground", "   - URI: $uri")
            
            // Small delay to allow previous LaunchedEffect to cancel
            kotlinx.coroutines.delay(50)
            
            // Check if still active after delay
            yield() // Checks for cancellation
            
            // CRITICAL: updateMedia is synchronized with mutex
            // Only one update can happen at a time, preventing race conditions
            playerManager.updateMedia(uri)
            
            android.util.Log.d("VideoBackground", "✅ Video background update complete")
            
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Cancellation is expected when switching events
            android.util.Log.d("VideoBackground", "⚠️ Video update cancelled (switching events)")
            throw e // Re-throw to properly cancel the coroutine
        } catch (e: Exception) {
            android.util.Log.e("VideoBackground", "❌ Error in LaunchedEffect", e)
        }
    }
    
    // NO cleanup in VideoBackground - player is managed at screen level
    // This prevents race conditions when switching events
    
    AndroidView(
        factory = { ctx ->
            try {
                android.util.Log.d("VideoBackground", "🏭 Creating PlayerView")
                PlayerView(ctx).apply {
                    player = playerManager.player
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
            } catch (e: Exception) {
                android.util.Log.e("VideoBackground", "❌ Error creating PlayerView", e)
                // Return a fallback view if PlayerView creation fails
                android.view.View(ctx)
            }
        },
        update = { view ->
            // Update player reference if view is PlayerView
            if (view is PlayerView) {
                try {
                    if (view.player != playerManager.player) {
                        view.player = playerManager.player
                        android.util.Log.d("VideoBackground", "🔄 PlayerView player reference updated")
                    }
                } catch (e: Exception) {
                    android.util.Log.w("VideoBackground", "Error updating PlayerView player", e)
                }
            }
        },
        modifier = modifier
    )
}

@Composable
fun ProgressBarSection(
    amountRaised: Int,
    amountToRaise: Int,
    textColor: Color,
    hideAmountText: Boolean = false,
    shouldGlow: Boolean = false,
    modifier: Modifier = Modifier
) {
    val progress = if (amountToRaise > 0) {
        (amountRaised.toFloat() / amountToRaise.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    
    // Glow animation when tip is received - smoother and brighter transition
    val glowAlpha by animateFloatAsState(
        targetValue = if (shouldGlow) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "glowAlpha"
    )
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Progress Bar Container with Icon
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Progress Bar Container (longer, taller)
                Box(
                    modifier = Modifier
                        .width(320.dp) // Longer fixed width
                        .height(28.dp) // Increased height a bit more
                        .background(Color.Black, RoundedCornerShape(6.dp)) // Just a bit rounded
                ) {
                    // Progress Fill with color #E8562E
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .background(Color(0xFFE8562E), RoundedCornerShape(6.dp)) // Color #E8562E, just a bit rounded
                    )
                    
                    // Glow effect overlay when tip is received - brighter and smoother transition
                    if (shouldGlow || glowAlpha > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxHeight()
                                .background(
                                    // Brighter yellow/orange glow that transitions smoothly
                                    Color(0xFFFFD700).copy(alpha = glowAlpha * 0.9f), // Bright gold/yellow for more visible glow
                                    RoundedCornerShape(6.dp)
                                )
                                .graphicsLayer {
                                    // Smooth scale effect for glow
                                    scaleX = 1f + (glowAlpha * 0.2f)
                                    scaleY = 1f + (glowAlpha * 0.2f)
                                }
                        )
                    }
                }
                
                // Spacing between progress bar and icon
                Spacer(modifier = Modifier.width(16.dp))
                
                // Goal Icon (Diamond/Gem) - icon only, target is in row below
                    Image(
                        painter = painterResource(id = R.drawable.diamond),
                        contentDescription = "Goal",
                        modifier = Modifier.size(36.dp),
                        contentScale = ContentScale.Fit
                    )
            }
        }
        
        // Row with three items below the progress bar: Amount Raised (left), tipmanna.com (center), Target (right)
        // All three on the same row, with target aligned to be beneath the icon
        // Hide this row when hideAmountText is true
        if (!hideAmountText) {
            // Match the exact width of progress bar (320.dp) + spacing (16.dp) + icon (36.dp) = 372.dp
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .width(372.dp) // Match progress bar + spacing + icon width
                        .align(Alignment.Center),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Amount Raised (aligned with start of progress bar)
                    Text(
                        text = "£$amountRaised",
                        fontSize = 12.sp,
                        color = textColor,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // Center: tipmanna.com
                    Text(
                        text = "tipmanna.com",
                        fontSize = 12.sp,
                        color = textColor,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                    
                    // Right: Target Price (aligned to be beneath the icon)
                    Text(
                        text = "£$amountToRaise",
                        fontSize = 12.sp,
                        color = textColor,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(min = 50.dp) // Minimum width to prevent breaking, allow expansion
                    )
                }
            }
        }
    }
}

@Composable
private fun ShakingSpeakerIcon(
    modifier: Modifier = Modifier,
    contentDescription: String = "Speaker"
) {
    val density = LocalDensity.current
    val infiniteTransition = rememberInfiniteTransition(label = "speakerShake")
    
    // Rotation animation for wiggling effect
    val rotation by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 600
                -12f at 0
                12f at 150
                -10f at 300
                10f at 450
                -12f at 600
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "speakerRotation"
    )
    
    // Translation animation for additional shake effect
    val translationXValue by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 600
                -2f at 0
                2f at 150
                -1.5f at 300
                1.5f at 450
                -2f at 600
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "speakerTranslationX"
    )
    
    // No background box - just the image with animation
    Image(
        painter = painterResource(id = R.drawable.ic_speaker_shake),
        contentDescription = contentDescription,
        modifier = modifier
            .graphicsLayer {
                // Rotate around Z axis for visible wiggle effect
                rotationZ = rotation
                // Add slight translation for shake effect
                translationX = with(density) { translationXValue.dp.toPx() }
            },
        contentScale = ContentScale.Fit
    )
}

@Composable
fun QRCodeImage(
    content: String,
    modifier: Modifier = Modifier
) {
    val qrBitmap = remember(content) {
        generateQRCode(content, 512)
    }
    
    qrBitmap?.let { bitmap ->
        // Display QR code directly without extra container - the bitmap already has white background
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "QR Code",
            modifier = modifier,
            contentScale = ContentScale.FillBounds
            )
    } ?: run {
        Box(
            modifier = modifier
                .background(Color.White, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "QR Error",
                fontSize = 12.sp,
                color = Color.Black
            )
        }
    }
}

private fun generateQRCode(content: String, size: Int): Bitmap? {
    return try {
        val hints = hashMapOf<EncodeHintType, Any>().apply {
            put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H)
            put(EncodeHintType.CHARACTER_SET, "UTF-8")
            put(EncodeHintType.MARGIN, 0) // No margin so QR code fills entire space
        }
        
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        
        // Use RGB_565 for better performance with white background
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                // Black QR code on white background
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
            }
        }
        bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Composable that displays multiple character animations as an overlay.
 * 
 * This composable handles displaying animations triggered by Pusher events.
 * Each animation is displayed independently and can overlap.
 * Animations are removed when they complete.
 * 
 * The overlay is positioned on top of all other content using zIndex.
 * 
 * @param animations List of animation triggers to display
 * @param onAnimationComplete Callback when an animation completes
 */
@Composable
private fun AnimationOverlay(
    animations: List<AnimationTrigger>,
    onAnimationComplete: (AnimationTrigger) -> Unit
) {
    val context = LocalContext.current
    
    if (animations.isEmpty()) {
        android.util.Log.d("AnimationOverlay", "📭 No animations to display")
        return
    }
    
    // Only display the FIRST animation in the queue (sequential playback)
    val currentAnimation = animations.first()
    val queueSize = animations.size
    
    android.util.Log.d("AnimationOverlay", "🎬 Queue: $queueSize animations, displaying: ${currentAnimation.animationType} (id: ${currentAnimation.id})")
    
    // Display only the first animation - sequential playback
    // Use zIndex to ensure animations appear on top of all content
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(1000f) // High z-index to ensure it's on top
    ) {
        // Use the unique ID as the key to ensure Compose properly tracks each animation
        key(currentAnimation.id) {
            android.util.Log.d("AnimationOverlay", "🎨 Rendering animation: ${currentAnimation.animationType} (id: ${currentAnimation.id})")
            CharacterAnimation(
                animationType = currentAnimation.animationType,
                imageUrl = currentAnimation.imageUrl,
                modifier = Modifier.fillMaxSize(),
                onAnimationComplete = {
                    android.util.Log.d("AnimationOverlay", "✅ Animation ${currentAnimation.id} (${currentAnimation.animationType}) completed, removing from queue")
                    android.util.Log.d("AnimationOverlay", "   - Remaining in queue: ${queueSize - 1}")
                    onAnimationComplete(currentAnimation)
                    // The next animation will automatically start since the list updates
                }
            )
        }
    }
}

/**
 * Composable that displays tip usage disclosure text with auto-scrolling animation.
 * Text enters from the left and exits from the right, repeating continuously.
 * 
 * @param disclosures List of tip usage disclosures
 * @param modifier Modifier for the component
 */
@Composable
private fun ScrollingTipUsageText(
    disclosures: List<TipUsageDisclosure>,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    // Format the text: "for [customLabel or usageType]"
    val text = remember(disclosures) {
        val parts = disclosures.map { disclosure ->
            val label = disclosure.customLabel ?: disclosure.usageType
                .split("_")
                .joinToString(" ") { word ->
                    word.lowercase().replaceFirstChar { it.uppercase() }
                }
            label
        }
        "for ${parts.joinToString(", ")}"
    }
    
    // Create a shorter text by repeating it only twice for seamless scrolling (reduced from 3)
    val scrollingText = remember(text) {
        "$text  •  $text  •  " // Repeat with separator for seamless loop (reduced from 3 to 2)
    }
    
    // Get container width (200px) and density for proper scrolling calculation
    val density = LocalDensity.current
    val containerWidthPx: Float = with(density) { 
        200.dp.toPx()
    }
    
    // Infinite transition for continuous scrolling
    val infiniteTransition = rememberInfiniteTransition(label = "scrollingText")
    
    // Calculate text width (approximate: 1 character ≈ 8dp at 14sp)
    val estimatedTextWidthPx: Float = with(density) { 
        val charCount = scrollingText.length
        val textWidthDp = 8.dp * charCount
        textWidthDp.toPx()
    }
    val scrollDistancePx: Float = estimatedTextWidthPx + containerWidthPx
    
    // Animate the offset from right (positive) to left (negative) - reduced duration for faster scroll
    val offsetX by infiniteTransition.animateFloat(
        initialValue = containerWidthPx,
        targetValue = -scrollDistancePx,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 12000 // Reduced from 20000ms to 12000ms for faster scrolling
                containerWidthPx at 0
                -scrollDistancePx at 12000
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "scrollOffset"
    )
    
    Box(
        modifier = modifier
            .height(24.dp)
            .width(200.dp) // Fixed width 200px for auto-scroll container
            .clip(RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = scrollingText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = textColor,
            modifier = Modifier
                .graphicsLayer {
                    translationX = offsetX
                }
                .wrapContentWidth(Alignment.Start)
        )
    }
}
