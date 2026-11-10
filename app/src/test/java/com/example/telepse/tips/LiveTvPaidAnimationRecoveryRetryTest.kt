package com.example.telepse.tips

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.telepse.data.auth.SessionExpiryHandler
import com.example.telepse.data.auth.SessionStore
import com.example.telepse.data.model.AnimationRecoveryClaimResponse
import com.example.telepse.data.model.AnimationRecoveryExchangeResponse
import com.example.telepse.data.model.AnimationRecoveryProgressRequest
import com.example.telepse.data.model.TipCharacterCatalogItem
import com.example.telepse.data.model.TvCheckoutAnimationRecoveryHints
import com.example.telepse.data.model.TvTipCheckoutSessionStatusResponse
import com.example.telepse.data.network.WalletHttpException
import com.example.telepse.data.realtime.PusherService
import com.example.telepse.data.repository.AnimationContractRepository
import com.example.telepse.data.repository.PaymentAnimationRecoveryOperations
import com.example.telepse.data.repository.TipperSession
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class LiveTvPaidAnimationRecoveryRetryTest {
    private val eventId = "11111111-1111-1111-1111-111111111111"
    private val txId = "22222222-2222-2222-2222-222222222222"

    @Test
    fun recoverAfterQrPaid_retries404UntilDeliveryReady() =
        runTest {
            var claimCalls = 0
            val ops =
                object : PaymentAnimationRecoveryOperations {
                    override suspend fun exchangeAnimationRecovery(
                        recoveryState: String,
                        transactionId: String,
                        stripePaymentIntentId: String,
                        eventId: String,
                    ): Result<AnimationRecoveryExchangeResponse> =
                        Result.failure(IllegalStateException())

                    override suspend fun claimAnimationRecovery(
                        recoverySessionToken: String,
                    ): Result<AnimationRecoveryClaimResponse> {
                        claimCalls++
                        return if (claimCalls < 3) {
                            Result.failure(WalletHttpException(httpStatus = 404, authPresent = false, apiMessage = "Animation delivery not found"))
                        } else {
                            Result.success(LiveTvPaidAnimationRecoveryTestHelper.sampleClaim(txId, eventId))
                        }
                    }

                    override suspend fun progressAnimationRecovery(
                        recoverySessionToken: String,
                        body: AnimationRecoveryProgressRequest,
                    ) = Result.success(Unit)
                }
            val host = newHost(this)
            host.onProgrammeEventChanged(eventId)
            host.updateCatalog(listOf(TipCharacterCatalogItem("c1", "Love", 5.0, "https://cdn/love.png")))
            val recovery = LiveTvPaidAnimationRecovery(ops)
            val ok =
                recovery.recoverAfterQrPaid(
                    paidStatus(txId, eventId),
                    host,
                    eventId,
                )
            assertTrue(ok)
            assertEquals(3, claimCalls)
            assertEquals(1, host.pendingAnimations.value.size)
            host.stop()
        }

    @Test
    fun recoverAfterQrPaid_noRecoveryHints_doesNotClaim() =
        runTest {
            var claimCalls = 0
            val ops =
                object : PaymentAnimationRecoveryOperations {
                    override suspend fun exchangeAnimationRecovery(
                        recoveryState: String,
                        transactionId: String,
                        stripePaymentIntentId: String,
                        eventId: String,
                    ): Result<AnimationRecoveryExchangeResponse> =
                        Result.failure(IllegalStateException())

                    override suspend fun claimAnimationRecovery(
                        recoverySessionToken: String,
                    ): Result<AnimationRecoveryClaimResponse> {
                        claimCalls++
                        return Result.success(LiveTvPaidAnimationRecoveryTestHelper.sampleClaim(txId, eventId))
                    }

                    override suspend fun progressAnimationRecovery(
                        recoverySessionToken: String,
                        body: AnimationRecoveryProgressRequest,
                    ) = Result.success(Unit)
                }
            val host = newHost(this)
            val recovery = LiveTvPaidAnimationRecovery(ops)
            val ok =
                recovery.recoverAfterQrPaid(
                    TvTipCheckoutSessionStatusResponse(status = "paid", eventId = eventId),
                    host,
                    eventId,
                )
            assertTrue(!ok)
            assertEquals(0, claimCalls)
            host.stop()
        }

    private fun paidStatus(tx: String, event: String) =
        TvTipCheckoutSessionStatusResponse(
            status = "paid",
            eventId = event,
            transactionId = tx,
            animationRecovery =
                TvCheckoutAnimationRecoveryHints(
                    transactionId = tx,
                    eventId = event,
                    stripePaymentIntentId = "pi_test",
                    recoverySessionToken = "jwt-display",
                ),
        )

    private fun newHost(scope: kotlinx.coroutines.CoroutineScope): LiveTvTipAnimationHost {
        val context: Context = ApplicationProvider.getApplicationContext()
        val http = HttpClient(MockEngine { respondOk("{}") })
        val sessionStore = SessionStore.createPlainForUnitTests(context)
        val sessionExpiryHandler = SessionExpiryHandler(NoOpTipperSession())
        return LiveTvTipAnimationHost(
            pusherService = PusherService(http, sessionStore, sessionExpiryHandler),
            animationContractRepository = AnimationContractRepository(http),
            scope = scope,
        ).also { it.start() }
    }
}

/** Shared claim fixture for recovery tests. */
private class NoOpTipperSession : TipperSession {
    override fun signOut() {}
}

object LiveTvPaidAnimationRecoveryTestHelper {
    fun sampleClaim(
        txId: String,
        eventId: String,
    ): AnimationRecoveryClaimResponse {
        val now = System.currentTimeMillis()
        return AnimationRecoveryClaimResponse(
            canonicalPayload =
                com.example.telepse.data.model.AnimationRecoveryCanonicalPayload(
                    id = "bundle-1",
                    seq = 1,
                    eventId = eventId,
                    transactionId = txId,
                    serverTs = now,
                    playbackEndsAt = now + 60_000L,
                    selectedTips = listOf("Love"),
                    resolvedTips =
                        listOf(
                            com.example.telepse.data.model.AnimationRecoveryResolvedTipCharacter(
                                name = "Love",
                                iconUrl = "https://cdn/love.png",
                                occurrenceId = "occ-1",
                            ),
                        ),
                    amount = 5.0,
                    sender = "Tipper",
                ),
            remainingOccurrenceIds = listOf("occ-1"),
            claimId = "claim-1",
        )
    }
}
