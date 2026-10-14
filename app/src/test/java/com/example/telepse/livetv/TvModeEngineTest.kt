package com.example.telepse.livetv

import com.example.telepse.data.model.TvScheduleFetchOutcome
import com.example.telepse.data.model.TvScheduleResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TvModeEngineTest {

    @Test
    fun start_loadsScheduleAndPlayerOnce() = runTest {
        val source = FakeTvScheduleSource()
        val player = FakeYouTubePlayerAdapter()
        val engine = TvModeEngine(source, player, this, pollIntervalMs = null)

        engine.start()
        advanceUntilIdle()

        assertEquals(TvModeEnginePhase.Ready, engine.state.value.phase)
        assertEquals(1, player.loadCalls.size)
        assertEquals("Ffs0YHm2jks", player.loadCalls.first().first)
        engine.dispose()
    }

    @Test
    fun refreshWithSameTarget_doesNotReloadPlayer() = runTest {
        val source = FakeTvScheduleSource()
        val player = FakeYouTubePlayerAdapter()
        val engine = TvModeEngine(source, player, this, pollIntervalMs = null)
        engine.start()
        advanceUntilIdle()
        engine.refreshSchedule()
        advanceUntilIdle()

        assertEquals(2, source.fetchCount.get())
        assertEquals(1, player.loadCalls.size)
        engine.dispose()
    }

    @Test
    fun refreshWithDifferentVideo_loadsAgain() = runTest {
        val source = FakeTvScheduleSource()
        val player = FakeYouTubePlayerAdapter()
        val engine = TvModeEngine(source, player, this, pollIntervalMs = null)
        engine.start()
        advanceUntilIdle()

        source.nextResult =
            Result.success(
                TvScheduleFetchOutcome(
                    schedule = FakeTvScheduleSource.sampleSchedule("P4kFofLwPLY", 0),
                    etag = "\"etag-2\"",
                ),
            )
        engine.refreshSchedule()
        advanceUntilIdle()

        assertEquals(2, player.loadCalls.size)
        assertEquals("P4kFofLwPLY", player.loadCalls.last().first)
        engine.dispose()
    }

    @Test
    fun sameVideoOffsetChange_seeksInsteadOfReload() = runTest {
        val source = FakeTvScheduleSource()
        val player = FakeYouTubePlayerAdapter()
        val engine = TvModeEngine(source, player, this, pollIntervalMs = null)
        engine.start()
        advanceUntilIdle()

        source.nextResult =
            Result.success(
                TvScheduleFetchOutcome(
                    schedule =
                        FakeTvScheduleSource.sampleSchedule(
                            "Ffs0YHm2jks",
                            0,
                            offsetSeconds = 99,
                        ),
                    etag = "\"etag-3\"",
                ),
            )
        engine.refreshSchedule()
        advanceUntilIdle()

        assertEquals(1, player.loadCalls.size)
        assertEquals(listOf(99f), player.seekCalls)
        engine.dispose()
    }

    @Test
    fun networkError_setsErrorPhase() = runTest {
        val source = FakeTvScheduleSource()
        source.nextResult = Result.failure(IllegalStateException("HTTP 503"))
        val player = FakeYouTubePlayerAdapter()
        val engine = TvModeEngine(source, player, this, pollIntervalMs = null)

        engine.start()
        advanceUntilIdle()

        assertEquals(TvModeEnginePhase.Error, engine.state.value.phase)
        assertTrue(player.loadCalls.isEmpty())
        engine.dispose()
    }

    @Test
    fun emptyNoSafeContent_setsEmptyPhase() = runTest {
        val source = FakeTvScheduleSource()
        source.nextResult =
            Result.success(
                TvScheduleFetchOutcome(
                    schedule =
                        TvScheduleResponse(
                            scheduleState = "NO_SAFE_CONTENT",
                            items = emptyList(),
                            message = "No TV-safe content",
                        ),
                ),
            )
        val player = FakeYouTubePlayerAdapter()
        val engine = TvModeEngine(source, player, this, pollIntervalMs = null)

        engine.start()
        advanceUntilIdle()

        assertEquals(TvModeEnginePhase.Empty, engine.state.value.phase)
        assertTrue(player.loadCalls.isEmpty())
        engine.dispose()
    }

    @Test
    fun notModified_doesNotReloadPlayer() = runTest {
        val source = FakeTvScheduleSource()
        val player = FakeYouTubePlayerAdapter()
        val engine = TvModeEngine(source, player, this, pollIntervalMs = null)
        engine.start()
        advanceUntilIdle()
        assertEquals(1, player.loadCalls.size)

        source.nextResult =
            Result.success(
                TvScheduleFetchOutcome(notModified = true, etag = "\"test-etag-1\""),
            )
        engine.refreshSchedule()
        advanceUntilIdle()
        assertEquals(2, source.fetchCount.get())
        assertEquals("\"test-etag-1\"", source.lastIfNoneMatch)
        assertEquals(1, player.loadCalls.size)
        engine.dispose()
    }

    @Test
    fun stop_ignoresStaleRefreshCompletion() = runTest {
        val source = FakeTvScheduleSource()
        val player = FakeYouTubePlayerAdapter()
        val engine = TvModeEngine(source, player, this, pollIntervalMs = null)
        engine.start()
        advanceUntilIdle()
        assertEquals(1, player.loadCalls.size)

        engine.stop()
        source.nextResult =
            Result.success(
                TvScheduleFetchOutcome(
                    schedule = FakeTvScheduleSource.sampleSchedule("P4kFofLwPLY", 0),
                    etag = "\"etag-2\"",
                ),
            )
        engine.refreshSchedule()
        advanceUntilIdle()

        assertEquals(1, player.loadCalls.size)
        engine.dispose()
    }
}
