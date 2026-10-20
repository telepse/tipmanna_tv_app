package com.example.telepse.data.realtime

import com.example.telepse.data.model.Event
import com.example.telepse.data.model.Performer
import com.example.telepse.data.model.ScreenDisplayStatePayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        eventBackground = "/default.png",
        backgroundType = "default",
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
        assertEquals("/default.png", updated.eventBackground)
    }

    @Test
    fun apply_backgroundAndFlags_updatesHudFields() {
        val patch = ScreenDisplayStatePayload(
            eventBackground = "#112233",
            backgroundType = "color",
            hideProgressBarAmount = true,
            showLatestAmount = false,
            moneyFor = "Charity",
            eventBackgroundPresent = true,
            backgroundTypePresent = true,
            hideProgressBarAmountPresent = true,
            showLatestAmountPresent = true,
            moneyForPresent = true,
        )
        val updated = ScreenDisplayStateApplier.apply(baseEvent, patch)
        assertEquals("#112233", updated.eventBackground)
        assertEquals("color", updated.backgroundType)
        assertEquals(true, updated.hideProgressBarAmount)
        assertEquals(false, updated.showLatestAmount)
        assertEquals("Charity", updated.moneyFor)
    }

    @Test
    fun apply_explicitNullEventBackground_clearsWhenPresent() {
        val patch = ScreenDisplayStatePayload(
            eventBackground = null,
            backgroundType = "gaming",
            eventBackgroundPresent = true,
            backgroundTypePresent = true,
        )
        val updated = ScreenDisplayStateApplier.apply(baseEvent, patch)
        assertNull(updated.eventBackground)
        assertEquals("gaming", updated.backgroundType)
    }

    @Test
    fun apply_hideProgressFalse_showsAmountsContract() {
        val hidden = baseEvent.copy(hideProgressBarAmount = true)
        val patch = ScreenDisplayStatePayload(
            hideProgressBarAmount = false,
            hideProgressBarAmountPresent = true,
        )
        val updated = ScreenDisplayStateApplier.apply(hidden, patch)
        assertEquals(false, updated.hideProgressBarAmount)
    }

    @Test
    fun apply_explicitNullHideProgress_treatsAsShowAmounts() {
        val hidden = baseEvent.copy(hideProgressBarAmount = true)
        val patch = ScreenDisplayStatePayload(
            hideProgressBarAmount = null,
            hideProgressBarAmountPresent = true,
        )
        val updated = ScreenDisplayStateApplier.apply(hidden, patch)
        assertEquals(false, updated.hideProgressBarAmount)
    }

    @Test
    fun apply_omittedHideProgress_preservesExisting() {
        val hidden = baseEvent.copy(hideProgressBarAmount = true)
        val patch = ScreenDisplayStatePayload(amountRaised = 200.0)
        val updated = ScreenDisplayStateApplier.apply(hidden, patch)
        assertEquals(true, updated.hideProgressBarAmount)
        assertEquals(200.0, updated.amountRaised)
    }

    @Test
    fun apply_performers_replacesListWhenPresent() {
        val patch = ScreenDisplayStatePayload(
            performers = listOf(Performer(id = "p1", name = "Act One")),
            performersPresent = true,
        )
        val updated = ScreenDisplayStateApplier.apply(baseEvent, patch)
        assertEquals(1, updated.performers?.size)
        assertEquals("Act One", updated.performers?.first()?.name)
    }
}
