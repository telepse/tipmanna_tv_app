package com.example.telepse.data.repository

import com.example.telepse.data.model.EventCharacterCatalogResponse
import com.example.telepse.data.model.WalletDebitRequest
import com.example.telepse.data.model.WalletDebitResponse
import com.example.telepse.data.model.WalletStatusResponse
import com.example.telepse.data.model.WalletTransactionsResponse
import android.util.Log
import com.example.telepse.data.auth.SessionStore
import com.example.telepse.data.network.ApiConfig
import com.example.telepse.data.network.ApiErrorMessageMapper
import com.example.telepse.data.network.WalletHttpException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalletRepository @Inject constructor(
    private val httpClient: HttpClient,
    private val sessionStore: SessionStore,
) : WalletOperations {
    override suspend fun fetchWallet(): Result<WalletStatusResponse> =
        runCatching {
            val response = httpClient.get("${ApiConfig.BASE_URL}/api/v1/wallet")
            ensureSuccess(response, "fetchWallet")
            response.body()
        }

    override suspend fun fetchTransactions(): Result<WalletTransactionsResponse> =
        runCatching {
            val response = httpClient.get("${ApiConfig.BASE_URL}/api/v1/wallet/transactions")
            ensureSuccess(response, "fetchTransactions")
            response.body()
        }

    override suspend fun fetchCharacterCatalog(eventId: String): Result<EventCharacterCatalogResponse> =
        runCatching {
            val trimmed = eventId.trim()
            require(trimmed.isNotEmpty()) { "Event ID is required" }
            val response =
                httpClient.get("${ApiConfig.BASE_URL}/api/v1/events/$trimmed/character-catalog")
            ensureSuccess(response, "fetchCharacterCatalog")
            response.body()
        }

    override suspend fun debitWallet(
        request: WalletDebitRequest,
        idempotencyKey: String,
    ): Result<WalletDebitResponse> =
        try {
            val response =
                httpClient.post("${ApiConfig.BASE_URL}/api/v1/wallet/debit") {
                    contentType(ContentType.Application.Json)
                    header("Idempotency-Key", idempotencyKey)
                    setBody(request)
                }
            if (!response.status.isSuccess()) {
                throw walletHttpFailure(response, "debitWallet")
            }
            Result.success(response.body())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Result.failure(e)
        }

    private suspend fun ensureSuccess(response: HttpResponse, operation: String) {
        if (!response.status.isSuccess()) {
            throw walletHttpFailure(response, operation)
        }
    }

    private suspend fun walletHttpFailure(response: HttpResponse, operation: String): WalletHttpException {
        val status = response.status.value
        val authPresent = sessionStore.accessToken() != null
        val rawBody = response.bodyAsText()
        val apiMessage = ApiErrorMessageMapper.parseHttpFailure(status, rawBody).apiMessage
        Log.w(
            TAG,
            "wallet op=$operation http=$status authPresent=$authPresent",
        )
        return WalletHttpException(
            httpStatus = status,
            authPresent = authPresent,
            apiMessage = apiMessage,
        )
    }

    companion object {
        private const val TAG = "WalletRepository"
    }
}
