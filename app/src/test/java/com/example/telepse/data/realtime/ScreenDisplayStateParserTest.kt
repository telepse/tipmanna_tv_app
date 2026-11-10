package com.example.telepse.data.realtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
        assertEquals(false, payload.hideProgressBarAmount)
        assertTrue(payload.eventBackgroundPresent)
        assertTrue(payload.performersPresent)
    }

    @Test
    fun parse_floatTipUsagePercentage_doesNotFailWholePayload() {
        // Prisma TipUsageDisclosure.percentage is Float — full kotlinx decode used to fail.
        val raw =
            """
            {
              "eventId": "evt-2",
              "eventName": "Room",
              "hideProgressBarAmount": true,
              "showLatestAmount": false,
              "backgroundType": "gaming",
              "eventBackground": null,
              "tipUsageDisclosure": [
                {
                  "id": "tud-1",
                  "usageType": "CHARITY",
                  "percentage": 33.5,
                  "customLabel": "Food"
                }
              ],
              "performers": [
                {
                  "id": "p1",
                  "name": "Act",
                  "amountReceived": 12.5,
                  "isPinned": true
                }
              ]
            }
            """.trimIndent()
        val payload = parseScreenDisplayStatePayload(raw)
        assertNotNull(payload)
        assertEquals("evt-2", payload!!.eventId)
        assertEquals(true, payload.hideProgressBarAmount)
        assertEquals("gaming", payload.backgroundType)
        assertNull(payload.eventBackground)
        assertTrue(payload.eventBackgroundPresent)
        assertEquals(1, payload.tipUsageDisclosure?.size)
        assertEquals(33, payload.tipUsageDisclosure?.first()?.percentage)
        assertEquals(1, payload.performers?.size)
        assertEquals("Act", payload.performers?.first()?.name)
    }

    @Test
    fun parse_malformedJson_returnsNull() {
        assertNull(parseScreenDisplayStatePayload("not-json"))
        assertNull(parseScreenDisplayStatePayload(""))
    }

    @Test
    fun parse_missingOptionalLists_presenceFalse() {
        val raw =
            """
            {
              "eventId": "evt-3",
              "hideProgressBarAmount": false,
              "backgroundType": "default"
            }
            """.trimIndent()
        val payload = parseScreenDisplayStatePayload(raw)
        assertNotNull(payload)
        assertFalse(payload!!.eventBackgroundPresent)
        assertFalse(payload.tipUsageDisclosurePresent)
        assertFalse(payload.performersPresent)
    }

    @Test
    fun parse_floatTipUsagePercentage_parsesDisclosureList() {
        val raw =
            """
            {
              "eventId": "evt-4",
              "hideProgressBarAmount": false,
              "tipUsageDisclosure": [
                {
                  "id": "d1",
                  "usageType": "PERFORMER_WELFARE",
                  "percentage": 33.33
                }
              ]
            }
            """.trimIndent()
        val payload = parseScreenDisplayStatePayload(raw)
        assertNotNull(payload)
        assertEquals(1, payload!!.tipUsageDisclosure?.size)
        assertEquals(33, payload.tipUsageDisclosure?.first()?.percentage)
        assertTrue(payload.hideProgressBarAmountPresent)
    }
}
