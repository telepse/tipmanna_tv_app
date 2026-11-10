package com.example.telepse.tips

import com.example.telepse.data.model.TipCharacterCatalogItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LiveTvCartDebitMapperTest {
    private val eventId = "11111111-1111-1111-1111-111111111111"

    @Test
    fun expandsQuantities_andBuildsFingerprint() {
        val cart =
            TipCartState(
                eventId = eventId,
                lines =
                    listOf(
                        TipCartLine(TipCharacterCatalogItem("a", "A", 5.0), 2),
                        TipCartLine(TipCharacterCatalogItem("b", "B", 3.0), 1),
                    ),
            )
        val lines = LiveTvCartDebitMapper.expandCharacterLines(cart, timeLabel = "12:00")
        assertEquals(3, lines.size)
        assertEquals(listOf("a", "a", "b"), lines.map { it.characterId })
        assertEquals("a:2|b:1", LiveTvCartDebitMapper.cartLinesKey(cart))

        val request =
            LiveTvCartDebitMapper.buildRequest(
                cart = cart,
                pin = "1234",
                amountGbp = 13.0,
                tipQuoteId = "22222222-2222-2222-2222-222222222222",
                timeLabel = "12:00",
            )
        assertEquals(13.0, request.amount, 0.001)
        assertEquals("kotlin_tv", request.paymentSurface)
        assertEquals("tv", request.deviceCategory)
        assertEquals("22222222-2222-2222-2222-222222222222", request.tipQuoteId)
        assertEquals(3, request.characters.size)

        val fp = LiveTvCartDebitMapper.fingerprint(cart, "1234", 13.0)
        assertEquals("a:2|b:1", fp.characterId)
        assertEquals(13.0, fp.amount, 0.001)
    }

    @Test
    fun tipQuoteIdOptional_whenLegacy() {
        val cart =
            TipCartState(
                eventId = eventId,
                lines = listOf(TipCartLine(TipCharacterCatalogItem("a", "A", 5.0), 1)),
            )
        val request =
            LiveTvCartDebitMapper.buildRequest(
                cart = cart,
                pin = "9999",
                amountGbp = 5.0,
                tipQuoteId = null,
                timeLabel = "09:00",
            )
        assertNull(request.tipQuoteId)
    }
}
