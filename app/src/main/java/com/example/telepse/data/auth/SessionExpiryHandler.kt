package com.example.telepse.data.auth

import com.example.telepse.data.network.AuthenticatedSessionPolicy
import com.example.telepse.data.repository.TipperSession
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Clears local tipper session on authenticated HTTP 401. [TipperSession.signOut] is idempotent.
 */
@Singleton
class SessionExpiryHandler @Inject constructor(
    private val tipperSession: TipperSession,
) {
    /**
     * @return true if the session was cleared due to an authenticated 401.
     */
    fun invalidateSessionIfNeeded(throwable: Throwable): Boolean {
        val failure = AuthenticatedSessionPolicy.authenticatedHttpFailure(throwable) ?: return false
        val (httpStatus, authPresent) = failure
        if (!AuthenticatedSessionPolicy.shouldClearLocalSession(httpStatus, authPresent)) {
            return false
        }
        tipperSession.signOut()
        return true
    }
}
