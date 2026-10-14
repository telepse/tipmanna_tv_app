package com.example.telepse.ui.tv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TvVerticalFocusIndexTest {

    @Test
    fun nextIndex_movesUntilLast() {
        assertEquals(1, TvVerticalFocusIndex.nextIndex(0, 4))
        assertEquals(4, TvVerticalFocusIndex.nextIndex(3, 4))
        assertNull(TvVerticalFocusIndex.nextIndex(4, 4))
    }

    @Test
    fun previousIndex_movesUntilFirst() {
        assertNull(TvVerticalFocusIndex.previousIndex(0))
        assertEquals(2, TvVerticalFocusIndex.previousIndex(3))
    }
}
