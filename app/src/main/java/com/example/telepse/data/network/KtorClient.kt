package com.example.telepse.data.network

import com.example.telepse.BuildConfig
import com.example.telepse.data.auth.SessionStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KtorClient @Inject constructor(
    private val sessionStore: SessionStore,
) {
    val httpClient: HttpClient = createClient(includeTipperDefaultAuth = true)

    /** No tipper Bearer on [io.ktor.client.plugins.defaultRequest] — recovery JWT only per call. */
    val recoveryHttpClient: HttpClient = createClient(includeTipperDefaultAuth = false)

    private fun createClient(includeTipperDefaultAuth: Boolean): HttpClient =
        HttpClient(Android) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                        encodeDefaults = false
                    },
                )
            }
            KtorClientLogging.configureHttpLogging(this, BuildConfig.DEBUG)
            KtorClientTimeouts.configureHttpTimeouts(this)
            install(WebSockets)
            if (includeTipperDefaultAuth) {
                defaultRequest {
                    sessionStore.accessToken()?.let { token ->
                        header(HttpHeaders.Authorization, "Bearer $token")
                    }
                }
            }
        }
}

