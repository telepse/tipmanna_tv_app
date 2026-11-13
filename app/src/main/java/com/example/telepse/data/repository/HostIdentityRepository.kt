package com.example.telepse.data.repository

import com.example.telepse.data.network.ApiConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.Serializable

/**
 * Resolves Event owner host id for an [EventChannel] id via verified public API.
 * GET `/api/v1/saved-hosts/by-channel/:channelId/host-id`
 */
@Singleton
class HostIdentityRepository @Inject constructor(
    private val httpClient: HttpClient,
) {
    suspend fun resolveHostIdForChannel(channelId: String): Result<String> {
        val trimmed = channelId.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("channelId required"))
        }
        return try {
            val response =
                httpClient.get(
                    "${ApiConfig.BASE_URL}/api/v1/saved-hosts/by-channel/$trimmed/host-id",
                )
            if (!response.status.isSuccess()) {
                return Result.failure(
                    HostIdentityException("Could not resolve host for this video."),
                )
            }
            val body = response.body<HostIdResponse>()
            val hostId = body.hostId?.trim().orEmpty()
            if (hostId.isEmpty()) {
                return Result.failure(HostIdentityException("Host not found for this video."))
            }
            Result.success(hostId)
        } catch (e: HostIdentityException) {
            Result.failure(e)
        } catch (e: Throwable) {
            Result.failure(HostIdentityException("Could not resolve host for this video.", e))
        }
    }

    class HostIdentityException(
        override val message: String,
        cause: Throwable? = null,
    ) : Exception(message, cause)

    @Serializable
    private data class HostIdResponse(val hostId: String? = null)
}
