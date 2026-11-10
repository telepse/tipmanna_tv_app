package com.example.telepse.livetv

import com.example.telepse.data.model.TvLivePosition
import com.example.telepse.data.model.TvScheduleChannel
import com.example.telepse.data.model.TvScheduleFetchOutcome
import com.example.telepse.data.model.TvScheduleItem
import com.example.telepse.data.model.TvScheduleResponse
import java.util.concurrent.atomic.AtomicInteger

class FakeTvScheduleSource : TvScheduleSource {
    var nextResult: Result<TvScheduleFetchOutcome> =
        Result.success(
            TvScheduleFetchOutcome(
                schedule = sampleSchedule("Ffs0YHm2jks", 0),
                etag = "\"test-etag-1\"",
            ),
        )
    /** When set, fetch throws instead of returning [nextResult] (e.g. CancellationException). */
    var throwOnFetch: Throwable? = null
    val fetchCount = AtomicInteger(0)
    var lastIfNoneMatch: String? = null

    override suspend fun fetchGlobalSchedule(ifNoneMatch: String?): Result<TvScheduleFetchOutcome> {
        fetchCount.incrementAndGet()
        lastIfNoneMatch = ifNoneMatch
        throwOnFetch?.let { throw it }
        return nextResult
    }

    companion object {
        fun sampleSchedule(
            videoId: String,
            index: Int,
            offsetSeconds: Int = 10,
        ): TvScheduleResponse {
            return sampleScheduleWithItems(
                videoIds = listOf(videoId),
                liveIndex = index,
                offsetSeconds = offsetSeconds,
            )
        }

        fun sampleScheduleWithItems(
            videoIds: List<String>,
            liveIndex: Int,
            offsetSeconds: Int = 10,
            eventIds: List<String?> = emptyList(),
        ): TvScheduleResponse {
            val items =
                videoIds.mapIndexed { idx, videoId ->
                    val eventId = eventIds.getOrNull(idx)
                    TvScheduleItem(
                        id = "row-$idx",
                        channel =
                            TvScheduleChannel(
                                url = "https://www.youtube.com/watch?v=$videoId",
                                title = "Test $idx",
                                eventId = eventId,
                            ),
                    )
                }
            return TvScheduleResponse(
                scheduleState = "OK",
                items = items,
                livePosition =
                    TvLivePosition(
                        scheduleIndex = liveIndex,
                        offsetSeconds = offsetSeconds,
                    ),
            )
        }
    }
}
