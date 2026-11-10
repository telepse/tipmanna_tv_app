package com.example.telepse.data.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class WalletModelsDeserializationTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    @Test
    fun walletStatus_parses() {
        val parsed =
            json.decodeFromString<WalletStatusResponse>(
                """{"exists":true,"status":"ACTIVE","balance":"12.50"}""",
            )
        assertEquals("12.50", parsed.balance)
        assertEquals("ACTIVE", parsed.status)
    }

    @Test
    fun characterCatalog_parsesNestedSets() {
        val parsed =
            json.decodeFromString<EventCharacterCatalogResponse>(
                """
                {
                  "eventId": "11111111-1111-4111-8111-111111111111",
                  "characterSets": [{
                    "id": "set-1",
                    "name": "Default",
                    "characters": [{
                      "id": "char-1",
                      "name": "Heart",
                      "price": 5
                    }]
                  }]
                }
                """.trimIndent(),
            )
        assertEquals(1, parsed.characterSets.size)
        assertEquals("Heart", parsed.characterSets[0].characters[0].name)
    }
}
