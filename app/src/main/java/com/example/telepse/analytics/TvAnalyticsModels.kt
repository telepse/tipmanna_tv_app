package com.example.telepse.analytics

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Subset of web `tvAnalyticsSchema.ts` v1 events used on Android TV.
 * POST `/api/v1/tv/analytics` with `{ v: 1, ... }`.
 */
@Serializable
data class TvAnalyticsBatchV1(
    val v: Int = 1,
    val sessionId: String,
    val eventId: String? = null,
    val surface: String? = null,
    val deviceCategory: String? = "tv",
    val videoChannelId: String? = null,
    val communityId: String? = null,
    val events: List<JsonObject>,
)

object TvAnalyticsSurfaces {
    const val TV_LOOPER = "tv_looper"
    const val EXPLORE_FEED = "explore_feed"
    const val ANDROID_TV_SCREEN = "android_tv_screen"
    /** Backend `paymentSurfaceSchema` value for native Kotlin TV client journeys. */
    const val KOTLIN_TV = "kotlin_tv"
}

/** Matches `TvAnalyticsClipMode` in `frontend/src/lib/analytics-types.ts` and `tvAnalyticsEventV1Schema`. */
object TvAnalyticsClipMode {
    const val GLOBAL = "GLOBAL"
    const val LOCAL = "LOCAL"
    const val EXPLORE = "EXPLORE"
}

object TvAnalyticsEventFactory {
    fun videoStart(
        ts: Long,
        itemId: String,
        scheduleIndex: Int,
        mode: String = TvAnalyticsClipMode.GLOBAL,
        channelId: String? = null,
    ): JsonObject =
        buildJsonObject {
            put("event", "video_start")
            put("ts", ts)
            put("itemId", itemId)
            put("scheduleIndex", scheduleIndex)
            put("mode", mode)
            channelId?.let { put("channelId", it) }
        }

    fun userInteraction(
        ts: Long,
        kind: String,
        detail: String? = null,
    ): JsonObject =
        buildJsonObject {
            put("event", "user_interaction")
            put("ts", ts)
            put("kind", kind)
            detail?.let { put("detail", it) }
        }

    fun watchDuration(
        ts: Long,
        itemId: String,
        watchedSec: Int,
        mode: String = TvAnalyticsClipMode.GLOBAL,
        channelId: String? = null,
    ): JsonObject =
        buildJsonObject {
            put("event", "watch_duration")
            put("ts", ts)
            put("itemId", itemId)
            put("watchedSec", watchedSec)
            put("mode", mode)
            channelId?.let { put("channelId", it) }
        }
}
