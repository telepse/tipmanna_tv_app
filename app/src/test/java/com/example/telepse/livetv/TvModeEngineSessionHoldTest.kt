package com.example.telepse.livetv

import com.example.telepse.data.model.TvScheduleFetchOutcome
import com.example.telepse.data.network.ScheduleHttpException
import com.example.telepse.player.YouTubePlayerState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TvModeEngineSessionHoldTest {

    private fun engineWith401Hold(
        source: FakeTvScheduleSource,
        scope: kotlinx.coroutines.CoroutineScope,
        pollIntervalMs: Long? = 1_000L,
        signals: TvScheduleRefreshSignals? = null,
    ): TvModeEngine {
        source.nextResult =
            Result.failure(ScheduleHttpException(httpStatus = 401, authPresent = true))
        return TvModeEngine(
            scheduleSource = source,
            player = FakeYouTubePlayerAdapter(),
            scope = scope,
            refreshSignals = signals,
            debounceMs = 100,
            pollIntervalMs = pollIntervalMs,
            errorRecoveryDelayMs = 5_000L,
            onAuthenticatedSessionFailure = { true },
        )
    }

    @Test
    fun stopAndStart_preservesHoldAndSkipsBackgroundPolling() =
        runTest {
            val source = FakeTvScheduleSource()
            val engine = engineWith401Hold(source, this)
            engine.start()
            advanceUntilIdle()
            assertTrue(engine.state.value.scheduleBackgroundRefreshSuspended)
            assertEquals(1, source.fetchCount.get())

            engine.stop()
            advanceUntilIdle()
            engine.start()
            advanceUntilIdle()
            assertTrue(engine.state.value.scheduleBackgroundRefreshSuspended)
            assertEquals(1, source.fetchCount.get())

            advanceTimeBy(5_000L)
            runCurrent()
            assertEquals(1, source.fetchCount.get())
            engine.dispose()
        }

    @Test
    fun whileHoldActive_pusherSignal_doesNotFetch() =
        runTest {
            val source = FakeTvScheduleSource()
            val signals = FakeTvScheduleRefreshSignals()
            val engine = engineWith401Hold(source, this, pollIntervalMs = null, signals = signals)
            engine.start()
            advanceUntilIdle()
            assertTrue(engine.state.value.scheduleBackgroundRefreshSuspended)

            signals.emit(TvScheduleRefreshTrigger.PUSHER_INVALIDATION)
            advanceTimeBy(200)
            advanceUntilIdle()
            assertEquals(1, source.fetchCount.get())
            engine.dispose()
        }

    @Test
    fun whileHoldActive_playerEnded_doesNotFetch() =
        runTest {
            val source = FakeTvScheduleSource()
            val player = FakeYouTubePlayerAdapter()
            val engine =
                TvModeEngine(
                    scheduleSource = source,
                    player = player,
                    scope = this,
                    debounceMs = 100,
                    pollIntervalMs = null,
                    errorRecoveryDelayMs = null,
                    onAuthenticatedSessionFailure = { true },
                )
            engine.start()
            advanceUntilIdle()
            assertEquals(1, source.fetchCount.get())

            source.nextResult =
                Result.failure(ScheduleHttpException(httpStatus = 401, authPresent = true))
            engine.refreshSchedule()
            advanceUntilIdle()
            assertTrue(engine.state.value.scheduleBackgroundRefreshSuspended)
            assertEquals(2, source.fetchCount.get())

            player.emitState(YouTubePlayerState.ENDED)
            advanceTimeBy(200)
            advanceUntilIdle()
            assertEquals(2, source.fetchCount.get())
            engine.dispose()
        }

    @Test
    fun whileHoldActive_debouncedRefresh_doesNotFetchAfterDelay() =
        runTest {
            val source = FakeTvScheduleSource()
            val engine = engineWith401Hold(source, this, pollIntervalMs = null)
            engine.start()
            advanceUntilIdle()

            engine.requestScheduleRefreshDebounced(fromRealtime = false)
            advanceTimeBy(200)
            advanceUntilIdle()
            assertEquals(1, source.fetchCount.get())
            engine.dispose()
        }

    @Test
    fun whileHoldActive_debounceScheduledBeforeHold_isCancelledOnHold() =
        runTest {
            val source = FakeTvScheduleSource()
            val player = FakeYouTubePlayerAdapter()
            val engine =
                TvModeEngine(
                    scheduleSource = source,
                    player = player,
                    scope = this,
                    debounceMs = 500,
                    pollIntervalMs = null,
                    errorRecoveryDelayMs = null,
                    onAuthenticatedSessionFailure = { true },
                )
            engine.start()
            advanceUntilIdle()
            player.emitError()
            advanceTimeBy(50)

            source.nextResult =
                Result.failure(ScheduleHttpException(httpStatus = 401, authPresent = true))
            engine.refreshSchedule()
            advanceUntilIdle()
            assertTrue(engine.state.value.scheduleBackgroundRefreshSuspended)

            advanceTimeBy(600)
            runCurrent()
            assertEquals(2, source.fetchCount.get())
            engine.dispose()
        }

    @Test
    fun explicitRefresh_successClearsHold() =
        runTest {
            val source = FakeTvScheduleSource()
            val engine = engineWith401Hold(source, this, pollIntervalMs = null)
            engine.start()
            advanceUntilIdle()

            source.nextResult =
                Result.success(
                    TvScheduleFetchOutcome(
                        schedule = FakeTvScheduleSource.sampleSchedule("Ffs0YHm2jks", 0),
                        etag = "\"ok\"",
                    ),
                )
            engine.refreshSchedule()
            advanceUntilIdle()
            assertFalse(engine.state.value.scheduleBackgroundRefreshSuspended)
            assertEquals(2, source.fetchCount.get())
            engine.dispose()
        }

    @Test
    fun transientNetworkError_stillSchedulesRecoveryWhenNotInHold() =
        runTest {
            val source = FakeTvScheduleSource()
            source.nextResult = Result.failure(IllegalStateException("HTTP 503"))
            val engine =
                TvModeEngine(
                    scheduleSource = source,
                    player = FakeYouTubePlayerAdapter(),
                    scope = this,
                    pollIntervalMs = null,
                    errorRecoveryDelayMs = 100L,
                    onAuthenticatedSessionFailure = { false },
                )
            engine.start()
            runCurrent()
            assertFalse(engine.state.value.scheduleBackgroundRefreshSuspended)
            assertEquals(1, source.fetchCount.get())

            source.nextResult =
                Result.success(
                    TvScheduleFetchOutcome(
                        schedule = FakeTvScheduleSource.sampleSchedule("Ffs0YHm2jks", 0),
                        etag = "\"recovery\"",
                    ),
                )
            advanceTimeBy(150)
            runCurrent()
            assertEquals(2, source.fetchCount.get())
            engine.dispose()
        }
}
