package com.example.telepse.legal

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

object TvLegalExternalLink {
    fun openHttpsPage(context: Context, url: String): OpenResult {
        if (!url.startsWith("https://", ignoreCase = true)) {
            return OpenResult.BlockedInvalidUrl
        }
        return try {
            val intent =
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            context.startActivity(intent)
            OpenResult.Opened
        } catch (_: ActivityNotFoundException) {
            OpenResult.NoBrowser
        }
    }

    sealed interface OpenResult {
        data object Opened : OpenResult

        data object NoBrowser : OpenResult

        data object BlockedInvalidUrl : OpenResult
    }
}
