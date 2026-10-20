package com.example.telepse.livetv

import com.example.telepse.data.model.TvScheduleResponse
import com.example.telepse.player.YouTubePlayerState

enum class TvModeEnginePhase {
    Idle,
    Loading,
    Ready,
    Empty,
    Error,
}

data class TvModeEngineState(
    val phase: TvModeEnginePhase = TvModeEnginePhase.Idle,
    val schedule: TvScheduleResponse? = null,
    val target: LiveScheduleTarget? = null,
    val errorMessage: String? = null,
    val loadedVideoId: String? = null,
    val loadedStartSeconds: Float? = null,
    val refreshGeneration: Long = 0,
    val playerState: YouTubePlayerState = YouTubePlayerState.UNKNOWN,
    val playerReady: Boolean = false,
    val isActive: Boolean = false,
    /** Best-effort position from player CurrentTime events (for seek). */
    val playbackPositionSeconds: Float = 0f,
    val lastScheduleRefreshSucceeded: Boolean = false,
    /**
     * Background poll, debounced/Pusher/player-ended refresh, and error-recovery retries are paused
     * after confirmed authenticated session expiry. Cleared only by a successful schedule fetch
     * (typically via explicit [com.example.telepse.livetv.TvModeEngine.refreshSchedule]).
     * Survives [com.example.telepse.livetv.TvModeEngine.stop]/[com.example.telepse.livetv.TvModeEngine.start].
     */
    val scheduleBackgroundRefreshSuspended: Boolean = false,
)
