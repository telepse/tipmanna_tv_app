package com.example.telepse.legal

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class TvLegalExternalLinkTest {
    @Test
    fun openHttpsPage_startsViewIntent() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val url = "https://tipmanna.tv/privacy-policy"
        val result = TvLegalExternalLink.openHttpsPage(context, url)
        assertEquals(TvLegalExternalLink.OpenResult.Opened, result)
        val shadow = Shadows.shadowOf(context as android.app.Application)
        val next = shadow.nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, next.action)
        assertEquals(url, next.data.toString())
    }

    @Test
    fun openHttpsPage_rejectsNonHttps() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val result = TvLegalExternalLink.openHttpsPage(context, "http://tipmanna.tv/privacy-policy")
        assertEquals(TvLegalExternalLink.OpenResult.BlockedInvalidUrl, result)
    }
}
