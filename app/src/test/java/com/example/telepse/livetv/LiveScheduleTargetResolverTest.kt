package com.example.telepse.livetv

import com.example.telepse.data.model.TvScheduleResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveScheduleTargetResolverTest {

    @Test
    fun resolve_extractsVideoFromLiveIndex() {
        val schedule = FakeTvScheduleSource.sampleSchedule("Ffs0YHm2jks", 0, 42)
        val target = LiveScheduleTargetResolver.resolve(schedule).getOrThrow()
        assertEquals("Ffs0YHm2jks", target.videoId)
        assertEquals(42f, target.startSeconds)
        assertEquals(0, target.scheduleIndex)
        assertEquals("", target.eventId)
    }

    @Test
    fun resolve_includesChannelEventId() {
        val schedule =
            FakeTvScheduleSource.sampleScheduleWithItems(
                videoIds = listOf("Ffs0YHm2jks"),
                liveIndex = 0,
                offsetSeconds = 3,
                eventIds = listOf("evt-live-1"),
            )
        val target = LiveScheduleTargetResolver.resolve(schedule).getOrThrow()
        assertEquals("evt-live-1", target.eventId)
    }

    @Test
    fun resolve_emptyItemsFails() {
        val result = LiveScheduleTargetResolver.resolve(TvScheduleResponse(items = emptyList()))
        assertTrue(result.isFailure)
    }

    @Test
    fun resolve_usesLiveIndexAcrossMultipleItems() {
        val schedule =
            FakeTvScheduleSource.sampleScheduleWithItems(
                videoIds = listOf("Ffs0YHm2jks", "P4kFofLwPLY"),
                liveIndex = 1,
                offsetSeconds = 7,
            )
        val target = LiveScheduleTargetResolver.resolve(schedule).getOrThrow()
        assertEquals("P4kFofLwPLY", target.videoId)
        assertEquals(1, target.scheduleIndex)
        assertEquals(7f, target.startSeconds)
    }

    @Test
    fun resolve_youtubeLiveChannelUrl() {
        val schedule =
            FakeTvScheduleSource.sampleSchedule("Ffs0YHm2jks", 0).copy(
                livePosition =
                    com.example.telepse.data.model.TvLivePosition(
                        scheduleIndex = 0,
                        offsetSeconds = 0,
                    ),
                items =
                    listOf(
                        com.example.telepse.data.model.TvScheduleItem(
                            channel =
                                com.example.telepse.data.model.TvScheduleChannel(
                                    url = "https://www.youtube.com/live/7NY3PAGzhBM",
                                    eventId = "302b6dda-02ab-49ff-9bed-1e8f9378557c",
                                ),
                        ),
                    ),
            )
        val target = LiveScheduleTargetResolver.resolve(schedule).getOrThrow()
        assertEquals("7NY3PAGzhBM", target.videoId)
        assertEquals("302b6dda-02ab-49ff-9bed-1e8f9378557c", target.eventId)
    }

    @Test
    fun resolve_malformedUrlFails() {
        val schedule =
            FakeTvScheduleSource.sampleSchedule("Ffs0YHm2jks", 0).copy(
                items =
                    listOf(
                        com.example.telepse.data.model.TvScheduleItem(
                            channel =
                                com.example.telepse.data.model.TvScheduleChannel(
                                    url = "https://example.com/not-youtube",
                                ),
                        ),
                    ),
            )
        assertTrue(LiveScheduleTargetResolver.resolve(schedule).isFailure)
    }
}
