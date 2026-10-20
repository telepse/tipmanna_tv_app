package com.example.telepse.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EventsByCodeResponseParserTest {

    private val minimalEventJson =
        """
        {
          "id": "evt-1",
          "eventName": "Test Event",
          "hostName": "Host One",
          "eventDate": "2025-01-01T00:00:00.000Z",
          "startTime": "18:00",
          "venueLocation": "Main Hall",
          "eventCode": "CODE1"
        }
        """.trimIndent()

    @Test
    fun parse_rawJsonArray_returnsEvents() {
        val body = "[$minimalEventJson]"
        val events = parseEventsByCodeResponseBody(body)
        assertEquals(1, events.size)
        assertEquals("evt-1", events[0].id)
        assertEquals("Test Event", events[0].eventName)
    }

    @Test
    fun parse_paginatedEnvelope_returnsDataArray() {
        val body =
            """
            {
              "data": [$minimalEventJson],
              "pagination": { "page": 1, "limit": 20, "total": 1, "totalPages": 1 }
            }
            """.trimIndent()
        val events = parseEventsByCodeResponseBody(body)
        assertEquals(1, events.size)
        assertEquals("CODE1", events[0].eventCode)
    }

    @Test
    fun parse_emptyRawArray_returnsEmptyList() {
        val events = parseEventsByCodeResponseBody("[]")
        assertTrue(events.isEmpty())
    }

    @Test
    fun parse_emptyPaginatedData_returnsEmptyList() {
        val body =
            """
            {
              "data": [],
              "pagination": { "page": 1, "limit": 20, "total": 0, "totalPages": 1 }
            }
            """.trimIndent()
        val events = parseEventsByCodeResponseBody(body)
        assertTrue(events.isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun parse_errorObject_throws() {
        parseEventsByCodeResponseBody("""{"error":{"status":404,"message":"Not found"}}""")
    }

    @Test
    fun parse_eventDateNull_succeeds() {
        val eventJson =
            """
            {
              "id": "evt-null-date",
              "eventName": "Staging Room",
              "hostName": "Host",
              "eventDate": null,
              "startTime": "18:00",
              "venueLocation": "Hall",
              "eventCode": "STG1"
            }
            """.trimIndent()
        val events = parseEventsByCodeResponseBody("[$eventJson]")
        assertEquals(1, events.size)
        assertEquals(null, events[0].eventDate)
        assertEquals("STG1", events[0].eventCode)
    }

    @Test
    fun parse_eventDateMissing_succeeds() {
        val eventJson =
            """
            {
              "id": "evt-no-date",
              "eventName": "No Date Field",
              "hostName": "Host",
              "startTime": "18:00",
              "venueLocation": "Hall",
              "eventCode": "NODATE"
            }
            """.trimIndent()
        val events = parseEventsByCodeResponseBody(
            """
            { "data": [$eventJson], "pagination": { "page": 1, "limit": 20, "total": 1, "totalPages": 1 } }
            """.trimIndent(),
        )
        assertEquals(1, events.size)
        assertEquals(null, events[0].eventDate)
    }

    @Test
    fun parse_eventDateValid_nonNull() {
        val events = parseEventsByCodeResponseBody("[$minimalEventJson]")
        assertEquals("2025-01-01T00:00:00.000Z", events[0].eventDate)
    }

    @Test
    fun parse_eventDateMalformedString_stillDecodesEvent() {
        val eventJson =
            """
            {
              "id": "evt-bad-date",
              "eventName": "Bad Date String",
              "hostName": "Host",
              "eventDate": "not-iso",
              "startTime": "18:00",
              "venueLocation": "Hall",
              "eventCode": "BAD1"
            }
            """.trimIndent()
        val events = parseEventsByCodeResponseBody("[$eventJson]")
        assertEquals("not-iso", events[0].eventDate)
    }
}
