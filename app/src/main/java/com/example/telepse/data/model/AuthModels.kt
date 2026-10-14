package com.example.telepse.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class LoginTipperRequest(val email: String)

@Serializable
data class VerifyCodeRequest(
    val email: String,
    val code: String,
    val accountType: String,
)

@Serializable
data class LoginTipperResponse(
    val message: String? = null,
    val requiresVerification: Boolean? = null,
)

@Serializable
data class VerifyCodeResponse(
    val message: String? = null,
    val token: String? = null,
    val role: String? = null,
    val requiresVerification: Boolean? = null,
)

@Serializable
data class ApiErrorBody(
    val message: String? = null,
    val error: JsonObject? = null,
)
