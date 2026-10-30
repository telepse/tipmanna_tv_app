package com.example.telepse.tips

import com.example.telepse.data.model.ActiveTipConfigResponse
import com.example.telepse.data.model.CreateTipQuoteRequest
import com.example.telepse.data.model.TipQuoteLineRequest
import com.example.telepse.data.model.TipQuoteTextLineRequest

/**
 * Builds POST /tip-quote bodies from LIVE cart + active tip config.
 * Returns null body when tip config is legacy (quote not required).
 */
object LiveTvTipQuotePlanner {
    data class PlannedQuote(
        val request: CreateTipQuoteRequest,
        /** Catalog-sum fallback is never used when this plan exists. */
        val expectedTotalMinor: Int,
    )

    sealed interface PlanResult {
        /** No live tip config — debit without tipQuoteId; server matches catalog prices. */
        data object LegacyNoQuoteRequired : PlanResult

        data class Ready(val planned: PlannedQuote) : PlanResult

        data class Failed(val message: String) : PlanResult
    }

    fun plan(
        cart: TipCartState,
        tipConfig: ActiveTipConfigResponse,
        surface: String = "kotlin_tv",
    ): PlanResult {
        val mode = tipConfig.mode.trim().lowercase()
        if (mode != "live") return PlanResult.LegacyNoQuoteRequired
        val version = tipConfig.version
        val config = tipConfig.config
        if (version == null || version < 1 || config == null) {
            return PlanResult.Failed("Live tip configuration is incomplete — refresh and try again.")
        }
        if (cart.isEmpty || cart.eventId.isBlank()) {
            return PlanResult.Failed("Cart is empty.")
        }

        val itemsByCharacter =
            (config.sectionItems + config.drawerItems)
                .filter { it.itemId.isNotBlank() && it.characterId.isNotBlank() }
                .associateBy { it.characterId }

        val quoteLines = mutableListOf<TipQuoteLineRequest>()
        var textLine: TipQuoteTextLineRequest? = null
        var totalMinor = 0

        for (line in cart.lines) {
            val slot = itemsByCharacter[line.character.id]
            if (slot != null) {
                quoteLines +=
                    TipQuoteLineRequest(
                        itemId = slot.itemId,
                        characterId = slot.characterId,
                        name = line.character.name,
                        priceMinor = slot.priceMinor,
                        qty = line.quantity,
                    )
                totalMinor += slot.priceMinor * line.quantity
                continue
            }
            val text = config.textCharacter
            if (text != null && text.enabled && isTextTipCharacter(line.character.name)) {
                if (line.quantity != 1 || textLine != null) {
                    return PlanResult.Failed("Text tip quantity is invalid for this event.")
                }
                textLine = TipQuoteTextLineRequest(priceMinor = text.priceMinor, qty = 1)
                totalMinor += text.priceMinor
                continue
            }
            return PlanResult.Failed(
                "A cart item is not in the published tip configuration. Refresh tips and try again.",
            )
        }

        if (quoteLines.isEmpty() && textLine == null) {
            return PlanResult.Failed("Could not build a tip quote from this cart.")
        }

        return PlanResult.Ready(
            PlannedQuote(
                request =
                    CreateTipQuoteRequest(
                        eventId = cart.eventId.trim(),
                        surface = surface,
                        configVersion = version,
                        lines = quoteLines,
                        textLine = textLine,
                    ),
                expectedTotalMinor = totalMinor,
            ),
        )
    }

    private fun isTextTipCharacter(name: String): Boolean {
        val n = name.trim().lowercase()
        return n == "comment" || n == "add comment" || n.contains("comment")
    }
}
