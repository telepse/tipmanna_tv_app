package com.example.telepse.tips

import com.example.telepse.data.network.AuthenticatedSessionPolicy
import com.example.telepse.data.network.WalletHttpException

object WalletUserFacingErrors {
    fun messageFor(throwable: Throwable): String {
        if (throwable is WalletHttpException) {
            return when (throwable.httpStatus) {
                401 ->
                    if (throwable.authPresent) {
                        AuthenticatedSessionPolicy.SESSION_EXPIRED_USER_MESSAGE
                    } else {
                        "Sign in required. Open Tips & wallet after logging in from Journey Hub."
                    }
                403 -> "Wallet not available for this account."
                404 -> "Event not found or catalog unavailable."
                423 -> "Wallet is locked. Use PIN reset on web."
                in 500..599 -> "Server error (${throwable.httpStatus}). Try again shortly."
                else -> "Request failed (HTTP ${throwable.httpStatus})."
            }
        }
        val msg = throwable.message.orEmpty()
        if (msg.contains("401")) return "Authentication failed. Sign in again from Journey Hub."
        if (msg.contains("403")) return "Wallet not available for this account."
        if (msg.contains("423")) return "Wallet is locked. Use PIN reset on web."
        return msg.ifBlank { "Request failed. Check connection and try again." }
    }
}
