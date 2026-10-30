package com.example.telepse.data.model

import kotlinx.serialization.Serializable

/** GET /api/v1/tip-config/:eventId/active */
@Serializable
data class ActiveTipConfigResponse(
    val mode: String = "legacy",
    val version: Int? = null,
    val etag: String? = null,
    val config: TipConfigPayloadDto? = null,
)

@Serializable
data class TipConfigPayloadDto(
    val sectionItems: List<TipConfigItemDto> = emptyList(),
    val drawerItems: List<TipConfigItemDto> = emptyList(),
    val textCharacter: TipConfigTextCharacterDto? = null,
)

@Serializable
data class TipConfigItemDto(
    val itemId: String = "",
    val characterId: String = "",
    val priceMinor: Int = 0,
    val kind: String? = null,
    val position: Int? = null,
)

@Serializable
data class TipConfigTextCharacterDto(
    val enabled: Boolean = false,
    val priceMinor: Int = 0,
)

@Serializable
data class TipQuoteLineRequest(
    val itemId: String,
    val characterId: String,
    val name: String,
    val priceMinor: Int,
    val qty: Int,
)

@Serializable
data class CreateTipQuoteRequest(
    val eventId: String,
    val surface: String,
    val configVersion: Int,
    val lines: List<TipQuoteLineRequest> = emptyList(),
    val textLine: TipQuoteTextLineRequest? = null,
)

@Serializable
data class TipQuoteTextLineRequest(
    val priceMinor: Int,
    val qty: Int = 1,
)

@Serializable
data class CreateTipQuoteResponse(
    val quoteId: String = "",
    val totalMinor: Int = 0,
    val currency: String = "GBP",
    val configVersion: Int = 0,
    val expiresAt: String? = null,
)
