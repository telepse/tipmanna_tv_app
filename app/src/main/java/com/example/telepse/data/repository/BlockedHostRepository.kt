package com.example.telepse.data.repository

import android.util.Log
import com.example.telepse.data.auth.SessionStore
import com.example.telepse.data.network.ApiConfig
import com.example.telepse.data.network.ApiErrorMessageMapper
import com.example.telepse.data.network.AuthenticatedHttpResponseSupport
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable

@Singleton
class BlockedHostRepository @Inject constructor(
    private val httpClient: HttpClient,
    private val sessionStore: SessionStore,
) {
    private val _blockedHostIds = MutableStateFlow<Set<String>>(emptySet())
    val blockedHostIds: StateFlow<Set<String>> = _blockedHostIds.asStateFlow()

    suspend fun refresh(): Result<Unit> {
        if (!sessionStore.isLoggedIn()) {
            _blockedHostIds.value = emptySet()
            return Result.success(Unit)
        }
        return runCatching {
            val response =
                httpClient.get("${ApiConfig.BASE_URL}/api/v1/tippers/me/blocked-hosts")
            AuthenticatedHttpResponseSupport.ensureSuccessOrThrow(
                response,
                sessionStore,
                operation = "listBlockedHosts",
            )
            val body = response.body<BlockedHostsResponse>()
            _blockedHostIds.value = body.hostIds.toSet()
            Log.d(TAG, "blockedHosts count=${body.hostIds.size}")
        }
    }

    suspend fun blockHost(hostId: String): Result<Unit> {
        val id = hostId.trim()
        if (id.isEmpty()) return Result.failure(IllegalArgumentException("Host id missing"))
        if (!sessionStore.isLoggedIn()) {
            return Result.failure(ModerationException("Sign in is required to block a host."))
        }
        return runCatching {
            val response =
                httpClient.post("${ApiConfig.BASE_URL}/api/v1/tippers/me/blocked-hosts/$id")
            if (!response.status.isSuccess()) {
                val parsed =
                    ApiErrorMessageMapper.parseHttpFailure(
                        response.status.value,
                        response.bodyAsText(),
                    )
                throw ModerationException(parsed.userMessage)
            }
            _blockedHostIds.value = _blockedHostIds.value + id
        }
    }

    suspend fun unblockHost(hostId: String): Result<Unit> {
        val id = hostId.trim()
        if (!sessionStore.isLoggedIn()) {
            return Result.failure(ModerationException("Sign in is required."))
        }
        return runCatching {
            val response =
                httpClient.delete("${ApiConfig.BASE_URL}/api/v1/tippers/me/blocked-hosts/$id")
            if (response.status.value == 404) {
                throw ModerationException("Host is not blocked.")
            }
            if (!response.status.isSuccess()) {
                val parsed =
                    ApiErrorMessageMapper.parseHttpFailure(
                        response.status.value,
                        response.bodyAsText(),
                    )
                throw ModerationException(parsed.userMessage)
            }
            _blockedHostIds.value = _blockedHostIds.value - id
        }
    }

    fun isHostBlocked(hostId: String?): Boolean {
        val id = hostId?.trim().orEmpty()
        if (id.isEmpty()) return false
        return _blockedHostIds.value.contains(id)
    }

    class ModerationException(
        override val message: String,
        cause: Throwable? = null,
    ) : Exception(message, cause)

    @Serializable
    private data class BlockedHostsResponse(
        val hostIds: List<String> = emptyList(),
    )

    private companion object {
        const val TAG = "BlockedHostRepository"
    }
}
