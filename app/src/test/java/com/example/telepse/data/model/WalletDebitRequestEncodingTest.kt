package com.example.telepse.data.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Regression: explicit JSON nulls on optional-only backend fields caused HTTP 400 "Validation failed". */
class WalletDebitRequestEncodingTest {
    private val debitJson =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            explicitNulls = false
        }

    @Test
    fun debitBody_omitsNullOptionals_butKeepsPaymentSurfaceDefaults() {
        val body =
            WalletDebitRequest(
                pin = "1234",
                amount = 5.0,
                eventId = "550e8400-e29b-41d4-a716-446655440000",
                characters =
                    listOf(
                        WalletDebitCharacterLine(
                            name = "Star",
                            time = "12:00",
                            price = 5.0,
                            characterId = "550e8400-e29b-41d4-a716-446655440001",
                        ),
                    ),
                tipQuoteId = "550e8400-e29b-41d4-a716-446655440004",
            )
        val encoded = debitJson.encodeToString(WalletDebitRequest.serializer(), body)
        val root = debitJson.parseToJsonElement(encoded).jsonObject
        assertTrue(root.containsKey("paymentSurface"))
        assertTrue(root.containsKey("deviceCategory"))
        assertTrue(root.containsKey("tipQuoteId"))
        assertFalse(root.containsKey("playbackOffsetSec"))
        assertFalse(root.containsKey("clientSessionId"))
        assertFalse(root.containsKey("paymentContentId"))
    }

    @Test
    fun debitBody_jsonObject_hasNoExplicitNullValues() {
        val body =
            WalletDebitRequest(
                pin = "5678",
                amount = 10.0,
                eventId = "11111111-1111-1111-1111-111111111111",
                characters =
                    listOf(
                        WalletDebitCharacterLine(
                            name = "Wave",
                            time = "09:15",
                            price = 10.0,
                            characterId = "22222222-2222-2222-2222-222222222222",
                        ),
                    ),
            )
        val root = debitJson.parseToJsonElement(debitJson.encodeToString(WalletDebitRequest.serializer(), body)).jsonObject
        for ((_, value) in root) {
            assertFalse("unexpected explicit null in debit JSON", value is JsonNull)
        }
    }
}
