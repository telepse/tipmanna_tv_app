package com.example.telepse.analytics

import kotlinx.serialization.json.JsonObject

/** Re-queue failed flush batches without exceeding cap (newest-first preservation). */
internal object AnalyticsQueueSupport {
    fun requeueAfterFailedFlush(
        failed: List<JsonObject>,
        maxSize: Int,
    ): ArrayDeque<JsonObject> {
        val merged = ArrayDeque<JsonObject>()
        failed.forEach { merged.addLast(it) }
        while (merged.size > maxSize) {
            merged.removeFirst()
        }
        return merged
    }

    fun appendCapped(
        queue: ArrayDeque<JsonObject>,
        event: JsonObject,
        maxSize: Int,
    ) {
        queue.addLast(event)
        while (queue.size > maxSize) {
            queue.removeFirst()
        }
    }
}
