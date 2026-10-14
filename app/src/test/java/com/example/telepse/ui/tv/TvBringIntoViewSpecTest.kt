package com.example.telepse.ui.tv

import org.junit.Assert.assertTrue
import org.junit.Test

class TvBringIntoViewSpecTest {

    @Test
    fun defaultSpec_returnsFiniteScrollDistance() {
        val spec = TvDefaultBringIntoViewSpec()
        val distance = spec.calculateScrollDistance(offset = 400f, size = 96f, containerSize = 1080f)
        assertTrue(distance.isFinite())
    }
}
