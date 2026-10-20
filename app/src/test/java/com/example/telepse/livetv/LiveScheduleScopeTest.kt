package com.example.telepse.livetv

import org.junit.Assert.assertEquals
import org.junit.Test

class LiveScheduleScopeTest {
    @Test
    fun globalBadgeLabel() {
        assertEquals("GLOBAL LIVE", LiveScheduleScope.Global.badgeLabel())
    }

    @Test
    fun communityBadgeLabel() {
        assertEquals(
            "COMMUNITY",
            LiveScheduleScope.Community("11111111-1111-4111-8111-111111111111").badgeLabel(),
        )
    }

    @Test
    fun unknownBadgeLabel() {
        assertEquals("LIVE TV", LiveScheduleScope.Unknown.badgeLabel())
    }
}
