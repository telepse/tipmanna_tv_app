package com.example.telepse.analytics

import java.util.UUID

object TvAnalyticsSession {
    private val sessionId: String = "android-tv-${UUID.randomUUID()}"

    fun id(): String = sessionId
}
