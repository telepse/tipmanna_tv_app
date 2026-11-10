package com.example.telepse.tips

import com.example.telepse.data.network.WalletHttpException
import org.junit.Assert.assertTrue
import org.junit.Test

class WalletUserFacingErrorsTest {
    @Test
    fun unauthorized_withToken_mentionsSessionExpired() {
        val msg =
            WalletUserFacingErrors.messageFor(
                WalletHttpException(httpStatus = 401, authPresent = true),
            )
        assertTrue(msg.contains("Session expired"))
    }

    @Test
    fun unauthorized_withoutToken_mentionsSignIn() {
        val msg =
            WalletUserFacingErrors.messageFor(
                WalletHttpException(httpStatus = 401, authPresent = false),
            )
        assertTrue(msg.contains("Sign in required"))
    }

    @Test
    fun networkFailure_doesNotClaimSessionExpired() {
        val msg = WalletUserFacingErrors.messageFor(IllegalStateException("Unable to resolve host"))
        assertTrue(!msg.contains("Session expired"))
    }
}
