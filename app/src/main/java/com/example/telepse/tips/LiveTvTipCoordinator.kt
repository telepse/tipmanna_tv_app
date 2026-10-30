package com.example.telepse.tips

import com.example.telepse.data.model.TipCharacterCatalogItem
import com.example.telepse.data.repository.WalletOperations
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LiveTvTipCatalogPhase {
    /** No programme event id yet (or waiting for first schedule). */
    NoEvent,

    Loading,
    Ready,
    Empty,
    Error,
}

data class LiveTvTipUiState(
    val catalogPhase: LiveTvTipCatalogPhase = LiveTvTipCatalogPhase.NoEvent,
    val eventId: String = "",
    val stripCharacters: List<TipCharacterCatalogItem> = emptyList(),
    val drawerSets: List<TipDrawerSet> = emptyList(),
    val cart: TipCartState = TipCartState(),
    val showDrawer: Boolean = false,
    val showStrip: Boolean = false,
    val statusMessage: String? = null,
    val checkout: TipCheckoutChoices =
        TipCheckoutChoices.forCart(cartReady = false, loggedIn = false),
    /** True when catalog UI is interactive (Ready with characters). */
    val canSelectTips: Boolean = false,
)

/**
 * Loads LIVE tip catalog and owns cart/drawer state without touching the player surface.
 */
class LiveTvTipCoordinator(
    private val walletOperations: WalletOperations,
    private val isLoggedIn: () -> Boolean,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(LiveTvTipUiState())
    val state: StateFlow<LiveTvTipUiState> = _state.asStateFlow()

    private var catalogJob: Job? = null
    private var boundEventId: String = ""
    private var catalogIds: Set<String> = emptySet()

    fun onProgrammeEventId(eventId: String?) {
        val trimmed = eventId?.trim().orEmpty()
        if (trimmed == boundEventId) return
        boundEventId = trimmed
        catalogJob?.cancel()
        catalogIds = emptySet()

        if (trimmed.isEmpty()) {
            _state.value =
                LiveTvTipUiState(
                    catalogPhase = LiveTvTipCatalogPhase.NoEvent,
                    eventId = "",
                    statusMessage = "Tip catalog unavailable — this programme has no event id.",
                    showStrip = true,
                    checkout = TipCheckoutChoices.forCart(false, isLoggedIn()),
                )
            return
        }

        _state.update {
            LiveTvTipUiState(
                catalogPhase = LiveTvTipCatalogPhase.Loading,
                eventId = trimmed,
                cart = TipCartState(eventId = trimmed),
                showStrip = true,
                showDrawer = false,
                statusMessage = "Loading tips…",
                checkout = TipCheckoutChoices.forCart(false, isLoggedIn()),
            )
        }
        loadCatalog(trimmed)
    }

    fun refreshCatalog() {
        val eventId = boundEventId
        if (eventId.isEmpty()) return
        catalogJob?.cancel()
        _state.update {
            it.copy(
                catalogPhase = LiveTvTipCatalogPhase.Loading,
                statusMessage = "Loading tips…",
            )
        }
        loadCatalog(eventId)
    }

    fun toggleStrip() {
        _state.update { it.copy(showStrip = !it.showStrip) }
    }

    fun openDrawer() {
        _state.update { it.copy(showDrawer = true) }
    }

    fun closeDrawer() {
        _state.update { it.copy(showDrawer = false) }
    }

    fun selectCharacter(characterId: String) {
        val character = findCharacter(characterId) ?: return
        val eventId = boundEventId
        if (eventId.isEmpty()) return
        _state.update { current ->
            if (!current.canSelectTips) return@update current
            val nextCart = current.cart.add(character, eventId, catalogIds)
            current.copy(
                cart = nextCart,
                showDrawer = true,
                checkout = TipCheckoutChoices.forCart(!nextCart.isEmpty, isLoggedIn()),
            )
        }
    }

    fun increment(characterId: String) {
        mutateQuantity(characterId) { it + 1 }
    }

    fun decrement(characterId: String) {
        mutateQuantity(characterId) { it - 1 }
    }

    fun removeFromCart(characterId: String) {
        _state.update { current ->
            val nextCart = current.cart.remove(characterId)
            current.copy(
                cart = nextCart,
                checkout = TipCheckoutChoices.forCart(!nextCart.isEmpty, isLoggedIn()),
            )
        }
    }

    fun clearCart() {
        _state.update { current ->
            val nextCart = current.cart.clear()
            current.copy(
                cart = nextCart,
                checkout = TipCheckoutChoices.forCart(false, isLoggedIn()),
            )
        }
    }

    fun refreshAuthCheckout() {
        _state.update { current ->
            current.copy(
                checkout =
                    TipCheckoutChoices.forCart(
                        cartReady = !current.cart.isEmpty,
                        loggedIn = isLoggedIn(),
                    ),
            )
        }
    }

    fun reset() {
        catalogJob?.cancel()
        boundEventId = ""
        catalogIds = emptySet()
        _state.value = LiveTvTipUiState()
    }

    private fun mutateQuantity(characterId: String, transform: (Int) -> Int) {
        _state.update { current ->
            if (!current.canSelectTips && current.cart.quantityOf(characterId) == 0) {
                return@update current
            }
            val qty = transform(current.cart.quantityOf(characterId))
            val nextCart = current.cart.setQuantity(characterId, qty, catalogIds)
            current.copy(
                cart = nextCart,
                checkout = TipCheckoutChoices.forCart(!nextCart.isEmpty, isLoggedIn()),
            )
        }
    }

    private fun findCharacter(characterId: String): TipCharacterCatalogItem? {
        val state = _state.value
        state.stripCharacters.firstOrNull { it.id == characterId }?.let { return it }
        state.drawerSets.forEach { set ->
            set.characters.firstOrNull { it.id == characterId }?.let { return it }
        }
        state.cart.lines.firstOrNull { it.character.id == characterId }?.let { return it.character }
        return null
    }

    private fun loadCatalog(eventId: String) {
        catalogJob =
            scope.launch {
                try {
                    val result = walletOperations.fetchCharacterCatalog(eventId)
                    if (boundEventId != eventId) return@launch
                    result.fold(
                        onSuccess = { response ->
                            val responseEvent = response.eventId.trim()
                            if (responseEvent.isNotEmpty() && responseEvent != eventId) {
                                // Guard against mismatched payload; treat as unavailable.
                                applyEmptyOrError(
                                    eventId = eventId,
                                    empty = true,
                                    message = "Tip catalog unavailable for this programme.",
                                )
                                return@fold
                            }
                            val presentation = TipDrawerOrdering.present(response.characterSets)
                            catalogIds =
                                presentation.drawerSets
                                    .flatMap { it.characters }
                                    .map { it.id }
                                    .toSet()
                            if (presentation.stripCharacters.isEmpty()) {
                                applyEmptyOrError(
                                    eventId = eventId,
                                    empty = true,
                                    message = "No tip characters are published for this event.",
                                )
                            } else {
                                _state.update { current ->
                                    val retained =
                                        current.cart
                                            .withEventClearedIfDifferent(eventId)
                                            .retainAvailable(catalogIds)
                                    current.copy(
                                        catalogPhase = LiveTvTipCatalogPhase.Ready,
                                        eventId = eventId,
                                        stripCharacters = presentation.stripCharacters,
                                        drawerSets = presentation.drawerSets,
                                        cart = retained,
                                        statusMessage = null,
                                        canSelectTips = true,
                                        showStrip = true,
                                        checkout =
                                            TipCheckoutChoices.forCart(
                                                cartReady = !retained.isEmpty,
                                                loggedIn = isLoggedIn(),
                                            ),
                                    )
                                }
                            }
                        },
                        onFailure = { error ->
                            if (error is CancellationException) throw error
                            applyEmptyOrError(
                                eventId = eventId,
                                empty = false,
                                message =
                                    error.message?.takeIf { it.isNotBlank() }
                                        ?: "Could not load tip catalog.",
                            )
                        },
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    if (boundEventId != eventId) return@launch
                    applyEmptyOrError(
                        eventId = eventId,
                        empty = false,
                        message = e.message?.takeIf { it.isNotBlank() } ?: "Could not load tip catalog.",
                    )
                }
            }
    }

    private fun applyEmptyOrError(
        eventId: String,
        empty: Boolean,
        message: String,
    ) {
        catalogIds = emptySet()
        _state.update {
            LiveTvTipUiState(
                catalogPhase =
                    if (empty) LiveTvTipCatalogPhase.Empty else LiveTvTipCatalogPhase.Error,
                eventId = eventId,
                cart = TipCartState(eventId = eventId),
                showStrip = true,
                showDrawer = false,
                statusMessage = message,
                canSelectTips = false,
                checkout = TipCheckoutChoices.forCart(false, isLoggedIn()),
            )
        }
    }
}
