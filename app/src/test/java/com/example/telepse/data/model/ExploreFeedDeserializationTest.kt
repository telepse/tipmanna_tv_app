package com.example.telepse.data.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExploreFeedDeserializationTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    @Test
    fun batchResponse_parsesItemsAndSeed() {
        val raw =
            """
            {
              "items": [
                {
                  "id": "11111111-1111-4111-8111-111111111111",
                  "title": "Sample video",
                  "url": "https://www.youtube.com/watch?v=Ffs0YHm2jks",
                  "isLive": true,
                  "event": {
                    "eventName": "Gala Night",
                    "hostName": "Host One",
                    "username": "hostone"
                  }
                }
              ],
              "seed": "seed-abc"
            }
            """.trimIndent()
        val parsed = json.decodeFromString<ExploreFeedBatchResponse>(raw)
        assertEquals("seed-abc", parsed.seed)
        assertEquals(1, parsed.items.size)
        assertEquals("Sample video", parsed.items[0].title)
        assertTrue(parsed.items[0].isLive)
        assertEquals("Gala Night", parsed.items[0].event?.eventName)
    }

    @Test
    fun batchResponse_emptyItems() {
        val raw = """{"items":[],"seed":"x"}"""
        val parsed = json.decodeFromString<ExploreFeedBatchResponse>(raw)
        assertTrue(parsed.items.isEmpty())
        assertEquals("x", parsed.seed)
    }

    @Test
    fun batchRequest_defaultsTrendsMode() {
        val req = ExploreFeedBatchRequest(count = 8)
        assertEquals("trends", req.feedMode)
        assertFalse(req.excludeChannelIds.isNotEmpty())
    }
}
