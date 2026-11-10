package com.example.telepse.ui.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CodeInputBlankCodeValidationTest {

    @Test
    fun blankOrWhitespaceCode_returnsPleaseEnterCodeMessage() {
        assertEquals("Please enter a code", codeInputBlankCodeError(""))
        assertEquals("Please enter a code", codeInputBlankCodeError("   "))
    }

    @Test
    fun nonBlankCode_returnsNoError() {
        assertNull(codeInputBlankCodeError("ABC123"))
        assertNull(codeInputBlankCodeError("  X  "))
    }
}
