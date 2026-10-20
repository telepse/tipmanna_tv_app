package com.example.telepse.data.network

import org.junit.Assert.assertEquals
import org.junit.Test

class EventCodeNormalizerTest {

    @Test
    fun normalize_trimsAndUppercases_localeRoot() {
        assertEquals("ABC123", normalizeEventCode("  abc123  "))
        assertEquals("HOST1", normalizeEventCode("host1"))
        assertEquals("MIXED", normalizeEventCode("MiXeD"))
    }

    @Test
    fun normalize_blankAfterTrim_staysBlank() {
        assertEquals("", normalizeEventCode("   "))
    }
}
