package com.example.telepse.livetv

enum class TvScheduleRefreshTrigger {
    /** Backend `tv-schedule-updated` on `global-live-tv` */
    PUSHER_INVALIDATION,
    /** Pusher connection (re)established while LIVE TV is active */
    PUSHER_RECONNECTED,
}
