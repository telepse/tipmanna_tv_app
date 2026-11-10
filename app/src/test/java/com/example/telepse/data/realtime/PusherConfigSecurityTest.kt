package com.example.telepse.data.realtime

import org.junit.Assert.assertFalse
import org.junit.Test

class PusherConfigSecurityTest {
    @Test
    fun pusherConfig_doesNotExposeSecretConstant() {
        val fields = PusherConfig::class.java.declaredFields.map { it.name.uppercase() }
        assertFalse(fields.any { it.contains("SECRET") })
    }
}
