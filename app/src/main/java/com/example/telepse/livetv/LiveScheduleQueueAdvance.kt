package com.example.telepse.livetv

/**
 * Local advancement within the ordered schedule `items` list after a player ENDED event.
 * Server [livePosition] remains authoritative on refresh; this avoids stalling when the
 * same index is returned until the next poll/Pusher update.
 */
object LiveScheduleQueueAdvance {
    fun nextIndexAfterEnd(currentIndex: Int, itemCount: Int): Int? {
        if (itemCount <= 1) return null
        if (currentIndex !in 0 until itemCount) return null
        val next = currentIndex + 1
        return if (next < itemCount) next else null
    }
}
