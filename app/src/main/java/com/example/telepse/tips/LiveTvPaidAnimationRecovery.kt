package com.example.telepse.tips

import android.util.Log
import com.example.telepse.data.model.AnimationRecoveryCanonicalPayload
import com.example.telepse.data.model.AnimationRecoveryProgressOccurrence
import com.example.telepse.data.model.AnimationRecoveryProgressRequest
import com.example.telepse.data.model.ResolvedTipCharacter
import com.example.telepse.data.model.TipAnimationEvent
import com.example.telepse.data.model.TvCheckoutAnimationRecoveryHints
import com.example.telepse.data.model.WalletDebitResponse
import com.example.telepse.data.model.TvTipCheckoutSessionStatusResponse
import com.example.telepse.data.network.WalletHttpException
import com.example.telepse.data.repository.PaymentAnimationRecoveryOperations
import kotlinx.coroutines.delay
import kotlin.math.min

/**
 * Authoritative paid animation via payments animation-recovery API when Pusher
 * tip-animation is missed. Does not treat checkout poll alone as an animation signal.
 */
class LiveTvPaidAnimationRecovery(
    private val recoveryOperations: PaymentAnimationRecoveryOperations,
) {
    private val tag = "LiveTvPaidAnimationRecovery"

    data class ActiveClaim(
        val claimId: String,
        val bundleId: String,
        val transactionId: String,
        val occurrenceIds: List<String>,
    )

    var activeClaim: ActiveClaim? = null
        private set

    private var recoverySessionToken: String? = null

    /**
     * Wallet debit success — same authoritative recovery path as QR when the server returns hints.
     * Pusher may still deliver `tip-animation`; this path matches Task 11AH QR acceptance.
     */
    suspend fun recoverAfterWalletPaid(
        debitResponse: WalletDebitResponse,
        eventId: String,
        animationHost: LiveTvTipAnimationHost,
        activeEventId: String,
    ): Boolean {
        val txId = debitResponse.transactionId
        WalletPaidAnimationTrace.log(
            stage = "wallet_debit_success_received",
            transactionId = txId,
            result = "ok",
        )
        val hints = debitResponse.animationRecovery
        if (hints == null) {
            WalletPaidAnimationTrace.log(
                stage = "wallet_animation_recovery_missing",
                transactionId = txId,
                result = "skip",
            )
            return false
        }
        WalletPaidAnimationTrace.log(
            stage = "wallet_animation_recovery_present",
            transactionId = hints.transactionId,
            result = "ok",
        )
        if (hints.transactionId.isBlank()) return false
        if (hints.recoverySessionToken.isNullOrBlank()) {
            WalletPaidAnimationTrace.log(
                stage = "wallet_animation_recovery_missing",
                transactionId = hints.transactionId,
                detail = "recoverySessionToken absent",
            )
            return false
        }
        WalletPaidAnimationTrace.log(
            stage = "wallet_recovery_callback_invoked",
            transactionId = hints.transactionId,
        )
        val status =
            TvTipCheckoutSessionStatusResponse(
                status = "paid",
                eventId = hints.eventId.trim().ifBlank { eventId.trim() },
                transactionId = hints.transactionId,
                animationRecovery = hints,
            )
        val programmeId =
            hints.eventId.trim().ifBlank { activeEventId.trim() }.ifBlank { eventId.trim() }
        return recoverAfterQrPaid(status, animationHost, programmeId)
    }

    suspend fun recoverAfterQrPaid(
        status: TvTipCheckoutSessionStatusResponse,
        animationHost: LiveTvTipAnimationHost,
        activeEventId: String,
    ): Boolean {
        val hints = status.animationRecovery ?: return false
        if (hints.transactionId.isBlank()) return false
        val sessionToken =
            resolveRecoverySessionToken(hints)
                ?: run {
                    Log.w(tag, "No recovery session token available for tx=${hints.transactionId.take(8)}")
                    return false
                }
        recoverySessionToken = sessionToken
        WalletPaidAnimationTrace.log(
            stage = "wallet_recovery_claim_started",
            transactionId = hints.transactionId,
        )
        val claim =
            claimWithDeliveryRetry(sessionToken).getOrElse { e ->
                val http = (e as? WalletHttpException)?.httpStatus
                WalletPaidAnimationTrace.log(
                    stage = "wallet_recovery_claim_failed",
                    transactionId = hints.transactionId,
                    httpStatus = http,
                    detail = e.message?.take(120),
                )
                Log.w(tag, "animation-recovery claim failed", e)
                return false
            }
        WalletPaidAnimationTrace.log(
            stage = "wallet_recovery_claim_result",
            transactionId = hints.transactionId,
            result = "ok",
        )
        val event = claim.canonicalPayload.toTipAnimationEvent()
        animationHost.acceptAuthoritativePaymentAnimation(event, activeEventId)
        WalletPaidAnimationTrace.log(
            stage = "tv_paid_animation_triggered",
            transactionId = event.transactionId,
            detail = "tips=${event.selectedTips.size}",
        )
        activeClaim =
            ActiveClaim(
                claimId = claim.claimId,
                bundleId = claim.canonicalPayload.id,
                transactionId = claim.canonicalPayload.transactionId,
                occurrenceIds = claim.remainingOccurrenceIds,
            )
        Log.d(
            tag,
            "Recovered paid animation via claim tx=${event.transactionId?.take(8)} tips=${event.selectedTips.size}",
        )
        return true
    }

    suspend fun reportPlaybackCompleted() {
        val sessionToken = recoverySessionToken ?: return
        val claim = activeClaim ?: return
        if (claim.occurrenceIds.isEmpty()) {
            activeClaim = null
            return
        }
        val body =
            AnimationRecoveryProgressRequest(
                transactionId = claim.transactionId,
                bundleId = claim.bundleId,
                claimId = claim.claimId,
                surface = "kotlin_tv",
                occurrences =
                    claim.occurrenceIds.map { id ->
                        AnimationRecoveryProgressOccurrence(
                            occurrenceId = id,
                            playbackResult = "completed",
                            // Server requires two non-empty outcomes for "completed"; we have a single
                            // TV completion callback when the overlay finishes — both mean playback ok.
                            cycle1Outcome = "ok",
                            cycle2Outcome = "ok",
                        )
                    },
            )
        recoveryOperations.progressAnimationRecovery(sessionToken, body).onFailure { e ->
            Log.w(tag, "animation-recovery progress failed", e)
        }
        activeClaim = null
        recoverySessionToken = null
    }

    fun clearClaim() {
        activeClaim = null
        recoverySessionToken = null
    }

    /**
     * Backend may mark checkout paid before [TipAnimationDelivery] exists (webhook lag).
     * Retry 404 only — never treat poll alone as an animation signal.
     */
    private suspend fun claimWithDeliveryRetry(
        sessionToken: String,
    ): Result<com.example.telepse.data.model.AnimationRecoveryClaimResponse> {
        var waitMs = RECOVERY_CLAIM_INITIAL_DELAY_MS
        repeat(RECOVERY_CLAIM_MAX_ATTEMPTS) { attempt ->
            val result = recoveryOperations.claimAnimationRecovery(sessionToken)
            if (result.isSuccess) return result
            val err = result.exceptionOrNull()
            val retryable =
                err is WalletHttpException &&
                    err.httpStatus == HTTP_NOT_FOUND &&
                    attempt < RECOVERY_CLAIM_MAX_ATTEMPTS - 1
            if (!retryable) return result
            Log.d(
                tag,
                "animation delivery not ready (404); retry in ${waitMs}ms attempt=${attempt + 1}/$RECOVERY_CLAIM_MAX_ATTEMPTS",
            )
            delay(waitMs)
            waitMs = min(waitMs + RECOVERY_CLAIM_BACKOFF_STEP_MS, RECOVERY_CLAIM_MAX_DELAY_MS)
        }
        return Result.failure(IllegalStateException("recovery claim exhausted retries"))
    }

    private suspend fun resolveRecoverySessionToken(
        hints: TvCheckoutAnimationRecoveryHints,
    ): String? =
        hints.recoverySessionToken?.trim()?.takeIf { it.isNotEmpty() }
            ?: hints.recoveryState?.trim()?.takeIf { it.isNotEmpty() }?.let { raw ->
                recoveryOperations
                    .exchangeAnimationRecovery(
                        recoveryState = raw,
                        transactionId = hints.transactionId,
                        stripePaymentIntentId = hints.stripePaymentIntentId,
                        eventId = hints.eventId,
                    )
                    .getOrNull()
                    ?.sessionToken
                    ?.takeIf { it.isNotEmpty() }
            }

    companion object {
        private const val HTTP_NOT_FOUND = 404
        private const val RECOVERY_CLAIM_MAX_ATTEMPTS = 10
        private const val RECOVERY_CLAIM_INITIAL_DELAY_MS = 2_000L
        private const val RECOVERY_CLAIM_BACKOFF_STEP_MS = 1_000L
        private const val RECOVERY_CLAIM_MAX_DELAY_MS = 5_000L
    }
}

private fun AnimationRecoveryCanonicalPayload.toTipAnimationEvent(): TipAnimationEvent =
    TipAnimationEvent(
        selectedTips = selectedTips,
        amount = amount,
        sender = sender,
        id = id,
        seq = seq,
        eventId = eventId,
        transactionId = transactionId,
        serverTs = serverTs,
        playbackEndsAt = playbackEndsAt,
        rewardTurbo = rewardTurbo,
        rewardGalaxy = rewardGalaxy,
        resolvedTips =
            resolvedTips.map {
                ResolvedTipCharacter(
                    name = it.name,
                    iconUrl = it.iconUrl,
                    styleName = it.styleName,
                    characterId = it.characterId,
                    occurrenceIndex = it.occurrenceIndex,
                    occurrenceId = it.occurrenceId,
                )
            },
    )
