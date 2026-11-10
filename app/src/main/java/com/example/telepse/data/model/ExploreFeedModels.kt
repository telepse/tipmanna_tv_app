package com.example.telepse.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ExploreFeedBatchRequest(
    val feedMode: String = "trends",
    val excludeChannelIds: List<String> = emptyList(),
    val seed: String? = null,
    val count: Int = 8,
    val communityIds: List<String> = emptyList(),
)

@Serializable
data class ExploreFeedBatchResponse(
    val items: List<ExploreFeedItem> = emptyList(),
    val seed: String = "",
    val savesFeedEligible: Boolean? = null,
    val savesFeedReason: String? = null,
)

@Serializable
data class ExploreFeedItem(
    val id: String,
    val title: String,
    val url: String,
    val isLive: Boolean = false,
    val event: ExploreFeedEvent? = null,
)

@Serializable
data class ExploreFeedEvent(
    val id: String? = null,
    val eventName: String? = null,
    val hostName: String? = null,
    val username: String? = null,
)
