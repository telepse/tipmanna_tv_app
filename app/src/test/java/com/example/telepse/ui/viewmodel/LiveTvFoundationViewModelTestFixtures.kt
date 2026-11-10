package com.example.telepse.ui.viewmodel

import android.content.Context
import com.example.telepse.analytics.TvAnalyticsClient
import com.example.telepse.data.auth.SessionExpiryHandler
import com.example.telepse.data.auth.SessionStore
import com.example.telepse.data.realtime.PusherService
import com.example.telepse.data.repository.AnimationContractRepository
import com.example.telepse.data.repository.TipperSession
import com.example.telepse.livetv.FakeTvScheduleRefreshSignals
import com.example.telepse.livetv.FakeTvScheduleSource
import com.example.telepse.tips.FakeWalletOperations
import com.example.telepse.tips.MemoryWalletDebitPendingPersistence
import com.example.telepse.tips.WalletDebitSubmitSession
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk

fun liveTvFoundationViewModelForTests(
    context: Context,
    tipperSession: TipperSession,
): LiveTvFoundationViewModel {
    val http = HttpClient(MockEngine { respondOk() })
    val sessionStore = SessionStore.createPlainForUnitTests(context)
    val sessionExpiryHandler = SessionExpiryHandler(tipperSession)
    return LiveTvFoundationViewModel(
        scheduleSource = FakeTvScheduleSource(),
        refreshSignals = FakeTvScheduleRefreshSignals(),
        sessionExpiryHandler = sessionExpiryHandler,
        analytics = TvAnalyticsClient(http),
        walletOperations = FakeWalletOperations(),
        debitSubmitSession = WalletDebitSubmitSession.createForTests(MemoryWalletDebitPendingPersistence()),
        sessionStore = sessionStore,
        pusherService = PusherService(http, sessionStore, sessionExpiryHandler),
        animationContractRepository = AnimationContractRepository(http),
    )
}
