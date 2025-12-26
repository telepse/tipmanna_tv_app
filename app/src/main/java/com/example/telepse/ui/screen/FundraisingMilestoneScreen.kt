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

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FundraisingMilestoneScreen(
    eventId: String,
    onBackClick: () -> Unit,
    viewModel: FundraisingMilestoneViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
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
                    // Header Text
                    Text(
                        text = "Together you've sent",
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
                        modifier = Modifier.padding(bottom = 32.dp)
                    )
                    
                    // Progress Bar Section
                    ProgressBarSection(
                        amountRaised = amountRaised,
                        amountToRaise = event.amountToRaise?.toInt() ?: 0,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Spacing between tipmanna.live and event title (reduced by half)
                    Spacer(modifier = Modifier.height(40.dp))
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
                    
                    // QR Code
                    event.scanLink?.let { scanLink ->
                        QRCodeImage(
                            content = scanLink,
                            modifier = Modifier.size(120.dp)
                        )
                    } ?: run {
                        // Fallback if no scanLink
                        Box(
                            modifier = Modifier
                                .size(120.dp)
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
                
                // Bottom Navigation
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shaking speaker icon (Bottom Left)
                    Box(modifier = Modifier.size(48.dp)) {
                        ShakingSpeakerIcon(
                            modifier = Modifier.size(40.dp),
                            contentDescription = "Speaker"
                        )
                    }
                    
                    // Progress Indicator Dots (Bottom Center)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        repeat(3) { index ->
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(
                                        color = if (index == 0) Color.White else Color.White.copy(alpha = 0.5f),
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    )
                            )
                        }
                    }
                }
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
    val infiniteTransition = rememberInfiniteTransition(label = "speakerShake")
    val tiltX by infiniteTransition.animateFloat(
        initialValue = -9f,
        targetValue = 9f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 900
                -9f at 0
                9f at 225
                -7f at 450
                7f at 675
                -9f at 900
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "speakerTiltX"
    )
    
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_speaker_shake),
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // Rotate around the X axis so the top leans forward while the bottom leans back
                    rotationX = tiltX
                    cameraDistance = 12 * density
                }
        )
    }
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
    if (animations.isEmpty()) return
    
    android.util.Log.d("AnimationOverlay", "Displaying ${animations.size} animations")
    
    // Display all animations simultaneously - they can overlap
    // Use zIndex to ensure animations appear on top of all content
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(1000f) // High z-index to ensure it's on top
    ) {
        // Use key to ensure each animation is properly tracked and removed
        animations.forEachIndexed { index, animation ->
            // Create unique key for each animation to ensure proper cleanup
            // Using a combination of type, URL, and index to ensure uniqueness
            key("${animation.animationType}_${animation.imageUrl}_$index") {
                android.util.Log.d("AnimationOverlay", "Rendering animation $index: ${animation.animationType}")
                CharacterAnimation(
                    animationType = animation.animationType,
                    imageUrl = animation.imageUrl,
                    modifier = Modifier.fillMaxSize(),
                    onAnimationComplete = {
                        android.util.Log.d("AnimationOverlay", "Animation $index (${animation.animationType}) completed, removing from list")
                        onAnimationComplete(animation)
                    }
                )
            }
        }
    }
}
