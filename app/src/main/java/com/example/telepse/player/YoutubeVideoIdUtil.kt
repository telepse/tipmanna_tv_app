package com.example.telepse.player

object YoutubeVideoIdUtil {
    private val videoIdPattern =
        Regex("""(?:[?&]v=|/embed/|youtu\.be/|/shorts/)([A-Za-z0-9_-]{11})""")

    fun fromUrl(url: String): String? {
        val trimmed = url.trim()
        if (trimmed.length == 11 && trimmed.none { it == '/' || it == '?' }) {
            return trimmed
        }
        return videoIdPattern.find(trimmed)?.groupValues?.getOrNull(1)
    }
}
