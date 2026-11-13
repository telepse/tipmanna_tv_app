package com.example.telepse.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.telepse.data.auth.SessionStore
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
class BlockedHostRepositoryTest {
    private lateinit var sessionStore: SessionStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        sessionStore = SessionStore.createPlainForUnitTests(context)
        sessionStore.clear()
    }

    private fun repository(engine: MockEngine): BlockedHostRepository {
        val client =
            HttpClient(engine) {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
            }
        return BlockedHostRepository(client, sessionStore)
    }

    @Test
    fun refresh_parsesHostIds() = runTest {
        sessionStore.saveSession("t", "a@b.c")
        val repo =
            repository(
                MockEngine {
                    respond(
                        """{"hostIds":["host-a"]}""",
                        HttpStatusCode.OK,
                        headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                },
            )
        repo.refresh().getOrThrow()
        assertEquals(setOf("host-a"), repo.blockedHostIds.value)
    }

    @Test
    fun blockHost_requiresSignIn() = runTest {
        val repo = repository(MockEngine { respond("", HttpStatusCode.Created) })
        val result = repo.blockHost("host-1")
        assertTrue(result.isFailure)
    }
}
