package com.example.telepse.ui.screen

import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.example.telepse.ui.moderation.ExploreModerationChipPolicy
import com.example.telepse.ui.moderation.TvModerationDialogOverlay
import com.example.telepse.ui.moderation.TvModerationToast
import com.example.telepse.ui.tv.TvLazyRow
import com.example.telepse.ui.viewmodel.TvModerationEffect
import com.example.telepse.ui.viewmodel.TvModerationViewModel
import com.example.telepse.player.WebViewYouTubePlayerAdapter
import com.example.telepse.player.YouTubePlayerEvent
import com.example.telepse.ui.share.VideoShareQrOverlay
import com.example.telepse.ui.tv.TvAppColors
import com.example.telepse.ui.tv.TvPlayerControlChip
import com.example.telepse.ui.tv.requestInitialTvFocus
import com.example.telepse.ui.tv.tvNotFocusable

/**
 * On-demand playback for Explore trends items (YouTube URL from feed batch).
 * Not the global LIVE TV schedule engine.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ExplorePlaybackScreen(
    videoId: String,
    title: String,
    eventId: String,
    channelId: String,
    onBack: () -> Unit,
    moderationViewModel: TvModerationViewModel = hiltViewModel(),
) {
    val moderationState by moderationViewModel.state.collectAsState()
    LaunchedEffect(moderationViewModel) {
        moderationViewModel.effects.collect { effect ->
            if (effect is TvModerationEffect.RefreshFeeds) {
                onBack()
            }
        }
    }
    val adapter = remember { WebViewYouTubePlayerAdapter() }
    var playerReady by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(true) }
    val backFocus = remember { FocusRequester() }
    val playFocus = remember { FocusRequester() }
    val pauseFocus = remember { FocusRequester() }
    val muteFocus = remember { FocusRequester() }
    val reportVideoFocus = remember { FocusRequester() }
    val reportHostFocus = remember { FocusRequester() }
    val blockHostFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        backFocus.requestInitialTvFocus()
    }

    val moderationEnabled = ExploreModerationChipPolicy.moderationEnabled(channelId)

    LaunchedEffect(adapter, videoId) {
        if (videoId.isBlank()) return@LaunchedEffect
        adapter.events.collect { event ->
            when (event) {
                is YouTubePlayerEvent.PlayerReady -> {
                    playerReady = true
                    if (isMuted) adapter.mute()
                    adapter.play()
                }
                else -> Unit
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize().tvNotFocusable(),
            factory = { context ->
                WebView(context).apply {
                    layoutParams =
                        ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                    setBackgroundColor(android.graphics.Color.BLACK)
                    isFocusable = false
                    adapter.attachWebView(this)
                    if (videoId.isNotBlank()) {
                        adapter.load(videoId, 0f)
                    }
                }
            },
            onRelease = { webView ->
                adapter.destroy()
                webView.destroy()
            },
        )

        if (playerReady) {
            VideoShareQrOverlay(
                eventId = eventId,
                channelId = channelId,
                modifier =
                    Modifier.align(Alignment.TopEnd)
                        .padding(top = 20.dp, end = 28.dp),
            )
        }

        Column(
            modifier =
                Modifier.align(Alignment.TopStart)
                    .fillMaxWidth()
                    .background(Color(0xCC000000))
                    .padding(28.dp),
        ) {
            Text(
                text = "Explore playback",
                color = TvAppColors.FocusRingSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = title,
                color = TvAppColors.TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        Box(
            modifier =
                Modifier.align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color(0xE6000000))
                    .padding(20.dp),
        ) {
            if (videoId.isBlank()) {
                Text(
                    text = "Playback unavailable for this item. Moderation actions below still apply.",
                    color = TvAppColors.TextSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            TvLazyRow {
                TvPlayerControlChip(
                    label = "Back",
                    onClick = onBack,
                    focusRequester = backFocus,
                    focusChainRight = playFocus,
                )
                TvPlayerControlChip(
                    label = "Play",
                    onClick = { if (playerReady) adapter.play() },
                    focusRequester = playFocus,
                    focusChainLeft = backFocus,
                    focusChainRight = pauseFocus,
                    enabled = playerReady,
                )
                TvPlayerControlChip(
                    label = "Pause",
                    onClick = { if (playerReady) adapter.pause() },
                    focusRequester = pauseFocus,
                    focusChainLeft = playFocus,
                    focusChainRight = muteFocus,
                    enabled = playerReady,
                )
                TvPlayerControlChip(
                    label = if (isMuted) "Unmute" else "Mute",
                    onClick = {
                        if (isMuted) {
                            adapter.unmute()
                            isMuted = false
                        } else {
                            adapter.mute()
                            isMuted = true
                        }
                    },
                    focusRequester = muteFocus,
                    focusChainLeft = pauseFocus,
                    focusChainRight = reportVideoFocus,
                    enabled = playerReady,
                )
                TvPlayerControlChip(
                    label = "Report video",
                    onClick = { moderationViewModel.openReportVideoDialog(channelId) },
                    focusRequester = reportVideoFocus,
                    focusChainLeft = muteFocus,
                    focusChainRight = reportHostFocus,
                    enabled = moderationEnabled,
                )
                TvPlayerControlChip(
                    label = "Report host",
                    onClick = { moderationViewModel.openReportHostDialog(channelId) },
                    focusRequester = reportHostFocus,
                    focusChainLeft = reportVideoFocus,
                    focusChainRight = blockHostFocus,
                    enabled = moderationEnabled,
                )
                TvPlayerControlChip(
                    label = "Block host",
                    onClick = { moderationViewModel.openBlockHostDialog(channelId) },
                    focusRequester = blockHostFocus,
                    focusChainLeft = reportHostFocus,
                    enabled = moderationEnabled,
                )
            }
        }

        moderationState.successMessage?.let { message ->
            TvModerationToast(
                message = message,
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 100.dp),
            )
        }

        if (channelId.isNotBlank()) {
            TvModerationDialogOverlay(
                channelId = channelId,
                state = moderationState,
                moderationViewModel = moderationViewModel,
            )
        }
    }
}
