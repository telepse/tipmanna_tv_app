package com.example.telepse.data.realtime

/**
 * Public Pusher client configuration only — no secret in the APK.
 * Private channel auth: POST /api/v1/tv-app/pusher/auth
 */
object PusherConfig {
    const val API_KEY = "09e014941983d1503d77"
    const val CLUSTER = "us3"
    const val GLOBAL_LIVE_TV_CHANNEL = "global-live-tv"
    const val TIP_ANIMATION_EVENT = "tip-animation"
    /** Invalidation hint only — clients refresh via GET /api/v1/tv-app/schedule */
    const val TV_SCHEDULE_UPDATED_EVENT = "tv-schedule-updated"
}
