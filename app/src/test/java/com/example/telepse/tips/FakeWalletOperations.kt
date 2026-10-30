package com.example.telepse.tips

import com.example.telepse.data.model.CharacterSetCatalogRow
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

    override suspend fun fetchWallet(): Result<WalletStatusResponse> =
        Result.failure(UnsupportedOperationException("not used"))

    override suspend fun fetchTransactions(): Result<WalletTransactionsResponse> =
        Result.failure(UnsupportedOperationException("not used"))

    override suspend fun fetchCharacterCatalog(eventId: String): Result<EventCharacterCatalogResponse> {
        catalogFetchCount.incrementAndGet()
        lastCatalogEventId = eventId
        throwOnCatalog?.let { throw it }
        return catalogResult
    }

    override suspend fun debitWallet(
        request: WalletDebitRequest,
        idempotencyKey: String,
    ): Result<WalletDebitResponse> = Result.failure(UnsupportedOperationException("not used"))

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
