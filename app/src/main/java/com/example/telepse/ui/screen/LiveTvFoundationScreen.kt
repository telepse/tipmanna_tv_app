package com.example.telepse.ui.screen

import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.example.telepse.BuildConfig
import com.example.telepse.livetv.TvModeEnginePhase
import com.example.telepse.ui.animations.TipAnimationOverlay
import com.example.telepse.ui.livetv.LiveTvTipDrawer
import com.example.telepse.ui.livetv.LiveTvTipPreviewStrip
import com.example.telepse.ui.tv.TvAppColors
import com.example.telepse.ui.tv.TvLazyRow
import com.example.telepse.ui.tv.TvPlayerControlChip
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
    val engine = uiState.engine
    val tip = uiState.tip
    val lifecycleOwner = LocalLifecycleOwner.current
    val backFocus = remember { FocusRequester() }
    val scheduleFocus = remember { FocusRequester() }
    val tipsFocus = remember { FocusRequester() }
    val refreshFocus = remember { FocusRequester() }
    val playFocus = remember { FocusRequester() }
    val pauseFocus = remember { FocusRequester() }
    val muteFocus = remember { FocusRequester() }
    val seekFocus = remember { FocusRequester() }
    val firstTipChipFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        viewModel.onEnter()
    }

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_STOP -> viewModel.pauseForBackground()
                    Lifecycle.Event.ON_START -> viewModel.resumeFromBackground()
                    else -> Unit
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.onLeave()
        }
    }

    BackHandler(enabled = tip.showDrawer && (tip.walletPay.isActive || tip.qrPay.isActive)) {
        viewModel.onWalletPayBack()
    }
    BackHandler(enabled = tip.showDrawer && !tip.walletPay.isActive && !tip.qrPay.isActive) {
        viewModel.closeTipDrawer()
    }
    BackHandler(enabled = !tip.showDrawer && uiState.showSchedulePanel) {
        viewModel.toggleSchedulePanel()
    }

    LaunchedEffect(Unit) {
        backFocus.requestInitialTvFocus()
    }

    val liveIndex = engine.target?.scheduleIndex ?: engine.schedule?.livePosition?.scheduleIndex ?: 0
    val title =
        engine.schedule?.items?.getOrNull(liveIndex)?.channel?.title
            ?: engine.target?.videoId
            ?: "LIVE TV"

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
    ) {
        if (engine.phase != TvModeEnginePhase.Empty) {
            // Key changes only via ViewModel.requestPlayerSurfaceRecreate(), not ordinary attach.
            key(viewModel.webViewAttachKey) {
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
        }

        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xE6000000), Color(0x00000000)),
                        ),
                    )
                    .padding(horizontal = 28.dp, vertical = 20.dp),
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LiveTvBadge(text = uiState.scheduleScope.badgeLabel())
                    LiveTvBadge(
                        text = uiState.userStatusLine,
                        accent = TvAppColors.FocusRingSecondary,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                    if (uiState.isMuted) {
                        LiveTvBadge(
                            text = "MUTED",
                            accent = Color(0xFF8899AA),
                            modifier = Modifier.padding(start = 10.dp),
                        )
                    }
                }
                Text(
                    text = title,
                    color = TvAppColors.TextPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 10.dp),
                )
                if (engine.phase == TvModeEnginePhase.Loading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 10.dp),
                            color = TvAppColors.FocusRing,
                            strokeWidth = 2.dp,
                        )
                        Text(
                            text = "Updating schedule…",
                            color = TvAppColors.TextSecondary,
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        }

        if (uiState.showSchedulePanel && !tip.showDrawer) {
            LiveTvSchedulePanel(
                scheduleState = engine.schedule?.scheduleState,
                rows =
                    engine.schedule?.items.orEmpty().mapIndexed { index, item ->
                        index to (item.channel?.title ?: item.id ?: "Row ${index + 1}")
                    },
                liveIndex = liveIndex,
                onSelectIndex = viewModel::playScheduleIndex,
                onClose = viewModel::toggleSchedulePanel,
            )
        }

        if (tip.showDrawer) {
            LiveTvTipDrawer(
                tip = tip,
                onClose = viewModel::closeTipDrawer,
                onIncrement = viewModel::incrementTipQuantity,
                onDecrement = viewModel::decrementTipQuantity,
                onRemove = viewModel::removeTipFromCart,
                onClear = viewModel::clearTipCart,
                onStartWalletCheckout = viewModel::startWalletCheckout,
                onStartQrCheckout = viewModel::startQrCheckout,
                onWalletPinDigit = viewModel::onWalletPinDigit,
                onWalletPinBackspace = viewModel::onWalletPinBackspace,
                onSubmitWalletCheckout = viewModel::submitWalletCheckout,
                onCancelWalletCheckout = viewModel::cancelWalletCheckout,
                onDismissWalletPending = viewModel::dismissWalletPendingRecovery,
                onCancelQrCheckout = viewModel::cancelQrCheckout,
                onAcknowledgeWalletSuccess = viewModel::acknowledgeWalletSuccess,
                onAcknowledgeQrSuccess = viewModel::acknowledgeQrSuccess,
            )
        }

        when (engine.phase) {
            TvModeEnginePhase.Empty -> {
                LiveTvMessageOverlay(
                    title = "Nothing on air",
                    body = engine.errorMessage ?: "No TV-safe content is available right now.",
                )
            }
            TvModeEnginePhase.Error -> {
                LiveTvMessageOverlay(
                    title = "Schedule issue",
                    body =
                        engine.errorMessage
                            ?: "Could not load the schedule. Auto-retry in ~30s or use Refresh.",
                    isError = true,
                )
            }
            else -> Unit
        }

        if (!tip.showDrawer && !uiState.showSchedulePanel) {
            LiveTvTipPreviewStrip(
                tip = tip,
                onSelectCharacter = viewModel::selectTipCharacter,
                onOpenCart = viewModel::openTipDrawer,
                onRetry = viewModel::refreshTipCatalog,
                firstChipFocusRequester = firstTipChipFocus,
                tipsControlFocusRequester = tipsFocus,
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = 96.dp),
            )
        }

        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0x00000000), Color(0xF0000000)),
                        ),
                    )
                    .padding(horizontal = 20.dp, vertical = 24.dp),
        ) {
            TvLazyRow {
                TvPlayerControlChip(
                    label = "Back",
                    onClick = onBack,
                    focusRequester = backFocus,
                    focusChainRight = scheduleFocus,
                )
                TvPlayerControlChip(
                    label = "Schedule",
                    onClick = viewModel::toggleSchedulePanel,
                    focusRequester = scheduleFocus,
                    focusChainLeft = backFocus,
                    focusChainRight = tipsFocus,
                )
                TvPlayerControlChip(
                    label =
                        when {
                            tip.showDrawer -> "Close tips"
                            tip.showStrip && tip.cart.itemCount > 0 ->
                                "Hide tips (${tip.cart.itemCount})"
                            tip.showStrip -> "Hide tips"
                            tip.cart.itemCount > 0 -> "Tips (${tip.cart.itemCount})"
                            else -> "Tips"
                        },
                    onClick = {
                        when {
                            tip.showDrawer -> viewModel.closeTipDrawer()
                            else -> viewModel.toggleTipStrip()
                        }
                    },
                    focusRequester = tipsFocus,
                    focusChainLeft = scheduleFocus,
                    focusChainRight = refreshFocus,
                    focusChainUp = firstTipChipFocus,
                )
                TvPlayerControlChip(
                    label = "Refresh",
                    onClick = viewModel::refreshSchedule,
                    focusRequester = refreshFocus,
                    focusChainLeft = tipsFocus,
                    focusChainRight = playFocus,
                )
                TvPlayerControlChip(
                    label = "Play",
                    onClick = viewModel::play,
                    focusRequester = playFocus,
                    focusChainLeft = refreshFocus,
                    focusChainRight = pauseFocus,
                )
                TvPlayerControlChip(
                    label = "Pause",
                    onClick = viewModel::pause,
                    focusRequester = pauseFocus,
                    focusChainLeft = playFocus,
                    focusChainRight = muteFocus,
                )
                TvPlayerControlChip(
                    label = if (uiState.isMuted) "Unmute" else "Mute",
                    onClick = viewModel::toggleMute,
                    focusRequester = muteFocus,
                    focusChainLeft = pauseFocus,
                    focusChainRight = seekFocus,
                )
                TvPlayerControlChip(
                    label = if (viewModel.canSeekForward()) "+30s" else "+30s (wait)",
                    onClick = viewModel::seekForward30Seconds,
                    focusRequester = seekFocus,
                    focusChainLeft = muteFocus,
                    enabled = viewModel.canSeekForward(),
                )
            }
        }

        TipAnimationOverlay(
            animations = uiState.pendingAnimations,
            onAnimationComplete = viewModel::removeTipAnimation,
        )

        if (BuildConfig.DEBUG) {
            Text(
                text =
                    buildString {
                        append("DBG gen=")
                        append(" phase=")
                        append(engine.phase.name)
                        append(" animQ=")
                        append(uiState.pendingAnimations.size)
                        append(" gen=")
                        append(engine.refreshGeneration)
                        append(" player=")
                        append(engine.playerState.name)
                        append(" pos=")
                        append(engine.playbackPositionSeconds.toInt())
                        engine.target?.videoId?.let { append(" vid=$it") }
                    },
                color = Color(0x88FFFFFF),
                fontSize = 11.sp,
                modifier =
                    Modifier.align(Alignment.BottomEnd)
                        .padding(end = 12.dp, bottom = 88.dp),
            )
        }
    }
}

@Composable
private fun LiveTvSchedulePanel(
    scheduleState: String?,
    rows: List<Pair<Int, String>>,
    liveIndex: Int,
    onSelectIndex: (Int) -> Unit,
    onClose: () -> Unit,
) {
    Box(
        modifier =
            Modifier.fillMaxSize()
                .background(Color(0xE6000000))
                .padding(48.dp),
    ) {
        Column(
            modifier =
                Modifier.align(Alignment.CenterStart)
                    .fillMaxWidth(0.55f)
                    .background(Color(0xFF12121A), RoundedCornerShape(16.dp))
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = "Schedule",
                color = TvAppColors.TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = scheduleState ?: "—",
                color = TvAppColors.TextSecondary,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            if (rows.isEmpty()) {
                Text(text = "No upcoming rows in this response.", color = TvAppColors.TextSecondary)
            } else {
                Text(
                    text = "Select a row with OK to play from the start of that item.",
                    color = TvAppColors.TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                rows.forEach { (index, rowTitle) ->
                    val marker = if (index == liveIndex) "▶ " else "   "
                    TvPlayerControlChip(
                        label = "$marker$rowTitle",
                        onClick = { onSelectIndex(index) },
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            }
            TvPlayerControlChip(
                label = "Close",
                onClick = onClose,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

@Composable
private fun LiveTvBadge(
    text: String,
    modifier: Modifier = Modifier,
    accent: Color = TvAppColors.FocusRing,
) {
    Text(
        text = text.uppercase(),
        color = Color.Black,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier =
            modifier
                .background(accent, RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun LiveTvMessageOverlay(
    title: String,
    body: String,
    isError: Boolean = false,
) {
    Box(
        modifier =
            Modifier.fillMaxSize()
                .background(Color(0xCC000000))
                .padding(48.dp)
                .tvNotFocusable(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                color = if (isError) Color(0xFFFF6B6B) else TvAppColors.TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = body,
                color = TvAppColors.TextSecondary,
                fontSize = 18.sp,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}
