package com.example.telepse.player

import android.webkit.WebView
import kotlinx.coroutines.flow.SharedFlow

/**
 * Platform-neutral control surface for Tipmanna TV orchestration (TVModeEngine).
 * Implementations must not leak WebView types to callers outside the player module.
 */
interface YouTubePlayerAdapter {
    val events: SharedFlow<YouTubePlayerEvent>

    /** Bind or recreate the underlying view; idempotent per instance. */
    fun attachWebView(webView: WebView)

    fun load(videoId: String, startSeconds: Float = 0f)
    fun cue(videoId: String, startSeconds: Float = 0f)
    fun play()
    fun pause()
    fun seekTo(seconds: Float)
    fun mute()
    fun unmute()
    fun requestCurrentTime()
    fun requestDuration()
    fun state(): YouTubePlayerState
    fun destroy()

    /** For spike / diagnostics only. */
    fun recreatePlayerSurface(webView: WebView)
}
