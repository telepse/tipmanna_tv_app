package com.example.telepse.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.telepse.data.auth.SessionStore
import com.example.telepse.data.model.WalletDebitCharacterLine
import com.example.telepse.data.model.WalletDebitRequest
import com.example.telepse.data.network.WalletHttpException
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class WalletRepositoryDebitErrorTest {
    private lateinit var sessionStore: SessionStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        sessionStore = SessionStore.createPlainForUnitTests(context)
        sessionStore.clear()
    }

    private fun repository(engine: MockEngine): WalletRepository {
        val client =
            HttpClient(engine) {
                install(ContentNegotiation) {
                    json(
                        Json {
                            ignoreUnknownKeys = true
                            isLenient = true
                        },
                    )
                }
            }
        return WalletRepository(
            httpClient = client,
            recoveryHttpClient = client,
            sessionStore = sessionStore,
        )
    }

    private fun sampleRequest() =
        WalletDebitRequest(
            pin = "1234",
            amount = 5.0,
            eventId = "11111111-1111-1111-1111-111111111111",
            characters =
                listOf(
                    WalletDebitCharacterLine(
                        name = "Wave",
                        time = "12:00",
                        price = 5.0,
                        characterId = "char-a",
                    ),
                ),
        )

    @Test
    fun debitFailure_parsesApiErrorEnvelope() =
        runTest {
            val engine =
                MockEngine {
                    respond(
                        content = """{"error":{"message":"Incorrect wallet PIN"}}""",
                        status = HttpStatusCode.BadRequest,
                    )
                }
            val result = repository(engine).debitWallet(sampleRequest(), "idem-1")
            assertTrue(result.isFailure)
            val ex = result.exceptionOrNull() as WalletHttpException
            assertEquals(400, ex.httpStatus)
            assertEquals("Incorrect wallet PIN", ex.apiMessage)
        }

    @Test
    fun debitFailure_emptyBody_hasNullApiMessage() =
        runTest {
            val engine =
                MockEngine {
                    respond(content = "", status = HttpStatusCode.BadRequest)
                }
            val result = repository(engine).debitWallet(sampleRequest(), "idem-2")
            val ex = result.exceptionOrNull() as WalletHttpException
            assertNull(ex.apiMessage)
        }

    @Test
    fun debitFailure_malformedBody_doesNotSurfaceRawTransport() =
        runTest {
            val engine =
                MockEngine {
                    respond(
                        content = "Internal stack trace: bearer sk-live-secret",
                        status = HttpStatusCode.BadRequest,
                    )
                }
            val result = repository(engine).debitWallet(sampleRequest(), "idem-3")
            val ex = result.exceptionOrNull() as WalletHttpException
            assertNull(ex.apiMessage)
            assertTrue(ex.message.orEmpty().contains("HTTP 400"))
        }

    @Test(expected = CancellationException::class)
    fun debitWallet_rethrowsCancellation() =
        runTest {
            val engine =
                MockEngine {
                    throw CancellationException("cancelled")
                }
            repository(engine).debitWallet(sampleRequest(), "idem-4")
        }
}
