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
                        }
                        is YouTubePlayerEvent.StateChanged ->
                            _state.update { s -> s.copy(playerState = event.state) }
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
                            if (_state.value.isActive) {
                                requestScheduleRefreshDebounced(fromRealtime = true)
                            }
                        }
                    } finally {
                        signals.stopListening()
                    }
                }
            }
        refreshSchedule(showLoading = true)
        pollJob?.cancel()
        pollJob =
            pollIntervalMs?.let { intervalMs ->
                scope.launch {
                    while (isActive && _state.value.isActive) {
                        delay(intervalMs)
                        if (_state.value.isActive) {
                            executeScheduleRefresh(showLoading = false)
                        }
                    }
                }
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
        refreshGeneration++
        refreshSignalsJob?.cancel()
        refreshSignalsJob = null
        playerEventsJob?.cancel()
        playerEventsJob = null
        player.pause()
    }

    fun dispose() {
        stop()
        playerEventsJob?.cancel()
        playerEventsJob = null
        player.destroy()
        _state.value = TvModeEngineState()
    }

    fun attachWebView(webView: WebView) {
        player.attachWebView(webView)
        _state.update { it.copy(playerReady = false, playerState = YouTubePlayerState.UNKNOWN) }
        syncPlaybackFromTarget(force = false)
    }

    fun onWebViewReleased() {
        player.destroy()
        _state.update {
            it.copy(
                playerReady = false,
                playerState = YouTubePlayerState.UNKNOWN,
            )
        }
    }

    fun refreshSchedule(showLoading: Boolean = true) {
        if (!_state.value.isActive) return
        debounceJob?.cancel()
        debounceJob = null
        executeScheduleRefresh(showLoading)
    }

    fun requestScheduleRefreshDebounced(fromRealtime: Boolean = true) {
        if (!_state.value.isActive) return
        debounceJob?.cancel()
        debounceJob =
            scope.launch {
                delay(debounceMs)
                if (!_state.value.isActive) return@launch
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
                        outcome.etag?.let { lastScheduleEtag = it }
                        if (outcome.notModified) {
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
                                    )
                                }
                                syncPlaybackFromTarget(force = false)
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
                        _state.update {
                            it.copy(
                                phase = TvModeEnginePhase.Error,
                                errorMessage = e.message ?: "Schedule load failed",
                                refreshGeneration = generation,
                            )
                        }
                    }
            }
    }

    fun play() {
        if (_state.value.isActive) player.play()
    }

    fun pause() {
        if (_state.value.isActive) player.pause()
    }

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
    }

    companion object {
        const val DEFAULT_DEBOUNCE_MS = 750L
        const val POLL_INTERVAL_MS = 90_000L
    }
}
