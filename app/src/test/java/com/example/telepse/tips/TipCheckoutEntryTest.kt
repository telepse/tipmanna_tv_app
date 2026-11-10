package com.example.telepse.tips

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TipCheckoutEntryTest {
    @Test
    fun walletAvailableOnlyWhenLoggedInAndCartReady() {
        val choices = TipCheckoutChoices.forCart(cartReady = true, loggedIn = true)
        assertTrue(choices.canEnterWalletCheckout)
        assertTrue(choices.canEnterQrCheckout)
    }

    @Test
    fun qrRequiresSignInWhenLoggedOut() {
        val choices = TipCheckoutChoices.forCart(cartReady = true, loggedIn = false)
        assertFalse(choices.canEnterQrCheckout)
        assertTrue(choices.qrCard == TipQrCheckoutAvailability.RequiresSignIn)
    }
}
