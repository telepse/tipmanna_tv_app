package com.example.telepse.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.telepse.data.auth.SessionStore
import com.example.telepse.data.model.ContentReportReason
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class ContentReportRepositoryTest {
    private lateinit var sessionStore: SessionStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        sessionStore = SessionStore.createPlainForUnitTests(context)
        sessionStore.clear()
    }

    private fun repository(engine: MockEngine): ContentReportRepository {
        val client =
            HttpClient(engine) {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
            }
        return ContentReportRepository(client, sessionStore)
    }

    @Test
    fun reportChannelContent_requiresSignIn() = runTest {
        val repo =
            repository(
                MockEngine {
                    respond(
                        "{}",
                        HttpStatusCode.Created,
                        headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                },
            )
        val result =
            repo.reportChannelContent(
                channelId = "550e8400-e29b-41d4-a716-446655440000",
                reason = ContentReportReason.Deceptive,
                otherText = null,
            )
        assertTrue(result.isFailure)
        assertEquals("Sign in is required to report content.", result.exceptionOrNull()?.message)
    }

    @Test
    fun reportChannelContent_postsEventChannelId() = runTest {
        sessionStore.saveSession("token-abc", "tipper@example.com")
        var capturedPath: String? = null
        val channelId = "550e8400-e29b-41d4-a716-446655440000"
        val repo =
            repository(
                MockEngine { request ->
                    capturedPath = request.url.encodedPath
                    respond(
                        content = """{"message":"Report submitted","id":"r1"}""",
                        status = HttpStatusCode.Created,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                },
            )
        val result =
            repo.reportChannelContent(
                channelId = channelId,
                reason = ContentReportReason.VerballyAbusive,
                otherText = null,
            )
        assertTrue(result.isSuccess)
        assertEquals("/api/v1/messages/channel/$channelId/report", capturedPath)
    }

    @Test
    fun reportChannelContent_maps429Duplicate() = runTest {
        sessionStore.saveSession("token-abc", "tipper@example.com")
        val repo =
            repository(
                MockEngine {
                    respond(
                        content = """{"error":{"message":"Too many"}}""",
                        status = HttpStatusCode.TooManyRequests,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                },
            )
        val result =
            repo.reportChannelContent(
                channelId = "550e8400-e29b-41d4-a716-446655440000",
                reason = ContentReportReason.Deceptive,
                otherText = null,
            )
        assertTrue(result.isFailure)
        assertEquals(
            "You already reported this video in the last 24 hours.",
            result.exceptionOrNull()?.message,
        )
    }
}
