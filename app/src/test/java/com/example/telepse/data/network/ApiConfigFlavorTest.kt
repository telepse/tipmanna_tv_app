package com.example.telepse.data.network

import com.example.telepse.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ensures the unit-test variant exposes a known API_BASE_URL and ApiConfig reads it.
 * Staging and prod constants are asserted when that flavor is under test.
 */
class ApiConfigFlavorTest {

    @Test
    fun apiConfig_matchesBuildConfigApiBaseUrl() {
        assertEquals(BuildConfig.API_BASE_URL, ApiConfig.BASE_URL)
        assertTrue(ApiConfig.BASE_URL.startsWith("https://"))
        assertTrue(
            ApiConfig.BASE_URL == "https://tipmanna-staging.onrender.com" ||
                ApiConfig.BASE_URL == "https://tipmanna-live.onrender.com",
        )
    }
}
