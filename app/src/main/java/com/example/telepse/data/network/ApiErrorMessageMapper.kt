package com.example.telepse.data.network

import com.example.telepse.data.model.ApiErrorEnvelope
import com.example.telepse.data.model.LegacyApiErrorBody
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import java.io.IOException
import kotlinx.serialization.json.Json

/** Maps API / transport failures to TV-safe user copy and log-safe diagnostics. */
object ApiErrorMessageMapper {
    private val errorJson =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    data class ParsedFailure(
        val httpStatus: Int?,
        val apiMessage: String?,
        val userMessage: String,
    )

    fun parseHttpFailure(httpStatus: Int, rawBody: String): ParsedFailure {
        val apiMessage = extractApiMessage(rawBody)
        return ParsedFailure(
            httpStatus = httpStatus,
            apiMessage = apiMessage,
            userMessage = userMessageForAuth(httpStatus, apiMessage),
        )
    }

    fun userMessageForThrowable(throwable: Throwable): String {
        if (throwable is AuthUserFacingException) {
            return throwable.userMessage
        }
        if (isTransportFailure(throwable)) {
            return GENERIC_AUTH_FAILURE
        }
        return GENERIC_AUTH_FAILURE
    }

    fun diagnosticForThrowable(throwable: Throwable): String? =
        when (throwable) {
            is AuthUserFacingException -> throwable.diagnostic
            else -> throwable.javaClass.simpleName + ": " + (throwable.message?.take(120) ?: "")
        }

    private fun extractApiMessage(rawBody: String): String? {
        val trimmed = rawBody.trim()
        if (trimmed.isEmpty()) return null
        return runCatching {
            errorJson.decodeFromString(ApiErrorEnvelope.serializer(), trimmed).error?.message
        }.getOrNull()
            ?: runCatching {
                errorJson.decodeFromString(LegacyApiErrorBody.serializer(), trimmed).message
            }.getOrNull()
    }

    private fun userMessageForAuth(httpStatus: Int, apiMessage: String?): String {
        if (httpStatus == 404 || isUnknownAccountApiMessage(apiMessage)) {
            return UNKNOWN_ACCOUNT
        }
        if (httpStatus == 429) {
            return "Too many attempts. Wait a few minutes and try again."
        }
        if (httpStatus in 400..499 && !apiMessage.isNullOrBlank()) {
            val safe = apiMessage.trim()
            if (safe.length <= 160 && !looksLikeSecretOrJson(safe)) {
                return safe
            }
        }
        return GENERIC_AUTH_FAILURE
    }

    private fun isUnknownAccountApiMessage(apiMessage: String?): Boolean {
        if (apiMessage.isNullOrBlank()) return false
        val lower = apiMessage.lowercase()
        return lower.contains("no tipper account") ||
            lower.contains("email not found") ||
            lower.contains("account not found") ||
            lower.contains("host account not found")
    }

    private fun looksLikeSecretOrJson(text: String): Boolean {
        val lower = text.lowercase()
        return text.startsWith("{") ||
            lower.contains("token") ||
            lower.contains("bearer") ||
            lower.contains("password")
    }

    private fun isTransportFailure(throwable: Throwable): Boolean {
        var cause: Throwable? = throwable
        while (cause != null) {
            when (cause) {
                is IOException,
                is ConnectTimeoutException,
                is HttpRequestTimeoutException,
                -> return true
            }
            cause = cause.cause
        }
        return false
    }

    const val UNKNOWN_ACCOUNT =
        "No Tipmanna account found for this email. Use an email registered as a tipper or host, or sign up on the web first."

    const val GENERIC_AUTH_FAILURE =
        "Unable to complete sign-in. Check your connection and try again."

    /** Thrown to [Result.failure] with user-safe [userMessage]; [diagnostic] is for debug logs only. */
    class AuthUserFacingException(
        val userMessage: String,
        val diagnostic: String?,
        cause: Throwable? = null,
    ) : Exception(userMessage, cause)
}
