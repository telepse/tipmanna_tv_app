package com.example.telepse.tips

import com.example.telepse.data.model.TvTipCheckoutSessionStatusResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LiveTvQrCheckoutTest {
    private val eventId = "11111111-1111-1111-1111-111111111111"

    private fun primedCoordinator(
        wallet: FakeWalletOperations,
        scope: kotlinx.coroutines.CoroutineScope,
    ): LiveTvTipCoordinator {
        wallet.catalogResult = Result.success(FakeWalletOperations.sampleCatalog(eventId))
        val coordinator =
            LiveTvTipCoordinator(
                walletOperations = wallet,
                debitSubmitSession =
                    WalletDebitSubmitSession.createForTests(MemoryWalletDebitPendingPersistence()),
                isLoggedIn = { true },
                scope = scope,
            )
        coordinator.onProgrammeEventId(eventId)
        return coordinator
    }

    @Test
    fun qrCheckout_createsSession_andShowsCheckoutUrl() =
        runTest {
            val wallet = FakeWalletOperations()
            val coordinator = primedCoordinator(wallet, this)
            runCurrent()
            coordinator.selectCharacter("p1")
            runCurrent()
            assertTrue(coordinator.state.value.checkout.canEnterQrCheckout)

            coordinator.startQrCheckout()
            runCurrent()

            assertEquals(1, wallet.tvCheckoutCreateCalls.size)
            assertEquals(
                LiveTvQrPayPhase.AwaitingPhonePayment,
                coordinator.state.value.qrPay.phase,
            )
            assertEquals(
                "https://app.example/c/pay/token-abc",
                coordinator.state.value.qrPay.checkoutUrl,
            )
            coordinator.cancelQrCheckout()
            runCurrent()
        }

    @Test
    fun qrCheckout_clearsCartWhenPollReportsPaid() =
        runTest {
            val wallet = FakeWalletOperations()
            wallet.tvCheckoutStatusResults =
                ArrayDeque(
                    listOf(
                        Result.success(
                            TvTipCheckoutSessionStatusResponse(
                                sessionId = "sess-1",
                                status = "paid",
                                transactionId = "tx-card-1",
                            ),
                        ),
                    ),
                )
            val coordinator = primedCoordinator(wallet, this)
            runCurrent()
            coordinator.selectCharacter("p1")
            runCurrent()
            coordinator.startQrCheckout()
            runCurrent()
            advanceTimeBy(3_500)
            runCurrent()

            assertEquals(LiveTvQrPayPhase.Paid, coordinator.state.value.qrPay.phase)
            assertTrue(coordinator.state.value.cart.isEmpty)
            assertFalse(coordinator.state.value.checkout.cartReady)
            coordinator.cancelQrCheckout()
            runCurrent()
        }

    @Test
    fun qrCheckout_blocksCartMutationWhileAwaitingPayment() =
        runTest {
            val wallet = FakeWalletOperations()
            val coordinator = primedCoordinator(wallet, this)
            runCurrent()
            coordinator.selectCharacter("p1")
            runCurrent()
            coordinator.startQrCheckout()
            runCurrent()
            coordinator.clearCart()
            assertFalse(coordinator.state.value.cart.isEmpty)
            coordinator.cancelQrCheckout()
            runCurrent()
        }
}
