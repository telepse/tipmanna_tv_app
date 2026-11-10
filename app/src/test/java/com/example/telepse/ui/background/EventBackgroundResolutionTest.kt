package com.example.telepse.ui.background

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EventBackgroundResolutionTest {

    @Test
    fun localBackground_mapsKnownPaths() {
        val media = resolveEventBackgroundMedia("/gaming.png", null)
        assertTrue(media is EventBackgroundMedia.LocalRaw)
    }

    @Test
    fun remoteHttpUrl_returnsRemoteImage() {
        val media =
            resolveEventBackgroundMedia(
                "https://cdn.example.com/bg.png",
                null,
            )
        assertTrue(media is EventBackgroundMedia.RemoteImage)
        assertEquals(
            "https://cdn.example.com/bg.png",
            (media as EventBackgroundMedia.RemoteImage).url,
        )
    }

    @Test
    fun unknownPath_withoutHttp_fallsBackToDefaultRaw() {
        val media = resolveEventBackgroundMedia("/unknown.png", null)
        assertTrue(media is EventBackgroundMedia.LocalRaw)
        assertNull(resolveRemoteBackgroundUrl("/unknown.png"))
    }

    @Test
    fun backgroundTypeTakesPriority() {
        val media = resolveEventBackgroundMedia("/unknown.png", "gaming")
        assertTrue(media is EventBackgroundMedia.LocalRaw)
    }

    @Test
    fun communityRoom_remoteUrl() {
        val media =
            resolveCommunityRoomBackgroundMedia(
                "https://cdn.example.com/room.png",
                "default",
            )
        assertTrue(media is EventBackgroundMedia.RemoteImage)
    }

    @Test
    fun communityRoom_typeMapsLocal() {
        val media = resolveCommunityRoomBackgroundMedia("", "gaming")
        assertTrue(media is EventBackgroundMedia.LocalRaw)
    }
}
