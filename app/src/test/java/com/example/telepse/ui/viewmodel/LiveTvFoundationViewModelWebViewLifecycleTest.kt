package com.example.telepse.ui.viewmodel

import android.content.Context
import android.webkit.WebView
import androidx.test.core.app.ApplicationProvider
import com.example.telepse.analytics.TvAnalyticsClient
import com.example.telepse.data.auth.SessionExpiryHandler
import com.example.telepse.data.auth.SessionStore
import com.example.telepse.data.repository.TipperSession
import com.example.telepse.livetv.FakeTvScheduleRefreshSignals
import com.example.telepse.livetv.FakeTvScheduleSource
import com.example.telepse.tips.FakeWalletOperations
import com.example.telepse.tips.MemoryWalletDebitPendingPersistence
import com.example.telepse.tips.WalletDebitSubmitSession
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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Regression: ordinary [LiveTvFoundationViewModel.attachWebView] must not bump
 * [LiveTvFoundationViewModel.webViewAttachKey], or Compose remounts the WebView on every
 * recomposition and playback never stays ready.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class LiveTvFoundationViewModelWebViewLifecycleTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(): LiveTvFoundationViewModel {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return LiveTvFoundationViewModel(
            scheduleSource = FakeTvScheduleSource(),
            refreshSignals = FakeTvScheduleRefreshSignals(),
            sessionExpiryHandler = SessionExpiryHandler(NoOpTipperSession()),
            analytics = TvAnalyticsClient(HttpClient(MockEngine { respondOk() })),
            walletOperations = FakeWalletOperations(),
            debitSubmitSession = WalletDebitSubmitSession.createForTests(MemoryWalletDebitPendingPersistence()),
            sessionStore = SessionStore.createPlainForUnitTests(context),
        )
    }

    private fun newWebView(): WebView =
        WebView(ApplicationProvider.getApplicationContext<Context>())

    @Test
    fun ordinaryAttach_doesNotIncrementComposeKey() =
        runTest {
            val vm = viewModel()
            runCurrent()
            val before = vm.webViewAttachKey

            vm.attachWebView(newWebView())
            runCurrent()
            assertEquals(before, vm.webViewAttachKey)

            vm.attachWebView(newWebView())
            runCurrent()
            assertEquals(
                "Repeated ordinary attach must not remount Compose via key change",
                before,
                vm.webViewAttachKey,
            )

            vm.onLeave()
            runCurrent()
        }

    @Test
    fun requestPlayerSurfaceRecreate_incrementsKeyAndClearsReady() =
        runTest {
            val vm = viewModel()
            runCurrent()
            vm.onEnter()
            runCurrent()
            vm.attachWebView(newWebView())
            runCurrent()
            val before = vm.webViewAttachKey

            vm.requestPlayerSurfaceRecreate()
            runCurrent()

            assertEquals(before + 1, vm.webViewAttachKey)
            assertFalse(vm.uiState.value.engine.playerReady)
            assertEquals(null, vm.uiState.value.engine.loadedVideoId)

            vm.onLeave()
            runCurrent()
        }

    @Test
    fun onWebViewReleased_clearsPlayerReadyWithoutChangingKey() =
        runTest {
            val vm = viewModel()
            runCurrent()
            vm.onEnter()
            runCurrent()
            vm.attachWebView(newWebView())
            runCurrent()
            val keyBefore = vm.webViewAttachKey

            vm.onWebViewReleased()
            runCurrent()

            assertEquals(keyBefore, vm.webViewAttachKey)
            assertFalse(vm.uiState.value.engine.playerReady)
            assertEquals(null, vm.uiState.value.engine.loadedVideoId)

            vm.onLeave()
            runCurrent()
        }

    private class NoOpTipperSession : TipperSession {
        override fun signOut() {}
    }
}
