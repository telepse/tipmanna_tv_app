package com.example.telepse.data.repository

import android.util.Log
import com.example.telepse.data.auth.SessionStore
import com.example.telepse.data.model.TvScheduleFetchOutcome
import com.example.telepse.data.model.TvScheduleResponse
import com.example.telepse.data.network.ApiConfig
import com.example.telepse.data.network.ScheduleHttpException
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
import kotlinx.coroutines.CancellationException

@Singleton
class TvScheduleRepository @Inject constructor(
    private val httpClient: HttpClient,
    private val sessionStore: SessionStore,
) {
    suspend fun fetchGlobalSchedule(ifNoneMatch: String? = null): Result<TvScheduleFetchOutcome> {
        return try {
            val response =
                httpClient.get("${ApiConfig.BASE_URL}/api/v1/tv-app/schedule") {
                    ifNoneMatch?.trim()?.takeIf { it.isNotEmpty() }?.let { tag ->
                        header(HttpHeaders.IfNoneMatch, tag)
                    }
                }
            val etag = response.headers[HttpHeaders.ETag]
            if (response.status == HttpStatusCode.NotModified) {
                return Result.success(
                    TvScheduleFetchOutcome(
                        notModified = true,
                        schedule = null,
                        etag = etag ?: ifNoneMatch,
                    ),
                )
            }
            if (!response.status.isSuccess()) {
                val status = response.status.value
                val authPresent = sessionStore.accessToken() != null
                Log.w(TAG, "fetchGlobalSchedule http=$status authPresent=$authPresent")
                throw ScheduleHttpException(httpStatus = status, authPresent = authPresent)
            }
            Result.success(
                TvScheduleFetchOutcome(
                    notModified = false,
                    schedule = response.body<TvScheduleResponse>(),
                    etag = etag,
                ),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "TvScheduleRepository"
    }
}
