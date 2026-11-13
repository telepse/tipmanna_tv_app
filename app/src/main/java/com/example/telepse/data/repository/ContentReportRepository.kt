package com.example.telepse.data.repository

import android.util.Log
import com.example.telepse.data.auth.SessionStore
import com.example.telepse.data.model.ContentReportReason
import com.example.telepse.data.network.ApiConfig
import com.example.telepse.data.network.ApiErrorMessageMapper
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

/**
 * Submits a [ContentReport] for an **EventChannel** (video row), via the messages route.
 * Backend: `reportChannelContent` → `prisma.contentReport.create({ channelId })` — not a chat [Message].
 */
@Singleton
class ContentReportRepository @Inject constructor(
    private val httpClient: HttpClient,
    private val sessionStore: SessionStore,
) {
    suspend fun reportChannelContent(
        channelId: String,
        reason: ContentReportReason,
        otherText: String?,
    ): Result<Unit> {
        val trimmedChannel = channelId.trim()
        if (trimmedChannel.isEmpty()) {
            return Result.failure(IllegalArgumentException("Video identifier missing"))
        }
        if (!sessionStore.isLoggedIn()) {
            return Result.failure(
                ContentReportException("Sign in is required to report content."),
            )
        }
        if (reason == ContentReportReason.Other && otherText.isNullOrBlank()) {
            return Result.failure(
                ContentReportException("Please describe the issue."),
            )
        }
        return try {
            val response =
                httpClient.post(
                    "${ApiConfig.BASE_URL}/api/v1/messages/channel/$trimmedChannel/report",
                ) {
                    contentType(ContentType.Application.Json)
                    setBody(
                        ContentReportBody(
                            reason = reason.wireValue,
                            otherText = otherText?.trim()?.takeIf { it.isNotEmpty() },
                        ),
                    )
                }
            if (response.status.value == 429) {
                return Result.failure(
                    ContentReportException(
                        "You already reported this video in the last 24 hours.",
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
            Log.d(TAG, "contentReport submitted channelId=${trimmedChannel.take(8)}… reason=${reason.wireValue}")
            Result.success(Unit)
        } catch (e: ContentReportException) {
            Result.failure(e)
        } catch (e: Throwable) {
            val message = ApiErrorMessageMapper.userMessageForThrowable(e)
            Result.failure(ContentReportException(message, e))
        }
    }

    class ContentReportException(
        override val message: String,
        cause: Throwable? = null,
    ) : Exception(message, cause)

    @Serializable
    private data class ContentReportBody(
        val reason: String,
        val otherText: String? = null,
    )

    private companion object {
        const val TAG = "ContentReportRepository"
    }
}
