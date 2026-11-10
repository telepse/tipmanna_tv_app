package com.example.telepse.data.network

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.HttpHeaders

internal fun HttpRequestBuilder.applyRecoverySessionBearer(recoverySessionToken: String) {
    val trimmed = recoverySessionToken.trim()
    require(trimmed.isNotEmpty()) { "Recovery session token is required" }
    headers.remove(HttpHeaders.Authorization)
    headers.append(HttpHeaders.Authorization, "Bearer $trimmed")
}
