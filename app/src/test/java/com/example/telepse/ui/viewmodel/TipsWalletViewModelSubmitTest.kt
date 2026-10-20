package com.example.telepse.ui.viewmodel

import android.content.Context
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import com.example.telepse.analytics.TvAnalyticsClient
import com.example.telepse.data.auth.SessionExpiryHandler
import com.example.telepse.data.model.CharacterSetCatalogRow
import com.example.telepse.data.model.EventCharacterCatalogResponse
import com.example.telepse.data.model.TipCharacterCatalogItem
import com.example.telepse.data.model.WalletDebitRequest
import com.example.telepse.data.model.WalletDebitResponse
import com.example.telepse.data.model.WalletStatusResponse
import com.example.telepse.data.model.WalletTransactionsResponse
import com.example.telepse.data.network.WalletHttpException
import com.example.telepse.data.repository.TipperSession
import com.example.telepse.data.repository.WalletOperations
import com.example.telepse.tips.WalletDebitPendingStore
import com.example.telepse.tips.WalletDebitSubmitMessages
import com.example.telepse.tips.WalletDebitSubmitSession
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class TipsWalletViewModelSubmitTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var context: Context
    private lateinit var pendingStore: WalletDebitPendingStore
    private lateinit var debitSession: WalletDebitSubmitSession

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        pendingStore = WalletDebitPendingStore.createPlainForUnitTests(context)
        pendingStore.clear()
        debitSession = WalletDebitSubmitSession(pendingStore)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun retryAfterFailure_reusesIdempotencyKeyAndRequest() =
        runTest {
            val wallet = RecordingWalletOperations()
            wallet.enqueueDebit(Result.failure(IOException("timeout")))
            wallet.enqueueDebit(Result.success(WalletDebitResponse(newBalance = "42.00")))
            val vm = createViewModel(wallet)
            primeReadyToSubmit(vm)
            advanceUntilIdle()

            vm.submitWalletTip()
            advanceUntilIdle()
            assertEquals(TipSubmitPhase.Error, vm.uiState.value.tipSubmitPhase)
            assertTrue(
                vm.uiState.value.tipResultMessage.orEmpty()
                    .contains("couldn't confirm", ignoreCase = true),
            )

            assertTrue(vm.uiState.value.hasUnresolvedPendingDebit)
            assertEquals("", vm.uiState.value.pinInput)
            vm.onPinChange("1234")
            vm.submitWalletTip()
            advanceUntilIdle()
            assertEquals(TipSubmitPhase.Success, vm.uiState.value.tipSubmitPhase)
            assertEquals(2, wallet.debitCalls.size)
            assertEquals(wallet.debitCalls[0].second, wallet.debitCalls[1].second)
            assertEquals(wallet.debitCalls[0].first, wallet.debitCalls[1].first)
            assertFalse(vm.uiState.value.hasUnresolvedPendingDebit)
        }

    @Test
    fun inFlightSubmit_onlyOneDebitCall() =
        runTest {
            val wallet = RecordingWalletOperations()
            wallet.enqueueDebit(Result.success(WalletDebitResponse(newBalance = "1.00")))
            val vm = createViewModel(wallet)
            primeReadyToSubmit(vm)
            advanceUntilIdle()

            vm.submitWalletTip()
            vm.submitWalletTip()
            advanceUntilIdle()
            assertEquals(1, wallet.debitCalls.size)
        }

    @Test
    fun debitFailure_doesNotShowSuccess() =
        runTest {
            val wallet = RecordingWalletOperations()
            wallet.enqueueDebit(Result.failure(IOException("network")))
            val vm = createViewModel(wallet)
            primeReadyToSubmit(vm)
            advanceUntilIdle()

            vm.submitWalletTip()
            advanceUntilIdle()
            assertEquals(TipSubmitPhase.Error, vm.uiState.value.tipSubmitPhase)
        }

    @Test
    fun server503_unknownOutcome_retainsKeyOnRetry() =
        runTest {
            val wallet = RecordingWalletOperations()
            wallet.enqueueDebit(
                Result.failure(WalletHttpException(httpStatus = 503, authPresent = true)),
            )
            wallet.enqueueDebit(Result.success(WalletDebitResponse(newBalance = "99.00")))
            val vm = createViewModel(wallet)
            primeReadyToSubmit(vm)
            advanceUntilIdle()

            vm.submitWalletTip()
            advanceUntilIdle()
            assertEquals(TipSubmitPhase.Error, vm.uiState.value.tipSubmitPhase)
            assertEquals(WalletDebitSubmitMessages.UNKNOWN_OUTCOME_MESSAGE, vm.uiState.value.tipResultMessage)
            assertTrue(vm.uiState.value.hasUnresolvedPendingDebit)

            vm.onPinChange("1234")
            vm.submitWalletTip()
            advanceUntilIdle()
            assertEquals(TipSubmitPhase.Success, vm.uiState.value.tipSubmitPhase)
            assertEquals(wallet.debitCalls[0].second, wallet.debitCalls[1].second)
        }

    @Test
    fun confirmed400_showsApiMessageWhenPresent() =
        runTest {
            val wallet = RecordingWalletOperations()
            wallet.enqueueDebit(
                Result.failure(
                    WalletHttpException(
                        httpStatus = 400,
                        authPresent = true,
                        apiMessage = "Incorrect wallet PIN",
                    ),
                ),
            )
            val vm = createViewModel(wallet)
            primeReadyToSubmit(vm)
            advanceUntilIdle()

            vm.submitWalletTip()
            advanceUntilIdle()
            assertEquals(TipSubmitPhase.Error, vm.uiState.value.tipSubmitPhase)
            assertEquals("Incorrect wallet PIN", vm.uiState.value.tipResultMessage)
        }

    @Test
    fun session401_onDebit_usesSessionExpiredMessage() =
        runTest {
            val wallet = RecordingWalletOperations()
            wallet.enqueueDebit(
                Result.failure(WalletHttpException(httpStatus = 401, authPresent = true)),
            )
            val session = RecordingTipperSession()
            val vm =
                TipsWalletViewModel(
                    walletOperations = wallet,
                    sessionExpiryHandler = SessionExpiryHandler(session),
                    analytics = TvAnalyticsClient(HttpClient(MockEngine { respondOk() })),
                    debitSubmitSession = debitSession,
                )
            primeReadyToSubmit(vm)
            advanceUntilIdle()

            vm.submitWalletTip()
            advanceUntilIdle()
            assertEquals(TipSubmitPhase.Error, vm.uiState.value.tipSubmitPhase)
            assertTrue(vm.uiState.value.tipResultMessage.orEmpty().contains("Session expired"))
            assertEquals(1, session.signOutCount)
        }

    @Test
    fun successThenNewTip_usesNewIdempotencyKey() =
        runTest {
            val wallet = RecordingWalletOperations()
            wallet.enqueueDebit(Result.success(WalletDebitResponse(newBalance = "50.00")))
            wallet.enqueueDebit(Result.success(WalletDebitResponse(newBalance = "45.00")))
            val vm = createViewModel(wallet)
            primeReadyToSubmit(vm)
            advanceUntilIdle()

            vm.submitWalletTip()
            advanceUntilIdle()
            assertEquals(TipSubmitPhase.Success, vm.uiState.value.tipSubmitPhase)

            vm.onPinChange("1234")
            vm.submitWalletTip()
            advanceUntilIdle()
            assertEquals(2, wallet.debitCalls.size)
            assertTrue(wallet.debitCalls[0].second != wallet.debitCalls[1].second)
        }

    @Test
    fun changedCharacterAfterUnknownOutcome_blocksSilentReplace() =
        runTest {
            val wallet = RecordingWalletOperations()
            wallet.enqueueDebit(Result.failure(IOException("timeout")))
            val vm = createViewModel(wallet)
            primeReadyToSubmit(vm)
            advanceUntilIdle()

            vm.submitWalletTip()
            advanceUntilIdle()
            vm.selectCharacter("char-b")
            advanceUntilIdle()

            assertEquals(1, wallet.debitCalls.size)
            assertEquals(TipSubmitPhase.Error, vm.uiState.value.tipSubmitPhase)
            assertTrue(vm.uiState.value.hasUnresolvedPendingDebit)
            assertEquals("char-a", vm.uiState.value.selectedCharacterId)
            assertEquals(
                WalletDebitSubmitMessages.PENDING_IDENTITY_LOCKED,
                vm.uiState.value.tipResultMessage,
            )
        }

    @Test
    fun pendingSurvivesViewModelRecreation_requiresPinReentry() =
        runTest {
            val wallet = RecordingWalletOperations()
            wallet.enqueueDebit(Result.failure(IOException("timeout")))
            val vm1 = createViewModel(wallet)
            primeReadyToSubmit(vm1)
            advanceUntilIdle()

            vm1.submitWalletTip()
            advanceUntilIdle()
            val key = wallet.debitCalls.single().second

            val session2 = WalletDebitSubmitSession(pendingStore)
            val vm2 = createViewModel(wallet, session2)
            assertEquals("", vm2.uiState.value.pinInput)
            assertTrue(vm2.uiState.value.hasUnresolvedPendingDebit)
            vm2.loadCharacterCatalog()
            advanceUntilIdle()

            wallet.enqueueDebit(Result.success(WalletDebitResponse(newBalance = "88.00")))
            vm2.onPinChange("1234")
            vm2.submitWalletTip()
            advanceUntilIdle()
            assertEquals(TipSubmitPhase.Success, vm2.uiState.value.tipSubmitPhase)
            assertEquals(2, wallet.debitCalls.size)
            assertEquals(key, wallet.debitCalls[1].second)
            assertEquals(null, pendingStore.load())
        }

    @Test
    fun cancellation_doesNotShowFalseSuccessOrNetworkError() =
        runTest {
            val wallet = SuspendingWalletOperations()
            val vm = createViewModel(wallet)
            primeReadyToSubmit(vm)
            advanceUntilIdle()

            vm.submitWalletTip()
            advanceUntilIdle()
            assertEquals(TipSubmitPhase.Submitting, vm.uiState.value.tipSubmitPhase)
            vm.viewModelScope.coroutineContext.job.cancelChildren()
            advanceUntilIdle()

            assertNotEquals(TipSubmitPhase.Success, vm.uiState.value.tipSubmitPhase)
            assertFalse(
                vm.uiState.value.tipResultMessage.orEmpty()
                    .contains("couldn't confirm", ignoreCase = true),
            )
            assertEquals(TipSubmitPhase.Idle, vm.uiState.value.tipSubmitPhase)
            assertTrue(vm.uiState.value.hasUnresolvedPendingDebit)
            assertEquals(1, wallet.debitCalls.size)
        }

    @Test
    fun clearTipResult_keepsPendingRecoveryVisible() =
        runTest {
            val wallet = RecordingWalletOperations()
            wallet.enqueueDebit(Result.failure(IOException("timeout")))
            val vm = createViewModel(wallet)
            primeReadyToSubmit(vm)
            advanceUntilIdle()

            vm.submitWalletTip()
            advanceUntilIdle()
            assertTrue(vm.uiState.value.hasUnresolvedPendingDebit)

            vm.clearTipResult()
            advanceUntilIdle()
            assertTrue(vm.uiState.value.hasUnresolvedPendingDebit)
            assertEquals(TipSubmitPhase.Idle, vm.uiState.value.tipSubmitPhase)
            assertEquals(null, vm.uiState.value.tipResultMessage)
        }

    @Test
    fun dismissPending_clearsLocalStateAndShowsHonestAck() =
        runTest {
            val wallet = RecordingWalletOperations()
            wallet.enqueueDebit(Result.failure(IOException("timeout")))
            val vm = createViewModel(wallet)
            primeReadyToSubmit(vm)
            advanceUntilIdle()

            vm.submitWalletTip()
            advanceUntilIdle()
            assertTrue(vm.uiState.value.hasUnresolvedPendingDebit)

            vm.dismissUnresolvedPendingTip()
            advanceUntilIdle()
            assertFalse(vm.uiState.value.hasUnresolvedPendingDebit)
            assertEquals(null, pendingStore.load())
            assertTrue(
                vm.uiState.value.pendingDiscardAcknowledgement.orEmpty()
                    .contains("does not undo", ignoreCase = true),
            )
        }

    @Test
    fun pendingRecovery_restoredAfterViewModelRecreation_flagVisible() =
        runTest {
            val wallet = RecordingWalletOperations()
            wallet.enqueueDebit(Result.failure(IOException("timeout")))
            val vm1 = createViewModel(wallet)
            primeReadyToSubmit(vm1)
            advanceUntilIdle()
            vm1.submitWalletTip()
            advanceUntilIdle()

            val vm2 = createViewModel(wallet, WalletDebitSubmitSession(pendingStore))
            assertTrue(vm2.uiState.value.hasUnresolvedPendingDebit)
            assertEquals("", vm2.uiState.value.pinInput)
        }

    private fun createViewModel(
        wallet: WalletOperations,
        session: WalletDebitSubmitSession = debitSession,
    ): TipsWalletViewModel {
        val analyticsHttp =
            HttpClient(MockEngine { respondOk() })
        return TipsWalletViewModel(
            walletOperations = wallet,
            sessionExpiryHandler = SessionExpiryHandler(NoOpTipperSession),
            analytics = TvAnalyticsClient(analyticsHttp),
            debitSubmitSession = session,
        )
    }

    private fun primeReadyToSubmit(vm: TipsWalletViewModel) {
        val eventId = "11111111-1111-1111-1111-111111111111"
        vm.onEventIdChange(eventId)
        vm.onPinChange("1234")
        vm.loadCharacterCatalog()
        vm.selectCharacter("char-a")
    }

    private object NoOpTipperSession : TipperSession {
        override fun signOut() = Unit
    }

    private class RecordingTipperSession : TipperSession {
        var signOutCount = 0
        override fun signOut() {
            signOutCount++
        }
    }

    private open class RecordingWalletOperations : WalletOperations {
        val debitCalls = mutableListOf<Pair<WalletDebitRequest, String>>()
        private val debitResults = ArrayDeque<Result<WalletDebitResponse>>()

        fun enqueueDebit(result: Result<WalletDebitResponse>) {
            debitResults.addLast(result)
        }

        override suspend fun fetchWallet(): Result<WalletStatusResponse> =
            Result.success(WalletStatusResponse(exists = true, balance = "100"))

        override suspend fun fetchTransactions(): Result<WalletTransactionsResponse> =
            Result.success(WalletTransactionsResponse())

        override suspend fun fetchCharacterCatalog(eventId: String): Result<EventCharacterCatalogResponse> =
            Result.success(
                EventCharacterCatalogResponse(
                    eventId = eventId,
                    characterSets =
                        listOf(
                            CharacterSetCatalogRow(
                                id = "set",
                                name = "Set",
                                characters =
                                    listOf(
                                        TipCharacterCatalogItem(
                                            id = "char-a",
                                            name = "Wave",
                                            price = 5.0,
                                        ),
                                        TipCharacterCatalogItem(
                                            id = "char-b",
                                            name = "Big",
                                            price = 10.0,
                                        ),
                                    ),
                            ),
                        ),
                ),
            )

        override suspend fun debitWallet(
            request: WalletDebitRequest,
            idempotencyKey: String,
        ): Result<WalletDebitResponse> {
            debitCalls.add(request to idempotencyKey)
            return debitResults.removeFirst()
        }
    }

    private class SuspendingWalletOperations : RecordingWalletOperations() {
        override suspend fun debitWallet(
            request: WalletDebitRequest,
            idempotencyKey: String,
        ): Result<WalletDebitResponse> {
            debitCalls.add(request to idempotencyKey)
            delay(Long.MAX_VALUE)
            error("unreachable")
        }
    }
}
