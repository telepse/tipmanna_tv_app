package com.example.telepse.data.network

import com.example.telepse.data.model.Event
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

/** Matches [KtorClient] JSON settings for event list decoding. */
internal val eventsByCodeJson =
    Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = false
    }

/**
 * Parses a successful (HTTP 200) body from `GET /api/v1/events/code/{eventCode}`.
 * Supports a raw event array or a paginated envelope `{ "data": [...], "pagination": {...} }`.
 * Error objects (e.g. `{ "error": ... }`) must not be passed here — use HTTP status first.
 */
internal fun parseEventsByCodeResponseBody(body: String): List<Event> {
    val trimmed = body.trim()
    if (trimmed.isEmpty()) {
        throw IllegalArgumentException("Empty events response body")
    }
    val element = eventsByCodeJson.parseToJsonElement(trimmed)
    return when (element) {
        is JsonArray ->
            eventsByCodeJson.decodeFromJsonElement<List<Event>>(element)
        is JsonObject -> {
            if (element.containsKey("error")) {
                throw IllegalArgumentException("API error payload in events response")
            }
            val dataElement =
                element["data"]
                    ?: throw IllegalArgumentException("Paginated events response missing data")
            if (dataElement !is JsonArray) {
                throw IllegalArgumentException("Paginated events data is not an array")
            }
            eventsByCodeJson.decodeFromJsonElement<List<Event>>(dataElement)
        }
        else -> throw IllegalArgumentException("Unexpected JSON for events response")
    }
}
