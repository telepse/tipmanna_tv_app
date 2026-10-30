package com.example.telepse.ui.viewmodel

import android.webkit.WebView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.telepse.analytics.TvAnalyticsClient
import com.example.telepse.analytics.TvAnalyticsEventFactory
import com.example.telepse.analytics.TvAnalyticsClipMode
import com.example.telepse.analytics.TvAnalyticsSurfaces
import com.example.telepse.data.auth.SessionExpiryHandler
import com.example.telepse.data.auth.SessionStore
import com.example.telepse.data.repository.WalletOperations
import com.example.telepse.livetv.LiveScheduleScope
import com.example.telepse.livetv.TvModeEngine
import com.example.telepse.livetv.TvModeEnginePhase
import com.example.telepse.livetv.TvModeEngineState
import com.example.telepse.livetv.TvScheduleRefreshSignals
import com.example.telepse.livetv.TvScheduleSource
import com.example.telepse.player.WebViewYouTubePlayerAdapter
import com.example.telepse.player.YouTubePlayerState
import com.example.telepse.tips.LiveTvTipCoordinator
import com.example.telepse.tips.LiveTvTipUiState
import com.example.telepse.tips.LiveTvWalletPayPhase
import com.example.telepse.tips.WalletDebitSubmitSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class LiveTvUiState(
    val engine: TvModeEngineState = TvModeEngineState(),
    val scheduleScope: LiveScheduleScope = LiveScheduleScope.Global,
    val isMuted: Boolean = true,
    val showSchedulePanel: Boolean = false,
    val tip: LiveTvTipUiState = LiveTvTipUiState(),
    val userStatusLine: String = "",
)

@HiltViewModel
class LiveTvFoundationViewModel @Inject constructor(
    scheduleSource: TvScheduleSource,
    refreshSignals: TvScheduleRefreshSignals,
    sessionExpiryHandler: SessionExpiryHandler,
    private val analytics: TvAnalyticsClient,
    walletOperations: WalletOperations,
    debitSubmitSession: WalletDebitSubmitSession,
    private val sessionStore: SessionStore,
) : ViewModel() {

    // Must be initialized before [init]: viewModelScope uses Main.immediate, so the analytics
    // collector coroutine can run during construction and must not observe a null [engine].
    private val adapter = WebViewYouTubePlayerAdapter()
    private val engine =
        TvModeEngine(
            scheduleSource = scheduleSource,
            player = adapter,
            scope = viewModelScope,
            refreshSignals = refreshSignals,
            onAuthenticatedSessionFailure = { sessionExpiryHandler.invalidateSessionIfNeeded(it) },
        )

    private val tipCoordinator =
        LiveTvTipCoordinator(
            walletOperations = walletOperations,
            debitSubmitSession = debitSubmitSession,
            isLoggedIn = { sessionStore.isLoggedIn() },
            scope = viewModelScope,
            onSessionAuthFailure = { sessionExpiryHandler.invalidateSessionIfNeeded(it) },
        )

    init {
        analytics.setContext(surface = TvAnalyticsSurfaces.TV_LOOPER)
        viewModelScope.launch {
            var lastTrackedVideoId: String? = null
            engine.state
                .map { it.target?.videoId to (it.playerReady && it.playerState == YouTubePlayerState.PLAYING) }
                .distinctUntilChanged()
                .collect { (videoId, playing) ->
                    if (playing && videoId != null && videoId != lastTrackedVideoId) {
                        lastTrackedVideoId = videoId
                        val index = engine.state.value.target?.scheduleIndex ?: 0
                        onVideoStarted(videoId, index)
                    }
                }
        }
        viewModelScope.launch {
            engine.state
                .map { it.target?.eventId.orEmpty() }
                .distinctUntilChanged()
                .collect { eventId ->
                    tipCoordinator.onProgrammeEventId(eventId)
                }
        }
        viewModelScope.launch {
            sessionStore.snapshot.collect {
                tipCoordinator.refreshAuthCheckout()
            }
        }
    }

    private val isMuted = MutableStateFlow(true)
    private val showSchedulePanel = MutableStateFlow(false)

    /**
     * Compose remount key for the Live TV [android.webkit.WebView].
     * Incremented only by [requestPlayerSurfaceRecreate] — never by ordinary [attachWebView],
     * which would remount the view on every recomposition and prevent [playerReady].
     */
    private var surfaceGeneration = 0

    val uiState: StateFlow<LiveTvUiState> =
        combine(
            engine.state,
            isMuted,
            showSchedulePanel,
            tipCoordinator.state,
        ) { engineState, muted, scheduleOpen, tip ->
            LiveTvUiState(
                engine = engineState,
                scheduleScope = LiveScheduleScope.Global,
                isMuted = muted,
                showSchedulePanel = scheduleOpen,
                tip = tip,
                userStatusLine = buildUserStatusLine(engineState),
            )
        // Eager: tip cart/strip must stay synced without depending on Compose collectors.
        }.stateIn(viewModelScope, SharingStarted.Eagerly, LiveTvUiState())

    val webViewAttachKey: Int get() = surfaceGeneration

    fun onEnter() {
        engine.start()
    }

    fun onLeave() {
        showSchedulePanel.value = false
        tipCoordinator.reset()
        engine.stop()
    }

    fun pauseForBackground() {
        engine.pauseForBackground()
    }

    fun resumeFromBackground() {
        engine.resumeFromBackground()
    }

    fun playScheduleIndex(index: Int) {
        showSchedulePanel.value = false
        tipCoordinator.closeDrawer()
        engine.playScheduleIndex(index)
    }

    fun attachWebView(webView: WebView) {
        engine.attachWebView(webView)
        isMuted.value = true
    }

    /**
     * Explicit hard reset: bump the Compose key (so [AndroidView] remounts) and tear down the
     * current player surface. Ordinary attachment must not call this.
     */
    fun requestPlayerSurfaceRecreate() {
        surfaceGeneration++
        engine.recreatePlayerAfterError()
    }

    fun onWebViewReleased() {
        engine.onWebViewReleased()
    }

    fun refreshSchedule() {
        engine.refreshSchedule()
    }

    fun toggleSchedulePanel() {
        if (!showSchedulePanel.value) {
            tipCoordinator.closeDrawer()
        }
        showSchedulePanel.value = !showSchedulePanel.value
    }

    fun toggleTipStrip() {
        tipCoordinator.toggleStrip()
    }

    fun openTipDrawer() {
        showSchedulePanel.value = false
        tipCoordinator.openDrawer()
    }

    fun closeTipDrawer() {
        tipCoordinator.closeDrawer()
    }

    fun selectTipCharacter(characterId: String) {
        showSchedulePanel.value = false
        tipCoordinator.selectCharacter(characterId)
    }

    fun incrementTipQuantity(characterId: String) {
        tipCoordinator.increment(characterId)
    }

    fun decrementTipQuantity(characterId: String) {
        tipCoordinator.decrement(characterId)
    }

    fun removeTipFromCart(characterId: String) {
        tipCoordinator.removeFromCart(characterId)
    }

    fun clearTipCart() {
        tipCoordinator.clearCart()
    }

    fun refreshTipCatalog() {
        tipCoordinator.refreshCatalog()
    }

    fun startWalletCheckout() {
        tipCoordinator.startWalletCheckout()
    }

    fun onWalletPinDigit(digit: Char) {
        tipCoordinator.onWalletPinDigit(digit)
    }

    fun onWalletPinBackspace() {
        tipCoordinator.onWalletPinBackspace()
    }

    fun submitWalletCheckout() {
        tipCoordinator.submitWalletCheckout()
    }

    fun cancelWalletCheckout() {
        tipCoordinator.cancelWalletCheckout(keepPending = true)
    }

    fun dismissWalletPendingRecovery() {
        tipCoordinator.dismissWalletPendingRecovery()
    }

    fun acknowledgeWalletSuccess() {
        tipCoordinator.acknowledgeWalletSuccess()
    }

    /** Back from PIN/payment overlay without leaving LIVE TV or remounting WebView. */
    fun onWalletPayBack() {
        val phase = tipCoordinator.state.value.walletPay.phase
        when (phase) {
            LiveTvWalletPayPhase.Submitting -> Unit
            LiveTvWalletPayPhase.Success -> tipCoordinator.acknowledgeWalletSuccess()
            LiveTvWalletPayPhase.Idle -> tipCoordinator.closeDrawer()
            else -> tipCoordinator.cancelWalletCheckout(keepPending = true)
        }
    }

    fun play() {
        if (!engine.state.value.playerReady) {
            engine.refreshSchedule(showLoading = false)
        }
        engine.play()
        trackInteraction("play", null)
    }

    fun pause() {
        engine.pause()
        trackInteraction("pause", null)
    }

    fun toggleMute() {
        if (isMuted.value) {
            adapter.unmute()
            isMuted.value = false
        } else {
            adapter.mute()
            isMuted.value = true
        }
    }

    fun seekForward30Seconds() {
        engine.seekRelative(30f)
    }

    fun canSeekForward(): Boolean {
        val s = engine.state.value
        return s.playerReady && s.phase == TvModeEnginePhase.Ready
    }

    private fun buildUserStatusLine(engineState: TvModeEngineState): String {
        return when (engineState.phase) {
            TvModeEnginePhase.Loading -> "Syncing schedule…"
            TvModeEnginePhase.Error -> "Schedule unavailable"
            TvModeEnginePhase.Empty -> "No content on air"
            TvModeEnginePhase.Ready -> playbackStatusLabel(engineState)
            TvModeEnginePhase.Idle -> "Starting…"
        }
    }

    private fun playbackStatusLabel(engineState: TvModeEngineState): String {
        val playback =
            when (engineState.playerState) {
                YouTubePlayerState.PLAYING -> "Playing"
                YouTubePlayerState.PAUSED -> "Paused"
                YouTubePlayerState.BUFFERING -> "Buffering"
                YouTubePlayerState.ENDED -> "Ended"
                YouTubePlayerState.UNSTARTED -> "Ready"
                else -> if (engineState.playerReady) "Ready" else "Loading player"
            }
        return if (engineState.lastScheduleRefreshSucceeded) {
            "$playback · Schedule OK"
        } else {
            playback
        }
    }

    fun onVideoStarted(itemId: String, scheduleIndex: Int) {
        analytics.track(
            TvAnalyticsEventFactory.videoStart(
                ts = System.currentTimeMillis(),
                itemId = itemId,
                scheduleIndex = scheduleIndex,
                mode = TvAnalyticsClipMode.GLOBAL,
            ),
        )
    }

    private fun trackInteraction(kind: String, detail: String?) {
        analytics.track(
            TvAnalyticsEventFactory.userInteraction(
                ts = System.currentTimeMillis(),
                kind = kind,
                detail = detail,
            ),
        )
    }

    override fun onCleared() {
        analytics.flushAsync()
        tipCoordinator.reset()
        engine.dispose()
        super.onCleared()
    }
}
