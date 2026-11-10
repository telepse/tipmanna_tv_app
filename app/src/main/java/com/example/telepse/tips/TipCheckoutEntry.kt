package com.example.telepse.tips

/**
 * Checkout entry surface for LIVE tip cart.
 * Wallet path enters real debit (PIN + idempotent submit). QR/card uses TV checkout sessions (Task 8B).
 */
enum class TipWalletCheckoutAvailability {
    /** Tipper session present — wallet debit UI may be entered. */
    Available,

    /** No tipper session — wallet path requires sign-in. */
    RequiresSignIn,
}

enum class TipQrCheckoutAvailability {
    /** Tipper session present — TV may create a checkout session for phone card pay. */
    Available,

    /** Card checkout still requires a tipper session on the TV app (same as wallet). */
    RequiresSignIn,
}

data class TipCheckoutChoices(
    val wallet: TipWalletCheckoutAvailability,
    val qrCard: TipQrCheckoutAvailability = TipQrCheckoutAvailability.RequiresSignIn,
    val cartReady: Boolean,
) {
    val canEnterWalletCheckout: Boolean
        get() = cartReady && wallet == TipWalletCheckoutAvailability.Available

    val canEnterQrCheckout: Boolean
        get() = cartReady && qrCard == TipQrCheckoutAvailability.Available

    companion object {
        fun forCart(
            cartReady: Boolean,
            loggedIn: Boolean,
        ): TipCheckoutChoices =
            TipCheckoutChoices(
                wallet =
                    if (loggedIn) {
                        TipWalletCheckoutAvailability.Available
                    } else {
                        TipWalletCheckoutAvailability.RequiresSignIn
                    },
                qrCard =
                    if (loggedIn) {
                        TipQrCheckoutAvailability.Available
                    } else {
                        TipQrCheckoutAvailability.RequiresSignIn
                    },
                cartReady = cartReady,
            )
    }
}
