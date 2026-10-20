package com.example.telepse.tips

import com.example.telepse.data.model.WalletDebitCharacterLine
import com.example.telepse.data.model.WalletDebitRequest
import kotlinx.serialization.Serializable

/** Frozen debit body without secrets — PIN applied at submit time from UI. */
@Serializable
data class WalletDebitRequestSnapshot(
    val amount: Double,
    val eventId: String,
    val characters: List<WalletDebitCharacterLine>,
    val paymentSurface: String = "kotlin_tv",
    val deviceCategory: String = "tv",
) {
    fun toRequest(pin: String): WalletDebitRequest =
        WalletDebitRequest(
            pin = pin,
            amount = amount,
            eventId = eventId,
            characters = characters,
            paymentSurface = paymentSurface,
            deviceCategory = deviceCategory,
        )

    companion object {
        fun from(request: WalletDebitRequest): WalletDebitRequestSnapshot =
            WalletDebitRequestSnapshot(
                amount = request.amount,
                eventId = request.eventId,
                characters = request.characters,
                paymentSurface = request.paymentSurface,
                deviceCategory = request.deviceCategory,
            )
    }
}
