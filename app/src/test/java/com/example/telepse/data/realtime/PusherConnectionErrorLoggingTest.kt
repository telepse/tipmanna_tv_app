package com.example.telepse.data.realtime

import org.junit.Assert.assertTrue
import org.junit.Test

class PusherConnectionErrorLoggingTest {

    @Test
    fun format_nullCodeAndMessage_usesPlaceholders() {
        val line = formatPusherConnectionError(null, null, null)
        assertTrue(line.contains("message=(null)"))
        assertTrue(line.contains("code=(null)"))
    }

    @Test
    fun format_withValues_includesMessageAndCode() {
        val line =
            formatPusherConnectionError(
                message = "An exception was thrown by the websocket",
                code = "4200",
                exception = IllegalStateException("broken pipe"),
            )
        assertTrue(line.contains("message=An exception was thrown by the websocket"))
        assertTrue(line.contains("code=4200"))
        assertTrue(line.contains("exception=broken pipe"))
    }
}
