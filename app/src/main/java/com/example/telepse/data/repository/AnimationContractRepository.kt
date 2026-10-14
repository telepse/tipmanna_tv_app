package com.example.telepse.data.repository

import com.example.telepse.data.network.ApiConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.Serializable

@Serializable
data class AnimationContractResponse(
    val version: Int = 1,
    val guard: GuardConfig = GuardConfig(),
) {
    @Serializable
    data class GuardConfig(val staleThresholdMs: Long = 180_000L)
}

@Singleton
class AnimationContractRepository @Inject constructor(
    private val httpClient: HttpClient,
) {
    suspend fun fetchStaleThresholdMs(): Long {
        return try {
            val contract: AnimationContractResponse =
                httpClient.get("${ApiConfig.BASE_URL}/api/v1/tv-app/animations/contract").body()
            contract.guard.staleThresholdMs
        } catch (_: Exception) {
            DEFAULT_STALE_MS
        }
    }

    companion object {
        const val DEFAULT_STALE_MS = 180_000L
    }
}
