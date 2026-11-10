package com.example.telepse.tips

import com.example.telepse.data.network.WalletHttpException
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class WalletDebitFailureClassifierTest {
    @Test
    fun ioFailure_isUnknownOutcome() {
        assertEquals(
            WalletDebitFailureClassification.UnknownOutcome,
            WalletDebitFailureClassifier.classify(IOException("timeout")),
        )
    }

    @Test
    fun httpTimeout_isUnknownOutcome() {
        assertEquals(
            WalletDebitFailureClassification.UnknownOutcome,
            WalletDebitFailureClassifier.classify(HttpRequestTimeoutException("", null)),
        )
    }

    @Test
    fun http503_isUnknownOutcome() {
        assertEquals(
            WalletDebitFailureClassification.UnknownOutcome,
            WalletDebitFailureClassifier.classify(
                WalletHttpException(httpStatus = 503, authPresent = true),
            ),
        )
    }

    /**
     * 429 is treated as confirmed rejection because today's global `/api/v1` rate limiter runs
     * before wallet debit route handling (see backend `index.ts`). If middleware order changes,
     * this classification may need revisiting — not a permanent guarantee independent of order.
     */
    @Test
    fun http429_isConfirmedRejection_givenGlobalLimiterBeforeDebitRoute() {
        val result =
            WalletDebitFailureClassifier.classify(
                WalletHttpException(httpStatus = 429, authPresent = true),
            )
        assertTrue(result is WalletDebitFailureClassification.ConfirmedRejection)
        assertEquals(429, (result as WalletDebitFailureClassification.ConfirmedRejection).httpStatus)
    }

    @Test
    fun http400_isConfirmedRejection() {
        val result =
            WalletDebitFailureClassifier.classify(
                WalletHttpException(httpStatus = 400, authPresent = true, apiMessage = "Incorrect wallet PIN"),
            )
        assertTrue(result is WalletDebitFailureClassification.ConfirmedRejection)
        assertEquals(400, (result as WalletDebitFailureClassification.ConfirmedRejection).httpStatus)
    }

    @Test
    fun http401_isSessionAuth() {
        assertEquals(
            WalletDebitFailureClassification.SessionAuth,
            WalletDebitFailureClassifier.classify(
                WalletHttpException(httpStatus = 401, authPresent = true),
            ),
        )
    }

    @Test
    fun cancellation_isCancelled() {
        assertEquals(
            WalletDebitFailureClassification.Cancelled,
            WalletDebitFailureClassifier.classify(CancellationException("job cancelled")),
        )
    }
}
