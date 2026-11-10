package com.example.telepse.livetv

import com.example.telepse.data.model.TvScheduleFetchOutcome

fun interface TvScheduleSource {
    suspend fun fetchGlobalSchedule(ifNoneMatch: String?): Result<TvScheduleFetchOutcome>
}
