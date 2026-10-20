package com.example.telepse.livetv

import com.example.telepse.data.network.AuthenticatedSessionPolicy
import com.example.telepse.data.network.ScheduleHttpException

object LiveScheduleUserMessages {
    fun fromThrowable(e: Throwable): String {
        if (e is ScheduleHttpException) {
            return when (e.httpStatus) {
                401 ->
                    if (e.authPresent) {
                        AuthenticatedSessionPolicy.SESSION_EXPIRED_USER_MESSAGE
                    } else {
                        "Sign in required to load the schedule. Open Journey Hub to log in."
                    }
                403 ->
                    "Could not load schedule (HTTP 403). Check connection and try Refresh."
                in 500..599 -> "Schedule server error (${e.httpStatus}). Try again shortly."
                else -> "Schedule request failed (HTTP ${e.httpStatus}). Try Refresh."
            }
        }
        val msg = e.message.orEmpty()
        if (msg.contains("StandaloneCoroutine", ignoreCase = true) ||
            msg.contains("Standalone coroutine", ignoreCase = true)
        ) {
            return "Temporary error while loading the schedule. Try Refresh."
        }
        if (e is kotlinx.coroutines.CancellationException) {
            return "Schedule load was interrupted. Try Refresh."
        }
        return msg.ifBlank { "Schedule load failed" }
    }
}
