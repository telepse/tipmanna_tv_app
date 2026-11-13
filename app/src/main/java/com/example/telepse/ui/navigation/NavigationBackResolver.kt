package com.example.telepse.ui.navigation

/**
 * System Back destination for hub-rooted journeys (matches [MainActivity] BackHandler).
 */
object NavigationBackResolver {
    fun resolveBack(current: Screen): Screen =
        when (current) {
            is Screen.FundraisingMilestone -> Screen.CodeInput
            is Screen.TipperLogin -> Screen.JourneyHub
            is Screen.CodeInput -> Screen.JourneyHub
            is Screen.ExplorePlayback -> Screen.ExplorePlaceholder
            is Screen.BlockedHosts,
            is Screen.LiveTvFoundation,
            is Screen.ExplorePlaceholder,
            is Screen.TipsWallet,
            is Screen.YouTubePlaybackSpike,
            is Screen.CommunityRoomIdInput,
            is Screen.CommunityRoomDisplay,
            -> Screen.JourneyHub
            is Screen.JourneyHub -> Screen.JourneyHub
            is Screen.FullDisplay -> Screen.JourneyHub
        }
}
