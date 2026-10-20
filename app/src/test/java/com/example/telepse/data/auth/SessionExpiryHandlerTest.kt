package com.example.telepse.data.auth

import com.example.telepse.data.network.AuthenticatedApiHttpException
import com.example.telepse.data.network.ScheduleHttpException
import com.example.telepse.data.network.WalletHttpException
import com.example.telepse.data.repository.TipperSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionExpiryHandlerTest {
    @Test
    fun authenticated401_clearsSession() {
        val session = RecordingTipperSession()
        val handler = SessionExpiryHandler(session)
        assertTrue(
            handler.invalidateSessionIfNeeded(
                WalletHttpException(httpStatus = 401, authPresent = true),
            ),
        )
        assertEquals(1, session.signOutCount)
        assertTrue(handler.invalidateSessionIfNeeded(WalletHttpException(401, authPresent = true)))
        assertEquals(2, session.signOutCount)
    }

    @Test
    fun non401_doesNotSignOut() {
        val session = RecordingTipperSession()
        val handler = SessionExpiryHandler(session)
        assertFalse(
            handler.invalidateSessionIfNeeded(
                ScheduleHttpException(httpStatus = 503, authPresent = true),
            ),
        )
        assertEquals(0, session.signOutCount)
    }

    @Test
    fun authenticatedApi401_clearsSession() {
        val session = RecordingTipperSession()
        val handler = SessionExpiryHandler(session)
        assertTrue(
            handler.invalidateSessionIfNeeded(
                AuthenticatedApiHttpException(401, authPresent = true, operation = "explore"),
            ),
        )
        assertEquals(1, session.signOutCount)
    }

    @Test
    fun unauthorizedWithoutToken_doesNotSignOut() {
        val session = RecordingTipperSession()
        val handler = SessionExpiryHandler(session)
        assertFalse(
            handler.invalidateSessionIfNeeded(
                ScheduleHttpException(httpStatus = 401, authPresent = false),
            ),
        )
        assertEquals(0, session.signOutCount)
    }

    private class RecordingTipperSession : TipperSession {
        var signOutCount = 0
        override fun signOut() {
            signOutCount++
        }
    }
}
