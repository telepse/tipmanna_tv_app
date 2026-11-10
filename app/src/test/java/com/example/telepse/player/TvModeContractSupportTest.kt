package com.example.telepse.player

import org.junit.Assert.assertNotNull
import org.junit.Test
import java.lang.reflect.Method

/**
 * Documents that [YouTubePlayerAdapter] exposes operations required by future TVModeEngine
 * without implementing GLOBAL/LOCAL/Go Live.
 */
class TvModeContractSupportTest {
    @Test
    fun adapterDeclaresOrchestrationOperations() {
        val required =
            listOf(
                "load",
                "cue",
                "play",
                "pause",
                "seekTo",
                "mute",
                "unmute",
                "requestCurrentTime",
                "requestDuration",
                "state",
                "destroy",
            )
        for (name in required) {
            val method: Method? =
                YouTubePlayerAdapter::class.java.methods.firstOrNull { it.name == name }
            assertNotNull("Missing YouTubePlayerAdapter.$name", method)
        }
    }
}
