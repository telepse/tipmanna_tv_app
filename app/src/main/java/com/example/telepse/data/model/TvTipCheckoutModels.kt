package com.example.telepse.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CreateTvTipCheckoutSessionRequest(
    val eventId: String,
    val amount: Double,
    val characters: List<WalletDebitCharacterLine>,
    val tipQuoteId: String? = null,
    val paymentSurface: String = "kotlin_tv",
    val deviceCategory: String = "tv",
    val idempotencyKey: String? = null,
)

@Serializable
data class CreateTvTipCheckoutSessionResponse(
    val sessionId: String = "",
    val checkoutUrl: String = "",
    val checkoutToken: String? = null,
    val amountGbp: Double = 0.0,
    val totalMinor: Int = 0,
    val currency: String = "GBP",
    val status: String = "pending",
    val expiresAt: String = "",
    val transactionId: String? = null,
)

@Serializable
data class TvTipCheckoutSessionStatusResponse(
    val sessionId: String = "",
    val status: String = "pending",
    val amountGbp: Double = 0.0,
    val expiresAt: String = "",
    val transactionId: String? = null,
)
