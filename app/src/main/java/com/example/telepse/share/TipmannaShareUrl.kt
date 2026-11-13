package com.example.telepse.share

import com.example.telepse.premvp.PreMvpFeatureFlags
import java.net.URLEncoder

/**
 * Mirrors web `buildContentSharePath`: `/content/{eventId}?v={channelId}`.
 */
object TipmannaShareUrl {
    const val VIDEO_DEEP_LINK_PARAM = "v"

    private val uuidRegex =
        Regex("""^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$""", RegexOption.IGNORE_CASE)

    fun isLikelyUuid(value: String?): Boolean {
        val trimmed = value?.trim().orEmpty()
        return trimmed.isNotEmpty() && uuidRegex.matches(trimmed)
    }

    /**
     * Returns an absolute HTTPS share URL, or null when identifiers are not safe to publish.
     */
    fun buildContentShareUrl(
        eventId: String?,
        channelId: String?,
        origin: String = PreMvpFeatureFlags.shareContentOrigin,
    ): String? {
        val event = eventId?.trim().orEmpty()
        val channel = channelId?.trim().orEmpty()
        if (!isLikelyUuid(event) || !isLikelyUuid(channel)) return null
        val encodedEvent = URLEncoder.encode(event, "UTF-8")
        val path = "/content/$encodedEvent?$VIDEO_DEEP_LINK_PARAM=${URLEncoder.encode(channel, "UTF-8")}"
        val base = origin.trimEnd('/')
        return "$base$path"
    }
}
