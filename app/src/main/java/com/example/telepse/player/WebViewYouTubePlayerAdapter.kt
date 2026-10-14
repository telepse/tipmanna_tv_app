package com.example.telepse.player

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * YouTube IFrame Player API behind WebView — isolated implementation of [YouTubePlayerAdapter].
 */
class WebViewYouTubePlayerAdapter : YouTubePlayerAdapter {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val _events = MutableSharedFlow<YouTubePlayerEvent>(extraBufferCapacity = 64)
    override val events: SharedFlow<YouTubePlayerEvent> = _events.asSharedFlow()

    private var webView: WebView? = null
    private var bridge: YouTubePlayerBridge? = null
    private var lastState = YouTubePlayerState.UNKNOWN
    private var playerReady = false
    private var pendingVideoId: String? = null
    private var pendingStart = 0f
    private var pendingCue = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun attachWebView(webView: WebView) {
        this.webView = webView
        emitMetric("webview_created", "attached")
        emitMetric("iframe_api_requested", YoutubeIframeHtml.IFRAME_API_SCRIPT)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            cacheMode = WebSettings.LOAD_DEFAULT
        }
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                emitMetric("page_finished", url ?: "")
            }
        }
        bridge = YouTubePlayerBridge(
            onIframeApiReadyCallback = {
                emitMetric("iframe_api_ready", "")
                _events.tryEmit(YouTubePlayerEvent.ApiReady)
            },
            onPlayerReadyCallback = {
                playerReady = true
                emitMetric("player_ready", "")
                _events.tryEmit(YouTubePlayerEvent.PlayerReady)
                pendingVideoId?.let { id ->
                    if (pendingCue) cue(id, pendingStart) else load(id, pendingStart)
                    pendingVideoId = null
                }
            },
            onStateChangeCallback = { code ->
                lastState = YouTubePlayerState.fromYtCode(code)
                emitMetric("state_${lastState.name.lowercase()}", "")
                _events.tryEmit(YouTubePlayerEvent.StateChanged(lastState))
            },
            onErrorCallback = { code ->
                _events.tryEmit(YouTubePlayerEvent.Error(code))
                emitMetric("player_error", "code=$code")
            },
            onVideoLoadedCallback = { videoId ->
                emitMetric("video_loaded", videoId)
            },
        )
        webView.addJavascriptInterface(bridge!!, "TipmannaBridge")
    }

    override fun load(videoId: String, startSeconds: Float) {
        if (!playerReady) {
            pendingVideoId = videoId
            pendingStart = startSeconds
            pendingCue = false
            bootstrapWebView(videoId, autoplay = true, mute = true, startSeconds)
            return
        }
        evalJs("loadVideoById('${escapeJs(videoId)}', ${startSeconds.toInt()});")
        emitMetric("load", videoId)
    }

    override fun cue(videoId: String, startSeconds: Float) {
        if (!playerReady) {
            pendingVideoId = videoId
            pendingStart = startSeconds
            pendingCue = true
            bootstrapWebView(videoId, autoplay = false, mute = true, startSeconds)
            return
        }
        evalJs("cueVideoById('${escapeJs(videoId)}', ${startSeconds.toInt()});")
        emitMetric("cue", videoId)
    }

    override fun play() {
        evalJs("playVideo();")
        emitMetric("play", "")
    }

    override fun pause() {
        evalJs("pauseVideo();")
        emitMetric("pause", "")
    }

    override fun seekTo(seconds: Float) {
        emitMetric("seek_requested", seconds.toString())
        evalJs("seekTo(${seconds});")
        mainHandler.postDelayed({
            webView?.evaluateJavascript("getCurrentTime();") { value ->
                val sec = value?.trim()?.replace("\"", "")?.toFloatOrNull()
                emitMetric("seek_observed", sec?.toString() ?: "unknown")
                if (sec != null) {
                    _events.tryEmit(YouTubePlayerEvent.CurrentTime(sec))
                }
            }
        }, 400)
    }

    override fun mute() {
        evalJs("mutePlayer();")
        emitMetric("mute", "")
    }

    override fun unmute() {
        evalJs("unMutePlayer();")
        emitMetric("unmute", "")
    }

    override fun requestCurrentTime() {
        evalJs(
            """
            (function(){
              var t = getCurrentTime();
              TipmannaBridge.onStateChange(getPlayerState());
            })();
            """.trimIndent(),
        )
        webView?.evaluateJavascript("getCurrentTime();") { value ->
            val sec = value?.trim()?.replace("\"", "")?.toFloatOrNull() ?: return@evaluateJavascript
            _events.tryEmit(YouTubePlayerEvent.CurrentTime(sec))
        }
    }

    override fun requestDuration() {
        webView?.evaluateJavascript("getDuration();") { value ->
            val sec = value?.trim()?.replace("\"", "")?.toFloatOrNull() ?: return@evaluateJavascript
            _events.tryEmit(YouTubePlayerEvent.Duration(sec))
        }
    }

    override fun state(): YouTubePlayerState = lastState

    override fun destroy() {
        emitMetric("destroy", "")
        webView?.let { wv ->
            bridge?.let { wv.removeJavascriptInterface("TipmannaBridge") }
            wv.stopLoading()
        }
        webView = null
        bridge = null
        playerReady = false
        lastState = YouTubePlayerState.UNKNOWN
    }

    override fun recreatePlayerSurface(webView: WebView) {
        emitMetric("recreate_surface", "")
        this.webView?.let { wv ->
            bridge?.let { wv.removeJavascriptInterface("TipmannaBridge") }
            wv.stopLoading()
        }
        bridge = null
        playerReady = false
        lastState = YouTubePlayerState.UNKNOWN
        attachWebView(webView)
    }

    private fun bootstrapWebView(
        videoId: String,
        autoplay: Boolean,
        mute: Boolean,
        startSeconds: Float,
    ) {
        val html = YoutubeIframeHtml.build(videoId, autoplay, mute, startSeconds)
        emitMetric("bootstrap_html", videoId)
        webView?.loadDataWithBaseURL(
            YoutubeIframeHtml.EMBED_ORIGIN,
            html,
            "text/html",
            "UTF-8",
            null,
        )
    }

    private fun evalJs(script: String) {
        mainHandler.post {
            webView?.evaluateJavascript(script, null)
        }
    }

    private fun emitMetric(name: String, detail: String) {
        _events.tryEmit(
            YouTubePlayerEvent.Metric(name, System.currentTimeMillis(), detail),
        )
    }

    private fun escapeJs(id: String): String = id.replace("'", "\\'")
}
