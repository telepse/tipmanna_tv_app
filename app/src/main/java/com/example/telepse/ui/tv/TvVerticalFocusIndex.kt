package com.example.telepse.ui.tv

/** Pure index math for vertical TV lists (unit-testable). */
object TvVerticalFocusIndex {
    fun nextIndex(current: Int, lastIndex: Int): Int? {
        if (lastIndex < 0) return null
        val next = (current + 1).coerceAtMost(lastIndex)
        return if (next == current) null else next
    }

    fun previousIndex(current: Int): Int? {
        val prev = (current - 1).coerceAtLeast(0)
        return if (prev == current) null else prev
    }
}
