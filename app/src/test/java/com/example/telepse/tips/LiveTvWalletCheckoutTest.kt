package com.example.telepse.tips

import com.example.telepse.data.model.ActiveTipConfigResponse
import com.example.telepse.data.model.CreateTipQuoteResponse
import com.example.telepse.data.model.TipConfigItemDto
import com.example.telepse.data.model.TipConfigPayloadDto
import com.example.telepse.data.model.WalletDebitResponse
import com.example.telepse.data.model.WalletStatusResponse
import com.example.telepse.data.network.WalletHttpException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class LiveTvWalletCheckoutTest {
    private val eventId = "11111111-1111-1111-1111-111111111111"

    private fun primedCoordinator(
        wallet: FakeWalletOperations,
        session: WalletDebitSubmitSession =
            WalletDebitSubmitSession.createForTests(MemoryWalletDebitPendingPersistence()),
        scope: kotlinx.coroutines.CoroutineScope,
    ): LiveTvTipCoordinator {
        wallet.catalogResult = Result.success(FakeWalletOperations.sampleCatalog(eventId))
        val coordinator =
            LiveTvTipCoordinator(
                walletOperations = wallet,
                debitSubmitSession = session,
                isLoggedIn = { true },
                scope = scope,
            )
        coordinator.onProgrammeEventId(eventId)
        return coordinator
    }

    @Test
    fun successfulWalletPayment_clearsCart_andUsesIdempotencyKey() =
        runTest {
            val wallet = FakeWalletOperations()
            val session = WalletDebitSubmitSession.createForTests(MemoryWalletDebitPendingPersistence())
            val coordinator = primedCoordinator(wallet, session, this)
            runCurrent()
            coordinator.selectCharacter("p1")
            runCurrent()

            coordinator.startWalletCheckout()
            runCurrent()
            assertEquals(LiveTvWalletPayPhase.EnterPin, coordinator.state.value.walletPay.phase)

            "1234".forEach { coordinator.onWalletPinDigit(it) }
            coordinator.submitWalletCheckout()
            runCurrent()

            assertEquals(LiveTvWalletPayPhase.Success, coordinator.state.value.walletPay.phase)
            assertTrue(coordinator.state.value.cart.isEmpty)
            assertEquals(1, wallet.debitCallCount.get())
            assertEquals("kotlin_tv", wallet.debitCalls.first().first.paymentSurface)
            assertEquals("tv", wallet.debitCalls.first().first.deviceCategory)
            assertEquals(5.0, wallet.debitCalls.first().first.amount, 0.001)
            assertTrue(session.pendingWalletDebit == null)
        }

    @Test
    fun walletNotActivated_blocksPinEntry() =
        runTest {
            val wallet = FakeWalletOperations()
            wallet.walletResult =
                Result.success(WalletStatusResponse(exists = false, status = null, balance = "0"))
            val coordinator = primedCoordinator(wallet, scope = this)
            runCurrent()
            coordinator.selectCharacter("p1")
            runCurrent()
            coordinator.startWalletCheckout()
            runCurrent()
            assertEquals(LiveTvWalletPayPhase.Failed, coordinator.state.value.walletPay.phase)
            assertTrue(coordinator.state.value.walletPay.message.orEmpty().contains("not activated", ignoreCase = true))
            assertEquals(0, wallet.debitCallCount.get())
            assertFalse(coordinator.state.value.cart.isEmpty)
        }

    @Test
    fun missingPin_doesNotDebit() =
        runTest {
            val wallet = FakeWalletOperations()
            val coordinator = primedCoordinator(wallet, scope = this)
            runCurrent()
            coordinator.selectCharacter("p1")
            runCurrent()
            coordinator.startWalletCheckout()
            runCurrent()
            coordinator.submitWalletCheckout()
            runCurrent()
            assertEquals(0, wallet.debitCallCount.get())
            assertEquals(LiveTvWalletPayPhase.EnterPin, coordinator.state.value.walletPay.phase)
        }

    @Test
    fun incorrectPin_keepsCart_andShowsApiMessage() =
        runTest {
            val wallet = FakeWalletOperations()
            wallet.debitResult =
                Result.failure(
                    WalletHttpException(httpStatus = 400, authPresent = true, apiMessage = "Incorrect wallet PIN"),
                )
            val coordinator = primedCoordinator(wallet, scope = this)
            runCurrent()
            coordinator.selectCharacter("p1")
            runCurrent()
            coordinator.startWalletCheckout()
            runCurrent()
            "1234".forEach { coordinator.onWalletPinDigit(it) }
            coordinator.submitWalletCheckout()
            runCurrent()
            assertEquals(LiveTvWalletPayPhase.Failed, coordinator.state.value.walletPay.phase)
            assertTrue(coordinator.state.value.walletPay.message.orEmpty().contains("Incorrect", ignoreCase = true))
            assertFalse(coordinator.state.value.cart.isEmpty)
        }

    @Test
    fun insufficientBalance_confirmedFailure() =
        runTest {
            val wallet = FakeWalletOperations()
            wallet.debitResult =
                Result.failure(
                    WalletHttpException(
                        httpStatus = 400,
                        authPresent = true,
                        apiMessage = "Insufficient wallet balance",
                    ),
                )
            val coordinator = primedCoordinator(wallet, scope = this)
            runCurrent()
            coordinator.selectCharacter("p1")
            runCurrent()
            coordinator.startWalletCheckout()
            runCurrent()
            "1234".forEach { coordinator.onWalletPinDigit(it) }
            coordinator.submitWalletCheckout()
            runCurrent()
            assertEquals(LiveTvWalletPayPhase.Failed, coordinator.state.value.walletPay.phase)
            assertTrue(
                coordinator.state.value.walletPay.message.orEmpty()
                    .contains("Insufficient", ignoreCase = true),
            )
        }

    @Test
    fun doubleSubmit_onlyOneDebit() =
        runTest {
            val wallet = FakeWalletOperations()
            val coordinator = primedCoordinator(wallet, scope = this)
            runCurrent()
            coordinator.selectCharacter("p1")
            runCurrent()
            coordinator.startWalletCheckout()
            runCurrent()
            "1234".forEach { coordinator.onWalletPinDigit(it) }
            coordinator.submitWalletCheckout()
            coordinator.submitWalletCheckout()
            runCurrent()
            assertEquals(1, wallet.debitCallCount.get())
        }

    @Test
    fun networkTimeout_unconfirmedPending_retryReusesIdempotencyKey() =
        runTest {
            val wallet = FakeWalletOperations()
            val session = WalletDebitSubmitSession.createForTests(MemoryWalletDebitPendingPersistence())
            wallet.debitResult = Result.failure(IOException("timeout"))
            val coordinator = primedCoordinator(wallet, session, this)
            runCurrent()
            coordinator.selectCharacter("p1")
            runCurrent()
            coordinator.startWalletCheckout()
            runCurrent()
            "1234".forEach { coordinator.onWalletPinDigit(it) }
            coordinator.submitWalletCheckout()
            runCurrent()
            assertEquals(LiveTvWalletPayPhase.UnconfirmedPending, coordinator.state.value.walletPay.phase)
            assertTrue(session.pendingWalletDebit != null)
            val firstKey = wallet.debitCalls.first().second

            wallet.debitResult = Result.success(WalletDebitResponse(transactionId = "tx-2", newBalance = "45.00"))
            "1234".forEach { coordinator.onWalletPinDigit(it) }
            coordinator.submitWalletCheckout()
            runCurrent()
            assertEquals(LiveTvWalletPayPhase.Success, coordinator.state.value.walletPay.phase)
            assertEquals(2, wallet.debitCallCount.get())
            assertEquals(firstKey, wallet.debitCalls[1].second)
        }

    @Test
    fun cartChangeDuringCheckout_blocksMutation_andStaleSubmitFails() =
        runTest {
            val wallet = FakeWalletOperations()
            val coordinator = primedCoordinator(wallet, scope = this)
            runCurrent()
            coordinator.selectCharacter("p1")
            runCurrent()
            coordinator.startWalletCheckout()
            runCurrent()
            assertEquals(LiveTvWalletPayPhase.EnterPin, coordinator.state.value.walletPay.phase)
            coordinator.increment("p1")
            assertEquals(1, coordinator.state.value.cart.quantityOf("p1"))
        }

    @Test
    fun liveTipConfig_createsQuote_andUsesLockedAmount() =
        runTest {
            val wallet = FakeWalletOperations()
            wallet.tipConfigResult =
                Result.success(
                    ActiveTipConfigResponse(
                        mode = "live",
                        version = 2,
                        config =
                            TipConfigPayloadDto(
                                sectionItems =
                                    listOf(
                                        TipConfigItemDto(
                                            itemId = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                                            characterId = "p1",
                                            priceMinor = 500,
                                        ),
                                    ),
                            ),
                    ),
                )
            wallet.tipQuoteResult =
                Result.success(
                    CreateTipQuoteResponse(
                        quoteId = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                        totalMinor = 500,
                        configVersion = 2,
                    ),
                )
            val coordinator = primedCoordinator(wallet, scope = this)
            runCurrent()
            coordinator.selectCharacter("p1")
            runCurrent()
            coordinator.startWalletCheckout()
            runCurrent()
            "1234".forEach { coordinator.onWalletPinDigit(it) }
            coordinator.submitWalletCheckout()
            runCurrent()
            assertEquals(LiveTvWalletPayPhase.Success, coordinator.state.value.walletPay.phase)
            assertEquals(1, wallet.tipQuoteCalls.size)
            assertEquals("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb", wallet.debitCalls.first().first.tipQuoteId)
            assertEquals(5.0, wallet.debitCalls.first().first.amount, 0.001)
        }

    @Test
    fun cancelWalletCheckout_returnsToIdle_preservesCart() =
        runTest {
            val wallet = FakeWalletOperations()
            val coordinator = primedCoordinator(wallet, scope = this)
            runCurrent()
            coordinator.selectCharacter("e1")
            runCurrent()
            coordinator.startWalletCheckout()
            runCurrent()
            coordinator.cancelWalletCheckout()
            runCurrent()
            assertEquals(LiveTvWalletPayPhase.Idle, coordinator.state.value.walletPay.phase)
            assertFalse(coordinator.state.value.cart.isEmpty)
            assertNotEquals(LiveTvWalletPayPhase.Success, coordinator.state.value.walletPay.phase)
        }
}
