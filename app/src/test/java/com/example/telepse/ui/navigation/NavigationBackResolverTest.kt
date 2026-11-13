package com.example.telepse.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationBackResolverTest {
    @Test
    fun explorePlayback_returnsToExploreList() {
        assertEquals(
            Screen.ExplorePlaceholder,
            NavigationBackResolver.resolveBack(
                Screen.ExplorePlayback(
                    videoId = "abc",
                    title = "T",
                    eventId = "11111111-1111-4111-8111-111111111111",
                    channelId = "22222222-2222-4222-8222-222222222222",
                ),
            ),
        )
    }

    @Test
    fun liveTv_returnsToHub() {
        assertEquals(Screen.JourneyHub, NavigationBackResolver.resolveBack(Screen.LiveTvFoundation))
    }

    @Test
    fun tipperLogin_returnsToHub() {
        assertEquals(
            Screen.JourneyHub,
            NavigationBackResolver.resolveBack(Screen.TipperLogin(returnTo = Screen.LiveTvFoundation)),
        )
    }

    @Test
    fun fundraising_returnsToCodeInput() {
        assertEquals(
            Screen.CodeInput,
            NavigationBackResolver.resolveBack(Screen.FundraisingMilestone("event-1")),
        )
    }
}
