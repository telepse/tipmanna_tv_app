package com.example.telepse.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.IOException

class ApiErrorMessageMapperTest {

    @Test
    fun parseHttpFailure_404_usesUnknownAccountCopy() {
        val parsed = ApiErrorMessageMapper.parseHttpFailure(404, """{"error":{"message":"not found"}}""")
        assertEquals(ApiErrorMessageMapper.UNKNOWN_ACCOUNT, parsed.userMessage)
    }

    @Test
    fun parseHttpFailure_429_mentionsRateLimit() {
        val parsed = ApiErrorMessageMapper.parseHttpFailure(429, "")
        assertEquals("Too many attempts. Wait a few minutes and try again.", parsed.userMessage)
    }

    @Test
    fun parseHttpFailure_safeApiMessage_isShown() {
        val parsed =
            ApiErrorMessageMapper.parseHttpFailure(
                400,
                """{"error":{"message":"Invalid verification code"}}""",
            )
        assertEquals("Invalid verification code", parsed.userMessage)
    }

    @Test
    fun parseHttpFailure_tokenInMessage_isRedactedToGeneric() {
        val parsed =
            ApiErrorMessageMapper.parseHttpFailure(
                400,
                """{"error":{"message":"bad bearer token xyz"}}""",
            )
        assertEquals(ApiErrorMessageMapper.GENERIC_AUTH_FAILURE, parsed.userMessage)
    }

    @Test
    fun userMessageForThrowable_ioException_isGenericAuthFailure() {
        val msg = ApiErrorMessageMapper.userMessageForThrowable(IOException("timeout"))
        assertEquals(ApiErrorMessageMapper.GENERIC_AUTH_FAILURE, msg)
    }

    @Test
    fun userMessageForThrowable_authUserFacingException_preservesMessage() {
        val ex =
            ApiErrorMessageMapper.AuthUserFacingException(
                userMessage = "Custom sign-in message",
                diagnostic = "test",
            )
        assertEquals("Custom sign-in message", ApiErrorMessageMapper.userMessageForThrowable(ex))
    }

    @Test
    fun diagnosticForThrowable_doesNotEchoSecrets() {
        val ex = ApiErrorMessageMapper.AuthUserFacingException("msg", "http=401 apiMessage=(none)")
        val diag = ApiErrorMessageMapper.diagnosticForThrowable(ex)
        assertFalse(diag!!.contains("Bearer"))
    }
}
