package com.example.telepse.tips

import com.example.telepse.data.model.ActiveTipConfigResponse
import com.example.telepse.data.model.CharacterSetCatalogRow
import com.example.telepse.data.model.CreateTipQuoteRequest
import com.example.telepse.data.model.CreateTipQuoteResponse
import com.example.telepse.data.model.CreateTvTipCheckoutSessionRequest
import com.example.telepse.data.model.CreateTvTipCheckoutSessionResponse
import com.example.telepse.data.model.TvTipCheckoutSessionStatusResponse
import com.example.telepse.data.model.EventCharacterCatalogResponse
import com.example.telepse.data.model.TipCharacterCatalogItem
import com.example.telepse.data.model.WalletDebitRequest
import com.example.telepse.data.model.WalletDebitResponse
import com.example.telepse.data.model.WalletStatusResponse
import com.example.telepse.data.model.WalletTransactionsResponse
import com.example.telepse.data.repository.WalletOperations
import java.util.concurrent.atomic.AtomicInteger

class FakeWalletOperations : WalletOperations {
    var catalogResult: Result<EventCharacterCatalogResponse> =
        Result.success(sampleCatalog("evt-1"))
    var throwOnCatalog: Throwable? = null
    val catalogFetchCount = AtomicInteger(0)
    var lastCatalogEventId: String? = null

    var walletResult: Result<WalletStatusResponse> =
        Result.success(WalletStatusResponse(exists = true, status = "ACTIVE", balance = "50.00"))
    var tipConfigResult: Result<ActiveTipConfigResponse> =
        Result.success(ActiveTipConfigResponse(mode = "legacy"))
    var tipQuoteResult: Result<CreateTipQuoteResponse> =
        Result.success(CreateTipQuoteResponse(quoteId = "quote-1", totalMinor = 500, configVersion = 1))
    var debitResult: Result<WalletDebitResponse> =
        Result.success(WalletDebitResponse(transactionId = "tx-1", newBalance = "40.00"))
    var tvCheckoutCreateResult: Result<CreateTvTipCheckoutSessionResponse> =
        Result.success(
            CreateTvTipCheckoutSessionResponse(
                sessionId = "sess-1",
                checkoutUrl = "https://app.example/c/pay/token-abc",
                expiresAt = "2026-10-30T12:00:00Z",
            ),
        )
    var tvCheckoutStatusResults: ArrayDeque<Result<TvTipCheckoutSessionStatusResponse>> =
        ArrayDeque(
            listOf(
                Result.success(TvTipCheckoutSessionStatusResponse(sessionId = "sess-1", status = "pending")),
            ),
        )

    val debitCalls = mutableListOf<Pair<WalletDebitRequest, String>>()
    val tvCheckoutCreateCalls = mutableListOf<CreateTvTipCheckoutSessionRequest>()
    val tvCheckoutStatusCalls = mutableListOf<String>()
    val tipQuoteCalls = mutableListOf<CreateTipQuoteRequest>()
    val debitCallCount = AtomicInteger(0)

    override suspend fun fetchWallet(): Result<WalletStatusResponse> = walletResult

    override suspend fun fetchTransactions(): Result<WalletTransactionsResponse> =
        Result.failure(UnsupportedOperationException("not used"))

    override suspend fun fetchCharacterCatalog(eventId: String): Result<EventCharacterCatalogResponse> {
        catalogFetchCount.incrementAndGet()
        lastCatalogEventId = eventId
        throwOnCatalog?.let { throw it }
        return catalogResult
    }

    override suspend fun fetchActiveTipConfig(eventId: String): Result<ActiveTipConfigResponse> =
        tipConfigResult

    override suspend fun createTipQuote(request: CreateTipQuoteRequest): Result<CreateTipQuoteResponse> {
        tipQuoteCalls += request
        return tipQuoteResult
    }

    override suspend fun debitWallet(
        request: WalletDebitRequest,
        idempotencyKey: String,
    ): Result<WalletDebitResponse> {
        debitCallCount.incrementAndGet()
        debitCalls += request to idempotencyKey
        return debitResult
    }

    override suspend fun createTvTipCheckoutSession(
        request: CreateTvTipCheckoutSessionRequest,
    ): Result<CreateTvTipCheckoutSessionResponse> {
        tvCheckoutCreateCalls += request
        return tvCheckoutCreateResult
    }

    override suspend fun fetchTvTipCheckoutSessionStatus(
        sessionId: String,
    ): Result<TvTipCheckoutSessionStatusResponse> {
        tvCheckoutStatusCalls += sessionId
        return tvCheckoutStatusResults.removeFirstOrNull()
            ?: Result.success(TvTipCheckoutSessionStatusResponse(sessionId = sessionId, status = "pending"))
    }

    override suspend fun cancelTvTipCheckoutSession(sessionId: String): Result<Unit> = Result.success(Unit)

    companion object {
        fun sampleCatalog(eventId: String): EventCharacterCatalogResponse =
            EventCharacterCatalogResponse(
                eventId = eventId,
                characterSets =
                    listOf(
                        CharacterSetCatalogRow(
                            id = "set-premium",
                            name = "Premium",
                            characters =
                                listOf(
                                    TipCharacterCatalogItem("p1", "Premium One", 5.0, null),
                                ),
                        ),
                        CharacterSetCatalogRow(
                            id = "set-elite",
                            name = "Elite Tips",
                            characters =
                                listOf(
                                    TipCharacterCatalogItem("e1", "Elite One", 10.0, "https://cdn/e1.png"),
                                    TipCharacterCatalogItem("c1", "Add Comment", 1.0, null),
                                ),
                        ),
                        CharacterSetCatalogRow(
                            id = "set-general",
                            name = "General",
                            characters =
                                listOf(
                                    TipCharacterCatalogItem("g1", "General One", 2.0, null),
                                ),
                        ),
                    ),
            )

        fun emptyCatalog(eventId: String): EventCharacterCatalogResponse =
            EventCharacterCatalogResponse(eventId = eventId, characterSets = emptyList())
    }
}
