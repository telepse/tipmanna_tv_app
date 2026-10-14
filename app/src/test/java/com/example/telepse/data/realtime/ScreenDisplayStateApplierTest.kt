package com.example.telepse.data.realtime

import com.example.telepse.data.model.Event
import com.example.telepse.data.model.Performer
import com.example.telepse.data.model.ScreenDisplayStatePayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenDisplayStateApplierTest {

    private val baseEvent = Event(
        id = "evt-1",
        eventName = "Original",
        hostName = "Host",
        eventDate = "2025-01-01T00:00:00.000Z",
        startTime = "18:00",
        venueLocation = "Hall",
        eventCode = "CODE1",
        amountToRaise = 1000.0,
        amountRaised = 100.0,
        numOfTips = 2,
        latestTipAmount = 10.0,
        hideProgressBarAmount = false,
        showLatestAmount = true,
    )

    @Test
    fun apply_partialPatch_updatesOnlyProvidedFields() {
        val patch = ScreenDisplayStatePayload(
            amountRaised = 250.0,
            numOfTips = 5,
        )
        val updated = ScreenDisplayStateApplier.apply(baseEvent, patch)
        assertEquals(250.0, updated.amountRaised)
        assertEquals(5, updated.numOfTips)
        assertEquals("Original", updated.eventName)
        assertEquals(1000.0, updated.amountToRaise)
    }

    @Test
    fun apply_backgroundAndFlags_updatesHudFields() {
        val patch = ScreenDisplayStatePayload(
            eventBackground = "#112233",
            backgroundType = "color",
            hideProgressBarAmount = true,
            showLatestAmount = false,
            moneyFor = "Charity",
        )
        val updated = ScreenDisplayStateApplier.apply(baseEvent, patch)
        assertEquals("#112233", updated.eventBackground)
        assertEquals("color", updated.backgroundType)
        assertEquals(true, updated.hideProgressBarAmount)
        assertEquals(false, updated.showLatestAmount)
        assertEquals("Charity", updated.moneyFor)
    }

    @Test
    fun apply_performers_replacesListWhenPresent() {
        val patch = ScreenDisplayStatePayload(
            performers = listOf(Performer(id = "p1", name = "Act One")),
        )
        val updated = ScreenDisplayStateApplier.apply(baseEvent, patch)
        assertEquals(1, updated.performers?.size)
        assertEquals("Act One", updated.performers?.first()?.name)
    }
}
