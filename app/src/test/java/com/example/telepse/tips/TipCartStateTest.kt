package com.example.telepse.tips

import com.example.telepse.data.model.TipCharacterCatalogItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TipCartStateTest {
    private val a = TipCharacterCatalogItem("a", "Alpha", 2.5, null)
    private val b = TipCharacterCatalogItem("b", "Beta", 1.0, null)
    private val ids = setOf("a", "b")

    @Test
    fun add_incrementsQuantityAndTotals() {
        var cart = TipCartState().add(a, "evt-1", ids)
        cart = cart.add(a, "evt-1", ids)
        cart = cart.add(b, "evt-1", ids)
        assertEquals(3, cart.itemCount)
        assertEquals(6.0, cart.provisionalTotal, 0.001)
        assertEquals(2, cart.quantityOf("a"))
    }

    @Test
    fun remove_andSetQuantity() {
        var cart = TipCartState().add(a, "evt-1", ids).add(b, "evt-1", ids)
        cart = cart.setQuantity("a", 3, ids)
        assertEquals(3, cart.quantityOf("a"))
        cart = cart.setQuantity("a", 0, ids)
        assertEquals(0, cart.quantityOf("a"))
        cart = cart.remove("b")
        assertTrue(cart.isEmpty)
    }

    @Test
    fun eventChange_clearsPreviousCart() {
        val cart =
            TipCartState()
                .add(a, "evt-1", ids)
                .withEventClearedIfDifferent("evt-2")
        assertEquals("evt-2", cart.eventId)
        assertTrue(cart.isEmpty)
    }

    @Test
    fun rejectUnavailableCharacter() {
        val cart = TipCartState().add(a, "evt-1", setOf("b"))
        assertTrue(cart.isEmpty)
    }

    @Test
    fun retainAvailable_dropsMissingIds() {
        val cart =
            TipCartState()
                .add(a, "evt-1", ids)
                .add(b, "evt-1", ids)
                .retainAvailable(setOf("a"))
        assertEquals(1, cart.lines.size)
        assertEquals("a", cart.lines.first().character.id)
    }
}
