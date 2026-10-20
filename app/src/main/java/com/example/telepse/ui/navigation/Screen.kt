package com.example.telepse.ui.navigation

import com.example.telepse.data.model.Event

sealed class Screen {
    data object JourneyHub : Screen()
    data object CodeInput : Screen()
    data object YouTubePlaybackSpike : Screen()
    data class TipperLogin(val returnTo: Screen) : Screen()
    data object LiveTvFoundation : Screen()
    data object ExplorePlaceholder : Screen()

    data class ExplorePlayback(
        val videoId: String,
        val title: String,
    ) : Screen()
    data object TipsWallet : Screen()
    data class FullDisplay(val event: Event?) : Screen()
    data class FundraisingMilestone(val eventId: String) : Screen()
    data object CommunityRoomIdInput : Screen()
    data class CommunityRoomDisplay(val communityId: String) : Screen()
}
