package com.example.telepse.data.repository

import com.example.telepse.data.model.TvScheduleFetchOutcome
import com.example.telepse.data.model.TvScheduleResponse
import com.example.telepse.data.network.ApiConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TvScheduleRepository @Inject constructor(
    private val httpClient: HttpClient,
) {
    suspend fun fetchGlobalSchedule(ifNoneMatch: String? = null): Result<TvScheduleFetchOutcome> {
        return runCatching {
            val response =
                httpClient.get("${ApiConfig.BASE_URL}/api/v1/tv-app/schedule") {
                    ifNoneMatch?.trim()?.takeIf { it.isNotEmpty() }?.let { tag ->
                        header(HttpHeaders.IfNoneMatch, tag)
                    }
                }
            val etag = response.headers[HttpHeaders.ETag]
            if (response.status == HttpStatusCode.NotModified) {
                return@runCatching TvScheduleFetchOutcome(
                    notModified = true,
                    schedule = null,
                    etag = etag ?: ifNoneMatch,
                )
            }
            if (!response.status.isSuccess()) {
                error("Schedule HTTP ${response.status.value}: ${response.bodyAsText()}")
            }
            TvScheduleFetchOutcome(
                notModified = false,
                schedule = response.body<TvScheduleResponse>(),
                etag = etag,
            )
        }
    }
}
