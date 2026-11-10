package com.example.telepse.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TelepseStartupNavigationTest {

    @Test
    fun initialAppScreen_isAlwaysJourneyHub() {
        assertEquals(Screen.JourneyHub, initialAppScreen())
    }

    @Test
    fun screenDisplayRestoreTarget_whenSavedIdAndOnHub_returnsFundraising() {
        assertEquals(
            Screen.FundraisingMilestone("evt-42"),
            screenDisplayRestoreTarget("evt-42", Screen.JourneyHub),
        )
    }

    @Test
    fun screenDisplayRestoreTarget_whenNoSavedId_returnsNull() {
        assertNull(screenDisplayRestoreTarget(null, Screen.JourneyHub))
        assertNull(screenDisplayRestoreTarget("", Screen.JourneyHub))
    }

    @Test
    fun screenDisplayRestoreTarget_whenNotOnHub_returnsNull() {
        assertNull(
            screenDisplayRestoreTarget(
                "evt-42",
                Screen.LiveTvFoundation,
            ),
        )
    }

    @Test
    fun shouldScheduleRestore_skipsFirstOnStartOnColdProcess() {
        assertFalse(
            shouldScheduleScreenDisplayRestoreOnStart(
                isFirstOnStartSinceProcessLaunched = true,
                savedEventId = "evt-42",
                currentScreen = Screen.JourneyHub,
            ),
        )
    }

    @Test
    fun shouldScheduleRestore_whenResumingFromBackgroundOnHub() {
        assertTrue(
            shouldScheduleScreenDisplayRestoreOnStart(
                isFirstOnStartSinceProcessLaunched = false,
                savedEventId = "evt-42",
                currentScreen = Screen.JourneyHub,
            ),
        )
    }
}
