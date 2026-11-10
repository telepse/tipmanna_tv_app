package com.example.telepse.tips

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.telepse.data.auth.SessionExpiryHandler
import com.example.telepse.data.auth.SessionStore
import com.example.telepse.data.model.ResolvedTipCharacter
import com.example.telepse.data.model.TipAnimationEvent
import com.example.telepse.data.realtime.PusherService
import com.example.telepse.data.repository.AnimationContractRepository
import com.example.telepse.data.repository.TipperSession
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class LiveTvTipAnimationRecoveryGuardTest {
    private val eventId = "11111111-1111-1111-1111-111111111111"
    private val txId = "22222222-2222-2222-2222-222222222222"

    @Test
    fun acceptAuthoritativePaymentAnimation_dedupesSameTransaction() =
        runTest {
            val host = createHost(this)
            host.start()
            val payload = samplePayload(seq = 1)
            host.acceptAuthoritativePaymentAnimation(payload, eventId)
            host.acceptAuthoritativePaymentAnimation(payload.copy(seq = 2), eventId)
            assertEquals(1, host.pendingAnimations.value.size)
            host.stop()
        }

    @Test
    fun acceptAuthoritativePaymentAnimation_rejectsWrongEvent() =
        runTest {
            val host = createHost(this)
            host.start()
            host.acceptAuthoritativePaymentAnimation(samplePayload(), "wrong-event")
            assertEquals(0, host.pendingAnimations.value.size)
            host.stop()
        }

    private fun createHost(scope: CoroutineScope): LiveTvTipAnimationHost {
        val context: Context = ApplicationProvider.getApplicationContext()
        val http = HttpClient(MockEngine { respondOk() })
        val sessionStore = SessionStore.createPlainForUnitTests(context)
        return LiveTvTipAnimationHost(
            pusherService = PusherService(http, sessionStore, SessionExpiryHandler(NoOpTipperSession())),
            animationContractRepository = AnimationContractRepository(http),
            scope = scope,
        )
    }

    private fun samplePayload(seq: Int = 1) =
        TipAnimationEvent(
            selectedTips = listOf("Love"),
            eventId = eventId,
            transactionId = txId,
            id = "bundle-1",
            seq = seq,
            serverTs = System.currentTimeMillis(),
            resolvedTips =
                listOf(
                    ResolvedTipCharacter(name = "Love", iconUrl = "https://cdn/x.png"),
                ),
        )

    private class NoOpTipperSession : TipperSession {
        override fun signOut() {}
    }
}
