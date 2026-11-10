package com.example.telepse.tips

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.telepse.data.auth.SessionExpiryHandler
import com.example.telepse.data.auth.SessionStore
import com.example.telepse.data.model.AnimationRecoveryCanonicalPayload
import com.example.telepse.data.model.AnimationRecoveryExchangeResponse
import com.example.telepse.data.model.AnimationRecoveryClaimResponse
import com.example.telepse.data.model.AnimationRecoveryProgressRequest
import com.example.telepse.data.model.AnimationRecoveryResolvedTipCharacter
import com.example.telepse.data.model.TipCharacterCatalogItem
import com.example.telepse.data.model.TvCheckoutAnimationRecoveryHints
import com.example.telepse.data.model.WalletDebitResponse
import com.example.telepse.data.model.TvTipCheckoutSessionStatusResponse
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
class LiveTvPaidAnimationRecoveryTest {
    private val eventId = "11111111-1111-1111-1111-111111111111"
    private val txId = "22222222-2222-2222-2222-222222222222"

    @Test
    fun recoverAfterQrPaid_claimsAndQueuesPaymentAnimation() =
        runTest {
            var claimToken: String? = null
            val ops =
                object : PaymentAnimationRecoveryOperations {
                    override suspend fun exchangeAnimationRecovery(
                        recoveryState: String,
                        transactionId: String,
                        stripePaymentIntentId: String,
                        eventId: String,
                    ): Result<AnimationRecoveryExchangeResponse> =
                        Result.failure(IllegalStateException())

                    override suspend fun claimAnimationRecovery(recoverySessionToken: String): Result<AnimationRecoveryClaimResponse> {
                        claimToken = recoverySessionToken
                        return Result.success(sampleClaim())
                    }

                    override suspend fun progressAnimationRecovery(
                        recoverySessionToken: String,
                        body: AnimationRecoveryProgressRequest,
                    ) = Result.success(Unit)
                }
            val context: Context = ApplicationProvider.getApplicationContext()
            val http = HttpClient(MockEngine { respondOk() })
            val sessionStore = SessionStore.createPlainForUnitTests(context)
            val sessionExpiryHandler = SessionExpiryHandler(NoOpTipperSession())
            val host =
                LiveTvTipAnimationHost(
                    pusherService = PusherService(http, sessionStore, sessionExpiryHandler),
                    animationContractRepository = AnimationContractRepository(http),
                    scope = this,
                )
            host.start()
            host.onProgrammeEventChanged(eventId)
            host.updateCatalog(listOf(TipCharacterCatalogItem("c1", "Love", 5.0, "https://cdn/love.png")))

            val recovery = LiveTvPaidAnimationRecovery(ops)
            val ok =
                recovery.recoverAfterQrPaid(
                    status =
                        TvTipCheckoutSessionStatusResponse(
                            status = "paid",
                            eventId = eventId,
                            transactionId = txId,
                            animationRecovery =
                                TvCheckoutAnimationRecoveryHints(
                                    transactionId = txId,
                                    eventId = eventId,
                                    stripePaymentIntentId = "pi_test",
                                    recoverySessionToken = "jwt-display",
                                    recoverySessionExpiresAt = "2026-12-01T00:00:00Z",
                                ),
                        ),
                    animationHost = host,
                    activeEventId = eventId,
                )
            assertTrue(ok)
            assertEquals("jwt-display", claimToken)
            assertEquals(1, host.pendingAnimations.value.size)
            assertTrue(host.pendingAnimations.value.first().id.startsWith("pay:$txId"))
            host.stop()
        }

    @Test
    fun recoverAfterWalletPaid_claimsWhenDebitResponseHasHints() =
        runTest {
            var claimToken: String? = null
            val ops =
                object : PaymentAnimationRecoveryOperations {
                    override suspend fun exchangeAnimationRecovery(
                        recoveryState: String,
                        transactionId: String,
                        stripePaymentIntentId: String,
                        eventId: String,
                    ): Result<AnimationRecoveryExchangeResponse> =
                        Result.failure(IllegalStateException())

                    override suspend fun claimAnimationRecovery(recoverySessionToken: String): Result<AnimationRecoveryClaimResponse> {
                        claimToken = recoverySessionToken
                        return Result.success(sampleClaim())
                    }

                    override suspend fun progressAnimationRecovery(
                        recoverySessionToken: String,
                        body: AnimationRecoveryProgressRequest,
                    ) = Result.success(Unit)
                }
            val context: Context = ApplicationProvider.getApplicationContext()
            val http = HttpClient(MockEngine { respondOk() })
            val sessionStore = SessionStore.createPlainForUnitTests(context)
            val sessionExpiryHandler = SessionExpiryHandler(NoOpTipperSession())
            val host =
                LiveTvTipAnimationHost(
                    pusherService = PusherService(http, sessionStore, sessionExpiryHandler),
                    animationContractRepository = AnimationContractRepository(http),
                    scope = this,
                )
            host.start()
            host.onProgrammeEventChanged(eventId)
            host.updateCatalog(listOf(TipCharacterCatalogItem("c1", "Love", 5.0, "https://cdn/love.png")))

            val recovery = LiveTvPaidAnimationRecovery(ops)
            val ok =
                recovery.recoverAfterWalletPaid(
                    debitResponse =
                        WalletDebitResponse(
                            transactionId = txId,
                            newBalance = "10",
                            animationRecovery =
                                TvCheckoutAnimationRecoveryHints(
                                    transactionId = txId,
                                    eventId = eventId,
                                    stripePaymentIntentId = "",
                                    recoverySessionToken = "wallet-recovery-jwt",
                                ),
                        ),
                    eventId = eventId,
                    animationHost = host,
                    activeEventId = eventId,
                )
            assertTrue(ok)
            assertEquals("wallet-recovery-jwt", claimToken)
            host.stop()
        }

    @Test
    fun recoverAfterWalletPaid_usesAuthoritativeHintsEventIdForGuard() =
        runTest {
            val authoritativeEventId = "33333333-3333-3333-3333-333333333333"
            val cartEventId = "99999999-9999-9999-9999-999999999999"
            val ops =
                object : PaymentAnimationRecoveryOperations {
                    override suspend fun exchangeAnimationRecovery(
                        recoveryState: String,
                        transactionId: String,
                        stripePaymentIntentId: String,
                        eventId: String,
                    ): Result<AnimationRecoveryExchangeResponse> =
                        Result.failure(IllegalStateException())

                    override suspend fun claimAnimationRecovery(recoverySessionToken: String): Result<AnimationRecoveryClaimResponse> =
                        Result.success(sampleClaim(eventId = authoritativeEventId))

                    override suspend fun progressAnimationRecovery(
                        recoverySessionToken: String,
                        body: AnimationRecoveryProgressRequest,
                    ) = Result.success(Unit)
                }
            val context: Context = ApplicationProvider.getApplicationContext()
            val http = HttpClient(MockEngine { respondOk() })
            val sessionStore = SessionStore.createPlainForUnitTests(context)
            val sessionExpiryHandler = SessionExpiryHandler(NoOpTipperSession())
            val host =
                LiveTvTipAnimationHost(
                    pusherService = PusherService(http, sessionStore, sessionExpiryHandler),
                    animationContractRepository = AnimationContractRepository(http),
                    scope = this,
                )
            host.start()
            host.onProgrammeEventChanged(authoritativeEventId)
            host.updateCatalog(listOf(TipCharacterCatalogItem("c1", "Love", 5.0, "https://cdn/love.png")))

            val recovery = LiveTvPaidAnimationRecovery(ops)
            val ok =
                recovery.recoverAfterWalletPaid(
                    debitResponse =
                        WalletDebitResponse(
                            transactionId = txId,
                            newBalance = "10",
                            animationRecovery =
                                TvCheckoutAnimationRecoveryHints(
                                    transactionId = txId,
                                    eventId = authoritativeEventId,
                                    stripePaymentIntentId = "",
                                    recoverySessionToken = "wallet-recovery-jwt",
                                ),
                        ),
                    eventId = cartEventId,
                    animationHost = host,
                    activeEventId = cartEventId,
                )
            assertTrue(ok)
            assertEquals(1, host.pendingAnimations.value.size)
            host.stop()
        }

    @Test
    fun reportPlaybackCompleted_includesCycleOutcomesForServerContract() =
        runTest {
            var progressBody: AnimationRecoveryProgressRequest? = null
            val ops =
                object : PaymentAnimationRecoveryOperations {
                    override suspend fun exchangeAnimationRecovery(
                        recoveryState: String,
                        transactionId: String,
                        stripePaymentIntentId: String,
                        eventId: String,
                    ): Result<AnimationRecoveryExchangeResponse> =
                        Result.failure(IllegalStateException())

                    override suspend fun claimAnimationRecovery(recoverySessionToken: String): Result<AnimationRecoveryClaimResponse> =
                        Result.success(sampleClaim())

                    override suspend fun progressAnimationRecovery(
                        recoverySessionToken: String,
                        body: AnimationRecoveryProgressRequest,
                    ): Result<Unit> {
                        progressBody = body
                        return Result.success(Unit)
                    }
                }
            val recovery = LiveTvPaidAnimationRecovery(ops)
            val host =
                LiveTvTipAnimationHost(
                    pusherService =
                        PusherService(
                            HttpClient(MockEngine { respondOk() }),
                            SessionStore.createPlainForUnitTests(ApplicationProvider.getApplicationContext()),
                            SessionExpiryHandler(NoOpTipperSession()),
                        ),
                    animationContractRepository = AnimationContractRepository(HttpClient(MockEngine { respondOk() })),
                    scope = this,
                )
            try {
                host.start()
                host.onProgrammeEventChanged(eventId)
                recovery.recoverAfterQrPaid(
                    status =
                        TvTipCheckoutSessionStatusResponse(
                            status = "paid",
                            eventId = eventId,
                            transactionId = txId,
                            animationRecovery =
                                TvCheckoutAnimationRecoveryHints(
                                    transactionId = txId,
                                    eventId = eventId,
                                    stripePaymentIntentId = "pi_test",
                                    recoverySessionToken = "jwt-display",
                                    recoverySessionExpiresAt = "2026-12-01T00:00:00Z",
                                ),
                        ),
                    animationHost = host,
                    activeEventId = eventId,
                )
                recovery.reportPlaybackCompleted()
            } finally {
                host.stop()
            }
            val body = progressBody ?: error("expected progress body")
            assertEquals("kotlin_tv", body.surface)
            assertEquals(1, body.occurrences.size)
            assertEquals("occ-1", body.occurrences[0].occurrenceId)
            assertEquals("completed", body.occurrences[0].playbackResult)
            assertEquals("ok", body.occurrences[0].cycle1Outcome)
            assertEquals("ok", body.occurrences[0].cycle2Outcome)
        }

    @Test
    fun reportPlaybackCompleted_secondCallIsNoOpAfterStateCleared() =
        runTest {
            var progressCalls = 0
            val ops =
                object : PaymentAnimationRecoveryOperations {
                    override suspend fun exchangeAnimationRecovery(
                        recoveryState: String,
                        transactionId: String,
                        stripePaymentIntentId: String,
                        eventId: String,
                    ): Result<AnimationRecoveryExchangeResponse> =
                        Result.failure(IllegalStateException())

                    override suspend fun claimAnimationRecovery(recoverySessionToken: String): Result<AnimationRecoveryClaimResponse> =
                        Result.success(sampleClaim())

                    override suspend fun progressAnimationRecovery(
                        recoverySessionToken: String,
                        body: AnimationRecoveryProgressRequest,
                    ): Result<Unit> {
                        progressCalls++
                        return Result.success(Unit)
                    }
                }
            val recovery = LiveTvPaidAnimationRecovery(ops)
            val host =
                LiveTvTipAnimationHost(
                    pusherService =
                        PusherService(
                            HttpClient(MockEngine { respondOk() }),
                            SessionStore.createPlainForUnitTests(ApplicationProvider.getApplicationContext()),
                            SessionExpiryHandler(NoOpTipperSession()),
                        ),
                    animationContractRepository = AnimationContractRepository(HttpClient(MockEngine { respondOk() })),
                    scope = this,
                )
            try {
                host.start()
                host.onProgrammeEventChanged(eventId)
                recovery.recoverAfterQrPaid(
                    status =
                        TvTipCheckoutSessionStatusResponse(
                            status = "paid",
                            eventId = eventId,
                            transactionId = txId,
                            animationRecovery =
                                TvCheckoutAnimationRecoveryHints(
                                    transactionId = txId,
                                    eventId = eventId,
                                    stripePaymentIntentId = "pi_test",
                                    recoverySessionToken = "jwt-display",
                                    recoverySessionExpiresAt = "2026-12-01T00:00:00Z",
                                ),
                        ),
                    animationHost = host,
                    activeEventId = eventId,
                )
                recovery.reportPlaybackCompleted()
                recovery.reportPlaybackCompleted()
            } finally {
                host.stop()
            }
            assertEquals(1, progressCalls)
        }

    private fun sampleClaim(eventId: String = this.eventId) =
        AnimationRecoveryClaimResponse(
            canonicalPayload =
                AnimationRecoveryCanonicalPayload(
                    id = "bundle-1",
                    seq = 1,
                    eventId = eventId,
                    transactionId = txId,
                    serverTs = System.currentTimeMillis(),
                    selectedTips = listOf("Love"),
                    resolvedTips =
                        listOf(
                            AnimationRecoveryResolvedTipCharacter(
                                name = "Love",
                                iconUrl = "https://cdn/love.png",
                                occurrenceId = "occ-1",
                            ),
                        ),
                ),
            remainingOccurrenceIds = listOf("occ-1"),
            claimId = "claim-1",
        )

    private class NoOpTipperSession : TipperSession {
        override fun signOut() {}
    }
}
