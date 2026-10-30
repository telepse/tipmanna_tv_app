package com.example.telepse.livetv

import com.example.telepse.data.model.TvScheduleChannel
import com.example.telepse.data.model.TvScheduleItem
import org.junit.Assert.assertEquals
import org.junit.Test

class ResolveTvScheduleEventIdTest {
    @Test
    fun prefersChannelEventId() {
        val item =
            TvScheduleItem(
                channel = TvScheduleChannel(eventId = " channel-evt "),
                defaultChannelEventId = "fallback-evt",
            )
        assertEquals("channel-evt", ResolveTvScheduleEventId.resolve(item))
    }

    @Test
    fun fallsBackToDefaultChannelEventId() {
        val item =
            TvScheduleItem(
                channel = TvScheduleChannel(url = "https://youtu.be/abc"),
                defaultChannelEventId = "fallback-evt",
            )
        assertEquals("fallback-evt", ResolveTvScheduleEventId.resolve(item))
    }

    @Test
    fun missingMapping_returnsEmpty() {
        assertEquals("", ResolveTvScheduleEventId.resolve(TvScheduleItem()))
        assertEquals("", ResolveTvScheduleEventId.resolve(null))
    }

    @Test
    fun resolveAtLiveIndex() {
        val items =
            listOf(
                TvScheduleItem(defaultChannelEventId = "e0"),
                TvScheduleItem(channel = TvScheduleChannel(eventId = "e1")),
            )
        assertEquals("e1", ResolveTvScheduleEventId.resolveAtLiveIndex(items, 1))
        assertEquals("", ResolveTvScheduleEventId.resolveAtLiveIndex(items, 9))
    }
}
