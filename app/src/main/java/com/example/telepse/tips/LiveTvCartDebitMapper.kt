package com.example.telepse.tips

import com.example.telepse.data.model.WalletDebitCharacterLine
import com.example.telepse.data.model.WalletDebitRequest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Maps an event-scoped LIVE tip cart to wallet debit request fields.
 * Expands quantity into repeated character lines (backend schema has no qty).
 * [provisionalTotal] is never treated as authoritative when a tip quote locks amount.
 */
object LiveTvCartDebitMapper {
    /** Stable multi-line identity token stored in [WalletDebitTipIdentity.characterId]. */
    fun cartLinesKey(cart: TipCartState): String =
        cart.lines
            .sortedBy { it.character.id }
            .joinToString("|") { "${it.character.id}:${it.quantity}" }

    fun expandCharacterLines(
        cart: TipCartState,
        timeLabel: String = defaultTimeLabel(),
    ): List<WalletDebitCharacterLine> {
        require(cart.eventId.isNotBlank()) { "eventId required" }
        require(cart.lines.isNotEmpty()) { "cart empty" }
        return cart.lines.flatMap { line ->
            List(line.quantity) {
                WalletDebitCharacterLine(
                    name = line.character.name,
                    time = timeLabel,
                    price = line.character.price,
                    characterId = line.character.id,
                )
            }
        }
    }

    /**
     * @param amountGbp authoritative amount when a tip quote exists; otherwise catalog sum
     *   (server still reconciles). Never invent amounts.
     */
    fun buildRequest(
        cart: TipCartState,
        pin: String,
        amountGbp: Double,
        tipQuoteId: String? = null,
        timeLabel: String = defaultTimeLabel(),
    ): WalletDebitRequest {
        val characters = expandCharacterLines(cart, timeLabel)
        return WalletDebitRequest(
            pin = pin,
            amount = amountGbp,
            eventId = cart.eventId.trim(),
            characters = characters,
            paymentSurface = "kotlin_tv",
            deviceCategory = "tv",
            tipQuoteId = tipQuoteId,
        )
    }

    fun fingerprint(
        cart: TipCartState,
        pin: String,
        amountGbp: Double,
    ): WalletDebitFingerprint =
        WalletDebitFingerprint(
            eventId = cart.eventId.trim(),
            characterId = cartLinesKey(cart),
            amount = amountGbp,
            pin = pin,
        )

    fun defaultTimeLabel(): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
}
