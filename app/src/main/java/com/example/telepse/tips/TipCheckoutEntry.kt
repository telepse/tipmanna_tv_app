package com.example.telepse.tips

/**
 * Checkout entry surface for LIVE tip cart.
 * Wallet path enters real debit (PIN + idempotent submit). QR/card remains blocked until Task 8B.
 */
enum class TipWalletCheckoutAvailability {
    /** Tipper session present — wallet debit UI may be entered. */
    Available,

    /** No tipper session — wallet path requires sign-in. */
    RequiresSignIn,
}

enum class TipQrCheckoutAvailability {
    /**
     * TV tip checkout-session (`/c/{token}`) is not deployed in the backend.
     * Do not invent tokens or pretend a room URL is QR checkout.
     */
    BackendUnavailable,
}

data class TipCheckoutChoices(
    val wallet: TipWalletCheckoutAvailability,
    val qrCard: TipQrCheckoutAvailability = TipQrCheckoutAvailability.BackendUnavailable,
    val cartReady: Boolean,
) {
    val canEnterWalletCheckout: Boolean
        get() = cartReady && wallet == TipWalletCheckoutAvailability.Available

    val canEnterQrCheckout: Boolean
        get() = false

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
                qrCard = TipQrCheckoutAvailability.BackendUnavailable,
                cartReady = cartReady,
            )
    }
}
