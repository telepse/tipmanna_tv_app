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
    val fetchCount = AtomicInteger(0)
    var lastIfNoneMatch: String? = null

    override suspend fun fetchGlobalSchedule(ifNoneMatch: String?): Result<TvScheduleFetchOutcome> {
        fetchCount.incrementAndGet()
        lastIfNoneMatch = ifNoneMatch
        return nextResult
    }

    companion object {
        fun sampleSchedule(
            videoId: String,
            index: Int,
            offsetSeconds: Int = 10,
        ): TvScheduleResponse {
            val url = "https://www.youtube.com/watch?v=$videoId"
            return TvScheduleResponse(
                scheduleState = "OK",
                items =
                    listOf(
                        TvScheduleItem(
                            id = "row-$index",
                            channel = TvScheduleChannel(url = url, title = "Test"),
                        ),
                    ),
                livePosition =
                    TvLivePosition(
                        scheduleIndex = index,
                        offsetSeconds = offsetSeconds,
                    ),
            )
        }
    }
}
