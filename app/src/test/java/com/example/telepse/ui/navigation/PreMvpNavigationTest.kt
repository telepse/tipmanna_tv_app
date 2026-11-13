package com.example.telepse.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PreMvpNavigationTest {
    @Test
    fun hubEntries_loggedOut_listsLiveExploreSignIn() {
        val labels = PreMvpNavigation.hubEntries(loggedIn = false).map { it.label }
        assertEquals(listOf("Watch LIVE TV", "Explore", "Sign in (tipper)"), labels)
    }

    @Test
    fun hubEntries_loggedIn_listsSignOut() {
        val labels = PreMvpNavigation.hubEntries(loggedIn = true).map { it.label }
        assertEquals(
            listOf("Watch LIVE TV", "Explore", "Blocked hosts", "Sign out"),
            labels,
        )
    }

    @Test
    fun hubEntries_excludeWalletAndScreenDisplay() {
        val all = PreMvpNavigation.hubEntries(loggedIn = false) + PreMvpNavigation.hubEntries(loggedIn = true)
        val text = all.joinToString { it.label }
        assertFalse(text.contains("Wallet", ignoreCase = true))
        assertFalse(text.contains("Screen Display", ignoreCase = true))
    }
}
