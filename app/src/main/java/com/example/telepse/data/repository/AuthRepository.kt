package com.example.telepse.data.repository

import android.util.Log
import com.example.telepse.data.auth.SessionStore
import com.example.telepse.data.model.LoginTipperRequest
import com.example.telepse.data.model.LoginTipperResponse
import com.example.telepse.data.model.VerifyCodeRequest
import com.example.telepse.data.model.VerifyCodeResponse
import com.example.telepse.data.network.ApiConfig
import com.example.telepse.data.network.ApiErrorMessageMapper
import com.example.telepse.data.network.ApiErrorMessageMapper.AuthUserFacingException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val httpClient: HttpClient,
    private val sessionStore: SessionStore,
) {
    suspend fun requestLoginCode(email: String): Result<Unit> =
        runAuthCall(operation = OP_LOGIN_TIPPER) {
            val response =
                httpClient.post("${ApiConfig.BASE_URL}/api/v1/auth/login-tipper") {
                    contentType(ContentType.Application.Json)
                    setBody(LoginTipperRequest(email = email.trim()))
                }
            if (!response.status.isSuccess()) {
                throw httpFailure(OP_LOGIN_TIPPER, response.status.value, response.bodyAsText())
            }
            response.body<LoginTipperResponse>()
            Unit
        }

    suspend fun verifyCode(email: String, code: String): Result<Unit> =
        runAuthCall(operation = OP_VERIFY_CODE) {
            val response =
                httpClient.post("${ApiConfig.BASE_URL}/api/v1/auth/verify-code") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        VerifyCodeRequest(
                            email = email.trim(),
                            code = code.trim(),
                            accountType = "TIPPER",
                        ),
                    )
                }
            if (!response.status.isSuccess()) {
                throw httpFailure(OP_VERIFY_CODE, response.status.value, response.bodyAsText())
            }
            val body = response.body<VerifyCodeResponse>()
            val token =
                body.token?.takeIf { it.isNotBlank() }
                    ?: throw AuthUserFacingException(
                        userMessage = "Sign-in could not be completed. Try requesting a new code.",
                        diagnostic = "verify-code success without token",
                    )
            sessionStore.saveSession(token, email)
        }

    fun signOut() {
        sessionStore.clear()
    }

    fun isLoggedIn(): Boolean = sessionStore.isLoggedIn()

    fun currentEmail(): String? = sessionStore.tipperEmail()

    private suspend fun <T> runAuthCall(operation: String, block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: AuthUserFacingException) {
            logAuthFailure(operation, e.diagnostic)
            Result.failure(e)
        } catch (e: Throwable) {
            val userMessage = ApiErrorMessageMapper.userMessageForThrowable(e)
            val diagnostic = ApiErrorMessageMapper.diagnosticForThrowable(e)
            logAuthFailure(operation, diagnostic)
            Result.failure(AuthUserFacingException(userMessage, diagnostic, e))
        }

    private fun httpFailure(operation: String, httpStatus: Int, rawBody: String): AuthUserFacingException {
        val parsed = ApiErrorMessageMapper.parseHttpFailure(httpStatus, rawBody)
        val diagnostic = "http=$httpStatus apiMessage=${parsed.apiMessage ?: "(none)"}"
        return AuthUserFacingException(parsed.userMessage, diagnostic)
    }

    private fun logAuthFailure(operation: String, diagnostic: String?) {
        Log.d(TAG, "auth operation=$operation ${diagnostic ?: "unknown"}")
    }

    companion object {
        private const val TAG = "AuthRepository"
        private const val OP_LOGIN_TIPPER = "login-tipper"
        private const val OP_VERIFY_CODE = "verify-code"
    }
}
