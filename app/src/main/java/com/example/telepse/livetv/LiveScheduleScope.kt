package com.example.telepse.livetv

/**
 * Authoritative LIVE TV schedule scope for UI labeling.
 * Global LIVE TV uses [Global] because the client calls `GET /api/v1/tv-app/schedule` only.
 * Community scope requires `GET /api/v1/tv-app/communities/{id}/schedule` (not wired on foundation screen).
 */
sealed class LiveScheduleScope {
    data object Global : LiveScheduleScope()

    data class Community(val communityId: String) : LiveScheduleScope()

    /** No authoritative scope field in the active API contract. */
    data object Unknown : LiveScheduleScope()

    fun badgeLabel(): String =
        when (this) {
            Global -> "GLOBAL LIVE"
            is Community -> "COMMUNITY"
            Unknown -> "LIVE TV"
        }

    fun subtitle(): String? =
        when (this) {
            is Community -> "Community schedule"
            Global -> "Tipmanna global broadcast"
            Unknown -> null
        }
}
