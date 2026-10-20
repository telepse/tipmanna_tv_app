package com.example.telepse.data.network

import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.android.AndroidEngineConfig
import io.ktor.client.plugins.HttpTimeout

/**
 * Bounded HTTP timeouts for the shared [io.ktor.client.HttpClient].
 *
 * Rationale (Android TV on Wi‑Fi):
 * - **Connect 20s:** tolerate slow DHCP/DNS on set-top networks without hanging indefinitely.
 * - **Request 45s:** schedule/catalog/login JSON on cold Render hosts can be slow; still bounded.
 * - **Socket 45s:** idle read gap between response chunks.
 *
 * Ktor excludes WebSocket upgrade from request timeout; this app uses Pusher Java for realtime,
 * not Ktor WebSockets, despite the installed [io.ktor.client.plugins.websocket.WebSockets] plugin.
 *
 * Unit tests verify plugin/engine configuration only — not real network timing.
 */
object KtorClientTimeouts {
    const val CONNECT_TIMEOUT_MS = 20_000L
    const val REQUEST_TIMEOUT_MS = 45_000L
    const val SOCKET_TIMEOUT_MS = 45_000L

    fun configureHttpTimeouts(config: HttpClientConfig<*>) {
        config.install(HttpTimeout) {
            connectTimeoutMillis = CONNECT_TIMEOUT_MS
            requestTimeoutMillis = REQUEST_TIMEOUT_MS
            socketTimeoutMillis = SOCKET_TIMEOUT_MS
        }
        config.engine {
            if (this is AndroidEngineConfig) {
                connectTimeout = CONNECT_TIMEOUT_MS.toInt()
                socketTimeout = SOCKET_TIMEOUT_MS.toInt()
            }
        }
    }
}
