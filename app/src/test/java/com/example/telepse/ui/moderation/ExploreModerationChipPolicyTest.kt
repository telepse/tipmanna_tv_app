package com.example.telepse.ui.moderation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExploreModerationChipPolicyTest {

    @Test
    fun moderationEnabled_whenChannelIdPresent_withoutPlayer() {
        assertTrue(ExploreModerationChipPolicy.moderationEnabled("22222222-2222-4222-8222-222222222222"))
    }

    @Test
    fun moderationDisabled_whenChannelIdBlank() {
        assertFalse(ExploreModerationChipPolicy.moderationEnabled(""))
        assertFalse(ExploreModerationChipPolicy.moderationEnabled("   "))
    }
}
