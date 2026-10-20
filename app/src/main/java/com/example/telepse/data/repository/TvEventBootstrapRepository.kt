package com.example.telepse.data.repository

import com.example.telepse.data.auth.SessionStore
import com.example.telepse.data.model.TvEventBootstrap
import com.example.telepse.data.network.ApiConfig
import com.example.telepse.data.network.AuthenticatedHttpResponseSupport
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TvEventBootstrapRepository @Inject constructor(
    private val httpClient: HttpClient,
    private val sessionStore: SessionStore,
) {
    suspend fun getEventBootstrap(eventId: String): Result<TvEventBootstrap> {
        return try {
            val response = httpClient.get(
                "${ApiConfig.BASE_URL}/api/v1/tv-app/events/$eventId/bootstrap",
            )
            when (response.status) {
                HttpStatusCode.OK -> Result.success(response.body())
                HttpStatusCode.NotFound -> Result.failure(Exception("Event not found: $eventId"))
                else ->
                    Result.failure(
                        AuthenticatedHttpResponseSupport.failureException(
                            httpStatus = response.status.value,
                            sessionStore = sessionStore,
                            operation = "getEventBootstrap",
                        ),
                    )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
