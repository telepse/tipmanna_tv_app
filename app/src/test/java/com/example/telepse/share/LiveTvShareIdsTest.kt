package com.example.telepse.share

import com.example.telepse.data.model.TvScheduleChannel
import com.example.telepse.data.model.TvScheduleItem
import com.example.telepse.data.model.TvLivePosition
import com.example.telepse.data.model.TvScheduleResponse
import com.example.telepse.livetv.LiveScheduleTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LiveTvShareIdsTest {
    private val eventId = "11111111-1111-4111-8111-111111111111"
    private val channelId = "22222222-2222-4222-8222-222222222222"

    @Test
    fun resolveLiveTvShareIds_fromTargetAndScheduleRow() {
        val schedule =
            TvScheduleResponse(
                scheduleState = "live",
                livePosition = TvLivePosition(scheduleIndex = 0),
                items =
                    listOf(
                        TvScheduleItem(
                            id = channelId,
                            channel = TvScheduleChannel(id = channelId, title = "Ch", eventId = eventId),
                        ),
                    ),
            )
        val target =
            LiveScheduleTarget(
                videoId = "yt",
                startSeconds = 0f,
                scheduleIndex = 0,
                scheduleState = "live",
                eventId = eventId,
            )
        val ids = resolveLiveTvShareIds(schedule, target)
        assertEquals(TipmannaShareIds(eventId, channelId), ids)
    }

    @Test
    fun resolveLiveTvShareIds_nullWhenIdsMissing() {
        assertNull(resolveLiveTvShareIds(null, null))
    }

    @Test
    fun resolveLiveTvShareIds_prefersChannelId_overVirtualRowId() {
        val virtualRowId = "virt:source-1:0:120"
        val schedule =
            TvScheduleResponse(
                scheduleState = "live",
                livePosition = TvLivePosition(scheduleIndex = 0),
                items =
                    listOf(
                        TvScheduleItem(
                            id = virtualRowId,
                            channel =
                                TvScheduleChannel(
                                    id = channelId,
                                    title = "Ch",
                                    eventId = eventId,
                                ),
                        ),
                    ),
            )
        val target =
            LiveScheduleTarget(
                videoId = "yt",
                startSeconds = 0f,
                scheduleIndex = 0,
                scheduleState = "live",
                eventId = eventId,
            )
        assertEquals(TipmannaShareIds(eventId, channelId), resolveLiveTvShareIds(schedule, target))
    }

    @Test
    fun resolveLiveTvShareIds_nullWhenChannelIdMissingAndRowNotUuid() {
        val schedule =
            TvScheduleResponse(
                scheduleState = "live",
                livePosition = TvLivePosition(scheduleIndex = 0),
                items =
                    listOf(
                        TvScheduleItem(
                            id = "virt:bad:0:0",
                            channel = TvScheduleChannel(title = "Ch", eventId = eventId),
                        ),
                    ),
            )
        val target =
            LiveScheduleTarget(
                videoId = "yt",
                startSeconds = 0f,
                scheduleIndex = 0,
                scheduleState = "live",
                eventId = eventId,
            )
        assertNull(resolveLiveTvShareIds(schedule, target))
    }
}
