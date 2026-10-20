package com.example.telepse.ui.screen

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class EventDisplayDatesTest {

    @Test
    fun formatEventDisplayDate_validIso_formatsLocalized() {
        val formatted = formatEventDisplayDate("2025-01-15T12:00:00.000Z")
        assert(formatted.isNotBlank())
        assert(!formatted.contains("2025-01-15T"))
    }

    @Test
    fun formatEventDisplayDate_nullOrBlank_returnsEmpty() {
        assertEquals("", formatEventDisplayDate(null))
        assertEquals("", formatEventDisplayDate(""))
        assertEquals("", formatEventDisplayDate("   "))
    }

    @Test
    fun formatEventDisplayDate_malformed_returnsRawString() {
        assertEquals("not-a-date", formatEventDisplayDate("not-a-date"))
    }

    @Test
    fun eventStatusFromDateFields_nullDate_isPending() {
        assertEquals(EventStatusBadge.Pending, eventStatusFromDateFields(null, null))
    }

    @Test
    fun eventStatusFromDateFields_malformedDate_isPending() {
        assertEquals(EventStatusBadge.Pending, eventStatusFromDateFields("bad", null))
    }

    @Test
    fun eventStatusFromDateFields_futureDate_isNotStarted() {
        val iso = "2099-06-01T12:00:00.000Z"
        val now = LocalDateTime.of(2025, 1, 1, 12, 0)
        assertEquals(EventStatusBadge.NotStarted, eventStatusFromDateFields(iso, null, now))
    }
}
