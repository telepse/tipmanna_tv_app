package com.example.telepse.data.realtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ScreenDisplayStateParserTest {

    @Test
    fun parse_validBroadcastPayload_succeeds() {
        val raw =
            """
            {
              "eventId": "evt-1",
              "eventName": "Gala",
              "amountRaised": 500,
              "numOfTips": 12,
              "hideProgressBarAmount": false,
              "showLatestAmount": true,
              "backgroundType": "color",
              "eventBackground": "#ffffff",
              "funnelPosition": "bottom",
              "projects": [],
              "performers": []
            }
            """.trimIndent()
        val payload = parseScreenDisplayStatePayload(raw)
        assertNotNull(payload)
        assertEquals("evt-1", payload!!.eventId)
        assertEquals("Gala", payload.eventName)
        assertEquals(500.0, payload.amountRaised)
    }

    @Test
    fun parse_malformedJson_returnsNull() {
        assertNull(parseScreenDisplayStatePayload("not-json"))
        assertNull(parseScreenDisplayStatePayload(""))
    }
}
