package com.example.telepse.ui.navigation

import com.example.telepse.data.model.Event

sealed class Screen {
    data object CodeInput : Screen()
    data class FullDisplay(val event: Event?) : Screen()
    data class FundraisingMilestone(val eventId: String) : Screen()
}

