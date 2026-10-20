package com.example.telepse.data.network

/**
 * When an authenticated request (Bearer was attached) returns HTTP 401, treat the local session as invalid.
 * Does not apply to 403 or to unauthenticated calls (no token stored).
 */
object AuthenticatedSessionPolicy {
    const val SESSION_EXPIRED_USER_MESSAGE =
        "Session expired. Sign in again from Journey Hub."

    fun shouldClearLocalSession(
        httpStatus: Int,
        authPresent: Boolean,
    ): Boolean = httpStatus == 401 && authPresent

    fun authenticatedHttpFailure(throwable: Throwable): Pair<Int, Boolean>? =
        when (throwable) {
            is ScheduleHttpException -> throwable.httpStatus to throwable.authPresent
            is WalletHttpException -> throwable.httpStatus to throwable.authPresent
            is AuthenticatedApiHttpException -> throwable.httpStatus to throwable.authPresent
            else -> null
        }
}
