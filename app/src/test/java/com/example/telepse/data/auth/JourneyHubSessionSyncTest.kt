package com.example.telepse.data.auth

import com.example.telepse.ui.viewmodel.JourneyHubUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Legacy flow check; see [com.example.telepse.ui.viewmodel.JourneyHubViewModelTest] for the real ViewModel. */
@OptIn(ExperimentalCoroutinesApi::class)
class JourneyHubSessionSyncTest {
    @Test
    fun snapshotFlow_updatesLoggedInStateWithoutManualRefresh() =
        runTest {
            val snapshots = MutableStateFlow(TipperSessionSnapshot(loggedIn = false, tipperEmail = null))
            var uiState = JourneyHubUiState()
            val job =
                launch {
                    snapshots.collect { snap ->
                        uiState =
                            JourneyHubUiState(
                                loggedIn = snap.loggedIn,
                                tipperEmail = snap.tipperEmail,
                            )
                    }
                }
            advanceUntilIdle()
            assertFalse(uiState.loggedIn)

            snapshots.value = TipperSessionSnapshot(loggedIn = true, tipperEmail = "tipper@example.com")
            advanceUntilIdle()
            assertTrue(uiState.loggedIn)
            assertEquals("tipper@example.com", uiState.tipperEmail)

            snapshots.value = TipperSessionSnapshot(loggedIn = false, tipperEmail = null)
            advanceUntilIdle()
            assertFalse(uiState.loggedIn)
            job.cancel()
        }
}
