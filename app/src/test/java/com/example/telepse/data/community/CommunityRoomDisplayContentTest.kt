package com.example.telepse.data.community

import com.example.telepse.data.model.CommunityRoomDisplaySettings
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CommunityRoomDisplayContentTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun parsePerformers_preservesOrderAndLabels() {
        val el =
            json.parseToJsonElement(
                """
                [
                  {"id":"p1","name":"Act One","amountReceived":"£120"},
                  {"name":"Act Two"}
                ]
                """.trimIndent(),
            )
        val list = parseCommunityRoomNamedList(el)
        assertEquals(2, list.size)
        assertEquals("Act One", list[0].name)
        assertEquals("£120", list[0].amountReceived)
        assertEquals("Act Two", list[1].name)
        assertNull(list[1].amountReceived)
        assertEquals("Act One — £120", formatCommunityRoomEntryLine(list[0]))
        assertEquals("Act Two", formatCommunityRoomEntryLine(list[1]))
    }

    @Test
    fun parsePerformers_skipsMalformedEntries() {
        val el =
            json.parseToJsonElement(
                """[{}, {"name":""}, {"name":"Valid"}, "string-only", 42]""",
            )
        val list = parseCommunityRoomNamedList(el)
        assertEquals(2, list.size)
        assertEquals("Valid", list[0].name)
        assertEquals("string-only", list[1].name)
    }

    @Test
    fun parseProjects_nullAndEmpty() {
        assertTrue(parseCommunityRoomNamedList(null).isEmpty())
        assertTrue(parseCommunityRoomNamedList(JsonNull).isEmpty())
        assertTrue(parseCommunityRoomNamedList(json.parseToJsonElement("[]")).isEmpty())
    }

    @Test
    fun parseProjects_nonArrayReturnsEmpty() {
        assertTrue(parseCommunityRoomNamedList(json.parseToJsonElement("""{"name":"x"}""")).isEmpty())
    }

    @Test
    fun tipUsageDisclosure_plainString() {
        assertNull(parseCommunityTipUsageDisclosure(null))
        assertNull(parseCommunityTipUsageDisclosure("   "))
        assertEquals(
            "Tips support performer welfare and venue costs.",
            parseCommunityTipUsageDisclosure("  Tips support performer welfare and venue costs.  "),
        )
    }

    @Test
    fun toDisplayContent_fromSettings() {
        val settings =
            CommunityRoomDisplaySettings(
                moneyFor = "Community goal",
                tipUsageDisclosure = "Where your tip goes",
                performers = json.parseToJsonElement("""[{"name":"A"}]"""),
                projects = json.parseToJsonElement("""[{"name":"Project B"}]"""),
            )
        val content = settings.toDisplayContent()
        assertEquals(1, content.performers.size)
        assertEquals(1, content.projects.size)
        assertEquals("Where your tip goes", content.tipUsageDisclosureText)
    }

    @Test
    fun toDisplayContent_refreshReflectsNewBootstrapData() {
        val before =
            CommunityRoomDisplaySettings(
                performers = json.parseToJsonElement("""[{"name":"Before"}]"""),
                tipUsageDisclosure = "Old",
            )
        val after =
            CommunityRoomDisplaySettings(
                performers = json.parseToJsonElement("""[{"name":"After"}]"""),
                tipUsageDisclosure = "New disclosure",
            )
        assertEquals("Before", before.toDisplayContent().performers.single().name)
        assertEquals("After", after.toDisplayContent().performers.single().name)
        assertEquals("New disclosure", after.toDisplayContent().tipUsageDisclosureText)
    }
}
