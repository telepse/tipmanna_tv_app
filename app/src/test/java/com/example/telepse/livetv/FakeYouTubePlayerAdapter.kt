package com.example.telepse.livetv

import com.example.telepse.player.YouTubePlayerAdapter
import com.example.telepse.player.YouTubePlayerEvent
import com.example.telepse.player.YouTubePlayerState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class FakeYouTubePlayerAdapter : YouTubePlayerAdapter {
    private val _events = MutableSharedFlow<YouTubePlayerEvent>(extraBufferCapacity = 16)
    override val events: SharedFlow<YouTubePlayerEvent> = _events.asSharedFlow()

    val loadCalls = mutableListOf<Pair<String, Float>>()
    val seekCalls = mutableListOf<Float>()
    val playCalls = mutableListOf<Unit>()
    var ready = true

    override fun attachWebView(webView: android.webkit.WebView) {}

    override fun load(videoId: String, startSeconds: Float) {
        loadCalls.add(videoId to startSeconds)
        if (ready) {
            _events.tryEmit(YouTubePlayerEvent.PlayerReady)
        }
    }

    override fun cue(videoId: String, startSeconds: Float) {
        load(videoId, startSeconds)
    }

    override fun play() {
        playCalls.add(Unit)
    }

    fun emitState(state: YouTubePlayerState) {
        _events.tryEmit(YouTubePlayerEvent.StateChanged(state))
    }

    fun emitError(code: Int = 150) {
        _events.tryEmit(YouTubePlayerEvent.Error(code))
    }
    override fun pause() {}
    override fun seekTo(seconds: Float) {
        seekCalls.add(seconds)
    }

    override fun mute() {}
    override fun unmute() {}
    override fun requestCurrentTime() {}
    override fun requestDuration() {}
    override fun state(): YouTubePlayerState = YouTubePlayerState.PLAYING
    override fun destroy() {}
    override fun recreatePlayerSurface(webView: android.webkit.WebView) {}
}
