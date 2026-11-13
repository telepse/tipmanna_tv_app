package com.example.telepse.data.repository

import com.example.telepse.data.auth.SessionStore
import com.example.telepse.data.model.ContentReportReason
import com.example.telepse.data.network.ApiConfig
import com.example.telepse.data.network.ApiErrorMessageMapper
import com.example.telepse.data.repository.ContentReportRepository.ContentReportException
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.Serializable

@Singleton
class HostReportRepository @Inject constructor(
    private val httpClient: HttpClient,
    private val sessionStore: SessionStore,
) {
    suspend fun reportHost(
        hostId: String,
        reason: ContentReportReason,
        otherText: String?,
        contextChannelId: String?,
    ): Result<Unit> {
        val trimmedHost = hostId.trim()
        if (trimmedHost.isEmpty()) {
            return Result.failure(IllegalArgumentException("Host identifier missing"))
        }
        if (!sessionStore.isLoggedIn()) {
            return Result.failure(
                ContentReportException("Sign in is required to report a host."),
            )
        }
        if (reason == ContentReportReason.Other && otherText.isNullOrBlank()) {
            return Result.failure(ContentReportException("Please describe the issue."))
        }
        return try {
            val response =
                httpClient.post(
                    "${ApiConfig.BASE_URL}/api/v1/moderation/hosts/$trimmedHost/report",
                ) {
                    contentType(ContentType.Application.Json)
                    setBody(
                        HostReportBody(
                            reason = reason.wireValue,
                            otherText = otherText?.trim()?.takeIf { it.isNotEmpty() },
                            contextChannelId = contextChannelId?.trim()?.takeIf { it.isNotEmpty() },
                        ),
                    )
                }
            if (response.status.value == 429) {
                return Result.failure(
                    ContentReportException(
                        "You already reported this host in the last 24 hours.",
                    ),
                )
            }
            if (!response.status.isSuccess()) {
                val parsed =
                    ApiErrorMessageMapper.parseHttpFailure(
                        response.status.value,
                        response.bodyAsText(),
                    )
                return Result.failure(ContentReportException(parsed.userMessage))
            }
            Result.success(Unit)
        } catch (e: ContentReportException) {
            Result.failure(e)
        } catch (e: Throwable) {
            val message = ApiErrorMessageMapper.userMessageForThrowable(e)
            Result.failure(ContentReportException(message, e))
        }
    }

    @Serializable
    private data class HostReportBody(
        val reason: String,
        val otherText: String? = null,
        val contextChannelId: String? = null,
    )
}
