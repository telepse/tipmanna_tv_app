package com.example.telepse.tips

import com.example.telepse.data.network.WalletHttpException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import kotlinx.coroutines.CancellationException
import org.junit.Test
import java.io.IOException

class WalletDebitSubmitMessagesTest {
    @Test
    fun unknownOutcome_usesHonestCopy() {
        val msg =
            WalletDebitSubmitMessages.messageFor(
                WalletDebitFailureClassification.UnknownOutcome,
                IOException("timeout"),
            )
        assertTrue(msg!!.contains("couldn't confirm"))
    }

    @Test
    fun confirmedRejection_withApiMessage_showsSafeApiText() {
        val msg =
            WalletDebitSubmitMessages.messageFor(
                WalletDebitFailureClassification.ConfirmedRejection(400),
                WalletHttpException(
                    httpStatus = 400,
                    authPresent = true,
                    apiMessage = "Incorrect wallet PIN",
                ),
            )
        assertEquals("Incorrect wallet PIN", msg)
    }

    @Test
    fun cancelled_returnsNullMessage() {
        assertNull(
            WalletDebitSubmitMessages.messageFor(
                WalletDebitFailureClassification.Cancelled,
                CancellationException(),
            ),
        )
    }

    @Test
    fun sessionAuth_delegatesToWalletUserFacingErrors() {
        val msg =
            WalletDebitSubmitMessages.messageFor(
                WalletDebitFailureClassification.SessionAuth,
                WalletHttpException(httpStatus = 401, authPresent = true),
            )
        assertTrue(msg!!.contains("Session expired"))
    }
}
