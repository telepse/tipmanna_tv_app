package com.example.telepse.ui.navigation

/**
 * Approved Android TV MVP product scope (PO 2026-11-10).
 * TV is a Tipmanna client only — tipping happens on [Screen.LiveTvFoundation];
 * wallet hub is balance/ledger visibility, not a second tipping surface.
 */
object TvMvpNavigation {
  data class HubEntry(
      val label: String,
      val requiresAuth: Boolean = false,
  )

  fun hubEntries(loggedIn: Boolean): List<HubEntry> =
      buildList {
          add(HubEntry(label = "Watch LIVE TV", requiresAuth = true))
          add(HubEntry(label = "Wallet balance & activity", requiresAuth = true))
          add(HubEntry(label = if (loggedIn) "Sign out" else "Sign in (tipper)"))
      }

  /** Deferred routes remain in codebase but are not offered from the MVP hub. */
  const val deferScreenDisplayAutoRestore: Boolean = true

  const val walletHubVisibilityOnly: Boolean = true
}
