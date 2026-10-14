package com.example.telepse.ui.screen

import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.telepse.ui.tv.TvLazyRow
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.example.telepse.livetv.TvModeEnginePhase
import com.example.telepse.ui.tv.TvAppColors
import com.example.telepse.ui.tv.TvNavButton
import com.example.telepse.ui.tv.requestInitialTvFocus
import com.example.telepse.ui.tv.tvNotFocusable
import com.example.telepse.ui.viewmodel.LiveTvFoundationViewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LiveTvFoundationScreen(
    onBack: () -> Unit,
    viewModel: LiveTvFoundationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val controlsFocus = remember { FocusRequester() }

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_STOP -> viewModel.onLeave()
                    Lifecycle.Event.ON_START -> viewModel.onEnter()
                    else -> Unit
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.onLeave()
        }
    }

    LaunchedEffect(Unit) {
        controlsFocus.requestInitialTvFocus()
    }

    Box(
        modifier =
            Modifier.fillMaxSize()
                .background(TvAppColors.ScreenBackground)
                .padding(16.dp),
    ) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxSize()) {
            Text(text = "LIVE TV (TVModeEngine)", color = TvAppColors.TextPrimary)
            Text(
                text =
                    buildString {
                        append("Phase: ")
                        append(uiState.phase.name)
                        append(" | Schedule: ")
                        append(uiState.schedule?.scheduleState ?: "—")
                        append(" | Player: ")
                        append(uiState.playerState.name)
                        uiState.target?.videoId?.let { append(" | Video: $it") }
                    },
                color = TvAppColors.TextSecondary,
            )
            uiState.errorMessage?.let {
                Text(text = it, color = androidx.compose.ui.graphics.Color(0xFFFF6B6B))
            }
            if (uiState.phase == TvModeEnginePhase.Loading) {
                CircularProgressIndicator(modifier = Modifier.padding(8.dp))
            }

            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .height(360.dp)
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
                            viewModel.attachWebView(this)
                        }
                    },
                    onRelease = { webView ->
                        viewModel.onWebViewReleased()
                        webView.destroy()
                    },
                )
            }

            TvLazyRow(
                modifier = Modifier.padding(top = 12.dp),
            ) {
                TvNavButton(
                    text = "Back",
                    onClick = onBack,
                    focusRequester = controlsFocus,
                    modifier = Modifier.padding(end = 12.dp),
                )
                TvNavButton(
                    text = "Refresh schedule",
                    onClick = viewModel::refreshSchedule,
                    modifier = Modifier.padding(end = 12.dp),
                )
                TvNavButton(
                    text = "Play",
                    onClick = viewModel::play,
                    modifier = Modifier.padding(end = 12.dp),
                )
                TvNavButton(text = "Pause", onClick = viewModel::pause)
            }
        }
    }
}
