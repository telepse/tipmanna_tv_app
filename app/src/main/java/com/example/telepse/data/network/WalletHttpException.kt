package com.example.telepse.data.network

/**
 * Wallet API failure with HTTP status and whether a Bearer token was attached.
 * Never includes the token value.
 */
class WalletHttpException(
    val httpStatus: Int,
    val authPresent: Boolean,
    /** Parsed API error message when the client read a safe failure body (debit path). */
    val apiMessage: String? = null,
    message: String = "HTTP $httpStatus",
) : Exception(message)
