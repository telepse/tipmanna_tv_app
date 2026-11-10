package com.example.telepse.livetv

import com.example.telepse.data.realtime.PusherService
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class PusherGlobalScheduleRefreshSignals @Inject constructor(
    private val pusherService: PusherService,
) : TvScheduleRefreshSignals {

    private val mutex = Mutex()
    private var listening = false
    private var collectorJob: Job? = null

    private val _triggers =
        MutableSharedFlow<TvScheduleRefreshTrigger>(extraBufferCapacity = 8)
    override val triggers = _triggers.asSharedFlow()

    override suspend fun startListening(scope: CoroutineScope) {
        mutex.withLock {
            if (listening) return
            listening = true
            if (!pusherService.initialize()) {
                listening = false
                return
            }
            if (!pusherService.subscribeToGlobalLiveTv()) {
                listening = false
                return
            }
            collectorJob =
                scope.launch {
                    launch {
                        pusherService.observeTvScheduleUpdated().collect {
                            _triggers.emit(TvScheduleRefreshTrigger.PUSHER_INVALIDATION)
                        }
                    }
                    launch {
                        pusherService.observeConnectionEstablished().collect {
                            _triggers.emit(TvScheduleRefreshTrigger.PUSHER_RECONNECTED)
                        }
                    }
                }
        }
    }

    override suspend fun stopListening() {
        mutex.withLock {
            if (!listening) return
            listening = false
            collectorJob?.cancel()
            collectorJob = null
        }
    }
}
