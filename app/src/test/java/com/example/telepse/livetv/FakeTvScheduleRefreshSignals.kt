package com.example.telepse.livetv

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.atomic.AtomicInteger

class FakeTvScheduleRefreshSignals : TvScheduleRefreshSignals {
    private val _triggers =
        MutableSharedFlow<TvScheduleRefreshTrigger>(extraBufferCapacity = 16)
    override val triggers = _triggers.asSharedFlow()

    val startCount = AtomicInteger(0)
    val stopCount = AtomicInteger(0)

    override suspend fun startListening(scope: CoroutineScope) {
        startCount.incrementAndGet()
    }

    override suspend fun stopListening() {
        stopCount.incrementAndGet()
    }

    suspend fun emit(trigger: TvScheduleRefreshTrigger) {
        _triggers.emit(trigger)
    }
}
