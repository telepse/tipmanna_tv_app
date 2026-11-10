package com.example.telepse.data.realtime

import com.example.telepse.data.model.Performer
import com.example.telepse.data.model.ScreenDisplayStatePayload
import com.example.telepse.data.model.TipUsageDisclosure
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private val screenDisplayJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = false
}

/** Documented public event name when bootstrap omits `realtime.events.screenDisplayState`. */
const val DEFAULT_SCREEN_DISPLAY_STATE_EVENT = "screen-display-state"

/** Documented private event name when bootstrap omits `realtime.events.screenSettingsUpdated`. */
const val DEFAULT_SCREEN_SETTINGS_UPDATED_EVENT = "screen-settings-updated"

/**
 * Parses `screen-display-state` / `screen-settings-updated` payloads.
 *
 * Field-by-field JsonObject extraction so nested list shape drift (e.g. Float
 * `percentage` on tipUsageDisclosure) cannot fail the entire HUD update.
 */
fun parseScreenDisplayStatePayload(raw: String): ScreenDisplayStatePayload? {
    if (raw.isBlank()) return null
    return try {
        val obj = screenDisplayJson.parseToJsonElement(raw) as? JsonObject ?: return null
        ScreenDisplayStatePayload(
            eventId = obj.stringOrNull("eventId"),
            eventName = obj.stringOrNull("eventName"),
            eventCode = obj.stringOrNull("eventCode"),
            hostEventSlug = obj.stringOrNull("hostEventSlug"),
            scanLink = obj.stringOrNull("scanLink"),
            amountToRaise = obj.doubleOrNull("amountToRaise"),
            amountRaised = obj.doubleOrNull("amountRaised"),
            numOfTips = obj.intOrNull("numOfTips"),
            latestTipAmount = obj.doubleOrNull("latestTipAmount"),
            eventBackground = obj.stringOrNull("eventBackground"),
            backgroundType = obj.stringOrNull("backgroundType"),
            hideProgressBarAmount = obj.booleanOrNull("hideProgressBarAmount"),
            showLatestAmount = obj.booleanOrNull("showLatestAmount"),
            backgroundTypePresent = obj.containsKey("backgroundType"),
            hideProgressBarAmountPresent = obj.containsKey("hideProgressBarAmount"),
            showLatestAmountPresent = obj.containsKey("showLatestAmount"),
            moneyFor = obj.stringOrNull("moneyFor"),
            tipUsageDisclosure = obj.parseTipUsageDisclosureList(),
            performers = obj.parsePerformerList(),
            eventBackgroundPresent = obj.containsKey("eventBackground"),
            moneyForPresent = obj.containsKey("moneyFor"),
            tipUsageDisclosurePresent = obj.containsKey("tipUsageDisclosure"),
            performersPresent = obj.containsKey("performers"),
        )
    } catch (_: Exception) {
        null
    }
}

private fun JsonObject.stringOrNull(key: String): String? {
    val el = this[key] ?: return null
    if (el is JsonNull) return null
    return el.jsonPrimitive.contentOrNull
}

private fun JsonObject.doubleOrNull(key: String): Double? {
    val el = this[key] ?: return null
    if (el is JsonNull) return null
    val prim = el as? JsonPrimitive ?: return null
    return prim.doubleOrNull ?: prim.contentOrNull?.toDoubleOrNull()
}

private fun JsonObject.intOrNull(key: String): Int? {
    val el = this[key] ?: return null
    if (el is JsonNull) return null
    val prim = el as? JsonPrimitive ?: return null
    return prim.intOrNull
        ?: prim.doubleOrNull?.toInt()
        ?: prim.contentOrNull?.toDoubleOrNull()?.toInt()
}

private fun JsonObject.booleanOrNull(key: String): Boolean? {
    val el = this[key] ?: return null
    if (el is JsonNull) return null
    return (el as? JsonPrimitive)?.booleanOrNull
}

private fun JsonObject.parseTipUsageDisclosureList(): List<TipUsageDisclosure>? {
    val el = this["tipUsageDisclosure"] ?: return null
    if (el is JsonNull) return emptyList()
    val arr = el as? JsonArray ?: return null
    return arr.mapNotNull { item ->
        val o = item as? JsonObject ?: return@mapNotNull null
        val id = o.stringOrNull("id") ?: return@mapNotNull null
        val usageType = o.stringOrNull("usageType") ?: return@mapNotNull null
        val percentage = o.percentageAsInt("percentage") ?: return@mapNotNull null
        TipUsageDisclosure(
            id = id,
            usageType = usageType,
            percentage = percentage,
            customLabel = o.stringOrNull("customLabel"),
            eventId = o.stringOrNull("eventId"),
        )
    }
}

private fun JsonObject.percentageAsInt(key: String): Int? {
    val el = this[key] ?: return null
    if (el is JsonNull) return null
    val prim = el as? JsonPrimitive ?: return null
    return prim.intOrNull
        ?: prim.doubleOrNull?.toInt()
        ?: prim.contentOrNull?.toDoubleOrNull()?.toInt()
}

private fun JsonObject.parsePerformerList(): List<Performer>? {
    val el = this["performers"] ?: return null
    if (el is JsonNull) return emptyList()
    val arr = el as? JsonArray ?: return null
    return arr.mapNotNull { item ->
        val o = item as? JsonObject ?: return@mapNotNull null
        val id = o.stringOrNull("id") ?: return@mapNotNull null
        val name = o.stringOrNull("name") ?: return@mapNotNull null
        Performer(
            id = id,
            name = name,
            description = o.stringOrNull("description"),
        )
    }
}
