package com.example.telepse.share

import com.example.telepse.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TipmannaShareUrlTest {
    private val eventId = "11111111-1111-4111-8111-111111111111"
    private val channelId = "22222222-2222-4222-8222-222222222222"

    private val prodOrigin = "https://tipmanna.tv"
    private val stagingOrigin = "https://tipmanna-fj3ffgvfq-test01.vercel.app"

    @Test
    fun buildContentShareUrl_defaultOrigin_matchesBuildConfig() {
        val url = TipmannaShareUrl.buildContentShareUrl(eventId = eventId, channelId = channelId)
        val expected =
            "${BuildConfig.SHARE_CONTENT_ORIGIN.trimEnd('/')}/content/$eventId?v=$channelId"
        assertEquals(expected, url)
        assertTrue(url?.contains("/c/pay/") == false)
    }

    @Test
    fun buildContentShareUrl_usesProductionOriginAndPath() {
        val url =
            TipmannaShareUrl.buildContentShareUrl(
                eventId = eventId,
                channelId = channelId,
                origin = prodOrigin,
            )
        assertEquals(
            "$prodOrigin/content/$eventId?v=$channelId",
            url,
        )
    }

    @Test
    fun buildContentShareUrl_usesStagingOriginAndPath() {
        val url =
            TipmannaShareUrl.buildContentShareUrl(
                eventId = eventId,
                channelId = channelId,
                origin = stagingOrigin,
            )
        assertEquals(
            "$stagingOrigin/content/$eventId?v=$channelId",
            url,
        )
        assertEquals(TipmannaShareUrl.VIDEO_DEEP_LINK_PARAM, "v")
    }

    @Test
    fun buildContentShareUrl_rejectsInvalidIds() {
        assertNull(TipmannaShareUrl.buildContentShareUrl(eventId = "bad", channelId = channelId))
        assertNull(TipmannaShareUrl.buildContentShareUrl(eventId = eventId, channelId = ""))
    }

    @Test
    fun isLikelyUuid_acceptsValidV4() {
        assertTrue(TipmannaShareUrl.isLikelyUuid(eventId))
    }
}
