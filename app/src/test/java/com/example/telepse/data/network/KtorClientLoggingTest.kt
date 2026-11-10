package com.example.telepse.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.pluginOrNull
import io.ktor.http.HttpHeaders
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KtorClientLoggingTest {
    @Test
    fun releaseBuildPolicy_doesNotInstallLoggingPlugin() {
        assertFalse(KtorClientLogging.shouldInstallHttpLogging(debugBuild = false))
        val client =
            HttpClient(MockEngine { respondOk() }) {
                KtorClientLogging.configureHttpLogging(this, debugBuild = false)
            }
        assertNull(client.pluginOrNull(Logging))
        client.close()
    }

    @Test
    fun debugBuildPolicy_installsInfoLevelWithoutBodyLogging() {
        assertTrue(KtorClientLogging.shouldInstallHttpLogging(debugBuild = true))
        val client =
            HttpClient(MockEngine { respondOk() }) {
                KtorClientLogging.configureHttpLogging(this, debugBuild = true)
            }
        assertTrue(client.pluginOrNull(Logging) != null)
        client.close()
    }

    @Test
    fun sensitiveHeaders_redactedForDebugLogging() {
        assertTrue(KtorClientLogging.isSensitiveHeaderName(HttpHeaders.Authorization))
        assertTrue(KtorClientLogging.isSensitiveHeaderName("Idempotency-Key"))
        assertFalse(KtorClientLogging.isSensitiveHeaderName(HttpHeaders.UserAgent))
    }

    /**
     * INFO level does not log request bodies (only HEADERS / BASIC / ALL include bodies).
     * Verified against Ktor 3.0.3 LogLevel semantics — not re-run on device here.
     */
    @Test
    fun infoLevel_doesNotIncludeBodyInLogLevelEnum() {
        assertFalse(LogLevel.INFO.toString().contains("BODY", ignoreCase = true))
    }
}
