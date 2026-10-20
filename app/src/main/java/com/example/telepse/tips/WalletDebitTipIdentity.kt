package com.example.telepse.tips

/** Tip-defining fields for idempotency (excludes PIN and display-only churn). */
data class WalletDebitTipIdentity(
    val eventId: String,
    val characterId: String,
    val amount: Double,
) {
    companion object {
        fun from(fingerprint: WalletDebitFingerprint): WalletDebitTipIdentity =
            WalletDebitTipIdentity(
                eventId = fingerprint.eventId,
                characterId = fingerprint.characterId,
                amount = fingerprint.amount,
            )
    }
}
