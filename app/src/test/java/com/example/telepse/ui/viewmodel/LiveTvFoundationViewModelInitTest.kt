package com.example.telepse.ui.viewmodel

import com.example.telepse.analytics.TvAnalyticsClient
import com.example.telepse.data.auth.SessionExpiryHandler
import com.example.telepse.data.repository.TipperSession
import com.example.telepse.livetv.FakeTvScheduleRefreshSignals
import com.example.telepse.livetv.FakeTvScheduleSource
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Regression: ViewModel [init] used to launch a collector that touched [engine] before the
 * property was initialized (Main.immediate), causing NPE on Watch Live TV entry.
 *
 * Keep this test free of [advanceUntilIdle] / long-lived [StateFlow] collectors: [init] starts a
 * permanent [engine.state] collector on [viewModelScope].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class LiveTvFoundationViewModelInitTest {
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
    fun construction_withImmediateMain_doesNotThrow() =
        runTest {
            // UnconfinedTestDispatcher + setMain mimics Main.immediate: init's launch runs
            // during construction. This would NPE if engine were still uninitialized.
            val vm =
                LiveTvFoundationViewModel(
                    scheduleSource = FakeTvScheduleSource(),
                    refreshSignals = FakeTvScheduleRefreshSignals(),
                    sessionExpiryHandler = SessionExpiryHandler(NoOpTipperSession()),
                    analytics = TvAnalyticsClient(HttpClient(MockEngine { respondOk() })),
                )
            runCurrent()

            assertNotNull(vm.uiState)
            assertTrue(vm.webViewAttachKey >= 0)

            // Stop schedule/player work started by construction side-effects only.
            vm.onLeave()
            runCurrent()
        }

    @Test
    fun onEnter_andOnLeave_doNotCrashAfterFixedInitOrder() =
        runTest {
            val vm =
                LiveTvFoundationViewModel(
                    scheduleSource = FakeTvScheduleSource(),
                    refreshSignals = FakeTvScheduleRefreshSignals(),
                    sessionExpiryHandler = SessionExpiryHandler(NoOpTipperSession()),
                    analytics = TvAnalyticsClient(HttpClient(MockEngine { respondOk() })),
                )
            runCurrent()

            vm.onEnter()
            runCurrent()
            vm.refreshSchedule()
            runCurrent()
            vm.onLeave()
            runCurrent()

            assertNotNull(vm.uiState.value)
        }

    private class NoOpTipperSession : TipperSession {
        override fun signOut() {}
    }
}
