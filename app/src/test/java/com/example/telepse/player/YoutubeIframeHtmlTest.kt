package com.example.telepse.player

import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubeIframeHtmlTest {
    @Test
    fun build_usesNoCookieOriginAndIframeApi() {
        val html =
            YoutubeIframeHtml.build(
                initialVideoId = TipmannaSpikeVideoIds.SCHEDULE_LIVE_POSITION_SEED,
                autoplay = true,
                mute = true,
                startSeconds = 0f,
            )
        assertTrue(html.contains("www.youtube-nocookie.com"))
        assertTrue(html.contains("iframe_api"))
        assertTrue(html.contains(TipmannaSpikeVideoIds.SCHEDULE_LIVE_POSITION_SEED))
        assertTrue(html.contains("loadVideoById"))
        assertTrue(html.contains("TipmannaBridge"))
    }

    @Test
    fun embedOrigin_matchesLoadDataBaseUrl() {
        assertTrue(YoutubeIframeHtml.EMBED_ORIGIN.startsWith("https://"))
    }
}
