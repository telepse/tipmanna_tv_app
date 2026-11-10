package com.example.telepse.data.repository

import com.example.telepse.data.model.ActiveTipConfigResponse
import com.example.telepse.data.model.CreateTipQuoteRequest
import com.example.telepse.data.model.CreateTipQuoteResponse
import com.example.telepse.data.model.EventCharacterCatalogResponse
import com.example.telepse.data.model.CreateTvTipCheckoutSessionRequest
import com.example.telepse.data.model.CreateTvTipCheckoutSessionResponse
import com.example.telepse.data.model.TvTipCheckoutSessionStatusResponse
import com.example.telepse.data.model.WalletDebitRequest
import com.example.telepse.data.model.WalletDebitResponse
import com.example.telepse.data.model.WalletStatusResponse
import com.example.telepse.data.model.WalletTransactionsResponse

/** Wallet API surface used by tips UI (enables JVM fakes without Robolectric). */
interface WalletOperations : PaymentAnimationRecoveryOperations {
    suspend fun fetchWallet(): Result<WalletStatusResponse>

    suspend fun fetchTransactions(): Result<WalletTransactionsResponse>

    suspend fun fetchCharacterCatalog(eventId: String): Result<EventCharacterCatalogResponse>

    suspend fun fetchActiveTipConfig(eventId: String): Result<ActiveTipConfigResponse>

    suspend fun createTipQuote(request: CreateTipQuoteRequest): Result<CreateTipQuoteResponse>

    suspend fun debitWallet(
        request: WalletDebitRequest,
        idempotencyKey: String,
    ): Result<WalletDebitResponse>

    suspend fun createTvTipCheckoutSession(
        request: CreateTvTipCheckoutSessionRequest,
    ): Result<CreateTvTipCheckoutSessionResponse>

    suspend fun fetchTvTipCheckoutSessionStatus(
        sessionId: String,
    ): Result<TvTipCheckoutSessionStatusResponse>

    suspend fun cancelTvTipCheckoutSession(sessionId: String): Result<Unit>
}
