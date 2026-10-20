package com.example.telepse.livetv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LiveScheduleQueueAdvanceTest {
    @Test
    fun nextIndex_singleItem_returnsNull() {
        assertNull(LiveScheduleQueueAdvance.nextIndexAfterEnd(0, 1))
    }

    @Test
    fun nextIndex_multiItem_advances() {
        assertEquals(1, LiveScheduleQueueAdvance.nextIndexAfterEnd(0, 3))
        assertEquals(2, LiveScheduleQueueAdvance.nextIndexAfterEnd(1, 3))
        assertNull(LiveScheduleQueueAdvance.nextIndexAfterEnd(2, 3))
    }

    @Test
    fun nextIndex_invalidCurrent_returnsNull() {
        assertNull(LiveScheduleQueueAdvance.nextIndexAfterEnd(-1, 2))
        assertNull(LiveScheduleQueueAdvance.nextIndexAfterEnd(5, 2))
    }
}
