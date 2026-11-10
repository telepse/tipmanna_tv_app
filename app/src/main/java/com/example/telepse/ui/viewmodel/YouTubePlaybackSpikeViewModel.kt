package com.example.telepse.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.telepse.player.TipmannaSpikeVideoIds
import com.example.telepse.player.WebViewYouTubePlayerAdapter
import com.example.telepse.player.YouTubePlayerEvent
import com.example.telepse.player.YouTubePlayerState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class YouTubeSpikeUiState(
    val videoId: String = TipmannaSpikeVideoIds.SCHEDULE_LIVE_POSITION_SEED,
    val altVideoId: String = TipmannaSpikeVideoIds.AXIS_GAP_FIXTURE,
    val playerState: YouTubePlayerState = YouTubePlayerState.UNKNOWN,
    val lastError: Int? = null,
    val logLines: List<String> = emptyList(),
    val loadStartedMs: Long? = null,
    val readyMs: Long? = null,
    val playingMs: Long? = null,
    val currentTimeSec: Float? = null,
    val durationSec: Float? = null,
)

@HiltViewModel
class YouTubePlaybackSpikeViewModel @Inject constructor() : ViewModel() {

    val adapter: WebViewYouTubePlayerAdapter = WebViewYouTubePlayerAdapter()

    private val _uiState = MutableStateFlow(YouTubeSpikeUiState())
    val uiState: StateFlow<YouTubeSpikeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            adapter.events.collect { event ->
                when (event) {
                    is YouTubePlayerEvent.PlayerReady -> {
                        val now = System.currentTimeMillis()
                        appendLog("READY")
                        _uiState.update { it.copy(readyMs = it.readyMs ?: now) }
                    }
                    is YouTubePlayerEvent.StateChanged -> {
                        appendLog("STATE ${event.state.name}")
                        _uiState.update { state ->
                            val playingMs =
                                if (event.state == YouTubePlayerState.PLAYING && state.playingMs == null) {
                                    System.currentTimeMillis()
                                } else {
                                    state.playingMs
                                }
                            state.copy(playerState = event.state, playingMs = playingMs)
                        }
                    }
                    is YouTubePlayerEvent.Error -> {
                        appendLog("ERROR code=${event.code}")
                        _uiState.update { it.copy(lastError = event.code) }
                    }
                    is YouTubePlayerEvent.CurrentTime ->
                        _uiState.update { it.copy(currentTimeSec = event.seconds) }
                    is YouTubePlayerEvent.Duration ->
                        _uiState.update { it.copy(durationSec = event.seconds) }
                    is YouTubePlayerEvent.Metric -> {
                        appendLog("${event.epochMs} METRIC ${event.name} ${event.detail}")
                        when (event.name) {
                            "bootstrap_html", "webview_created" -> {
                                _uiState.update {
                                    it.copy(loadStartedMs = it.loadStartedMs ?: event.epochMs)
                                }
                            }
                            "iframe_api_ready" -> appendLog("iframe API ready")
                            "state_playing" -> {
                                _uiState.update { state ->
                                    state.copy(
                                        playingMs = state.playingMs ?: event.epochMs,
                                    )
                                }
                            }
                        }
                    }
                    YouTubePlayerEvent.ApiReady -> appendLog("API_READY")
                }
            }
        }
    }

    fun onWebViewCreated() {
        appendLog("${System.currentTimeMillis()} WebView created — press Load")
    }

    fun logLifecycle(phase: String) {
        appendLog("${System.currentTimeMillis()} lifecycle_$phase")
    }

    fun logNetwork(status: String) {
        appendLog("${System.currentTimeMillis()} network_$status")
    }

    fun startInitialLoad() {
        val id = _uiState.value.videoId
        _uiState.update { it.copy(loadStartedMs = System.currentTimeMillis(), lastError = null) }
        appendLog("load($id) muted autoplay")
        adapter.load(id, startSeconds = 0f)
    }

    fun cueCurrent() {
        adapter.cue(_uiState.value.videoId, 0f)
    }

    fun play() = adapter.play()
    fun pause() = adapter.pause()
    fun seek30() = adapter.seekTo(30f)
    fun mute() = adapter.mute()
    fun unmute() = adapter.unmute()
    fun swapVideo() {
        val next =
            if (_uiState.value.videoId == TipmannaSpikeVideoIds.SCHEDULE_LIVE_POSITION_SEED) {
                TipmannaSpikeVideoIds.AXIS_GAP_FIXTURE
            } else {
                TipmannaSpikeVideoIds.SCHEDULE_LIVE_POSITION_SEED
            }
        _uiState.update { it.copy(videoId = next) }
        appendLog("swap load($next)")
        adapter.load(next, 0f)
    }

    fun pollTime() {
        adapter.requestCurrentTime()
        adapter.requestDuration()
    }

    fun clearLog() {
        _uiState.update { it.copy(logLines = emptyList()) }
    }

    private fun appendLog(line: String) {
        _uiState.update { it.copy(logLines = it.logLines.takeLast(80) + line) }
    }

    override fun onCleared() {
        adapter.destroy()
        super.onCleared()
    }
}
