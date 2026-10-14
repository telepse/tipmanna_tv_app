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
    }

    @Test
    fun resolve_emptyItemsFails() {
        val result = LiveScheduleTargetResolver.resolve(TvScheduleResponse(items = emptyList()))
        assertTrue(result.isFailure)
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
