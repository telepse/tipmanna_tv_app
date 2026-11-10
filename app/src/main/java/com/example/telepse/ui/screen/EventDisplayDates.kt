package com.example.telepse.ui.screen

import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Formats [isoDate] for the event picker card. Returns empty when the API omits a date.
 */
fun formatEventDisplayDate(isoDate: String?): String {
    if (isoDate.isNullOrBlank()) return ""
    return try {
        val instant = Instant.parse(isoDate)
        val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(dateTime)
    } catch (_: Exception) {
        isoDate
    }
}

/**
 * Status badge for the event picker when [isoDate] may be null or unparsable.
 */
fun eventStatusFromDateFields(
    isoDate: String?,
    endTime: String?,
    now: LocalDateTime = LocalDateTime.now(),
): EventStatusBadge {
    if (isoDate.isNullOrBlank()) {
        return EventStatusBadge.Pending
    }
    return try {
        val eventInstant = Instant.parse(isoDate)
        val eventDateTime = LocalDateTime.ofInstant(eventInstant, ZoneId.systemDefault())
        val eventDate = eventDateTime.toLocalDate()
        val currentDate = now.toLocalDate()
        val parsedEndTime = endTime?.let {
            try {
                LocalTime.parse(it)
            } catch (_: Exception) {
                null
            }
        }
        when {
            eventDate.isBefore(currentDate) -> EventStatusBadge.Completed
            eventDate.isEqual(currentDate) -> {
                if (parsedEndTime != null && now.toLocalTime().isAfter(parsedEndTime)) {
                    EventStatusBadge.Completed
                } else {
                    EventStatusBadge.Ongoing
                }
            }
            else -> EventStatusBadge.NotStarted
        }
    } catch (_: Exception) {
        EventStatusBadge.Pending
    }
}

enum class EventStatusBadge {
    Completed,
    Ongoing,
    NotStarted,
    Pending,
}
