package com.example.telepse.data.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class TvEventBootstrapDeserializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun deserialize_minimalBootstrap_parsesEventAndChannels() {
        val body =
            """
            {
              "event": {
                "id": "evt-1",
                "eventName": "Venue Night",
                "hostName": "Host",
                "eventDate": "2025-06-01T00:00:00.000Z",
                "startTime": "19:00",
                "venueLocation": "Hall",
                "eventCode": "ABC123"
              },
              "realtime": {
                "pusherKey": "pk-test",
                "pusherCluster": "eu",
                "channels": {
                  "public": "event-evt-1",
                  "private": "private-event-evt-1"
                },
                "events": {
                  "tipAnimation": "tip-animation",
                  "screenDisplayState": "screen-display-state"
                }
              },
              "animationContractUrl": "/api/v1/tv-app/animations/contract",
              "animationStylesSpecUrl": "/api/v1/tv-app/animations/styles"
            }
            """.trimIndent()

        val bootstrap = json.decodeFromString<TvEventBootstrap>(body)

        assertEquals("evt-1", bootstrap.event.id)
        assertEquals("Venue Night", bootstrap.event.eventName)
        assertEquals("event-evt-1", bootstrap.realtime.channels.public)
        assertEquals("private-event-evt-1", bootstrap.realtime.channels.private)
        assertEquals("/api/v1/tv-app/animations/contract", bootstrap.animationContractUrl)
        assertNotNull(bootstrap.realtime.events?.tipAnimation)
    }
}
