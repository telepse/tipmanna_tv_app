package com.example.telepse.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.pluginOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class KtorClientTimeoutsTest {
    @Test
    fun configureHttpTimeouts_installsPluginWithDocumentedDefaults() {
        val client =
            HttpClient(MockEngine { respondOk() }) {
                KtorClientTimeouts.configureHttpTimeouts(this)
            }
        assertNotNull(client.pluginOrNull(HttpTimeout))
        assertEquals(20_000L, KtorClientTimeouts.CONNECT_TIMEOUT_MS)
        assertEquals(45_000L, KtorClientTimeouts.REQUEST_TIMEOUT_MS)
        assertEquals(45_000L, KtorClientTimeouts.SOCKET_TIMEOUT_MS)
        client.close()
    }
}
