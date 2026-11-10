package com.example.telepse.player

/**
 * Mirrors YouTube IFrame Player API player states (YT.PlayerState).
 * @see https://developers.google.com/youtube/iframe_api_reference#Playback_status
 */
enum class YouTubePlayerState(val ytCode: Int) {
    UNSTARTED(-1),
    ENDED(0),
    PLAYING(1),
    PAUSED(2),
    BUFFERING(3),
    CUED(5),
    UNKNOWN(Int.MIN_VALUE);

    companion object {
        fun fromYtCode(code: Int): YouTubePlayerState =
            entries.firstOrNull { it.ytCode == code } ?: UNKNOWN
    }
}
