package com.example.telepse.tips

import com.example.telepse.data.network.WalletHttpException

object WalletDebitSubmitMessages {
    const val PENDING_RECOVERY_TITLE = "Unconfirmed wallet tip"

    const val PENDING_RECOVERY_HINT =
        "You have an unfinished wallet tip. Re-enter your PIN and retry, or refresh your balance first."

    const val PENDING_RECOVERY_DETAIL =
        "We could not confirm whether this tip completed. Retry must use the same pending tip " +
            "(not a new one) — the app will send the same idempotency key. Re-enter your wallet PIN and confirm again."

    const val PENDING_DISCARD_BUTTON = "Discard local recovery only"

    const val PENDING_DISCARD_EXPLAINER =
        "Discards only this TV's saved recovery state. It does not undo a tip that may already have completed. " +
            "Check your balance and recent activity first."

    const val PENDING_DISCARD_ACK =
        "Local pending tip cleared. This does not undo a tip that may already have completed — " +
            "check your balance and recent activity."

    const val PENDING_IDENTITY_LOCKED =
        "This tip is locked while recovery is active. Retry with the same event and item, " +
            "or discard local recovery after checking your balance."

    const val UNKNOWN_OUTCOME_MESSAGE =
        "We couldn't confirm whether this tip went through. " +
            "Check your wallet balance, then retry this tip if needed. " +
            "Don't send a new tip until you're sure the first one didn't complete."

    fun messageFor(
        classification: WalletDebitFailureClassification,
        throwable: Throwable,
    ): String? =
        when (classification) {
            WalletDebitFailureClassification.Cancelled -> null
            WalletDebitFailureClassification.UnknownOutcome -> UNKNOWN_OUTCOME_MESSAGE
            WalletDebitFailureClassification.SessionAuth ->
                WalletUserFacingErrors.messageFor(throwable)
            is WalletDebitFailureClassification.ConfirmedRejection ->
                confirmedRejectionMessage(throwable, classification.httpStatus)
        }

    private fun confirmedRejectionMessage(throwable: Throwable, httpStatus: Int): String {
        if (throwable is WalletHttpException) {
            val api = throwable.apiMessage?.trim()?.takeIf { it.isNotEmpty() }
            if (api != null && api.length <= 160 && !looksSensitive(api)) {
                return api
            }
            return WalletUserFacingErrors.messageFor(throwable)
        }
        return WalletUserFacingErrors.messageFor(throwable)
    }

    private fun looksSensitive(text: String): Boolean {
        val lower = text.lowercase()
        return text.startsWith("{") ||
            lower.contains("token") ||
            lower.contains("bearer") ||
            lower.contains("password")
    }
}
