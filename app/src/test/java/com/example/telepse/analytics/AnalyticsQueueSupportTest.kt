package com.example.telepse.analytics

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Test

class AnalyticsQueueSupportTest {
    @Test
    fun requeueAfterFailedFlush_respectsMaxSize() {
        val failed =
            (1..5).map { i ->
                buildJsonObject { put("event", "e$i") }
            }
        val merged = AnalyticsQueueSupport.requeueAfterFailedFlush(failed, maxSize = 3)
        assertEquals(3, merged.size)
    }

    @Test
    fun appendCapped_dropsOldest() {
        val q = ArrayDeque<kotlinx.serialization.json.JsonObject>()
        repeat(4) { i ->
            AnalyticsQueueSupport.appendCapped(
                q,
                buildJsonObject { put("i", i) },
                maxSize = 3,
            )
        }
        assertEquals(3, q.size)
    }
}
