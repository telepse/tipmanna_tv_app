package com.example.telepse.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YoutubeVideoIdUtilTest {

    @Test
    fun fromUrl_watchQuery() {
        assertEquals(
            "Ffs0YHm2jks",
            YoutubeVideoIdUtil.fromUrl("https://youtube.com/watch?v=Ffs0YHm2jks"),
        )
    }

    @Test
    fun fromUrl_shortLink() {
        assertEquals(
            "P4kFofLwPLY",
            YoutubeVideoIdUtil.fromUrl("https://youtu.be/P4kFofLwPLY"),
        )
    }

    @Test
    fun fromUrl_invalid() {
        assertNull(YoutubeVideoIdUtil.fromUrl("https://example.com/not-youtube"))
    }
}
