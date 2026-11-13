package com.example.telepse.data.realtime

import com.example.telepse.BuildConfig

/**
 * Public Pusher client configuration only — no secret in the APK.
 * Per-environment key/cluster from [BuildConfig]; venue bootstrap may override via [PusherService].
 * Private channel auth: POST /api/v1/tv-app/pusher/auth
 */
object PusherConfig {
    val API_KEY: String = BuildConfig.PUSHER_PUBLIC_KEY
    val CLUSTER: String = BuildConfig.PUSHER_CLUSTER
    const val GLOBAL_LIVE_TV_CHANNEL = "global-live-tv"
    const val TIP_ANIMATION_EVENT = "tip-animation"
    /** Invalidation hint only — clients refresh via GET /api/v1/tv-app/schedule */
    const val TV_SCHEDULE_UPDATED_EVENT = "tv-schedule-updated"
}
