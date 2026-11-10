package com.example.telepse.player

import com.example.telepse.data.model.TipAnimationEvent

/**
 * Port of web [animation-guard] accept/reject semantics for payment animations.
 */
class TipAnimationGuard(
    private var staleThresholdMs: Long,
) {
    private val lastSeqByEvent = mutableMapOf<String, Int>()
    private val lastServerTsByEvent = mutableMapOf<String, Long>()
    private val seenBundleIds = mutableSetOf<String>()
    private val seenTransactionIds = mutableSetOf<String>()

    fun updateStaleThresholdMs(ms: Long) {
        staleThresholdMs = ms
    }

    /**
     * @param activeEventId Current screen event; payloads for other events are rejected.
     */
    fun shouldAccept(payload: TipAnimationEvent, activeEventId: String): Boolean {
        val eventId = payload.eventId?.trim().orEmpty()
        if (eventId.isEmpty() || eventId != activeEventId.trim()) {
            return false
        }

        val serverTs = payload.serverTs ?: return false
        val ageMs = System.currentTimeMillis() - serverTs
        if (ageMs > staleThresholdMs) {
            return false
        }

        payload.id?.let { bundleId ->
            if (!seenBundleIds.add(bundleId)) {
                return false
            }
        }

        payload.transactionId?.let { txId ->
            if (!seenTransactionIds.add(txId)) {
                return false
            }
        }

        val seq = payload.seq
        if (seq != null) {
            val lastSeq = lastSeqByEvent[eventId] ?: 0
            if (seq <= lastSeq) {
                return false
            }
            lastSeqByEvent[eventId] = seq
            lastServerTsByEvent[eventId] = serverTs
            return true
        }

        val lastTs = lastServerTsByEvent[eventId] ?: 0L
        if (serverTs <= lastTs) {
            return false
        }
        lastServerTsByEvent[eventId] = serverTs
        return true
    }

    fun reset() {
        lastSeqByEvent.clear()
        lastServerTsByEvent.clear()
        seenBundleIds.clear()
        seenTransactionIds.clear()
    }
}
