package com.example.telepse.data.network

import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpHeaders

/**
 * Ktor 3.x [Logging] at [LogLevel.INFO] logs request/response **headers** (not bodies).
 * Release builds must not install the plugin. Debug builds redact sensitive headers.
 */
object KtorClientLogging {
    const val REDACTED = "***"

    fun shouldInstallHttpLogging(debugBuild: Boolean): Boolean = debugBuild

    /** Whether Ktor [Logging] should replace this header's value with [REDACTED]. */
    fun isSensitiveHeaderName(headerName: String): Boolean {
        return when (headerName.lowercase()) {
            HttpHeaders.Authorization.lowercase(),
            HttpHeaders.Cookie.lowercase(),
            "idempotency-key" -> true
            else -> false
        }
    }

    fun configureHttpLogging(
        config: HttpClientConfig<*>,
        debugBuild: Boolean,
    ) {
        if (!shouldInstallHttpLogging(debugBuild)) return
        config.install(Logging) {
            level = LogLevel.INFO
            sanitizeHeader(placeholder = REDACTED) { headerName ->
                isSensitiveHeaderName(headerName)
            }
        }
    }
}
