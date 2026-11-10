package com.example.telepse.data.realtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PusherBootstrapCredentialsTest {

    @Test
    fun resolve_usesBootstrapWhenPresent() {
        val resolved = resolvePusherCredentials("pk-from-api", "eu", "embedded", "us3")
        assertEquals("pk-from-api", resolved.apiKey)
        assertEquals("eu", resolved.cluster)
        assertEquals("bootstrap", resolved.source)
        assertTrue(resolved.usedBootstrapKey)
        assertTrue(resolved.usedBootstrapCluster)
    }

    @Test
    fun resolve_fallsBackWhenBlank() {
        val resolved = resolvePusherCredentials("  ", null, "embedded-key", "us3")
        assertEquals("embedded-key", resolved.apiKey)
        assertEquals("us3", resolved.cluster)
        assertEquals("embedded_fallback", resolved.source)
        assertFalse(resolved.usedBootstrapKey)
    }

    @Test
    fun diagnostic_masksKey() {
        val line =
            formatPusherCredentialDiagnostic(
                ResolvedPusherCredentials(
                    apiKey = "1234567890ab",
                    cluster = "us3",
                    source = "bootstrap",
                    usedBootstrapKey = true,
                    usedBootstrapCluster = true,
                ),
            )
        assertTrue(line.contains("1234…90ab"))
        assertFalse(line.contains("1234567890ab"))
    }
}
