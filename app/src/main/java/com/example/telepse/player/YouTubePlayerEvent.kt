package com.example.telepse.player

sealed class YouTubePlayerEvent {
    data object ApiReady : YouTubePlayerEvent()
    data object PlayerReady : YouTubePlayerEvent()
    data class StateChanged(val state: YouTubePlayerState) : YouTubePlayerEvent()
    data class Error(val code: Int) : YouTubePlayerEvent()
    data class CurrentTime(val seconds: Float) : YouTubePlayerEvent()
    data class Duration(val seconds: Float) : YouTubePlayerEvent()
    data class Metric(val name: String, val epochMs: Long, val detail: String = "") :
        YouTubePlayerEvent()
}
