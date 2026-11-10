package com.example.telepse.player

import com.example.telepse.data.model.TipAnimationEvent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TipAnimationGuardTest {
    private val eventId = "event-11111111-1111-1111-1111-111111111111"

    @Test
    fun rejectsWrongEventId() {
        val guard = TipAnimationGuard(180_000)
        val payload =
            TipAnimationEvent(
                selectedTips = listOf("A"),
                eventId = "other",
                serverTs = System.currentTimeMillis(),
            )
        assertFalse(guard.shouldAccept(payload, eventId))
    }

    @Test
    fun rejectsStalePayload() {
        val guard = TipAnimationGuard(1_000)
        val payload =
            TipAnimationEvent(
                selectedTips = listOf("A"),
                eventId = eventId,
                serverTs = System.currentTimeMillis() - 5_000,
            )
        assertFalse(guard.shouldAccept(payload, eventId))
    }

    @Test
    fun dedupesBySeq() {
        val guard = TipAnimationGuard(180_000)
        val now = System.currentTimeMillis()
        val first =
            TipAnimationEvent(
                selectedTips = listOf("A"),
                eventId = eventId,
                serverTs = now,
                seq = 2,
                id = "bundle-1",
            )
        val duplicateSeq =
            TipAnimationEvent(
                selectedTips = listOf("A"),
                eventId = eventId,
                serverTs = now + 1,
                seq = 2,
                id = "bundle-2",
            )
        assertTrue(guard.shouldAccept(first, eventId))
        assertFalse(guard.shouldAccept(duplicateSeq, eventId))
    }
}
