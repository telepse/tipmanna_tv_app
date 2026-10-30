package com.example.telepse.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.telepse.data.auth.SessionStore
import com.example.telepse.data.network.ScheduleHttpException
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
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class TvScheduleRepositoryCancellationTest {
    private lateinit var sessionStore: SessionStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        sessionStore = SessionStore.createPlainForUnitTests(context)
        sessionStore.clear()
    }

    private fun repository(engine: MockEngine): TvScheduleRepository =
        TvScheduleRepository(
            httpClient =
                HttpClient(engine) {
                    install(ContentNegotiation) {
                        json(
                            Json {
                                ignoreUnknownKeys = true
                                isLenient = true
                            },
                        )
                    }
                },
            sessionStore = sessionStore,
        )

    @Test
    fun fetchGlobalSchedule_rethrowsCancellation_notAsResultFailure() =
        runTest {
            val engine =
                MockEngine {
                    throw CancellationException("StandaloneCoroutine was cancelled")
                }
            try {
                val result = repository(engine).fetchGlobalSchedule()
                fail(
                    "Expected CancellationException to propagate; got Result failure=${result.isFailure} " +
                        "ex=${result.exceptionOrNull()?.javaClass?.simpleName}",
                )
            } catch (e: CancellationException) {
                assertTrue(e.message.orEmpty().contains("cancelled", ignoreCase = true))
            }
        }

    @Test
    fun fetchGlobalSchedule_httpError_stillReturnsFailureResult() =
        runTest {
            val engine =
                MockEngine {
                    respond(content = "", status = HttpStatusCode.ServiceUnavailable)
                }
            val result = repository(engine).fetchGlobalSchedule()
            assertTrue(result.isFailure)
            val ex = result.exceptionOrNull() as ScheduleHttpException
            assertEquals(503, ex.httpStatus)
        }
}
