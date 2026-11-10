package com.example.telepse.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.telepse.data.auth.SessionStore
import com.example.telepse.data.network.applyRecoverySessionBearer
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondOk
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.ContentType
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
class WalletRepositoryRecoveryAuthTest {
    private lateinit var sessionStore: SessionStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        sessionStore = SessionStore.createPlainForUnitTests(context)
        sessionStore.clear()
    }

    private fun tipperAuthedClient(engine: MockEngine): HttpClient =
        HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            defaultRequest {
                sessionStore.accessToken()?.let { token ->
                    headers.append(HttpHeaders.Authorization, "Bearer $token")
                }
            }
        }

    private fun recoveryOnlyClient(engine: MockEngine): HttpClient =
        HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

    private fun MockRequestHandleScope.jsonOk(body: String) =
        respond(
            content = body,
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
        )

    private fun repository(
        walletEngine: MockEngine,
        recoveryEngine: MockEngine,
    ): WalletRepository =
        WalletRepository(
            httpClient = tipperAuthedClient(walletEngine),
            recoveryHttpClient = recoveryOnlyClient(recoveryEngine),
            sessionStore = sessionStore,
        )

    private fun claimOkBody(): String {
        val now = System.currentTimeMillis()
        return """
        {
          "canonicalPayload": {
            "id": "bundle-1",
            "seq": 1,
            "eventId": "11111111-1111-1111-1111-111111111111",
            "transactionId": "22222222-2222-2222-2222-222222222222",
            "serverTs": $now,
            "playbackEndsAt": ${now + 60_000},
            "selectedTips": ["Love"],
            "resolvedTips": [],
            "amount": 5.0,
            "sender": "Tipper",
            "rewardTurbo": false,
            "rewardGalaxy": false
          },
          "remainingOccurrenceIds": ["occ-1"],
          "claimId": "claim-1",
          "leaseExpiresAt": "2026-12-01T00:00:00Z",
          "deliveryState": "EMITTED"
        }
        """.trimIndent()
    }

    @Test
    fun claim_sendsExactlyOneRecoveryAuthorizationHeader_notTipperJwt() =
        runTest {
            sessionStore.saveSession(token = "tipper-access-token", email = "tipper@test.local")
            var authHeaders: List<String> = emptyList()
            val recoveryEngine =
                MockEngine { request ->
                    authHeaders =
                        request.headers.getAll(HttpHeaders.Authorization)?.toList().orEmpty()
                    jsonOk(claimOkBody())
                }
            val walletEngine = MockEngine { respondOk("{}") }
            val repo = repository(walletEngine, recoveryEngine)
            val result = repo.claimAnimationRecovery("recovery-session-jwt")
            assertTrue(result.exceptionOrNull()?.toString() ?: "ok", result.isSuccess)
            assertEquals(listOf("Bearer recovery-session-jwt"), authHeaders)
        }

    @Test
    fun legacyDefaultRequestWithoutRemove_sendsDuplicateAuthorizationHeaders() =
        runTest {
            var authHeaders: List<String> = emptyList()
            val engine =
                MockEngine { request ->
                    authHeaders =
                        request.headers.getAll(HttpHeaders.Authorization)?.toList().orEmpty()
                    jsonOk(claimOkBody())
                }
            val combined =
                HttpClient(engine) {
                    install(ContentNegotiation) {
                        json(Json { ignoreUnknownKeys = true })
                    }
                    defaultRequest {
                        headers.append(HttpHeaders.Authorization, "Bearer tipper-access-token")
                    }
                }
            combined.post("https://example.test/claim") {
                header(HttpHeaders.Authorization, "Bearer recovery-session-jwt")
                contentType(ContentType.Application.Json)
                setBody("{}")
            }
            assertEquals(2, authHeaders.size)
            assertTrue(authHeaders.any { it == "Bearer tipper-access-token" })
            assertTrue(authHeaders.any { it == "Bearer recovery-session-jwt" })
        }

    @Test
    fun fetchWallet_stillUsesTipperAuthorization() =
        runTest {
            sessionStore.saveSession(token = "tipper-access-token", email = "tipper@test.local")
            var authHeader: String? = null
            val walletEngine =
                MockEngine { request ->
                    authHeader = request.headers[HttpHeaders.Authorization]
                    respondOk("""{"balanceGbp":0.0}""")
                }
            val recoveryEngine = MockEngine { respondOk("{}") }
            repository(walletEngine, recoveryEngine).fetchWallet()
            assertEquals("Bearer tipper-access-token", authHeader)
        }

    @Test
    fun claim_withWrongCredentialRejectedByServer() =
        runTest {
            val recoveryEngine =
                MockEngine {
                    respond(
                        content = """{"error":{"message":"Invalid recovery session"}}""",
                        status = HttpStatusCode.Unauthorized,
                    )
                }
            val walletEngine = MockEngine { respondOk("{}") }
            val result =
                repository(walletEngine, recoveryEngine).claimAnimationRecovery("bad-token")
            assertTrue(result.isFailure)
            assertEquals(401, (result.exceptionOrNull() as? com.example.telepse.data.network.WalletHttpException)?.httpStatus)
        }
}
