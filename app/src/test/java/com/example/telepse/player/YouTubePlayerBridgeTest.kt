package com.example.telepse.player

import org.junit.Assert.assertEquals
import org.junit.Test

class YouTubePlayerBridgeTest {

    @Test
    fun javascriptInterfaceMethods_invokeConstructorCallbacks() {
        var iframeReady = 0
        var playerReady = 0
        var lastState = -1
        var lastError = -1
        var lastVideoId: String? = null

        val bridge =
            YouTubePlayerBridge(
                onIframeApiReadyCallback = { iframeReady++ },
                onPlayerReadyCallback = { playerReady++ },
                onStateChangeCallback = { lastState = it },
                onErrorCallback = { lastError = it },
                onVideoLoadedCallback = { lastVideoId = it },
            )

        bridge.onIframeApiReady()
        bridge.onPlayerReady()
        bridge.onStateChange(1)
        bridge.onError(101)
        bridge.onVideoLoaded("Ffs0YHm2jks")

        assertEquals(1, iframeReady)
        assertEquals(1, playerReady)
        assertEquals(1, lastState)
        assertEquals(101, lastError)
        assertEquals("Ffs0YHm2jks", lastVideoId)
    }
}
