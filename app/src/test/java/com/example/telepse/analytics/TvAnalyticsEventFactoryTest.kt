package com.example.telepse.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class TvAnalyticsEventFactoryTest {
    private fun fieldString(event: kotlinx.serialization.json.JsonObject, key: String): String =
        event[key]!!.toString().trim('"')

    @Test
    fun videoStart_defaultMode_isGlobalPerContract() {
        val ev =
            TvAnalyticsEventFactory.videoStart(
                ts = 1_000L,
                itemId = "abc123",
                scheduleIndex = 2,
            )
        assertEquals("video_start", fieldString(ev, "event"))
        assertEquals("abc123", fieldString(ev, "itemId"))
        assertEquals(TvAnalyticsClipMode.GLOBAL, fieldString(ev, "mode"))
    }

    @Test
    fun videoStart_exploreMode_isExplore() {
        val ev =
            TvAnalyticsEventFactory.videoStart(
                ts = 1_000L,
                itemId = "item-1",
                scheduleIndex = 0,
                mode = TvAnalyticsClipMode.EXPLORE,
            )
        assertEquals(TvAnalyticsClipMode.EXPLORE, fieldString(ev, "mode"))
    }

    @Test
    fun watchDuration_defaultMode_isGlobalNotLegacyVod() {
        val ev =
            TvAnalyticsEventFactory.watchDuration(
                ts = 1_000L,
                itemId = "abc123",
                watchedSec = 30,
            )
        assertEquals(TvAnalyticsClipMode.GLOBAL, fieldString(ev, "mode"))
        assertFalse(fieldString(ev, "mode").equals("vod", ignoreCase = true))
    }

    @Test
    fun clipModeConstants_matchBackendEnum() {
        val allowed = setOf("GLOBAL", "LOCAL", "EXPLORE")
        assertEquals(allowed, setOf(TvAnalyticsClipMode.GLOBAL, TvAnalyticsClipMode.LOCAL, TvAnalyticsClipMode.EXPLORE))
    }

    @Test
    fun userInteraction_kindIsPreserved() {
        val ev =
            TvAnalyticsEventFactory.userInteraction(
                ts = 2_000L,
                kind = "pause",
                detail = "remote",
            )
        assertEquals("user_interaction", fieldString(ev, "event"))
        assertEquals("pause", fieldString(ev, "kind"))
    }
}
