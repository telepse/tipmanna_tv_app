package com.example.telepse.data.network

import com.example.telepse.data.auth.SessionStore
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess

object AuthenticatedHttpResponseSupport {
    fun authPresentAtFailure(sessionStore: SessionStore): Boolean =
        sessionStore.accessToken() != null

    fun failureException(
        httpStatus: Int,
        sessionStore: SessionStore,
        operation: String,
    ): AuthenticatedApiHttpException =
        AuthenticatedApiHttpException(
            httpStatus = httpStatus,
            authPresent = authPresentAtFailure(sessionStore),
            operation = operation,
        )

    fun ensureSuccessOrThrow(
        response: HttpResponse,
        sessionStore: SessionStore,
        operation: String,
    ) {
        if (!response.status.isSuccess()) {
            throw failureException(response.status.value, sessionStore, operation)
        }
    }
}
