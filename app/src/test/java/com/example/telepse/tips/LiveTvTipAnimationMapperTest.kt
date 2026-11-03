package com.example.telepse.tips

import com.example.telepse.data.model.ResolvedTipCharacter
import com.example.telepse.data.model.TipAnimationEvent
import com.example.telepse.data.model.TipCharacterCatalogItem
import com.example.telepse.ui.animations.AnimationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveTvTipAnimationMapperTest {
    @Test
    fun prefersResolvedTipsForPaymentAnimation() {
        val event =
            TipAnimationEvent(
                selectedTips = listOf("Legacy Name"),
                eventId = "ev-1",
                transactionId = "tx-99",
                resolvedTips =
                    listOf(
                        ResolvedTipCharacter(
                            name = "Wave",
                            iconUrl = "https://cdn/wave.png",
                            styleName = "Burst",
                            occurrenceId = "occ-1",
                        ),
                    ),
            )
        val triggers = LiveTvTipAnimationMapper.fromPaymentEvent(event, emptyMap())
        assertEquals(1, triggers.size)
        assertEquals(AnimationType.BURST, triggers[0].animationType)
        assertEquals("https://cdn/wave.png", triggers[0].imageUrl)
        assertTrue(triggers[0].id.startsWith("pay:tx-99:"))
    }

    @Test
    fun fallsBackToCatalogWhenNoResolvedTips() {
        val catalog =
            mapOf(
                "confetti hero" to
                    TipCharacterCatalogItem(
                        id = "c1",
                        name = "Confetti Hero",
                        price = 5.0,
                        iconUrl = "https://cdn/c1.png",
                    ),
            )
        val event =
            TipAnimationEvent(
                selectedTips = listOf("Confetti Hero"),
                transactionId = "tx-abc",
            )
        val triggers = LiveTvTipAnimationMapper.fromPaymentEvent(event, catalog)
        assertEquals(1, triggers.size)
        assertEquals("https://cdn/c1.png", triggers[0].imageUrl)
    }

    @Test
    fun previewIdsAreDistinctAndPrefixed() {
        val character = TipCharacterCatalogItem("id1", "A", 1.0, null)
        val a = LiveTvTipAnimationMapper.preview(character)
        val b = LiveTvTipAnimationMapper.preview(character)
        assertTrue(a.id.startsWith("preview:id1_"))
        assertTrue(b.id.startsWith("preview:id1_"))
        assertTrue(a.id != b.id)
    }
}
