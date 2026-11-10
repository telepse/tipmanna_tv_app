package com.example.telepse.data.model

data class TvScheduleFetchOutcome(
    val notModified: Boolean = false,
    val schedule: TvScheduleResponse? = null,
    val etag: String? = null,
)
