package com.example.telepse.tips

import com.example.telepse.data.network.WalletHttpException
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import java.io.IOException
import kotlinx.coroutines.CancellationException

/**
 * Classifies wallet debit failures for idempotent retry and user messaging.
 *
 * **Unknown outcome** — debit may have succeeded (timeout, transport, 5xx). Keep pending + same key.
 * **Confirmed rejection** — API returned a client/server error response without success body; backend
 * did not cache idempotent success ([backend wallet-debit-idempotency.ts] only stores on 200 success).
 * **Session auth** — 401 with existing session policy (handled separately).
 *
 * HTTP 400 may carry a safe [WalletHttpException.apiMessage] when the repository parsed the body;
 * PIN vs insufficient balance is not distinguished when the body is missing.
 */
sealed interface WalletDebitFailureClassification {
    data object UnknownOutcome : WalletDebitFailureClassification

    data class ConfirmedRejection(
        val httpStatus: Int,
    ) : WalletDebitFailureClassification

    data object SessionAuth : WalletDebitFailureClassification

    data object Cancelled : WalletDebitFailureClassification
}

object WalletDebitFailureClassifier {
    fun classify(throwable: Throwable): WalletDebitFailureClassification {
        if (throwable is CancellationException) {
            return WalletDebitFailureClassification.Cancelled
        }
        val unwrapped = unwrap(throwable)
        if (unwrapped is CancellationException) {
            return WalletDebitFailureClassification.Cancelled
        }
        if (isTransportFailure(unwrapped)) {
            return WalletDebitFailureClassification.UnknownOutcome
        }
        if (unwrapped is WalletHttpException) {
            return when (unwrapped.httpStatus) {
                401 -> WalletDebitFailureClassification.SessionAuth
                in 500..599 -> WalletDebitFailureClassification.UnknownOutcome
                else -> WalletDebitFailureClassification.ConfirmedRejection(unwrapped.httpStatus)
            }
        }
        return WalletDebitFailureClassification.UnknownOutcome
    }

    fun shouldRetainPending(classification: WalletDebitFailureClassification): Boolean =
        when (classification) {
            WalletDebitFailureClassification.Cancelled -> true
            else -> true
        }

    private fun unwrap(throwable: Throwable): Throwable {
        var current: Throwable? = throwable
        while (current is java.util.concurrent.ExecutionException && current.cause != null) {
            current = current.cause
        }
        return current ?: throwable
    }

    private fun isTransportFailure(throwable: Throwable): Boolean {
        var cause: Throwable? = throwable
        while (cause != null) {
            when (cause) {
                is IOException,
                is ConnectTimeoutException,
                is HttpRequestTimeoutException,
                -> return true
            }
            cause = cause.cause
        }
        return false
    }
}
