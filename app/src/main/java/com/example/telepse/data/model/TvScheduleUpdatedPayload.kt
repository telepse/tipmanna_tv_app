package com.example.telepse.data.model

import kotlinx.serialization.Serializable

/** Payload for `global-live-tv` / `tv-schedule-updated` (invalidation hint only). */
@Serializable
data class TvScheduleUpdatedPayload(
    val at: Long? = null,
)
