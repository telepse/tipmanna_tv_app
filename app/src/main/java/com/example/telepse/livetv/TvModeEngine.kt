package com.example.telepse.livetv

import android.webkit.WebView
import com.example.telepse.player.YouTubePlayerAdapter
import com.example.telepse.player.YouTubePlayerEvent
import com.example.telepse.player.YouTubePlayerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Coordinates global LIVE TV schedule reads and a single [YouTubePlayerAdapter] instance.
 * UI layers attach WebView and observe [state]; they must call [stop] when leaving the surface.
 */
class TvModeEngine(
    private val scheduleSource: TvScheduleSource,
    private val player: YouTubePlayerAdapter,
    private val scope: CoroutineScope,
    private val refreshSignals: TvScheduleRefreshSignals? = null,
    private val debounceMs: Long = DEFAULT_DEBOUNCE_MS,
    /** Null disables background polling (unit tests). Production uses [POLL_INTERVAL_MS]. */
    private val pollIntervalMs: Long? = POLL_INTERVAL_MS,
    /** Null disables automatic retry after schedule errors (unit tests). */
    private val errorRecoveryDelayMs: Long? = ERROR_RECOVERY_DELAY_MS,
    /**
     * Invoked on schedule fetch failure; return true when local session was cleared (authenticated 401).
     * Skips automatic error-recovery polling to avoid 401 loops.
     */
    private val onAuthenticatedSessionFailure: ((Throwable) -> Boolean)? = null,
) {
    private val _state = MutableStateFlow(TvModeEngineState())
    val state: StateFlow<TvModeEngineState> = _state.asStateFlow()

    private var refreshJob: Job? = null
    private var debounceJob: Job? = null
    private var playerEventsJob: Job? = null
    private var refreshSignalsJob: Job? = null
    private var pollJob: Job? = null
    private var refreshGeneration = 0L
    private var lastScheduleEtag: String? = null
    private var errorRecoveryJob: Job? = null
    private var endedAdvanceJob: Job? = null
    private var lastEndedHandledAtMs: Long = 0L

    val playerAdapter: YouTubePlayerAdapter get() = player

    fun start() {
        if (_state.value.isActive) return
        _state.update { it.copy(isActive = true) }
        playerEventsJob?.cancel()
        playerEventsJob =
            scope.launch {
                player.events.collect { event ->
                    if (!_state.value.isActive) return@collect
                    when (event) {
                        is YouTubePlayerEvent.PlayerReady -> {
                            _state.update { it.copy(playerReady = true) }
                            syncPlaybackFromTarget(force = false)
                            resumePlaybackIfReady()
                        }
                        is YouTubePlayerEvent.StateChanged -> {
                            _state.update { s -> s.copy(playerState = event.state) }
                            if (event.state == YouTubePlayerState.ENDED && _state.value.isActive) {
                                handlePlaybackEnded()
                            }
                        }
                        is YouTubePlayerEvent.CurrentTime -> {
                            _state.update { it.copy(playbackPositionSeconds = event.seconds) }
                        }
                        is YouTubePlayerEvent.Error -> {
                            if (_state.value.isActive && !backgroundRefreshSuspended()) {
                                requestScheduleRefreshDebounced(fromRealtime = false)
                            }
                        }
                        else -> Unit
                    }
                }
            }
        refreshSignalsJob?.cancel()
        refreshSignalsJob =
            refreshSignals?.let { signals ->
                scope.launch {
                    try {
                        signals.startListening(this)
                        signals.triggers.collect {
                            if (_state.value.isActive && !backgroundRefreshSuspended()) {
                                requestScheduleRefreshDebounced(fromRealtime = true)
                            }
                        }
                    } finally {
                        signals.stopListening()
                    }
                }
            }
        if (!backgroundRefreshSuspended()) {
            refreshSchedule(showLoading = true)
            startPeriodicPollingIfNeeded()
        }
    }

    fun stop() {
        _state.update { it.copy(isActive = false) }
        debounceJob?.cancel()
        debounceJob = null
        pollJob?.cancel()
        pollJob = null
        refreshJob?.cancel()
        refreshJob = null
        errorRecoveryJob?.cancel()
        errorRecoveryJob = null
        refreshGeneration++
        refreshSignalsJob?.cancel()
        refreshSignalsJob = null
        playerEventsJob?.cancel()
        playerEventsJob = null
        endedAdvanceJob?.cancel()
        endedAdvanceJob = null
        player.pause()
    }

    fun dispose() {
        stop()
        errorRecoveryJob?.cancel()
        errorRecoveryJob = null
        playerEventsJob?.cancel()
        playerEventsJob = null
        player.destroy()
        _state.value = TvModeEngineState()
    }

    fun attachWebView(webView: WebView) {
        player.attachWebView(webView)
        _state.update {
            it.copy(
                playerReady = false,
                playerState = YouTubePlayerState.UNKNOWN,
                playbackPositionSeconds = 0f,
            )
        }
        syncPlaybackFromTarget(force = true)
    }

    fun onWebViewReleased() {
        player.destroy()
        _state.update {
            it.copy(
                playerReady = false,
                playerState = YouTubePlayerState.UNKNOWN,
                loadedVideoId = null,
                loadedStartSeconds = null,
                playbackPositionSeconds = 0f,
            )
        }
    }

    fun recreatePlayerAfterError() {
        onWebViewReleased()
    }

    /**
     * User-initiated or explicit recovery fetch. Clears session-expired hold only after a successful
     * schedule response (see [completeSessionExpiredRecoveryIfNeeded]).
     */
    fun refreshSchedule(showLoading: Boolean = true) {
        if (!_state.value.isActive) return
        debounceJob?.cancel()
        debounceJob = null
        executeScheduleRefresh(showLoading)
    }

    fun requestScheduleRefreshDebounced(fromRealtime: Boolean = true) {
        if (!_state.value.isActive || backgroundRefreshSuspended()) return
        debounceJob?.cancel()
        debounceJob =
            scope.launch {
                delay(debounceMs)
                if (!_state.value.isActive || backgroundRefreshSuspended()) return@launch
                executeScheduleRefresh(showLoading = false)
            }
    }

    private fun executeScheduleRefresh(showLoading: Boolean) {
        refreshJob?.cancel()
        val generation = ++refreshGeneration
        refreshJob =
            scope.launch {
                if (showLoading) {
                    _state.update {
                        it.copy(
                            phase = TvModeEnginePhase.Loading,
                            errorMessage = null,
                            refreshGeneration = generation,
                        )
                    }
                }
                val result = scheduleSource.fetchGlobalSchedule(lastScheduleEtag)
                if (!isGenerationCurrent(generation)) return@launch
                result
                    .onSuccess { outcome ->
                        errorRecoveryJob?.cancel()
                        errorRecoveryJob = null
                        outcome.etag?.let { lastScheduleEtag = it }
                        if (outcome.notModified) {
                            _state.update { s ->
                                val nextPhase =
                                    if (s.phase == TvModeEnginePhase.Loading && s.schedule != null) {
                                        TvModeEnginePhase.Ready
                                    } else {
                                        s.phase
                                    }
                                s.copy(phase = nextPhase, refreshGeneration = generation)
                            }
                            completeSessionExpiredRecoveryIfNeeded()
                            return@launch
                        }
                        val schedule =
                            outcome.schedule
                                ?: run {
                                    _state.update {
                                        it.copy(
                                            phase = TvModeEnginePhase.Error,
                                            errorMessage = "Schedule response missing body",
                                            refreshGeneration = generation,
                                        )
                                    }
                                    return@launch
                                }
                        if (schedule.scheduleState == "NO_SAFE_CONTENT" && schedule.items.isEmpty()) {
                            _state.update {
                                it.copy(
                                    phase = TvModeEnginePhase.Empty,
                                    schedule = schedule,
                                    target = null,
                                    errorMessage =
                                        schedule.message ?: "No TV-safe content is currently available.",
                                    refreshGeneration = generation,
                                )
                            }
                            return@launch
                        }
                        LiveScheduleTargetResolver.resolve(schedule)
                            .onSuccess { target ->
                                _state.update {
                                    it.copy(
                                        phase = TvModeEnginePhase.Ready,
                                        schedule = schedule,
                                        target = target,
                                        errorMessage = null,
                                        refreshGeneration = generation,
                                        lastScheduleRefreshSucceeded = true,
                                    )
                                }
                                completeSessionExpiredRecoveryIfNeeded()
                                syncPlaybackFromTarget(force = false)
                                resumePlaybackIfReady()
                            }
                            .onFailure { e ->
                                _state.update {
                                    it.copy(
                                        phase = TvModeEnginePhase.Error,
                                        schedule = schedule,
                                        target = null,
                                        errorMessage = e.message ?: "Unplayable schedule row",
                                        refreshGeneration = generation,
                                    )
                                }
                            }
                    }
                    .onFailure { e ->
                        val sessionCleared = onAuthenticatedSessionFailure?.invoke(e) == true
                        if (sessionCleared) {
                            enterSessionExpiredHold()
                        }
                        _state.update {
                            it.copy(
                                phase = TvModeEnginePhase.Error,
                                errorMessage = mapScheduleError(e),
                                refreshGeneration = generation,
                                lastScheduleRefreshSucceeded = false,
                            )
                        }
                        if (!sessionCleared) {
                            scheduleErrorRecoveryRetry(generation)
                        }
                    }
            }
    }

    private fun backgroundRefreshSuspended(): Boolean =
        _state.value.scheduleBackgroundRefreshSuspended

    private fun enterSessionExpiredHold() {
        debounceJob?.cancel()
        debounceJob = null
        errorRecoveryJob?.cancel()
        errorRecoveryJob = null
        pollJob?.cancel()
        pollJob = null
        _state.update { it.copy(scheduleBackgroundRefreshSuspended = true) }
    }

    /** Successful schedule fetch after authenticated session expiry — resume background refresh. */
    private fun completeSessionExpiredRecoveryIfNeeded() {
        if (!_state.value.scheduleBackgroundRefreshSuspended) return
        _state.update { it.copy(scheduleBackgroundRefreshSuspended = false) }
        startPeriodicPollingIfNeeded()
    }

    private fun startPeriodicPollingIfNeeded() {
        if (backgroundRefreshSuspended()) return
        pollJob?.cancel()
        pollJob =
            pollIntervalMs?.let { intervalMs ->
                scope.launch {
                    while (isActive && _state.value.isActive) {
                        delay(intervalMs)
                        if (_state.value.isActive && !backgroundRefreshSuspended()) {
                            executeScheduleRefresh(showLoading = false)
                        }
                    }
                }
            }
    }

    private fun scheduleErrorRecoveryRetry(scheduleGeneration: Long) {
        if (backgroundRefreshSuspended()) return
        val delayMs = errorRecoveryDelayMs ?: return
        errorRecoveryJob?.cancel()
        errorRecoveryJob =
            scope.launch {
                delay(delayMs)
                if (!isGenerationCurrent(scheduleGeneration)) return@launch
                if (_state.value.phase != TvModeEnginePhase.Error || !_state.value.isActive) {
                    return@launch
                }
                executeScheduleRefresh(showLoading = false)
            }
    }

    private fun resumePlaybackIfReady() {
        val snapshot = _state.value
        if (!snapshot.isActive || snapshot.target == null || !snapshot.playerReady) return
        player.play()
    }

    fun play() {
        if (_state.value.isActive) player.play()
    }

    fun pause() {
        if (_state.value.isActive) player.pause()
    }

    /** Activity backgrounding — keep engine active so WebView + event collector stay wired. */
    fun pauseForBackground() {
        if (!_state.value.isActive) return
        pollJob?.cancel()
        pollJob = null
        player.pause()
    }

    fun resumeFromBackground() {
        if (!_state.value.isActive) return
        resumePlaybackIfReady()
        startPeriodicPollingIfNeeded()
    }

    fun playScheduleIndex(index: Int) {
        if (!_state.value.isActive) return
        val schedule = _state.value.schedule ?: return
        LiveScheduleTargetResolver.resolveAtIndex(schedule, index, 0f)
            .onSuccess { target ->
                _state.update {
                    it.copy(
                        target = target,
                        phase = TvModeEnginePhase.Ready,
                        errorMessage = null,
                    )
                }
                syncPlaybackFromTarget(force = true)
            }
            .onFailure { e ->
                _state.update {
                    it.copy(
                        errorMessage = e.message ?: "Could not play that schedule row.",
                    )
                }
            }
    }

    fun seekRelative(secondsDelta: Float) {
        if (!_state.value.isActive || !_state.value.playerReady) return
        val snapshot = _state.value
        val base =
            if (snapshot.playbackPositionSeconds > 0f) {
                snapshot.playbackPositionSeconds
            } else {
                snapshot.loadedStartSeconds ?: 0f
            }
        player.seekTo(base + secondsDelta)
        player.requestCurrentTime()
    }

    internal fun handlePlaybackEndedForTest() {
        handlePlaybackEnded()
    }

    private fun handlePlaybackEnded() {
        val now = System.currentTimeMillis()
        if (now - lastEndedHandledAtMs < 2_000L) return
        lastEndedHandledAtMs = now
        endedAdvanceJob?.cancel()
        endedAdvanceJob =
            scope.launch {
                val snapshot = _state.value
                val schedule = snapshot.schedule ?: run {
                    if (!backgroundRefreshSuspended()) {
                        requestScheduleRefreshDebounced(fromRealtime = false)
                    }
                    return@launch
                }
                val currentIndex = snapshot.target?.scheduleIndex ?: schedule.livePosition?.scheduleIndex ?: 0
                val nextIndex =
                    LiveScheduleQueueAdvance.nextIndexAfterEnd(currentIndex, schedule.items.size)
                if (nextIndex == null) {
                    if (!backgroundRefreshSuspended()) {
                        requestScheduleRefreshDebounced(fromRealtime = false)
                    }
                    return@launch
                }
                LiveScheduleTargetResolver.resolveAtIndex(schedule, nextIndex, 0f)
                    .onSuccess { target ->
                        _state.update {
                            it.copy(target = target, errorMessage = null)
                        }
                        syncPlaybackFromTarget(force = true)
                    }
                    .onFailure {
                        if (!backgroundRefreshSuspended()) {
                            requestScheduleRefreshDebounced(fromRealtime = false)
                        }
                    }
            }
    }

    private fun mapScheduleError(e: Throwable): String = LiveScheduleUserMessages.fromThrowable(e)

    private fun isGenerationCurrent(generation: Long): Boolean {
        return generation == refreshGeneration && _state.value.isActive
    }

    /**
     * Loads or seeks only when the live target identity differs from what is already loaded.
     */
    internal fun syncPlaybackFromTarget(force: Boolean) {
        val snapshot = _state.value
        if (!snapshot.isActive) return
        val target = snapshot.target ?: return
        if (!force &&
            snapshot.loadedVideoId == target.videoId &&
            snapshot.loadedStartSeconds == target.startSeconds
        ) {
            return
        }
        if (snapshot.loadedVideoId == target.videoId &&
            snapshot.loadedStartSeconds != target.startSeconds
        ) {
            player.seekTo(target.startSeconds)
            _state.update { it.copy(loadedStartSeconds = target.startSeconds) }
            return
        }
        player.load(target.videoId, target.startSeconds)
        _state.update {
            it.copy(
                loadedVideoId = target.videoId,
                loadedStartSeconds = target.startSeconds,
            )
        }
        resumePlaybackIfReady()
    }

    companion object {
        const val DEFAULT_DEBOUNCE_MS = 750L
        const val POLL_INTERVAL_MS = 90_000L
        const val ERROR_RECOVERY_DELAY_MS = 30_000L
    }
}
