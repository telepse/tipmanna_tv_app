package com.example.telepse.data.repository

import com.example.telepse.data.model.EventCharacterCatalogResponse
import com.example.telepse.data.model.WalletDebitRequest
import com.example.telepse.data.model.WalletDebitResponse
import com.example.telepse.data.model.WalletStatusResponse
import com.example.telepse.data.model.WalletTransactionsResponse

/** Wallet API surface used by tips UI (enables JVM fakes without Robolectric). */
interface WalletOperations {
    suspend fun fetchWallet(): Result<WalletStatusResponse>

    suspend fun fetchTransactions(): Result<WalletTransactionsResponse>

    suspend fun fetchCharacterCatalog(eventId: String): Result<EventCharacterCatalogResponse>

    suspend fun debitWallet(
        request: WalletDebitRequest,
        idempotencyKey: String,
    ): Result<WalletDebitResponse>
}
