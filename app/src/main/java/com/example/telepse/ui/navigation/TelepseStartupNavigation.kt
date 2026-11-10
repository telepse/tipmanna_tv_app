package com.example.telepse.ui.navigation

/**
 * Cold start must always compose [Screen.JourneyHub] first so MainActivity can report
 * the first frame before heavy Screen Display (WebView / Pusher) work begins.
 */
fun initialAppScreen(): Screen = Screen.JourneyHub

fun screenDisplayRestoreTarget(
    savedEventId: String?,
    currentScreen: Screen,
): Screen? {
    if (savedEventId.isNullOrBlank()) return null
    if (currentScreen !is Screen.JourneyHub) return null
    return Screen.FundraisingMilestone(savedEventId)
}

/**
 * Screen Display auto-restore runs on [Lifecycle.Event.ON_START], which also fires on cold
 * process start. Skipping the first ON_START avoids scheduling Fundraising (WebView/Pusher)
 * during the activity startup window — a common source of startup ANR on TV emulators.
 */
fun shouldScheduleScreenDisplayRestoreOnStart(
    isFirstOnStartSinceProcessLaunched: Boolean,
    savedEventId: String?,
    currentScreen: Screen,
): Boolean {
    if (isFirstOnStartSinceProcessLaunched) return false
    if (savedEventId.isNullOrBlank()) return false
    return currentScreen is Screen.JourneyHub
}
