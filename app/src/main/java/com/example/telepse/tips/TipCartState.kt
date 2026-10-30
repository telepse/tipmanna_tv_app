package com.example.telepse.tips

import com.example.telepse.data.model.TipCharacterCatalogItem

data class TipCartLine(
    val character: TipCharacterCatalogItem,
    val quantity: Int,
) {
    init {
        require(quantity > 0) { "quantity must be > 0" }
    }

    /** Presentation-only line total; backend remains authoritative at payment. */
    val lineTotal: Double get() = character.price * quantity
}

/**
 * Event-scoped tip cart. Totals are provisional UI amounts only.
 */
data class TipCartState(
    val eventId: String = "",
    val lines: List<TipCartLine> = emptyList(),
) {
    val itemCount: Int get() = lines.sumOf { it.quantity }

    /** Provisional cart total for display — not a trusted payment amount. */
    val provisionalTotal: Double get() = lines.sumOf { it.lineTotal }

    val isEmpty: Boolean get() = lines.isEmpty()

    fun quantityOf(characterId: String): Int =
        lines.firstOrNull { it.character.id == characterId }?.quantity ?: 0

    fun withEventClearedIfDifferent(newEventId: String): TipCartState {
        val trimmed = newEventId.trim()
        if (trimmed.isEmpty()) return TipCartState()
        if (eventId.isNotEmpty() && eventId != trimmed) return TipCartState(eventId = trimmed)
        return if (eventId == trimmed) this else copy(eventId = trimmed)
    }

    fun add(
        character: TipCharacterCatalogItem,
        eventId: String,
        catalogIds: Set<String>,
    ): TipCartState {
        val trimmedEvent = eventId.trim()
        require(trimmedEvent.isNotEmpty()) { "eventId required" }
        if (character.id !in catalogIds) return this
        val base = withEventClearedIfDifferent(trimmedEvent)
        val existing = base.lines.indexOfFirst { it.character.id == character.id }
        val nextLines =
            if (existing >= 0) {
                base.lines.toMutableList().also { list ->
                    val line = list[existing]
                    list[existing] = line.copy(quantity = line.quantity + 1)
                }
            } else {
                base.lines + TipCartLine(character = character, quantity = 1)
            }
        return base.copy(eventId = trimmedEvent, lines = nextLines)
    }

    fun setQuantity(
        characterId: String,
        quantity: Int,
        catalogIds: Set<String>,
    ): TipCartState {
        if (characterId !in catalogIds && quantity > 0) return this
        if (quantity <= 0) {
            return copy(lines = lines.filterNot { it.character.id == characterId })
        }
        val existing = lines.indexOfFirst { it.character.id == characterId }
        if (existing < 0) return this
        val next = lines.toMutableList()
        next[existing] = next[existing].copy(quantity = quantity)
        return copy(lines = next)
    }

    fun remove(characterId: String): TipCartState =
        copy(lines = lines.filterNot { it.character.id == characterId })

    fun clear(): TipCartState = TipCartState(eventId = eventId)

    /** Drop lines that are no longer in the catalog after a refresh. */
    fun retainAvailable(catalogIds: Set<String>): TipCartState =
        copy(lines = lines.filter { it.character.id in catalogIds })
}
