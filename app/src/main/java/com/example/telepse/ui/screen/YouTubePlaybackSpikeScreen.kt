package com.example.telepse.ui.screen

import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.telepse.ui.tv.TvLazyColumn
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.example.telepse.ui.network.rememberNetworkMonitor
import com.example.telepse.ui.tv.TvAppColors
import com.example.telepse.ui.tv.TvNavButton
import com.example.telepse.ui.tv.requestInitialTvFocus
import com.example.telepse.ui.tv.tvNotFocusable
import com.example.telepse.ui.viewmodel.YouTubePlaybackSpikeViewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun YouTubePlaybackSpikeScreen(
    onBack: () -> Unit,
    viewModel: YouTubePlaybackSpikeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    val controlsFocus = remember { FocusRequester() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val networkMonitor = rememberNetworkMonitor()

    LaunchedEffect(Unit) {
        controlsFocus.requestInitialTvFocus()
    }

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_STOP -> viewModel.logLifecycle("pause")
                    Lifecycle.Event.ON_START -> viewModel.logLifecycle("resume")
                    else -> Unit
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(networkMonitor) {
        networkMonitor.isOnline.collect { online ->
            viewModel.logNetwork(if (online) "online" else "offline")
        }
    }

    Box(
        modifier =
            Modifier.fillMaxSize()
                .background(TvAppColors.ScreenBackground)
                .padding(16.dp),
    ) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "YouTube playback spike (architecture gate)",
                color = TvAppColors.TextPrimary,
            )
            Text(
                text = "Video: ${uiState.videoId} | State: ${uiState.playerState.name}",
                color = TvAppColors.TextSecondary,
            )
            if (uiState.loadStartedMs == null) {
                Text(
                    text = "Press Load below to initialize the player (playback does not start automatically).",
                    color = TvAppColors.TextSecondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
                )
            }

            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .height(300.dp)
                        .background(androidx.compose.ui.graphics.Color.Black)
                        .tvNotFocusable(),
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize().tvNotFocusable(),
                    factory = { context ->
                        WebView(context).apply {
                            layoutParams =
                                ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                )
                            isFocusable = false
                            isFocusableInTouchMode = false
                            webViewRef = this
                            viewModel.adapter.attachWebView(this)
                            viewModel.onWebViewCreated()
                        }
                    },
                    update = { webView ->
                        if (webViewRef !== webView) {
                            webViewRef = webView
                            viewModel.adapter.recreatePlayerSurface(webView)
                        }
                    },
                    onRelease = { webView ->
                        viewModel.adapter.destroy()
                        webView.destroy()
                        webViewRef = null
                    },
                )
                when {
                    uiState.lastError != null ->
                        Text(
                            text = "Player error (code ${uiState.lastError}). Press Load to retry.",
                            color = androidx.compose.ui.graphics.Color(0xFFFF6B6B),
                            modifier = Modifier.align(Alignment.Center).padding(16.dp),
                        )
                    uiState.loadStartedMs != null && uiState.readyMs == null ->
                        androidx.compose.foundation.layout.Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            CircularProgressIndicator(color = TvAppColors.TextPrimary)
                            Text(
                                text = "Loading player…",
                                color = TvAppColors.TextSecondary,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    uiState.loadStartedMs == null ->
                        Text(
                            text = "Press Load to start",
                            color = TvAppColors.TextSecondary,
                            modifier = Modifier.align(Alignment.Center),
                        )
                }
            }

            TvLazyColumn(
                modifier = Modifier.padding(top = 8.dp),
                scrollable = true,
            ) {
                TvNavButton(
                    text = "Back",
                    onClick = onBack,
                    focusRequester = controlsFocus,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                TvNavButton(
                    text = "Load",
                    onClick = { viewModel.startInitialLoad() },
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                TvNavButton(
                    text = "Cue",
                    onClick = { viewModel.cueCurrent() },
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                TvNavButton(
                    text = "Play",
                    onClick = { viewModel.play() },
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                TvNavButton(
                    text = "Pause",
                    onClick = { viewModel.pause() },
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                TvNavButton(
                    text = "Seek 30s",
                    onClick = { viewModel.seek30() },
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                TvNavButton(
                    text = "Mute",
                    onClick = { viewModel.mute() },
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                TvNavButton(
                    text = "Unmute",
                    onClick = { viewModel.unmute() },
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                TvNavButton(
                    text = "Swap video",
                    onClick = { viewModel.swapVideo() },
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                TvNavButton(
                    text = "Recreate WebView",
                    onClick = {
                        webViewRef?.let { viewModel.adapter.recreatePlayerSurface(it) }
                    },
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                TvNavButton(
                    text = "Poll time",
                    onClick = { viewModel.pollTime() },
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                TvNavButton(
                    text = "Net probe",
                    onClick = {
                        viewModel.logNetwork(
                            if (networkMonitor.isCurrentlyOnline()) "probe_online" else "probe_offline",
                        )
                    },
                )
            }

            Box(
                modifier =
                    Modifier.weight(1f)
                        .padding(top = 8.dp)
                        .verticalScroll(rememberScrollState())
                        .tvNotFocusable(),
            ) {
                androidx.compose.foundation.layout.Column {
                    uiState.logLines.forEach { line ->
                        Text(text = line, color = TvAppColors.TextSecondary)
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.adapter.destroy()
        }
    }
}
