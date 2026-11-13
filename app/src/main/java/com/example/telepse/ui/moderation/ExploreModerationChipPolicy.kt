package com.example.telepse.ui.moderation

/**
 * Moderation must not depend on YouTube player readiness or payment feature flags.
 */
object ExploreModerationChipPolicy {
    fun moderationEnabled(channelId: String): Boolean = channelId.isNotBlank()
}
