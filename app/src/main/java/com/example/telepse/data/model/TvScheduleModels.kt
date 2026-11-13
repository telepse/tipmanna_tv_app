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
    /** Fallback tip event when [TvScheduleChannel.eventId] is absent (matches web). */
    val defaultChannelEventId: String? = null,
)

@Serializable
data class TvScheduleChannel(
    /** EventChannel UUID for share deep link `?v=` (matches web TV `current.channel.id`). */
    val id: String? = null,
    val url: String? = null,
    val title: String? = null,
    /** Authoritative LIVE tip catalog event id from schedule channel. */
    val eventId: String? = null,
)

@Serializable
data class TvLivePosition(
    val scheduleIndex: Int = 0,
    val offsetSeconds: Int = 0,
)
