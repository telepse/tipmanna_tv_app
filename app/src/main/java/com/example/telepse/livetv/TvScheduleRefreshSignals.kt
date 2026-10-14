package com.example.telepse.livetv

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

/**
 * Hints that the authoritative schedule should be re-fetched from HTTP.
 * Pusher payloads are not schedule data — see [TvScheduleSource].
 */
interface TvScheduleRefreshSignals {
    val triggers: Flow<TvScheduleRefreshTrigger>

    suspend fun startListening(scope: CoroutineScope)

    suspend fun stopListening()
}
