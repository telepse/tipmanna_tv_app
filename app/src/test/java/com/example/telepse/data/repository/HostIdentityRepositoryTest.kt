package com.example.telepse.data.repository

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
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class HostIdentityRepositoryTest {
  private fun repository(engine: MockEngine): HostIdentityRepository {
        val client =
            HttpClient(engine) {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
            }
        return HostIdentityRepository(client)
    }

    @Test
    fun resolveHostIdForChannel_parsesHostId() = runTest {
        val channelId = "550e8400-e29b-41d4-a716-446655440000"
        val hostId = "host-123"
        var capturedPath: String? = null
        val repo =
            repository(
                MockEngine { request ->
                    capturedPath = request.url.encodedPath
                    respond(
                        content = """{"hostId":"$hostId"}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                },
            )
        val result = repo.resolveHostIdForChannel(channelId)
        assertTrue(result.isSuccess)
        assertEquals(hostId, result.getOrNull())
        assertTrue(capturedPath?.contains("saved-hosts/by-channel") == true)
        assertTrue(capturedPath?.contains(channelId) == true)
    }

    @Test
    fun resolveHostIdForChannel_notFound() = runTest {
        val repo =
            repository(
                MockEngine {
                    respond(
                        content = """{"error":"not found"}""",
                        status = HttpStatusCode.NotFound,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                },
            )
        val result =
            repo.resolveHostIdForChannel("550e8400-e29b-41d4-a716-446655440000")
        assertTrue(result.isFailure)
    }
}
