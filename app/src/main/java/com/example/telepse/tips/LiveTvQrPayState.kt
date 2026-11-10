package com.example.telepse.tips

enum class LiveTvQrPayPhase {
    Idle,
    CreatingSession,
    AwaitingPhonePayment,
    Paid,
    Failed,
    Expired,
    Cancelled,
}

data class LiveTvQrPayState(
    val phase: LiveTvQrPayPhase = LiveTvQrPayPhase.Idle,
    val message: String? = null,
    val checkoutUrl: String? = null,
    val sessionId: String? = null,
    val expiresAtIso: String? = null,
    val transactionId: String? = null,
) {
    val isActive: Boolean
        get() = phase != LiveTvQrPayPhase.Idle

    val blocksCartMutation: Boolean
        get() =
            phase == LiveTvQrPayPhase.CreatingSession ||
                phase == LiveTvQrPayPhase.AwaitingPhonePayment
}
