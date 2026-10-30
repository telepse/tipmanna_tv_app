package com.example.telepse.tips

/**
 * LIVE TV wallet checkout UI/state. Does not imply QR/card readiness.
 * Amounts shown are provisional until the server returns success (or a tip quote locks total).
 */
enum class LiveTvWalletPayPhase {
    Idle,
    CheckingPrerequisites,
    EnterPin,
    Submitting,
    Success,
    Failed,
    /** Timeout/5xx/transport — do not treat as confirmed; retry with same idempotency key. */
    UnconfirmedPending,
}

data class LiveTvWalletPayState(
    val phase: LiveTvWalletPayPhase = LiveTvWalletPayPhase.Idle,
    val pinDigits: String = "",
    val message: String? = null,
    val walletBalance: String? = null,
    val walletStatus: String? = null,
    val walletExists: Boolean = false,
    /** Catalog/cart display total — not trusted as debit amount. */
    val provisionalDisplayTotal: Double? = null,
    /** Tip-quote locked GBP major units when live tip config applies. */
    val quoteLockedTotalGbp: Double? = null,
    val newBalance: String? = null,
    val transactionId: String? = null,
    val hasUnresolvedPending: Boolean = false,
) {
    val isActive: Boolean
        get() = phase != LiveTvWalletPayPhase.Idle

    val blocksCartMutation: Boolean
        get() =
            phase == LiveTvWalletPayPhase.CheckingPrerequisites ||
                phase == LiveTvWalletPayPhase.Submitting ||
                phase == LiveTvWalletPayPhase.EnterPin ||
                phase == LiveTvWalletPayPhase.UnconfirmedPending
}
