package com.example.telepse.data.realtime

/**
 * Formats Pusher connection error callbacks for logging.
 * The Java client may pass null [code] or [message] (Kotlin non-null params would NPE before logging).
 */
internal fun formatPusherConnectionError(
    message: String?,
    code: String?,
    exception: Exception?,
): String {
    val parts = mutableListOf<String>()
    parts.add("message=${message ?: "(null)"}")
    parts.add("code=${code ?: "(null)"}")
    exception?.message?.let { parts.add("exception=$it") }
    return parts.joinToString(", ")
}
