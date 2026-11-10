package com.example.telepse.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TvMvpNavigationTest {
    @Test
    fun hubEntries_mvpScope_excludesDeferredSurfaces() {
        val loggedOut = TvMvpNavigation.hubEntries(loggedIn = false)
        val labels = loggedOut.map { it.label }
        assertTrue(labels.contains("Watch LIVE TV"))
        assertTrue(labels.contains("Wallet balance & activity"))
        assertTrue(labels.contains("Sign in (tipper)"))
        assertTrue(labels.none { it.contains("Explore", ignoreCase = true) })
        assertTrue(labels.none { it.contains("Community", ignoreCase = true) })
        assertTrue(labels.none { it.contains("screen display", ignoreCase = true) })
        assertTrue(labels.none { it.contains("YouTube", ignoreCase = true) })
    }

    @Test
    fun hubEntries_whenLoggedIn_showsSignOut() {
        val loggedIn = TvMvpNavigation.hubEntries(loggedIn = true)
        assertEquals("Sign out", loggedIn.last().label)
    }
}
