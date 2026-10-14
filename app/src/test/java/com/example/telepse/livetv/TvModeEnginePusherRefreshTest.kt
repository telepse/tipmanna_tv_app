package com.example.telepse.livetv

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TvModeEnginePusherRefreshTest {

    @Test
    fun pusherInvalidation_triggersDebouncedRefresh() = runTest {
        val source = FakeTvScheduleSource()
        val player = FakeYouTubePlayerAdapter()
        val signals = FakeTvScheduleRefreshSignals()
        val engine =
            TvModeEngine(
                scheduleSource = source,
                player = player,
                scope = this,
                refreshSignals = signals,
                debounceMs = 100,
                pollIntervalMs = null,
            )
        engine.start()
        advanceUntilIdle()
        assertEquals(1, source.fetchCount.get())

        signals.emit(TvScheduleRefreshTrigger.PUSHER_INVALIDATION)
        advanceTimeBy(50)
        assertEquals(1, source.fetchCount.get())
        advanceTimeBy(100)
        advanceUntilIdle()
        assertEquals(2, source.fetchCount.get())
        assertEquals(1, player.loadCalls.size)
        engine.dispose()
    }

    @Test
    fun burstInvalidations_coalesceToSingleRefresh() = runTest {
        val source = FakeTvScheduleSource()
        val player = FakeYouTubePlayerAdapter()
        val signals = FakeTvScheduleRefreshSignals()
        val engine =
            TvModeEngine(
                scheduleSource = source,
                player = player,
                scope = this,
                refreshSignals = signals,
                debounceMs = 200,
                pollIntervalMs = null,
            )
        engine.start()
        advanceUntilIdle()

        repeat(5) {
            engine.requestScheduleRefreshDebounced(fromRealtime = true)
            advanceTimeBy(30)
        }
        advanceTimeBy(250)
        advanceUntilIdle()
        assertEquals(2, source.fetchCount.get())
        engine.dispose()
    }

    @Test
    fun stop_cancelsPendingDebounceAndStopsSignals() = runTest {
        val source = FakeTvScheduleSource()
        val signals = FakeTvScheduleRefreshSignals()
        val engine =
            TvModeEngine(
                scheduleSource = source,
                player = FakeYouTubePlayerAdapter(),
                scope = this,
                refreshSignals = signals,
                debounceMs = 500,
                pollIntervalMs = null,
            )
        engine.start()
        advanceUntilIdle()
        engine.requestScheduleRefreshDebounced(fromRealtime = true)
        engine.stop()
        advanceTimeBy(600)
        advanceUntilIdle()
        assertEquals(1, source.fetchCount.get())
        assertEquals(1, signals.stopCount.get())
    }

    @Test
    fun worksWithoutRealtimeSignals() = runTest {
        val source = FakeTvScheduleSource()
        val engine =
            TvModeEngine(
                scheduleSource = source,
                player = FakeYouTubePlayerAdapter(),
                scope = this,
                refreshSignals = null,
                pollIntervalMs = null,
            )
        engine.start()
        advanceUntilIdle()
        engine.refreshSchedule()
        advanceUntilIdle()
        assertEquals(2, source.fetchCount.get())
        engine.dispose()
    }

    @Test
    fun startListening_onlyOnceWhenReenteringActive() = runTest {
        val signals = FakeTvScheduleRefreshSignals()
        val engine =
            TvModeEngine(
                scheduleSource = FakeTvScheduleSource(),
                player = FakeYouTubePlayerAdapter(),
                scope = this,
                refreshSignals = signals,
                pollIntervalMs = null,
            )
        engine.start()
        advanceUntilIdle()
        engine.stop()
        advanceUntilIdle()
        engine.start()
        advanceUntilIdle()
        assertTrue(signals.startCount.get() >= 1)
        engine.dispose()
    }
}
