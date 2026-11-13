package com.example.telepse.premvp

import com.example.telepse.BuildConfig

/**
 * Pre-MVP consumption-only TV. Payment UX remains in codebase but must not be reachable from UI.
 */
object PreMvpFeatureFlags {
    const val enableTvPaymentUx: Boolean = false

    const val deferScreenDisplayAutoRestore: Boolean = true

    /** Public share links for video QR (flavor-specific via [BuildConfig.SHARE_CONTENT_ORIGIN]). */
    val shareContentOrigin: String = BuildConfig.SHARE_CONTENT_ORIGIN
}
