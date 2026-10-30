package com.example.telepse.data.model

import kotlinx.serialization.Serializable

@Serializable
data class WalletStatusResponse(
    val exists: Boolean = false,
    val status: String? = null,
    val balance: String = "0",
)

@Serializable
data class WalletTransactionsResponse(
    val items: List<WalletLedgerRow> = emptyList(),
)

@Serializable
data class WalletLedgerRow(
    val id: String,
    val kind: String,
    val amount: String,
    val balanceAfter: String,
    val createdAt: String,
    val eventId: String? = null,
)

@Serializable
data class WalletDebitCharacterLine(
    val name: String,
    val time: String,
    val price: Double,
    val characterId: String,
)

@Serializable
data class WalletDebitRequest(
    val pin: String,
    val amount: Double,
    val eventId: String,
    val characters: List<WalletDebitCharacterLine>,
    val paymentSurface: String = "kotlin_tv",
    val deviceCategory: String = "tv",
    val paymentContentId: String? = null,
    val playbackOffsetSec: Int? = null,
    val clientSessionId: String? = null,
    /** Required by backend when the event has a published live tip config. */
    val tipQuoteId: String? = null,
)

@Serializable
data class WalletDebitResponse(
    val transactionId: String? = null,
    val newBalance: String? = null,
)

@Serializable
data class EventCharacterCatalogResponse(
    val eventId: String = "",
    val characterSets: List<CharacterSetCatalogRow> = emptyList(),
)

@Serializable
data class CharacterSetCatalogRow(
    val id: String = "",
    val name: String = "",
    val characters: List<TipCharacterCatalogItem> = emptyList(),
)

@Serializable
data class TipCharacterCatalogItem(
    val id: String,
    val name: String,
    val price: Double,
    val iconUrl: String? = null,
)
