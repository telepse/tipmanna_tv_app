package com.example.telepse.data.repository

import com.example.telepse.data.auth.SessionStore
import com.example.telepse.data.model.CommunityRoomDisplayBootstrap
import com.example.telepse.data.network.ApiConfig
import com.example.telepse.data.network.AuthenticatedHttpResponseSupport
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommunityRoomDisplayRepository @Inject constructor(
    private val httpClient: HttpClient,
    private val sessionStore: SessionStore,
) {
    suspend fun fetchBootstrap(communityId: String): Result<CommunityRoomDisplayBootstrap> {
        val trimmed = communityId.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Community ID is required"))
        }
        return try {
            val response =
                httpClient.get(
                    "${ApiConfig.BASE_URL}/api/v1/tv-app/communities/$trimmed/room-display-bootstrap",
                )
            when (response.status) {
                HttpStatusCode.OK -> Result.success(response.body())
                HttpStatusCode.NotFound ->
                    Result.failure(Exception("Community not found: $trimmed"))
                HttpStatusCode.Forbidden ->
                    Result.failure(
                        AuthenticatedHttpResponseSupport.failureException(
                            httpStatus = 403,
                            sessionStore = sessionStore,
                            operation = "fetchCommunityBootstrap",
                        ),
                    )
                else ->
                    Result.failure(
                        AuthenticatedHttpResponseSupport.failureException(
                            httpStatus = response.status.value,
                            sessionStore = sessionStore,
                            operation = "fetchCommunityBootstrap",
                        ),
                    )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
