package com.example.telepse.tips

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TipCheckoutEntryTest {
    @Test
    fun walletAvailableOnlyWhenLoggedInAndCartReady() {
        val choices = TipCheckoutChoices.forCart(cartReady = true, loggedIn = true)
        assertTrue(choices.canEnterWalletCheckout)
        assertFalse(choices.canEnterQrCheckout)
    }

    @Test
    fun qrAlwaysUnavailableUntilBackendShips() {
        val choices = TipCheckoutChoices.forCart(cartReady = true, loggedIn = true)
        assertFalse(choices.canEnterQrCheckout)
        assertTrue(
            choices.qrCard == TipQrCheckoutAvailability.BackendUnavailable,
        )
    }
}
