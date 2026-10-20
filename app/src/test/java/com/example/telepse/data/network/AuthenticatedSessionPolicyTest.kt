package com.example.telepse.data.network

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthenticatedSessionPolicyTest {
    @Test
    fun clearSession_onlyFor401WithStoredToken() {
        assertTrue(AuthenticatedSessionPolicy.shouldClearLocalSession(401, authPresent = true))
        assertFalse(AuthenticatedSessionPolicy.shouldClearLocalSession(401, authPresent = false))
        assertFalse(AuthenticatedSessionPolicy.shouldClearLocalSession(403, authPresent = true))
        assertFalse(AuthenticatedSessionPolicy.shouldClearLocalSession(503, authPresent = true))
    }

    @Test
    fun extractsWalletAndScheduleFailures() {
        val wallet = WalletHttpException(401, authPresent = true)
        val schedule = ScheduleHttpException(401, authPresent = true)
        assertTrue(AuthenticatedSessionPolicy.authenticatedHttpFailure(wallet) != null)
        assertTrue(AuthenticatedSessionPolicy.authenticatedHttpFailure(schedule) != null)
        assertFalse(AuthenticatedSessionPolicy.authenticatedHttpFailure(IllegalStateException()) != null)
    }

    @Test
    fun extractsAuthenticatedApiHttpException() {
        val failure = AuthenticatedApiHttpException(401, authPresent = true, operation = "explore")
        assertTrue(AuthenticatedSessionPolicy.shouldClearLocalSession(401, failure.authPresent))
    }
}
