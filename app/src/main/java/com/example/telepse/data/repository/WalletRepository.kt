package com.example.telepse.data.repository

import com.example.telepse.data.model.ActiveTipConfigResponse
import com.example.telepse.data.model.CreateTipQuoteRequest
import com.example.telepse.data.model.CreateTipQuoteResponse
import com.example.telepse.data.model.CreateTvTipCheckoutSessionRequest
import com.example.telepse.data.model.CreateTvTipCheckoutSessionResponse
import com.example.telepse.data.model.EventCharacterCatalogResponse
import com.example.telepse.data.model.TvTipCheckoutSessionStatusResponse
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
import io.ktor.http.content.TextContent
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalletRepository @Inject constructor(
    private val httpClient: HttpClient,
    private val sessionStore: SessionStore,
) : WalletOperations {
    /** Debit body must emit defaulted paymentSurface/deviceCategory (global Json uses encodeDefaults=false). */
    private val debitJson =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    private val checkoutJson =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
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

    override suspend fun fetchActiveTipConfig(eventId: String): Result<ActiveTipConfigResponse> =
        runCatching {
            val trimmed = eventId.trim()
            require(trimmed.isNotEmpty()) { "Event ID is required" }
            val response =
                httpClient.get("${ApiConfig.BASE_URL}/api/v1/tip-config/$trimmed/active")
            ensureSuccess(response, "fetchActiveTipConfig")
            response.body()
        }

    override suspend fun createTipQuote(request: CreateTipQuoteRequest): Result<CreateTipQuoteResponse> =
        try {
            val response =
                httpClient.post("${ApiConfig.BASE_URL}/api/v1/tip-quote") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }
            if (!response.status.isSuccess()) {
                throw walletHttpFailure(response, "createTipQuote")
            }
            Result.success(response.body())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Result.failure(e)
        }

    override suspend fun debitWallet(
        request: WalletDebitRequest,
        idempotencyKey: String,
    ): Result<WalletDebitResponse> =
        try {
            val response =
                httpClient.post("${ApiConfig.BASE_URL}/api/v1/wallet/debit") {
                    header("Idempotency-Key", idempotencyKey)
                    // Encode with defaults so paymentSurface=kotlin_tv / deviceCategory=tv are sent.
                    setBody(
                        TextContent(
                            text = debitJson.encodeToString(WalletDebitRequest.serializer(), request),
                            contentType = ContentType.Application.Json,
                        ),
                    )
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

    override suspend fun createTvTipCheckoutSession(
        request: CreateTvTipCheckoutSessionRequest,
    ): Result<CreateTvTipCheckoutSessionResponse> =
        try {
            val idempotencyKey = request.idempotencyKey?.trim().orEmpty()
            val response =
                httpClient.post("${ApiConfig.BASE_URL}/api/v1/tv-tip-checkout/sessions") {
                    if (idempotencyKey.isNotEmpty()) {
                        header("Idempotency-Key", idempotencyKey)
                    }
                    setBody(
                        TextContent(
                            text =
                                checkoutJson.encodeToString(
                                    CreateTvTipCheckoutSessionRequest.serializer(),
                                    request,
                                ),
                            contentType = ContentType.Application.Json,
                        ),
                    )
                }
            if (!response.status.isSuccess()) {
                throw walletHttpFailure(response, "createTvTipCheckoutSession")
            }
            Result.success(response.body())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Result.failure(e)
        }

    override suspend fun fetchTvTipCheckoutSessionStatus(
        sessionId: String,
    ): Result<TvTipCheckoutSessionStatusResponse> =
        runCatching {
            val trimmed = sessionId.trim()
            require(trimmed.isNotEmpty()) { "Session id is required" }
            val response =
                httpClient.get("${ApiConfig.BASE_URL}/api/v1/tv-tip-checkout/sessions/$trimmed/status")
            ensureSuccess(response, "fetchTvTipCheckoutSessionStatus")
            response.body()
        }

    override suspend fun cancelTvTipCheckoutSession(sessionId: String): Result<Unit> =
        runCatching {
            val trimmed = sessionId.trim()
            require(trimmed.isNotEmpty()) { "Session id is required" }
            val response =
                httpClient.post("${ApiConfig.BASE_URL}/api/v1/tv-tip-checkout/sessions/$trimmed/cancel")
            ensureSuccess(response, "cancelTvTipCheckoutSession")
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
