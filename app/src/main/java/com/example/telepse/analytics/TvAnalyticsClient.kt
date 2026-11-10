package com.example.telepse.analytics

import android.util.Log
import com.example.telepse.data.network.ApiConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Best-effort v1 batch ingest matching web `tvAnalytics.ts`.
 * Failed flushes re-queue events (capped) and log — must not break playback or wallet UI.
 */
@Singleton
class TvAnalyticsClient @Inject constructor(
    private val httpClient: HttpClient,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val queue = ArrayDeque<JsonObject>()
    private var contextSurface: String? = null
    private var contextEventId: String? = null
    private var contextChannelId: String? = null

    fun setContext(
        surface: String?,
        eventId: String? = null,
        videoChannelId: String? = null,
    ) {
        contextSurface = surface
        contextEventId = eventId
        contextChannelId = videoChannelId
    }

    fun track(event: JsonObject) {
        scope.launch {
            mutex.withLock {
                AnalyticsQueueSupport.appendCapped(queue, event, MAX_QUEUE)
                if (queue.size >= FLUSH_THRESHOLD) {
                    flushLocked()
                }
            }
        }
    }

    fun flushAsync() {
        scope.launch {
            mutex.withLock { flushLocked() }
        }
    }

    private suspend fun flushLocked() {
        if (queue.isEmpty()) return
        val batch =
            TvAnalyticsBatchV1(
                sessionId = TvAnalyticsSession.id(),
                surface = contextSurface,
                eventId = contextEventId,
                videoChannelId = contextChannelId,
                deviceCategory = "tv",
                events = queue.toList(),
            )
        val events = batch.events
        queue.clear()
        runCatching {
            val response =
                httpClient.post("${ApiConfig.BASE_URL}/api/v1/tv/analytics") {
                    contentType(ContentType.Application.Json)
                    setBody(batch)
                }
            if (!response.status.isSuccess()) {
                Log.w(TAG, "analytics flush http=${response.status.value}")
                queue.addAll(AnalyticsQueueSupport.requeueAfterFailedFlush(events, MAX_QUEUE))
            }
        }.onFailure { e ->
            Log.w(TAG, "analytics flush failed: ${e.javaClass.simpleName}")
            queue.addAll(AnalyticsQueueSupport.requeueAfterFailedFlush(events, MAX_QUEUE))
        }
    }

    companion object {
        private const val TAG = "TvAnalyticsClient"
        private const val MAX_QUEUE = 80
        private const val FLUSH_THRESHOLD = 20
    }
}
