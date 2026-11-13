package com.example.telepse.build

import com.example.telepse.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApplicationIdFlavorTest {
    @Test
    fun applicationId_usesApprovedTipmannaPackage() {
        assertTrue(
            BuildConfig.APPLICATION_ID == "tv.tipmanna.androidtv" ||
                BuildConfig.APPLICATION_ID == "tv.tipmanna.androidtv.staging",
        )
    }

    @Test
    fun applicationId_stagingFlavor_hasStagingSuffix_whenApplicable() {
        val api = BuildConfig.API_BASE_URL
        if (api.contains("staging")) {
            assertEquals("tv.tipmanna.androidtv.staging", BuildConfig.APPLICATION_ID)
        } else {
            assertEquals("tv.tipmanna.androidtv", BuildConfig.APPLICATION_ID)
        }
    }

    @Test
    fun videoShareQr_disabledOnProd_untilE2eVerified() {
        val staging = BuildConfig.API_BASE_URL.contains("tipmanna-staging")
        if (staging) {
            assertTrue(BuildConfig.ENABLE_VIDEO_SHARE_QR)
        } else {
            assertFalse(BuildConfig.ENABLE_VIDEO_SHARE_QR)
        }
    }
}
