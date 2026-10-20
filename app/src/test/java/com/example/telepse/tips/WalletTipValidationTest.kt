package com.example.telepse.tips

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WalletTipValidationTest {
    @Test
    fun normalizeEventId_acceptsUuid() {
        val id = "11111111-1111-4111-8111-111111111111"
        assertTrue(WalletTipValidation.normalizeEventId(id) == id)
    }

    @Test
    fun normalizeEventId_rejectsGarbage() {
        assertNull(WalletTipValidation.normalizeEventId("not-a-uuid"))
    }

    @Test
    fun pin_mustBeFourDigits() {
        assertTrue(WalletTipValidation.isValidPin("1234"))
        assertFalse(WalletTipValidation.isValidPin("123"))
        assertFalse(WalletTipValidation.isValidPin("abcd"))
    }

    @Test
    fun amountsMatch_withinTolerance() {
        assertTrue(WalletTipValidation.amountsMatch(5.0, 5.0))
        assertFalse(WalletTipValidation.amountsMatch(5.0, 6.0))
    }
}
