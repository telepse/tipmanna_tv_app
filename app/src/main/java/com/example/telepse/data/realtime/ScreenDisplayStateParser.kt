package com.example.telepse.data.realtime

import com.example.telepse.data.model.ScreenDisplayStatePayload
import kotlinx.serialization.json.Json

private val screenDisplayJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = false
}

/** Documented public event name when bootstrap omits `realtime.events.screenDisplayState`. */
const val DEFAULT_SCREEN_DISPLAY_STATE_EVENT = "screen-display-state"

/** Documented private event name when bootstrap omits `realtime.events.screenSettingsUpdated`. */
const val DEFAULT_SCREEN_SETTINGS_UPDATED_EVENT = "screen-settings-updated"

fun parseScreenDisplayStatePayload(raw: String): ScreenDisplayStatePayload? {
    if (raw.isBlank()) return null
    return try {
        screenDisplayJson.decodeFromString<ScreenDisplayStatePayload>(raw)
    } catch (_: Exception) {
        null
    }
}
