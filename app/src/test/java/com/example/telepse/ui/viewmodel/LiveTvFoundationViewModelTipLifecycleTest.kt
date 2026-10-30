package com.example.telepse.ui.viewmodel

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.telepse.analytics.TvAnalyticsClient
import com.example.telepse.data.auth.SessionExpiryHandler
import com.example.telepse.data.auth.SessionStore
import com.example.telepse.data.repository.TipperSession
import com.example.telepse.livetv.FakeTvScheduleRefreshSignals
import com.example.telepse.livetv.FakeTvScheduleSource
import com.example.telepse.tips.FakeWalletOperations
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tip overlay open/close must not remount the player WebView (Compose attach key stable).
 * Avoids engine.start() polling/collect loops that can stall under Robolectric + Unconfined.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class LiveTvFoundationViewModelTipLifecycleTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun tipDrawerOpenClose_doesNotBumpWebViewAttachKey() =
        runTest {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val vm =
                LiveTvFoundationViewModel(
                    scheduleSource = FakeTvScheduleSource(),
                    refreshSignals = FakeTvScheduleRefreshSignals(),
                    sessionExpiryHandler = SessionExpiryHandler(NoOpTipperSession()),
                    analytics = TvAnalyticsClient(HttpClient(MockEngine { respondOk() })),
                    walletOperations = FakeWalletOperations(),
                    sessionStore = SessionStore.createPlainForUnitTests(context),
                )
            runCurrent()

            val keyBefore = vm.webViewAttachKey
            vm.openTipDrawer()
            runCurrent()
            assertTrue(vm.uiState.value.tip.showDrawer)
            assertEquals(keyBefore, vm.webViewAttachKey)

            vm.closeTipDrawer()
            runCurrent()
            assertFalse(vm.uiState.value.tip.showDrawer)
            assertEquals(keyBefore, vm.webViewAttachKey)

            vm.toggleTipStrip()
            runCurrent()
            assertEquals(keyBefore, vm.webViewAttachKey)

            vm.onLeave()
            runCurrent()
        }

    private class NoOpTipperSession : TipperSession {
        override fun signOut() {}
    }
}
