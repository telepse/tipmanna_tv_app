package com.example.telepse.tips

import com.example.telepse.data.model.WalletDebitRequest
import java.util.UUID

/** Transaction-defining inputs for a single logical wallet tip (excluding display-only fields). */
data class WalletDebitFingerprint(
    val eventId: String,
    val characterId: String,
    val amount: Double,
    val pin: String,
)

data class PendingWalletDebit(
    val idempotencyKey: String,
    val identity: WalletDebitTipIdentity,
    val requestSnapshot: WalletDebitRequestSnapshot,
) {
    fun requestWithPin(pin: String): WalletDebitRequest = requestSnapshot.toRequest(pin)
}

data class WalletDebitSubmitPlan(
    val idempotencyKey: String,
    val request: WalletDebitRequest,
    val pendingAfterSubmit: PendingWalletDebit,
)

/** User changed tip identity while an unresolved debit is still pending. */
class WalletDebitPendingConflictException(
    message: String = "Finish or clear your pending wallet tip before starting a different tip.",
) : Exception(message)

/**
 * Plans idempotent wallet debits: one key per logical tip identity, reused on retry (incl. PIN re-entry).
 * Backend dedupe is in-memory (~5 min) per tipper+key — not durable across restarts/instances.
 */
object WalletDebitSubmissionPlanner {
    fun planSubmit(
        pending: PendingWalletDebit?,
        fingerprint: WalletDebitFingerprint,
        buildNewRequest: () -> WalletDebitRequest,
    ): WalletDebitSubmitPlan {
        val identity = WalletDebitTipIdentity.from(fingerprint)
        if (pending != null && pending.identity != identity) {
            throw WalletDebitPendingConflictException()
        }
        if (pending != null) {
            val request = pending.requestWithPin(fingerprint.pin)
            return WalletDebitSubmitPlan(
                idempotencyKey = pending.idempotencyKey,
                request = request,
                pendingAfterSubmit = pending,
            )
        }
        val key = UUID.randomUUID().toString()
        val request = buildNewRequest()
        val snapshot = WalletDebitRequestSnapshot.from(request)
        val newPending =
            PendingWalletDebit(
                idempotencyKey = key,
                identity = identity,
                requestSnapshot = snapshot,
            )
        return WalletDebitSubmitPlan(
            idempotencyKey = key,
            request = request,
            pendingAfterSubmit = newPending,
        )
    }
}
