package com.example.telepse.data.repository

import com.example.telepse.data.auth.SessionStore
import com.example.telepse.data.model.ExploreFeedBatchRequest
import com.example.telepse.data.model.ExploreFeedBatchResponse
import com.example.telepse.data.network.ApiConfig
import com.example.telepse.data.network.AuthenticatedHttpResponseSupport
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExploreFeedRepository @Inject constructor(
    private val httpClient: HttpClient,
    private val sessionStore: SessionStore,
) {
    /**
     * Same contract as web `postExploreFeedBatchAction`:
     * POST `/api/v1/contents/feed/batch` with optional Bearer (via [HttpClient] defaultRequest).
     */
    suspend fun fetchTrendsBatch(
        excludeChannelIds: List<String>,
        seed: String?,
        count: Int = 8,
    ): Result<ExploreFeedBatchResponse> {
        return runCatching {
            val response =
                httpClient.post("${ApiConfig.BASE_URL}/api/v1/contents/feed/batch") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        ExploreFeedBatchRequest(
                            feedMode = "trends",
                            excludeChannelIds = excludeChannelIds.take(200),
                            seed = seed?.takeIf { it.isNotBlank() },
                            count = count.coerceIn(1, 15),
                        ),
                    )
                }
            AuthenticatedHttpResponseSupport.ensureSuccessOrThrow(
                response,
                sessionStore,
                operation = "fetchTrendsBatch",
            )
            response.body()
        }
    }
}
