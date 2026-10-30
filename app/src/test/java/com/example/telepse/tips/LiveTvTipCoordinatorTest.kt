package com.example.telepse.tips

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LiveTvTipCoordinatorTest {
    private fun newSession() = WalletDebitSubmitSession.createForTests(MemoryWalletDebitPendingPersistence())

    private fun coordinator(
        wallet: FakeWalletOperations,
        loggedIn: () -> Boolean,
        scope: kotlinx.coroutines.CoroutineScope,
    ) = LiveTvTipCoordinator(
        walletOperations = wallet,
        debitSubmitSession = newSession(),
        isLoggedIn = loggedIn,
        scope = scope,
    )

    @Test
    fun loadsCatalogForEvent_andBuildsCartCheckoutBoundary() =
        runTest {
            val wallet = FakeWalletOperations()
            wallet.catalogResult = Result.success(FakeWalletOperations.sampleCatalog("evt-1"))
            var loggedIn = false
            val tipCoordinator = coordinator(wallet, { loggedIn }, this)

            tipCoordinator.onProgrammeEventId("evt-1")
            runCurrent()

            val ready = tipCoordinator.state.value
            assertEquals(LiveTvTipCatalogPhase.Ready, ready.catalogPhase)
            assertEquals("evt-1", wallet.lastCatalogEventId)
            assertEquals("c1", ready.stripCharacters.first().id)
            assertEquals(listOf("Elite Tips", "General", "Premium"), ready.drawerSets.map { it.name })

            tipCoordinator.selectCharacter("e1")
            runCurrent()
            assertEquals(1, tipCoordinator.state.value.cart.itemCount)
            assertTrue(tipCoordinator.state.value.showDrawer)
            assertFalse(tipCoordinator.state.value.checkout.canEnterWalletCheckout)
            assertFalse(tipCoordinator.state.value.checkout.canEnterQrCheckout)
            assertEquals(
                TipWalletCheckoutAvailability.RequiresSignIn,
                tipCoordinator.state.value.checkout.wallet,
            )
            assertEquals(
                TipQrCheckoutAvailability.BackendUnavailable,
                tipCoordinator.state.value.checkout.qrCard,
            )

            loggedIn = true
            tipCoordinator.refreshAuthCheckout()
            assertTrue(tipCoordinator.state.value.checkout.canEnterWalletCheckout)
            assertFalse(tipCoordinator.state.value.checkout.canEnterQrCheckout)
        }

    @Test
    fun emptyCatalog_unavailableState_noFabrication() =
        runTest {
            val wallet = FakeWalletOperations()
            wallet.catalogResult = Result.success(FakeWalletOperations.emptyCatalog("evt-1"))
            val tipCoordinator = coordinator(wallet, { true }, this)

            tipCoordinator.onProgrammeEventId("evt-1")
            runCurrent()

            assertEquals(LiveTvTipCatalogPhase.Empty, tipCoordinator.state.value.catalogPhase)
            assertTrue(tipCoordinator.state.value.stripCharacters.isEmpty())
            assertFalse(tipCoordinator.state.value.canSelectTips)
        }

    @Test
    fun noEventId_showsNoEvent_withoutFetch() =
        runTest {
            val wallet = FakeWalletOperations()
            val tipCoordinator = coordinator(wallet, { false }, this)

            tipCoordinator.onProgrammeEventId("")
            runCurrent()

            assertEquals(0, wallet.catalogFetchCount.get())
            assertEquals(LiveTvTipCatalogPhase.NoEvent, tipCoordinator.state.value.catalogPhase)
        }

    @Test
    fun eventChange_invalidatesCart_andReloads() =
        runTest {
            val wallet = FakeWalletOperations()
            wallet.catalogResult = Result.success(FakeWalletOperations.sampleCatalog("evt-1"))
            val tipCoordinator = coordinator(wallet, { true }, this)

            tipCoordinator.onProgrammeEventId("evt-1")
            runCurrent()
            tipCoordinator.selectCharacter("g1")
            runCurrent()
            assertEquals(1, tipCoordinator.state.value.cart.itemCount)

            wallet.catalogResult = Result.success(FakeWalletOperations.sampleCatalog("evt-2"))
            tipCoordinator.onProgrammeEventId("evt-2")
            runCurrent()

            assertEquals("evt-2", tipCoordinator.state.value.eventId)
            assertTrue(tipCoordinator.state.value.cart.isEmpty)
            assertEquals(2, wallet.catalogFetchCount.get())
            assertEquals("evt-2", wallet.lastCatalogEventId)
        }

    @Test
    fun catalogError_andQuantityEdits() =
        runTest {
            val wallet = FakeWalletOperations()
            wallet.catalogResult = Result.failure(IllegalStateException("boom"))
            val tipCoordinator = coordinator(wallet, { true }, this)

            tipCoordinator.onProgrammeEventId("evt-1")
            runCurrent()
            assertEquals(LiveTvTipCatalogPhase.Error, tipCoordinator.state.value.catalogPhase)

            wallet.catalogResult = Result.success(FakeWalletOperations.sampleCatalog("evt-1"))
            tipCoordinator.refreshCatalog()
            runCurrent()
            assertEquals(LiveTvTipCatalogPhase.Ready, tipCoordinator.state.value.catalogPhase)

            tipCoordinator.selectCharacter("p1")
            tipCoordinator.increment("p1")
            tipCoordinator.decrement("p1")
            runCurrent()
            assertEquals(1, tipCoordinator.state.value.cart.quantityOf("p1"))
            tipCoordinator.removeFromCart("p1")
            assertTrue(tipCoordinator.state.value.cart.isEmpty)
        }

    @Test
    fun sameEventId_doesNotRefetch() =
        runTest {
            val wallet = FakeWalletOperations()
            val tipCoordinator = coordinator(wallet, { false }, this)
            tipCoordinator.onProgrammeEventId("evt-1")
            runCurrent()
            tipCoordinator.onProgrammeEventId("evt-1")
            runCurrent()
            assertEquals(1, wallet.catalogFetchCount.get())
        }
}
