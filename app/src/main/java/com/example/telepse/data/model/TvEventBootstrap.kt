package com.example.telepse.data.model

import kotlinx.serialization.Serializable

/**
 * Response from `GET /api/v1/tv-app/events/:id/bootstrap`.
 * See `backend/src/controllers/tvApp.controller.ts` (`getBootstrap`).
 */
@Serializable
data class TvEventBootstrap(
    val event: Event,
    val realtime: TvEventRealtimeHints,
    val animationContractUrl: String,
    val animationStylesSpecUrl: String? = null,
)

@Serializable
data class TvEventRealtimeHints(
    val pusherKey: String? = null,
    val pusherCluster: String? = null,
    val channels: TvEventRealtimeChannels,
    val events: TvEventRealtimeEventNames? = null,
)

@Serializable
data class TvEventRealtimeChannels(
    val `public`: String,
    val `private`: String,
)

@Serializable
data class TvEventRealtimeEventNames(
    val tipAnimation: String? = null,
    val screenDisplayState: String? = null,
    val screenSettingsUpdated: String? = null,
    val eventAmountUpdated: String? = null,
    val latestTipUpdated: String? = null,
    val newMessage: String? = null,
    val liveTip: String? = null,
)
