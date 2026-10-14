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
)
