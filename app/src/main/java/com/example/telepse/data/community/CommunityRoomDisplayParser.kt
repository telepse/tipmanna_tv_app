package com.example.telepse.data.community

import com.example.telepse.data.model.CommunityDisplayUpdatedPayload
import com.example.telepse.data.model.CommunityRoomDisplaySettings

const val DEFAULT_COMMUNITY_DISPLAY_UPDATED_EVENT = "display-updated"

fun parseCommunityDisplayUpdatedPayload(raw: String?): CommunityDisplayUpdatedPayload? {
    if (raw.isNullOrBlank()) return null
    return try {
        kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
            isLenient = true
        }.decodeFromString(CommunityDisplayUpdatedPayload.serializer(), raw)
    } catch (_: Exception) {
        null
    }
}

fun communityDisplayUpdatedApplies(
    payload: CommunityDisplayUpdatedPayload,
    activeCommunityId: String,
): Boolean {
    val id = payload.communityId?.trim().orEmpty()
    return id.isEmpty() || id == activeCommunityId.trim()
}

/** Progress bar amounts visible when hideProgressAmount is not true (matches venue contract). */
fun communityShowProgressBarAmounts(settings: CommunityRoomDisplaySettings): Boolean {
    return !settings.hideProgressAmount
}
