package com.example.telepse.livetv

import com.example.telepse.data.model.TvScheduleFetchOutcome
import com.example.telepse.data.model.TvScheduleResponse
import com.example.telepse.data.network.ScheduleHttpException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import com.example.telepse.player.YouTubePlayerState
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TvModeEngineTest {

    @Test
    fun start_loadsScheduleAndPlayerOnce() = runTest {
        val source = FakeTvScheduleSource()
        val player = FakeYouTubePlayerAdapter()
        val engine = TvModeEngine(source, player, this, pollIntervalMs = null, errorRecoveryDelayMs = null)

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
        val engine = TvModeEngine(source, player, this, pollIntervalMs = null, errorRecoveryDelayMs = null)
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
        val engine = TvModeEngine(source, player, this, pollIntervalMs = null, errorRecoveryDelayMs = null)
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
        val engine = TvModeEngine(source, player, this, pollIntervalMs = null, errorRecoveryDelayMs = null)
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
    fun authenticated401_invokesSessionHandler_andSkipsRecoveryRetry() =
        runTest {
            val source = FakeTvScheduleSource()
            source.nextResult =
                Result.failure(ScheduleHttpException(httpStatus = 401, authPresent = true))
            val player = FakeYouTubePlayerAdapter()
            var handlerCalls = 0
            val engine =
                TvModeEngine(
                    source,
                    player,
                    this,
                    pollIntervalMs = null,
                    errorRecoveryDelayMs = 5_000L,
                    onAuthenticatedSessionFailure = {
                        handlerCalls++
                        true
                    },
                )
            engine.start()
            advanceUntilIdle()
            assertEquals(TvModeEnginePhase.Error, engine.state.value.phase)
            assertTrue(engine.state.value.errorMessage.orEmpty().contains("Session expired"))
            assertTrue(engine.state.value.scheduleBackgroundRefreshSuspended)
            assertEquals(1, handlerCalls)
            assertEquals(1, source.fetchCount.get())
            advanceTimeBy(6_000L)
            runCurrent()
            assertEquals(1, source.fetchCount.get())
            engine.dispose()
        }

    @Test
    fun authenticated401_stopsPeriodicPolling() =
        runTest {
            val source = FakeTvScheduleSource()
            source.nextResult =
                Result.failure(ScheduleHttpException(httpStatus = 401, authPresent = true))
            val player = FakeYouTubePlayerAdapter()
            val engine =
                TvModeEngine(
                    source,
                    player,
                    this,
                    pollIntervalMs = 1_000L,
                    errorRecoveryDelayMs = 5_000L,
                    onAuthenticatedSessionFailure = { true },
                )
            engine.start()
            advanceUntilIdle()
            advanceTimeBy(3_000L)
            runCurrent()
            assertEquals(1, source.fetchCount.get())
            engine.dispose()
        }

    @Test
    fun userRefresh_afterSessionHold_resumesFetching() =
        runTest {
            val source = FakeTvScheduleSource()
            source.nextResult =
                Result.failure(ScheduleHttpException(httpStatus = 401, authPresent = true))
            val player = FakeYouTubePlayerAdapter()
            val engine =
                TvModeEngine(
                    source,
                    player,
                    this,
                    pollIntervalMs = null,
                    errorRecoveryDelayMs = null,
                    onAuthenticatedSessionFailure = { true },
                )
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
            assertEquals(TvModeEnginePhase.Ready, engine.state.value.phase)
            assertFalse(engine.state.value.scheduleBackgroundRefreshSuspended)
            assertEquals(2, source.fetchCount.get())
            engine.dispose()
        }

    @Test
    fun networkError_setsErrorPhase() = runTest {
        val source = FakeTvScheduleSource()
        source.nextResult = Result.failure(IllegalStateException("HTTP 503"))
        val player = FakeYouTubePlayerAdapter()
        val engine = TvModeEngine(source, player, this, pollIntervalMs = null, errorRecoveryDelayMs = null)

        engine.start()
        advanceUntilIdle()

        assertEquals(TvModeEnginePhase.Error, engine.state.value.phase)
        assertTrue(player.loadCalls.isEmpty())
        engine.stop()
        advanceUntilIdle()
        engine.dispose()
    }

    @Test
    fun scheduleFetchCancellation_doesNotSurfaceAsScheduleError() = runTest {
        val source = FakeTvScheduleSource()
        source.throwOnFetch =
            CancellationException("StandaloneCoroutine was cancelled")
        val player = FakeYouTubePlayerAdapter()
        val engine = TvModeEngine(source, player, this, pollIntervalMs = null, errorRecoveryDelayMs = null)

        engine.start()
        advanceUntilIdle()

        assertNotEquals(TvModeEnginePhase.Error, engine.state.value.phase)
        assertFalse(
            engine.state.value.errorMessage.orEmpty().contains("Temporary error"),
        )
        assertTrue(player.loadCalls.isEmpty())
        engine.stop()
        advanceUntilIdle()
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
        val engine = TvModeEngine(source, player, this, pollIntervalMs = null, errorRecoveryDelayMs = null)

        engine.start()
        advanceUntilIdle()

        assertEquals(TvModeEnginePhase.Empty, engine.state.value.phase)
        assertTrue(player.loadCalls.isEmpty())
        engine.dispose()
    }

    @Test
    fun notModified_clearsLoadingWhenScheduleAlreadyCached() = runTest {
        val source = FakeTvScheduleSource()
        val player = FakeYouTubePlayerAdapter()
        val engine = TvModeEngine(source, player, this, pollIntervalMs = null, errorRecoveryDelayMs = null)
        engine.start()
        advanceUntilIdle()
        assertEquals(TvModeEnginePhase.Ready, engine.state.value.phase)

        source.nextResult =
            Result.success(
                TvScheduleFetchOutcome(notModified = true, etag = "\"test-etag-1\""),
            )
        engine.refreshSchedule(showLoading = true)
        advanceUntilIdle()
        assertEquals(TvModeEnginePhase.Ready, engine.state.value.phase)
        engine.dispose()
    }

    @Test
    fun notModified_doesNotReloadPlayer() = runTest {
        val source = FakeTvScheduleSource()
        val player = FakeYouTubePlayerAdapter()
        val engine = TvModeEngine(source, player, this, pollIntervalMs = null, errorRecoveryDelayMs = null)
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
        val engine = TvModeEngine(source, player, this, pollIntervalMs = null, errorRecoveryDelayMs = null)
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

    @Test
    fun playerEnded_triggersDebouncedRefresh() = runTest {
        val source = FakeTvScheduleSource()
        val player = FakeYouTubePlayerAdapter()
        val engine =
            TvModeEngine(
                source,
                player,
                this,
                debounceMs = 100,
                pollIntervalMs = null,
            )
        engine.start()
        advanceUntilIdle()
        assertEquals(1, source.fetchCount.get())

        player.emitState(YouTubePlayerState.ENDED)
        advanceTimeBy(150)
        advanceUntilIdle()
        assertEquals(2, source.fetchCount.get())
        engine.dispose()
    }

    @Test
    fun playerError_triggersDebouncedRefresh() = runTest {
        val source = FakeTvScheduleSource()
        val player = FakeYouTubePlayerAdapter()
        val engine =
            TvModeEngine(
                source,
                player,
                this,
                debounceMs = 100,
                pollIntervalMs = null,
            )
        engine.start()
        advanceUntilIdle()

        player.emitError()
        advanceTimeBy(150)
        advanceUntilIdle()
        assertEquals(2, source.fetchCount.get())
        engine.dispose()
    }

    @Test
    fun scheduleFetchError_schedulesRecoveryRetry() = runTest {
        val source = FakeTvScheduleSource()
        source.nextResult = Result.failure(IllegalStateException("HTTP 503"))
        val player = FakeYouTubePlayerAdapter()
        val engine =
            TvModeEngine(
                source,
                player,
                this,
                pollIntervalMs = null,
                errorRecoveryDelayMs = 100L,
            )
        engine.start()
        runCurrent()
        assertEquals(TvModeEnginePhase.Error, engine.state.value.phase)
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
        assertEquals(TvModeEnginePhase.Ready, engine.state.value.phase)
        engine.stop()
        advanceUntilIdle()
        engine.dispose()
    }

    @Test
    fun onWebViewReleased_clearsLoadedIdentityAndAllowsReload() = runTest {
        val source = FakeTvScheduleSource()
        val player = FakeYouTubePlayerAdapter()
        val engine = TvModeEngine(source, player, this, pollIntervalMs = null, errorRecoveryDelayMs = null)
        engine.start()
        advanceUntilIdle()
        assertEquals(1, player.loadCalls.size)
        assertEquals("Ffs0YHm2jks", engine.state.value.loadedVideoId)

        engine.onWebViewReleased()
        assertEquals(null, engine.state.value.loadedVideoId)
        assertFalse(engine.state.value.playerReady)
        assertEquals(1, player.destroyCount)

        engine.syncPlaybackFromTarget(force = false)
        assertEquals(2, player.loadCalls.size)
        engine.dispose()
    }

    @Test
    fun singleSurface_loadDeliversPlayerReadyAndPlay() = runTest {
        val source = FakeTvScheduleSource()
        val player = FakeYouTubePlayerAdapter()
        val engine = TvModeEngine(source, player, this, pollIntervalMs = null, errorRecoveryDelayMs = null)
        engine.start()
        advanceUntilIdle()

        assertEquals(TvModeEnginePhase.Ready, engine.state.value.phase)
        assertTrue(engine.state.value.playerReady)
        assertEquals(1, player.loadCalls.size)
        assertTrue(player.playCalls.isNotEmpty())
        assertEquals(0, player.destroyCount)

        player.emitState(YouTubePlayerState.PLAYING)
        advanceUntilIdle()
        assertEquals(YouTubePlayerState.PLAYING, engine.state.value.playerState)
        engine.dispose()
    }

    @Test
    fun repeatedSyncWithoutRelease_doesNotDestroyOrReloadSameTarget() = runTest {
        val source = FakeTvScheduleSource()
        val player = FakeYouTubePlayerAdapter()
        val engine = TvModeEngine(source, player, this, pollIntervalMs = null, errorRecoveryDelayMs = null)
        engine.start()
        advanceUntilIdle()
        assertEquals(1, player.loadCalls.size)

        repeat(5) {
            engine.syncPlaybackFromTarget(force = false)
            advanceUntilIdle()
        }

        assertEquals(1, player.loadCalls.size)
        assertEquals(0, player.destroyCount)
        assertTrue(engine.state.value.playerReady)
        engine.dispose()
    }

    @Test
    fun playerEnded_multiItemSchedule_advancesLocally() = runTest {
        val source = FakeTvScheduleSource()
        source.nextResult =
            Result.success(
                TvScheduleFetchOutcome(
                    schedule =
                        FakeTvScheduleSource.sampleScheduleWithItems(
                            videoIds = listOf("Ffs0YHm2jks", "P4kFofLwPLY"),
                            liveIndex = 0,
                        ),
                    etag = "\"multi\"",
                ),
            )
        val player = FakeYouTubePlayerAdapter()
        val engine =
            TvModeEngine(
                source,
                player,
                this,
                debounceMs = 100,
                pollIntervalMs = null,
                errorRecoveryDelayMs = null,
            )
        engine.start()
        advanceUntilIdle()
        assertEquals(1, player.loadCalls.size)

        player.emitState(YouTubePlayerState.ENDED)
        advanceUntilIdle()

        assertEquals(2, player.loadCalls.size)
        assertEquals("P4kFofLwPLY", player.loadCalls.last().first)
        engine.dispose()
    }

    @Test
    fun playScheduleIndex_loadsSelectedRow() = runTest {
        val source = FakeTvScheduleSource()
        source.nextResult =
            Result.success(
                TvScheduleFetchOutcome(
                    schedule =
                        FakeTvScheduleSource.sampleScheduleWithItems(
                            videoIds = listOf("Ffs0YHm2jks", "P4kFofLwPLY"),
                            liveIndex = 0,
                        ),
                    etag = "\"multi\"",
                ),
            )
        val player = FakeYouTubePlayerAdapter()
        val engine = TvModeEngine(source, player, this, pollIntervalMs = null, errorRecoveryDelayMs = null)
        engine.start()
        advanceUntilIdle()
        engine.playScheduleIndex(1)
        advanceUntilIdle()
        assertEquals("P4kFofLwPLY", player.loadCalls.last().first)
        engine.dispose()
    }

    @Test
    fun liveIndexChange_loadsNewVideo() = runTest {
        val source = FakeTvScheduleSource()
        val player = FakeYouTubePlayerAdapter()
        val engine = TvModeEngine(source, player, this, pollIntervalMs = null, errorRecoveryDelayMs = null)
        engine.start()
        advanceUntilIdle()

        source.nextResult =
            Result.success(
                TvScheduleFetchOutcome(
                    schedule =
                        FakeTvScheduleSource.sampleScheduleWithItems(
                            videoIds = listOf("Ffs0YHm2jks", "P4kFofLwPLY"),
                            liveIndex = 1,
                            offsetSeconds = 5,
                        ),
                    etag = "\"etag-batch\"",
                ),
            )
        engine.refreshSchedule()
        advanceUntilIdle()

        assertEquals(2, player.loadCalls.size)
        assertEquals("P4kFofLwPLY", player.loadCalls.last().first)
        assertEquals(5f, player.loadCalls.last().second)
        engine.dispose()
    }

    @Test
    fun releaseThenReenter_withNotModified_bootstrapsPlayerAgain() = runTest {
        val source = FakeTvScheduleSource()
        val player = FakeYouTubePlayerAdapter()
        val engine = TvModeEngine(source, player, this, pollIntervalMs = null, errorRecoveryDelayMs = null)
        engine.start()
        advanceUntilIdle()
        assertEquals(1, player.loadCalls.size)
        assertEquals("Ffs0YHm2jks", engine.state.value.loadedVideoId)
        assertTrue(engine.state.value.playerReady)

        // Leave LIVE: stop + WebView release (Compose onRelease).
        engine.stop()
        engine.onWebViewReleased()
        assertEquals(null, engine.state.value.loadedVideoId)
        assertFalse(engine.state.value.playerReady)
        assertFalse(engine.state.value.isActive)

        // Attach while inactive must not load (same guard as attachWebView → sync).
        engine.syncPlaybackFromTarget(force = true)
        assertEquals(1, player.loadCalls.size)

        // Re-enter: schedule unchanged → HTTP 304, but new surface needs bootstrap.
        source.nextResult =
            Result.success(
                TvScheduleFetchOutcome(notModified = true, etag = "\"test-etag-1\""),
            )
        engine.start()
        advanceUntilIdle()

        assertEquals(TvModeEnginePhase.Ready, engine.state.value.phase)
        assertEquals(2, player.loadCalls.size)
        assertEquals("Ffs0YHm2jks", engine.state.value.loadedVideoId)
        assertTrue(engine.state.value.playerReady)
        engine.dispose()
    }

    @Test
    fun releaseThenReenter_withNotModified_keepsReadyFalseUntilPlayerReady() = runTest {
        val source = FakeTvScheduleSource()
        val player = FakeYouTubePlayerAdapter()
        player.ready = false
        val engine = TvModeEngine(source, player, this, pollIntervalMs = null, errorRecoveryDelayMs = null)
        engine.start()
        advanceUntilIdle()
        assertEquals(1, player.loadCalls.size)
        assertFalse(engine.state.value.playerReady)

        engine.stop()
        engine.onWebViewReleased()

        source.nextResult =
            Result.success(
                TvScheduleFetchOutcome(notModified = true, etag = "\"test-etag-1\""),
            )
        engine.start()
        advanceUntilIdle()

        assertEquals(2, player.loadCalls.size)
        assertEquals("Ffs0YHm2jks", engine.state.value.loadedVideoId)
        assertFalse(engine.state.value.playerReady)

        player.emitPlayerReady()
        advanceUntilIdle()
        assertTrue(engine.state.value.playerReady)
        engine.dispose()
    }

    @Test
    fun syncWhileInactive_doesNotLoadEvenWithForce() = runTest {
        val source = FakeTvScheduleSource()
        val player = FakeYouTubePlayerAdapter()
        val engine = TvModeEngine(source, player, this, pollIntervalMs = null, errorRecoveryDelayMs = null)
        engine.start()
        advanceUntilIdle()
        assertEquals(1, player.loadCalls.size)

        engine.stop()
        engine.onWebViewReleased()
        engine.syncPlaybackFromTarget(force = true)
        assertEquals(1, player.loadCalls.size)
        assertEquals(null, engine.state.value.loadedVideoId)
        engine.dispose()
    }

    @Test
    fun repeatedReleaseReenter_withNotModified_bootstrapsEachCycle() = runTest {
        val source = FakeTvScheduleSource()
        val player = FakeYouTubePlayerAdapter()
        val engine = TvModeEngine(source, player, this, pollIntervalMs = null, errorRecoveryDelayMs = null)
        engine.start()
        advanceUntilIdle()
        assertEquals(1, player.loadCalls.size)

        repeat(3) { cycle ->
            engine.stop()
            engine.onWebViewReleased()
            assertEquals(null, engine.state.value.loadedVideoId)
            assertFalse(engine.state.value.playerReady)

            source.nextResult =
                Result.success(
                    TvScheduleFetchOutcome(notModified = true, etag = "\"test-etag-1\""),
                )
            engine.start()
            advanceUntilIdle()

            assertEquals(cycle + 2, player.loadCalls.size)
            assertEquals("Ffs0YHm2jks", engine.state.value.loadedVideoId)
            assertTrue(engine.state.value.playerReady)
            assertEquals(TvModeEnginePhase.Ready, engine.state.value.phase)
        }
        engine.dispose()
    }
}
