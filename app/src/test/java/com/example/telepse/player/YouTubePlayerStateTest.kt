package com.example.telepse.player

import org.junit.Assert.assertEquals
import org.junit.Test

class YouTubePlayerStateTest {
    @Test
    fun fromYtCode_mapsIframeApiStates() {
        assertEquals(YouTubePlayerState.PLAYING, YouTubePlayerState.fromYtCode(1))
        assertEquals(YouTubePlayerState.PAUSED, YouTubePlayerState.fromYtCode(2))
        assertEquals(YouTubePlayerState.BUFFERING, YouTubePlayerState.fromYtCode(3))
        assertEquals(YouTubePlayerState.ENDED, YouTubePlayerState.fromYtCode(0))
        assertEquals(YouTubePlayerState.CUED, YouTubePlayerState.fromYtCode(5))
    }

    @Test
    fun spikeVideoIds_matchTipmannaScheduleFixtures() {
        assertEquals("Ffs0YHm2jks", TipmannaSpikeVideoIds.SCHEDULE_LIVE_POSITION_SEED)
        assertEquals("P4kFofLwPLY", TipmannaSpikeVideoIds.AXIS_GAP_FIXTURE)
    }
}
