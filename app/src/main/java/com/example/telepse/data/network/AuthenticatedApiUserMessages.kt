package com.example.telepse.data.network

object AuthenticatedApiUserMessages {
    fun messageFor(throwable: Throwable): String {
        if (throwable is AuthenticatedApiHttpException) {
            return when (throwable.httpStatus) {
                401 ->
                    if (throwable.authPresent) {
                        AuthenticatedSessionPolicy.SESSION_EXPIRED_USER_MESSAGE
                    } else {
                        "Sign in required. Open Journey Hub to log in."
                    }
                403 -> "Not allowed for this account."
                in 500..599 -> "Server error (${throwable.httpStatus}). Try again shortly."
                else -> "Request failed (HTTP ${throwable.httpStatus})."
            }
        }
        return throwable.message.orEmpty().ifBlank { "Request failed. Check connection and try again." }
    }
}
