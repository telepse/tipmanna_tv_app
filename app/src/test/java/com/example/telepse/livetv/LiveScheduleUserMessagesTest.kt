package com.example.telepse.livetv

import com.example.telepse.data.network.ScheduleHttpException
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveScheduleUserMessagesTest {
    @Test
    fun http401_withToken_sessionExpiredMessage() {
        val msg =
            LiveScheduleUserMessages.fromThrowable(
                ScheduleHttpException(httpStatus = 401, authPresent = true),
            )
        assertTrue(msg.contains("Session expired"))
    }

    @Test
    fun http401_withoutToken_signInRequired() {
        val msg =
            LiveScheduleUserMessages.fromThrowable(
                ScheduleHttpException(httpStatus = 401, authPresent = false),
            )
        assertTrue(msg.contains("Sign in required"))
    }

    @Test
    fun http503_mentionsServerError() {
        val msg =
            LiveScheduleUserMessages.fromThrowable(
                ScheduleHttpException(httpStatus = 503, authPresent = false),
            )
        assertTrue(msg.contains("503"))
    }

    @Test
    fun standaloneCoroutine_notConnectionIssueLabel() {
        val msg =
            LiveScheduleUserMessages.fromThrowable(
                RuntimeException("Standalone coroutine was cancelled"),
            )
        assertTrue(msg.contains("Temporary error"))
    }
}
