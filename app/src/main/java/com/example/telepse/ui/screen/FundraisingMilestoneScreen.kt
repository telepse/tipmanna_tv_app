package com.example.telepse.ui.screen

import android.net.Uri
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import android.widget.Toast
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.text.style.TextAlign
import com.example.telepse.data.model.TipUsageDisclosure
import kotlinx.coroutines.delay

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FundraisingMilestoneScreen(
    eventId: String,
    onBackClick: () -> Unit,
    viewModel: FundraisingMilestoneViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    // Show toast messages - track last shown message to avoid duplicates
    var lastShownToast by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { message ->
            if (message != lastShownToast) {
                try {
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    lastShownToast = message
                } catch (e: Exception) {
                    android.util.Log.e("FundraisingMilestoneScreen", "Error showing toast", e)
                }
            }
        }
    }
    
    // Fetch event details
    LaunchedEffect(eventId) {
        viewModel.loadEvent(eventId)
    }
    
    val event = uiState.event
    
    if (event != null) {
        // Get background resource based on event background type
        val backgroundResourceId = getEventBackgroundResource(
            backgroundType = event.backgroundType,
            eventBackground = event.eventBackground
        )
        val backgroundUri = Uri.parse("android.resource://${context.packageName}/${backgroundResourceId}")
        
        Box(modifier = Modifier.fillMaxSize()) {
            // Video Background
            VideoBackground(
                uri = backgroundUri,
                modifier = Modifier.fillMaxSize()
            )
            
            // Content Overlay
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 48.dp, vertical = 32.dp)
            ) {
                // Top Navigation Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
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
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Center Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Header Text - Change based on showLatestAmount
                    Text(
                        text = if (event.showLatestAmount == true) "Latest amount raised" else "Together you've sent",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // Amount Display
                    val amountRaised = event.amountRaised?.toInt() ?: 0
                    Text(
                        text = "£$amountRaised",
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // Tip Usage Disclosure - Auto-scrolling text
                    event.tipUsageDisclosure?.let { disclosures ->
                        if (disclosures.isNotEmpty()) {
                            ScrollingTipUsageText(
                                disclosures = disclosures,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 24.dp)
                            )
                        }
                    }
                    
                    // Progress Bar Section - Hide when hideProgressBarAmount is true
                    if (event.hideProgressBarAmount != true) {
                        ProgressBarSection(
                            amountRaised = amountRaised,
                            amountToRaise = event.amountToRaise?.toInt() ?: 0,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    // Spacing between tipmanna.live and event title (reduced)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Bottom Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Event Name with background #212121 and border #667185 (reduced width)
                    Box(
                        modifier = Modifier
                            .widthIn(max = 200.dp) // Reduced width
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
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // QR Code - Increased size for better scanability
                    event.scanLink?.let { scanLink ->
                        QRCodeImage(
                            content = scanLink,
                            modifier = Modifier.size(200.dp) // Increased from 120.dp to 200.dp
                        )
                    } ?: run {
                        // Fallback if no scanLink
                        Box(
                            modifier = Modifier
                                .size(200.dp) // Increased from 120.dp to 200.dp
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
    } else if (uiState.isLoading) {
        // Loading state
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Loading...",
                color = Color.White,
                fontSize = 18.sp
            )
        }
    } else {
        // Error state
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = uiState.error ?: "Failed to load event",
                    color = Color.White,
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
fun VideoBackground(
    uri: Uri,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(uri)
            setMediaItem(mediaItem)
            repeatMode = Player.REPEAT_MODE_ONE
            prepare()
            playWhenReady = true
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }
    
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            }
        },
        modifier = modifier
    )
}

@Composable
fun ProgressBarSection(
    amountRaised: Int,
    amountToRaise: Int,
    modifier: Modifier = Modifier
) {
    val progress = if (amountToRaise > 0) {
        (amountRaised.toFloat() / amountToRaise.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    
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
                // Progress Bar Container (narrower, taller)
                Box(
                    modifier = Modifier
                        .width(240.dp) // Narrower fixed width
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
                }
                
                // Spacing between progress bar and icon
                Spacer(modifier = Modifier.width(16.dp))
                
                // Goal Icon (Diamond/Gem) with target amount below it
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_goal_diamond),
                        contentDescription = "Goal",
                        modifier = Modifier.size(36.dp),
                        tint = Color.Unspecified // Use original colors from SVG
                    )
                    // Target amount below the diamond icon
                    Text(
                        text = "£$amountToRaise",
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            // Amount Raised label positioned below the progress bar, aligned to where progress stops
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp) // Below the progress bar
                    .align(Alignment.Center)
            ) {
                // Calculate position based on progress
                val progressBarWidth = 240.dp
                val leftOffset = (progressBarWidth * progress) - (progressBarWidth / 2f)
                
                Text(
                    text = "£$amountRaised",
                    fontSize = 12.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .offset(x = leftOffset)
                        .align(Alignment.CenterStart)
                )
            }
        }
        
        // Center label: tipmanna.live
        Text(
            text = "tipmanna.live",
            fontSize = 12.sp,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 12.dp)
        )
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
        Box(
            modifier = modifier
                .background(Color.White, RoundedCornerShape(8.dp))
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "QR Code",
                modifier = Modifier.fillMaxSize()
            )
        }
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
    
    // Show toast when new animations are added to queue
    var previousQueueSize by remember { mutableStateOf(0) }
    LaunchedEffect(queueSize) {
        if (queueSize > previousQueueSize) {
            try {
                Toast.makeText(context, "📋 Queue: $queueSize animations", Toast.LENGTH_SHORT).show()
                previousQueueSize = queueSize
            } catch (e: Exception) {
                android.util.Log.e("AnimationOverlay", "Error showing toast", e)
            }
        }
    }
    
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
    
    // Get screen width and density for proper scrolling calculation
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx: Float = with(density) { 
        configuration.screenWidthDp.dp.toPx()
    }
    
    // Infinite transition for continuous scrolling
    val infiniteTransition = rememberInfiniteTransition(label = "scrollingText")
    
    // Calculate text width (approximate: 1 character ≈ 8dp at 14sp)
    val estimatedTextWidthPx: Float = with(density) { 
        val charCount = scrollingText.length
        val textWidthDp = 8.dp * charCount
        textWidthDp.toPx()
    }
    val scrollDistancePx: Float = estimatedTextWidthPx + screenWidthPx
    
    // Animate the offset from right (positive) to left (negative) - reduced duration for faster scroll
    val offsetX by infiniteTransition.animateFloat(
        initialValue = screenWidthPx,
        targetValue = -scrollDistancePx,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 12000 // Reduced from 20000ms to 12000ms for faster scrolling
                screenWidthPx at 0
                -scrollDistancePx at 12000
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "scrollOffset"
    )
    
    Box(
        modifier = modifier
            .height(24.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = scrollingText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.9f),
            modifier = Modifier
                .graphicsLayer {
                    translationX = offsetX
                }
                .wrapContentWidth(Alignment.Start)
        )
    }
}
