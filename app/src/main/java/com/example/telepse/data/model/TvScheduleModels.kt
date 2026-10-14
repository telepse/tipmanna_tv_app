package com.example.telepse.data.model

import kotlinx.serialization.Serializable

@Serializable
data class TvScheduleResponse(
    val broadcastEpoch: Long? = null,
    val totalDuration: Int? = null,
    val items: List<TvScheduleItem> = emptyList(),
    val livePosition: TvLivePosition? = null,
    val scheduleState: String? = null,
    val message: String? = null,
)

@Serializable
data class TvScheduleItem(
    val id: String? = null,
    val channel: TvScheduleChannel? = null,
)

@Serializable
data class TvScheduleChannel(
    val url: String? = null,
    val title: String? = null,
)

@Serializable
data class TvLivePosition(
    val scheduleIndex: Int = 0,
    val offsetSeconds: Int = 0,
)
