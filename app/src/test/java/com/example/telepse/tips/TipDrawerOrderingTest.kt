package com.example.telepse.tips

import com.example.telepse.data.model.CharacterSetCatalogRow
import com.example.telepse.data.model.TipCharacterCatalogItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TipDrawerOrderingTest {
    @Test
    fun sortsEliteGeneralPremiumAndPinsComment() {
        val presentation =
            TipDrawerOrdering.present(
                listOf(
                    CharacterSetCatalogRow(
                        "premium",
                        "Premium",
                        listOf(TipCharacterCatalogItem("p1", "P", 5.0, null)),
                    ),
                    CharacterSetCatalogRow(
                        "elite",
                        "Elite Tips",
                        listOf(
                            TipCharacterCatalogItem("e1", "E", 10.0, null),
                            TipCharacterCatalogItem("c1", "Comment", 1.0, null),
                        ),
                    ),
                    CharacterSetCatalogRow(
                        "general",
                        "General",
                        listOf(TipCharacterCatalogItem("g1", "G", 2.0, null)),
                    ),
                ),
            )

        assertEquals(listOf("Elite Tips", "General", "Premium"), presentation.drawerSets.map { it.name })
        assertTrue(TipDrawerOrdering.isCommentCharacter(presentation.stripCharacters.first()))
        assertEquals("c1", presentation.stripCharacters.first().id)
        assertEquals(4, presentation.stripCharacters.size)
    }

    @Test
    fun emptySets_yieldEmptyPresentation() {
        val presentation = TipDrawerOrdering.present(emptyList())
        assertTrue(presentation.drawerSets.isEmpty())
        assertTrue(presentation.stripCharacters.isEmpty())
    }
}
