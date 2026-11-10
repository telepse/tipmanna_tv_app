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
data class TvCheckoutAnimationRecoveryHints(
    val transactionId: String = "",
    val eventId: String = "",
    val stripePaymentIntentId: String = "",
    val recoveryState: String? = null,
    val recoverySessionToken: String? = null,
    val recoverySessionExpiresAt: String? = null,
)

@Serializable
data class TvTipCheckoutSessionStatusResponse(
    val sessionId: String = "",
    val eventId: String = "",
    val status: String = "pending",
    val amountGbp: Double = 0.0,
    val expiresAt: String = "",
    val transactionId: String? = null,
    val animationRecovery: TvCheckoutAnimationRecoveryHints? = null,
)

@Serializable
data class AnimationRecoveryExchangeRequest(
    val recoveryState: String,
    val transactionId: String,
    val stripePaymentIntentId: String,
    val eventId: String,
)

@Serializable
data class AnimationRecoveryExchangeResponse(
    val ok: Boolean = false,
    val sessionToken: String = "",
    val expiresAt: String = "",
)

@Serializable
data class AnimationRecoveryResolvedTipCharacter(
    val name: String = "",
    val iconUrl: String = "",
    val styleName: String? = null,
    val characterId: String? = null,
    val occurrenceIndex: Int? = null,
    val occurrenceId: String? = null,
)

@Serializable
data class AnimationRecoveryCanonicalPayload(
    val id: String = "",
    val seq: Int = 0,
    val eventId: String = "",
    val transactionId: String = "",
    val serverTs: Long = 0L,
    val playbackEndsAt: Long = 0L,
    val selectedTips: List<String> = emptyList(),
    val resolvedTips: List<AnimationRecoveryResolvedTipCharacter> = emptyList(),
    val amount: Double = 0.0,
    val sender: String = "",
    val rewardTurbo: Boolean = false,
    val rewardGalaxy: Boolean = false,
)

@Serializable
data class AnimationRecoveryClaimResponse(
    val canonicalPayload: AnimationRecoveryCanonicalPayload,
    val remainingOccurrenceIds: List<String> = emptyList(),
    val claimId: String = "",
    val leaseExpiresAt: String = "",
    val deliveryState: String = "",
)

@Serializable
data class AnimationRecoveryProgressOccurrence(
    val occurrenceId: String,
    val playbackResult: String,
    /** Required by server when playbackResult is "completed" (non-empty; see payment-animation-recovery.service). */
    val cycle1Outcome: String? = null,
    val cycle2Outcome: String? = null,
)

@Serializable
data class AnimationRecoveryProgressRequest(
    val transactionId: String,
    val bundleId: String,
    val claimId: String,
    val surface: String,
    val occurrences: List<AnimationRecoveryProgressOccurrence>,
)
