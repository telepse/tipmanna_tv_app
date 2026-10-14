package com.example.telepse.data.model

import kotlinx.serialization.Serializable

/** Standard API error envelope: `{ "error": { "status", "message", ... } }`. */
@Serializable
data class ApiErrorEnvelope(
    val error: ApiErrorPayload? = null,
)

@Serializable
data class ApiErrorPayload(
    val status: Int? = null,
    val message: String? = null,
)

/** Legacy/alternate shape if present in some responses. */
@Serializable
data class LegacyApiErrorBody(
    val message: String? = null,
)
