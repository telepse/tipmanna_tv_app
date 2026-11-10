package com.example.telepse.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class CommunityRoomDisplayBootstrap(
    val communityId: String,
    val display: CommunityRoomDisplaySettings,
    val realtime: CommunityRoomRealtimeHints,
)

@Serializable
data class CommunityRoomDisplaySettings(
    val background: String = "",
    val backgroundType: String = "default",
    val moneyFor: String = "",
    val showLatest: Boolean = false,
    val hideProgressAmount: Boolean = false,
    val tipUsageDisclosure: String = "",
    val performers: JsonElement? = null,
    val projects: JsonElement? = null,
)

@Serializable
data class CommunityRoomRealtimeHints(
    val pusherKey: String? = null,
    val pusherCluster: String? = null,
    val channel: String,
    val events: CommunityRoomRealtimeEvents? = null,
)

@Serializable
data class CommunityRoomRealtimeEvents(
    val displayUpdated: String? = null,
)

@Serializable
data class CommunityDisplayUpdatedPayload(
    val communityId: String? = null,
    val at: Long? = null,
)
