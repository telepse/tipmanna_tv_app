package com.example.telepse.data.realtime

import com.example.telepse.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Flavor-specific public Pusher identity must match [BuildConfig] (staging ↔ staging backend).
 */
class PusherConfigFlavorTest {

    @Test
    fun pusherConfig_matchesBuildConfigPublicCredentials() {
        assertEquals(BuildConfig.PUSHER_PUBLIC_KEY, PusherConfig.API_KEY)
        assertEquals(BuildConfig.PUSHER_CLUSTER, PusherConfig.CLUSTER)
    }

    @Test
    fun pusherConfig_alignsWithApiEnvironment() {
        val stagingApi = BuildConfig.API_BASE_URL.contains("tipmanna-staging")
        if (stagingApi) {
            assertEquals("eu", PusherConfig.CLUSTER)
            assertEquals(
                "staging Pusher key must match staging tv-app bootstrap",
                BuildConfig.PUSHER_PUBLIC_KEY,
                PusherConfig.API_KEY,
            )
        } else {
            assertEquals("us3", PusherConfig.CLUSTER)
            assertTrue(PusherConfig.API_KEY.isNotBlank())
        }
    }
}
