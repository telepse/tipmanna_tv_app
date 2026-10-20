package com.example.telepse.data.community

import com.example.telepse.data.model.CommunityRoomDisplaySettings
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/** One named row from community `performers` / `projects` JSON (host event screen shape). */
data class CommunityRoomNamedEntry(
    val name: String,
    val amountReceived: String? = null,
)

data class CommunityRoomDisplayContent(
    val performers: List<CommunityRoomNamedEntry> = emptyList(),
    val projects: List<CommunityRoomNamedEntry> = emptyList(),
    val tipUsageDisclosureText: String? = null,
)

fun formatCommunityRoomEntryLine(entry: CommunityRoomNamedEntry): String {
    val amount = entry.amountReceived?.trim().orEmpty()
    return if (amount.isNotEmpty()) {
        "${entry.name} — $amount"
    } else {
        entry.name
    }
}

/**
 * Parses community room `performers` / `projects` persisted JSON.
 * Contract: JSON array of objects with `name` (required for display) and optional `amountReceived`;
 * optional `id` is ignored on TV. Order is preserved. Malformed entries are skipped.
 */
fun parseCommunityRoomNamedList(element: JsonElement?): List<CommunityRoomNamedEntry> {
    if (element == null || element is JsonNull) return emptyList()
    val arr = element as? JsonArray ?: return emptyList()
    return arr.mapNotNull { item -> parseCommunityRoomNamedEntry(item) }
}

private fun parseCommunityRoomNamedEntry(item: JsonElement): CommunityRoomNamedEntry? {
    return when (item) {
        is JsonObject -> {
            val name = item.stringField("name") ?: return null
            CommunityRoomNamedEntry(
                name = name,
                amountReceived = item.stringField("amountReceived"),
            )
        }
        is JsonPrimitive -> {
            if (!item.isString) return null
            val name = item.contentOrNull?.trim().orEmpty()
            if (name.isEmpty()) null else CommunityRoomNamedEntry(name = name)
        }
        else -> null
    }
}

private fun JsonObject.stringField(key: String): String? {
    val el = this[key] ?: return null
    if (el is JsonNull) return null
    return el.jsonPrimitive.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }
}

/**
 * Community room `tipUsageDisclosure` is a persisted plain string (`roomTipUsageDisclosure`), not the
 * venue event array. Show verbatim when non-blank.
 */
fun parseCommunityTipUsageDisclosure(raw: String?): String? {
    return raw?.trim()?.takeIf { it.isNotEmpty() }
}

fun CommunityRoomDisplaySettings.toDisplayContent(): CommunityRoomDisplayContent {
    return CommunityRoomDisplayContent(
        performers = parseCommunityRoomNamedList(performers),
        projects = parseCommunityRoomNamedList(projects),
        tipUsageDisclosureText = parseCommunityTipUsageDisclosure(tipUsageDisclosure),
    )
}
