package com.example.telepse.tips

import com.example.telepse.data.model.ActiveTipConfigResponse
import com.example.telepse.data.model.TipCharacterCatalogItem
import com.example.telepse.data.model.TipConfigItemDto
import com.example.telepse.data.model.TipConfigPayloadDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveTvTipQuotePlannerTest {
    private val eventId = "11111111-1111-1111-1111-111111111111"

    @Test
    fun legacyMode_noQuoteRequired() {
        val cart =
            TipCartState(
                eventId = eventId,
                lines = listOf(TipCartLine(TipCharacterCatalogItem("a", "A", 5.0), 1)),
            )
        val result =
            LiveTvTipQuotePlanner.plan(
                cart,
                ActiveTipConfigResponse(mode = "legacy"),
            )
        assertEquals(LiveTvTipQuotePlanner.PlanResult.LegacyNoQuoteRequired, result)
    }

    @Test
    fun liveMode_mapsCartToQuoteLines() {
        val cart =
            TipCartState(
                eventId = eventId,
                lines = listOf(TipCartLine(TipCharacterCatalogItem("char-a", "Wave", 5.0), 2)),
            )
        val config =
            ActiveTipConfigResponse(
                mode = "live",
                version = 3,
                config =
                    TipConfigPayloadDto(
                        drawerItems =
                            listOf(
                                TipConfigItemDto(
                                    itemId = "item-1",
                                    characterId = "char-a",
                                    priceMinor = 500,
                                ),
                            ),
                    ),
            )
        val result = LiveTvTipQuotePlanner.plan(cart, config)
        assertTrue(result is LiveTvTipQuotePlanner.PlanResult.Ready)
        val ready = result as LiveTvTipQuotePlanner.PlanResult.Ready
        assertEquals(1000, ready.planned.expectedTotalMinor)
        assertEquals(3, ready.planned.request.configVersion)
        assertEquals(1, ready.planned.request.lines.size)
        assertEquals(2, ready.planned.request.lines.first().qty)
    }

    @Test
    fun liveMode_unmappedCharacter_fails() {
        val cart =
            TipCartState(
                eventId = eventId,
                lines = listOf(TipCartLine(TipCharacterCatalogItem("unknown", "X", 5.0), 1)),
            )
        val config =
            ActiveTipConfigResponse(
                mode = "live",
                version = 1,
                config = TipConfigPayloadDto(),
            )
        val result = LiveTvTipQuotePlanner.plan(cart, config)
        assertTrue(result is LiveTvTipQuotePlanner.PlanResult.Failed)
    }
}
