package com.example.telepse.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WebViewYouTubePlayerAdapterSanitizeTest {
    @Test
    fun sanitizePageUrl_stripsQueryAndDataBodies() {
        assertEquals("empty", WebViewYouTubePlayerAdapter.sanitizePageUrl(null))
        assertEquals("data", WebViewYouTubePlayerAdapter.sanitizePageUrl("data:text/html,secret"))
        assertEquals(
            "www.youtube-nocookie.com/embed/x",
            WebViewYouTubePlayerAdapter.sanitizePageUrl(
                "https://www.youtube-nocookie.com/embed/x?token=secret&v=abc",
            ),
        )
    }

    @Test
    fun sanitizeVideoId_keepsElevenCharIdsOnly() {
        assertEquals("Ffs0YHm2jks", WebViewYouTubePlayerAdapter.sanitizeVideoId("Ffs0YHm2jks"))
        assertTrue(
            WebViewYouTubePlayerAdapter.sanitizeVideoId("https://youtube.com/watch?v=x")
                .startsWith("id_len="),
        )
    }
}
