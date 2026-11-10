package com.example.telepse.data.community

import com.example.telepse.data.model.CommunityDisplayUpdatedPayload
import com.example.telepse.data.model.CommunityRoomDisplaySettings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CommunityRoomDisplayParserTest {

    @Test
    fun parseDisplayUpdated_validJson() {
        val payload =
            parseCommunityDisplayUpdatedPayload(
                """{"communityId":"c1","at":12345}""",
            )
        assertNotNull(payload)
        assertEquals("c1", payload!!.communityId)
        assertEquals(12345L, payload.at)
    }

    @Test
    fun parseDisplayUpdated_malformed() {
        assertNull(parseCommunityDisplayUpdatedPayload("{bad"))
    }

    @Test
    fun displayUpdatedApplies_matchingId() {
        assertTrue(
            communityDisplayUpdatedApplies(
                CommunityDisplayUpdatedPayload("c1", 1L),
                "c1",
            ),
        )
    }

    @Test
    fun displayUpdatedApplies_otherIdRejected() {
        assertFalse(
            communityDisplayUpdatedApplies(
                CommunityDisplayUpdatedPayload("other", 1L),
                "c1",
            ),
        )
    }

    @Test
    fun progressAmounts_visibleWhenNotHidden() {
        assertTrue(
            communityShowProgressBarAmounts(
                CommunityRoomDisplaySettings(hideProgressAmount = false),
            ),
        )
        assertFalse(
            communityShowProgressBarAmounts(
                CommunityRoomDisplaySettings(hideProgressAmount = true),
            ),
        )
    }

    private fun assertEquals(expected: String, actual: String?) {
        org.junit.Assert.assertEquals(expected, actual)
    }

    private fun assertEquals(expected: Long, actual: Long?) {
        org.junit.Assert.assertEquals(expected, actual)
    }
}
