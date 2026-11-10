package com.example.telepse.ui.screen

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenDisplayHudRulesTest {

    @Test
    fun showAmounts_whenHideFlagFalseOrNull() {
        assertTrue(shouldShowProgressBarAmounts(false))
        assertTrue(shouldShowProgressBarAmounts(null))
    }

    @Test
    fun hideAmounts_whenHideFlagTrue() {
        assertFalse(shouldShowProgressBarAmounts(true))
    }
}
