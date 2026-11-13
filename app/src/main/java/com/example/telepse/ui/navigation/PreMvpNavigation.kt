package com.example.telepse.ui.navigation

import com.example.telepse.legal.LegalDocument

/**
 * Hub and lifecycle flags for the consumption-only Pre-MVP (not the payment-enabled TV MVP).
 */
object PreMvpNavigation {
    data class HubEntry(
        val label: String,
        val requiresAuth: Boolean = false,
        val legalDocument: LegalDocument? = null,
    )

    fun hubEntries(loggedIn: Boolean): List<HubEntry> =
        buildList {
            add(HubEntry(label = "Watch LIVE TV", requiresAuth = true))
            add(HubEntry(label = "Explore", requiresAuth = true))
            if (loggedIn) {
                add(HubEntry(label = "Blocked hosts", requiresAuth = true))
            }
            add(HubEntry(label = if (loggedIn) "Sign out" else "Sign in (tipper)"))
        }

    fun legalHubEntries(): List<HubEntry> =
        LegalDocument.entries.map { doc ->
            HubEntry(label = doc.hubLabel, legalDocument = doc)
        }

    fun allHubEntries(loggedIn: Boolean): List<HubEntry> = hubEntries(loggedIn) + legalHubEntries()
}
