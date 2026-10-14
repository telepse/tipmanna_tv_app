package com.example.telepse.data.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertTrue
import org.junit.Test

class VerifyCodeRequestSerializationTest {

    private val appJson =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = false
        }

    @Test
    fun encode_includesAccountType_whenExplicitlySet() {
        val body =
            appJson.encodeToString(
                VerifyCodeRequest(
                    email = "dummy@example.com",
                    code = "000000",
                    accountType = "TIPPER",
                ),
            )
        assertTrue(body.contains("\"accountType\":\"TIPPER\""))
        assertTrue(body.contains("\"email\":\"dummy@example.com\""))
        assertTrue(body.contains("\"code\":\"000000\""))
    }
}
