package com.example.telepse.tips

import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-flight guard and pending logical debit for wallet tip submit.
 * Pending is cleared on success or explicit dismiss; synced to [WalletDebitPendingStore].
 */
@Singleton
class WalletDebitSubmitSession @Inject constructor(
    private val pendingStore: WalletDebitPendingStore,
) {
    var pendingWalletDebit: PendingWalletDebit? = null
        private set

    var submitInFlight: Boolean = false
        private set

    init {
        restoreFromStore()
    }

    fun tryBeginSubmit(): Boolean {
        if (submitInFlight) return false
        submitInFlight = true
        return true
    }

    fun finishSubmit() {
        submitInFlight = false
    }

    fun planSubmit(
        fingerprint: WalletDebitFingerprint,
        buildNewRequest: () -> com.example.telepse.data.model.WalletDebitRequest,
    ): WalletDebitSubmitPlan {
        val plan =
            WalletDebitSubmissionPlanner.planSubmit(
                pending = pendingWalletDebit,
                fingerprint = fingerprint,
                buildNewRequest = buildNewRequest,
            )
        pendingWalletDebit = plan.pendingAfterSubmit
        persistPending(plan.pendingAfterSubmit)
        return plan
    }

    fun clearAfterAuthoritativeSuccess() {
        pendingWalletDebit = null
        pendingStore.clear()
    }

    /** User confirmed balance/history — abandon unresolved pending (may still retry same key manually). */
    fun dismissUnresolvedPending() {
        pendingWalletDebit = null
        pendingStore.clear()
    }

    fun restoreFromStore() {
        val record = pendingStore.load() ?: return
        if (record.idempotencyKey.isBlank() || record.eventId.isBlank() || record.characterId.isBlank()) {
            pendingStore.clear()
            return
        }
        pendingWalletDebit =
            PendingWalletDebit(
                idempotencyKey = record.idempotencyKey,
                identity =
                    WalletDebitTipIdentity(
                        eventId = record.eventId,
                        characterId = record.characterId,
                        amount = record.amount,
                    ),
                requestSnapshot = record.requestSnapshot,
            )
    }

    private fun persistPending(pending: PendingWalletDebit) {
        pendingStore.save(
            PersistedWalletDebitPending(
                idempotencyKey = pending.idempotencyKey,
                eventId = pending.identity.eventId,
                characterId = pending.identity.characterId,
                amount = pending.identity.amount,
                requestSnapshot = pending.requestSnapshot,
                savedAtEpochMs = System.currentTimeMillis(),
            ),
        )
    }
}
