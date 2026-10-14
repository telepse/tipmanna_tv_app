package com.example.telepse.player

import android.webkit.JavascriptInterface

class YouTubePlayerBridge(
    private val onIframeApiReadyCallback: () -> Unit,
    private val onPlayerReadyCallback: () -> Unit,
    private val onStateChangeCallback: (Int) -> Unit,
    private val onErrorCallback: (Int) -> Unit,
    private val onVideoLoadedCallback: (String) -> Unit,
) {
    @JavascriptInterface
    fun onIframeApiReady() {
        onIframeApiReadyCallback()
    }

    @JavascriptInterface
    fun onPlayerReady() {
        onPlayerReadyCallback()
    }

    @JavascriptInterface
    fun onStateChange(state: Int) {
        onStateChangeCallback(state)
    }

    @JavascriptInterface
    fun onError(code: Int) {
        onErrorCallback(code)
    }

    @JavascriptInterface
    fun onVideoLoaded(videoId: String) {
        onVideoLoadedCallback(videoId)
    }
}
