package com.example.telepse.tips

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LiveTvTipCoordinatorTest {
    @Test
    fun loadsCatalogForEvent_andBuildsCartCheckoutBoundary() =
        runTest {
            val wallet = FakeWalletOperations()
            wallet.catalogResult = Result.success(FakeWalletOperations.sampleCatalog("evt-1"))
            var loggedIn = false
            val coordinator =
                LiveTvTipCoordinator(
                    walletOperations = wallet,
                    isLoggedIn = { loggedIn },
                    scope = this,
                )

            coordinator.onProgrammeEventId("evt-1")
            runCurrent()

            val ready = coordinator.state.value
            assertEquals(LiveTvTipCatalogPhase.Ready, ready.catalogPhase)
            assertEquals("evt-1", wallet.lastCatalogEventId)
            assertEquals("c1", ready.stripCharacters.first().id)
            assertEquals(listOf("Elite Tips", "General", "Premium"), ready.drawerSets.map { it.name })

            coordinator.selectCharacter("e1")
            runCurrent()
            assertEquals(1, coordinator.state.value.cart.itemCount)
            assertTrue(coordinator.state.value.showDrawer)
            assertFalse(coordinator.state.value.checkout.canEnterWalletCheckout)
            assertFalse(coordinator.state.value.checkout.canEnterQrCheckout)
            assertEquals(
                TipWalletCheckoutAvailability.RequiresSignIn,
                coordinator.state.value.checkout.wallet,
            )
            assertEquals(
                TipQrCheckoutAvailability.BackendUnavailable,
                coordinator.state.value.checkout.qrCard,
            )

            loggedIn = true
            coordinator.refreshAuthCheckout()
            assertTrue(coordinator.state.value.checkout.canEnterWalletCheckout)
            assertFalse(coordinator.state.value.checkout.canEnterQrCheckout)
        }

    @Test
    fun emptyCatalog_unavailableState_noFabrication() =
        runTest {
            val wallet = FakeWalletOperations()
            wallet.catalogResult = Result.success(FakeWalletOperations.emptyCatalog("evt-1"))
            val coordinator =
                LiveTvTipCoordinator(wallet, isLoggedIn = { true }, scope = this)

            coordinator.onProgrammeEventId("evt-1")
            runCurrent()

            assertEquals(LiveTvTipCatalogPhase.Empty, coordinator.state.value.catalogPhase)
            assertTrue(coordinator.state.value.stripCharacters.isEmpty())
            assertFalse(coordinator.state.value.canSelectTips)
        }

    @Test
    fun noEventId_showsNoEvent_withoutFetch() =
        runTest {
            val wallet = FakeWalletOperations()
            val coordinator =
                LiveTvTipCoordinator(wallet, isLoggedIn = { false }, scope = this)

            coordinator.onProgrammeEventId("")
            runCurrent()

            assertEquals(0, wallet.catalogFetchCount.get())
            assertEquals(LiveTvTipCatalogPhase.NoEvent, coordinator.state.value.catalogPhase)
        }

    @Test
    fun eventChange_invalidatesCart_andReloads() =
        runTest {
            val wallet = FakeWalletOperations()
            wallet.catalogResult = Result.success(FakeWalletOperations.sampleCatalog("evt-1"))
            val coordinator =
                LiveTvTipCoordinator(wallet, isLoggedIn = { true }, scope = this)

            coordinator.onProgrammeEventId("evt-1")
            runCurrent()
            coordinator.selectCharacter("g1")
            runCurrent()
            assertEquals(1, coordinator.state.value.cart.itemCount)

            wallet.catalogResult = Result.success(FakeWalletOperations.sampleCatalog("evt-2"))
            coordinator.onProgrammeEventId("evt-2")
            runCurrent()

            assertEquals("evt-2", coordinator.state.value.eventId)
            assertTrue(coordinator.state.value.cart.isEmpty)
            assertEquals(2, wallet.catalogFetchCount.get())
            assertEquals("evt-2", wallet.lastCatalogEventId)
        }

    @Test
    fun catalogError_andQuantityEdits() =
        runTest {
            val wallet = FakeWalletOperations()
            wallet.catalogResult = Result.failure(IllegalStateException("boom"))
            val coordinator =
                LiveTvTipCoordinator(wallet, isLoggedIn = { true }, scope = this)

            coordinator.onProgrammeEventId("evt-1")
            runCurrent()
            assertEquals(LiveTvTipCatalogPhase.Error, coordinator.state.value.catalogPhase)

            wallet.catalogResult = Result.success(FakeWalletOperations.sampleCatalog("evt-1"))
            coordinator.refreshCatalog()
            runCurrent()
            assertEquals(LiveTvTipCatalogPhase.Ready, coordinator.state.value.catalogPhase)

            coordinator.selectCharacter("p1")
            coordinator.increment("p1")
            coordinator.decrement("p1")
            runCurrent()
            assertEquals(1, coordinator.state.value.cart.quantityOf("p1"))
            coordinator.removeFromCart("p1")
            assertTrue(coordinator.state.value.cart.isEmpty)
        }

    @Test
    fun sameEventId_doesNotRefetch() =
        runTest {
            val wallet = FakeWalletOperations()
            val coordinator =
                LiveTvTipCoordinator(wallet, isLoggedIn = { false }, scope = this)
            coordinator.onProgrammeEventId("evt-1")
            runCurrent()
            coordinator.onProgrammeEventId("evt-1")
            runCurrent()
            assertEquals(1, wallet.catalogFetchCount.get())
        }
}
