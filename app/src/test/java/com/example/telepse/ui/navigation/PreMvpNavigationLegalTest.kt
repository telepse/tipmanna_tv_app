package com.example.telepse.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PreMvpNavigationLegalTest {
    @Test
    fun legalHubEntries_listsPrivacyTermsAndDeletion() {
        val labels = PreMvpNavigation.legalHubEntries().map { it.label }
        assertEquals(
            listOf("Privacy policy", "Terms of use", "Account deletion (web)"),
            labels,
        )
        PreMvpNavigation.legalHubEntries().forEach {
            assertNotNull(it.legalDocument)
        }
    }

    @Test
    fun allHubEntries_includeLegalWithoutWallet() {
        val labels = PreMvpNavigation.allHubEntries(loggedIn = false).map { it.label }
        val text = labels.joinToString()
        assertFalse(text.contains("Wallet", ignoreCase = true))
        assertTrue(text.contains("Privacy policy"))
    }
}
