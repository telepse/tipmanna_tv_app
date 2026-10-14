package com.example.telepse.ui.viewmodel

import android.webkit.WebView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.telepse.livetv.PusherGlobalScheduleRefreshSignals
import com.example.telepse.livetv.TvModeEngine
import com.example.telepse.livetv.TvModeEngineState
import com.example.telepse.livetv.TvScheduleRepositorySource
import com.example.telepse.player.WebViewYouTubePlayerAdapter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class LiveTvFoundationViewModel @Inject constructor(
    scheduleSource: TvScheduleRepositorySource,
    refreshSignals: PusherGlobalScheduleRefreshSignals,
) : ViewModel() {

    private val adapter = WebViewYouTubePlayerAdapter()
    private val engine =
        TvModeEngine(
            scheduleSource = scheduleSource,
            player = adapter,
            scope = viewModelScope,
            refreshSignals = refreshSignals,
        )

    val uiState: StateFlow<TvModeEngineState> = engine.state

    fun onEnter() {
        engine.start()
    }

    fun onLeave() {
        engine.stop()
    }

    fun attachWebView(webView: WebView) {
        engine.attachWebView(webView)
    }

    fun onWebViewReleased() {
        engine.onWebViewReleased()
    }

    fun refreshSchedule() {
        engine.refreshSchedule()
    }

    fun play() = engine.play()

    fun pause() = engine.pause()

    override fun onCleared() {
        engine.dispose()
        super.onCleared()
    }
}
