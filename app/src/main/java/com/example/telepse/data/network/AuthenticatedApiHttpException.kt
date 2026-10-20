package com.example.telepse.data.network

/**
 * HTTP failure from a shared-client API call with whether a Bearer token was in [SessionStore] at failure time.
 */
class AuthenticatedApiHttpException(
    val httpStatus: Int,
    val authPresent: Boolean,
    val operation: String,
    message: String = "HTTP $httpStatus ($operation)",
) : Exception(message)
