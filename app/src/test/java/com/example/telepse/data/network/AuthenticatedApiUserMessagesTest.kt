package com.example.telepse.data.network

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthenticatedApiUserMessagesTest {
    @Test
    fun authenticated401_usesSessionExpiredCopy() {
        val msg =
            AuthenticatedApiUserMessages.messageFor(
                AuthenticatedApiHttpException(401, authPresent = true, operation = "test"),
            )
        assertTrue(msg.contains("Session expired"))
    }

    @Test
    fun serverError_doesNotMentionSessionExpired() {
        val msg =
            AuthenticatedApiUserMessages.messageFor(
                AuthenticatedApiHttpException(503, authPresent = true, operation = "test"),
            )
        assertFalse(msg.contains("Session expired"))
    }
}
