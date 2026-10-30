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
    val walletPay: LiveTvWalletPayState = LiveTvWalletPayState(),
)

/**
 * Loads LIVE tip catalog and owns cart/drawer/wallet-checkout state without touching the player surface.
 */
class LiveTvTipCoordinator(
    private val walletOperations: WalletOperations,
    private val debitSubmitSession: WalletDebitSubmitSession,
    private val isLoggedIn: () -> Boolean,
    private val scope: CoroutineScope,
    private val onSessionAuthFailure: (Throwable) -> Unit = {},
) {
    private val _state = MutableStateFlow(LiveTvTipUiState())
    val state: StateFlow<LiveTvTipUiState> = _state.asStateFlow()

    private var catalogJob: Job? = null
    private var walletJob: Job? = null
    private var boundEventId: String = ""
    private var catalogIds: Set<String> = emptySet()
    /** Frozen cart lines key + amount for the in-progress payment attempt. */
    private var payCartSnapshot: TipCartState? = null
    private var payQuoteId: String? = null
    private var payAmountGbp: Double? = null

    fun onProgrammeEventId(eventId: String?) {
        val trimmed = eventId?.trim().orEmpty()
        if (trimmed == boundEventId) return
        boundEventId = trimmed
        catalogJob?.cancel()
        cancelWalletJob()
        clearPayAttemptLocals()
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
        if (_state.value.walletPay.phase == LiveTvWalletPayPhase.Submitting) return
        cancelWalletCheckout(keepPending = true)
        _state.update { it.copy(showDrawer = false) }
    }

    fun selectCharacter(characterId: String) {
        if (_state.value.walletPay.blocksCartMutation) return
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
        if (_state.value.walletPay.blocksCartMutation) return
        _state.update { current ->
            val nextCart = current.cart.remove(characterId)
            current.copy(
                cart = nextCart,
                checkout = TipCheckoutChoices.forCart(!nextCart.isEmpty, isLoggedIn()),
            )
        }
    }

    fun clearCart() {
        if (_state.value.walletPay.blocksCartMutation) return
        _state.update { current ->
            val nextCart = current.cart.clear()
            current.copy(
                cart = nextCart,
                checkout = TipCheckoutChoices.forCart(false, isLoggedIn()),
            )
        }
    }

    /** Begin wallet checkout from the tip drawer (PIN entry after prerequisite checks). */
    fun startWalletCheckout() {
        val current = _state.value
        if (!current.checkout.canEnterWalletCheckout) return
        if (current.walletPay.phase == LiveTvWalletPayPhase.Submitting) return
        if (!isLoggedIn()) {
            _state.update {
                it.copy(
                    walletPay =
                        LiveTvWalletPayState(
                            phase = LiveTvWalletPayPhase.Failed,
                            message = "Sign in as a tipper to use wallet debit.",
                        ),
                )
            }
            return
        }
        val cart = current.cart
        if (cart.isEmpty || cart.eventId.isBlank()) return
        if (WalletTipValidation.normalizeEventId(cart.eventId) == null) {
            _state.update {
                it.copy(
                    walletPay =
                        LiveTvWalletPayState(
                            phase = LiveTvWalletPayPhase.Failed,
                            message = "This programme event id is not a valid tip target.",
                            provisionalDisplayTotal = cart.provisionalTotal,
                        ),
                )
            }
            return
        }

        // Pending recovery for a different cart identity must not silently start a new tip.
        val pending = debitSubmitSession.pendingWalletDebit
        if (pending != null) {
            val pendingKey = pending.identity.characterId
            val cartKey = LiveTvCartDebitMapper.cartLinesKey(cart)
            if (pending.identity.eventId != cart.eventId || pendingKey != cartKey) {
                _state.update {
                    it.copy(
                        walletPay =
                            LiveTvWalletPayState(
                                phase = LiveTvWalletPayPhase.Failed,
                                message = WalletDebitSubmitMessages.PENDING_IDENTITY_LOCKED,
                                hasUnresolvedPending = true,
                                provisionalDisplayTotal = cart.provisionalTotal,
                            ),
                    )
                }
                return
            }
        }

        payCartSnapshot = cart
        payQuoteId = null
        payAmountGbp = null
        cancelWalletJob()
        _state.update {
            it.copy(
                showDrawer = true,
                walletPay =
                    LiveTvWalletPayState(
                        phase = LiveTvWalletPayPhase.CheckingPrerequisites,
                        message = "Checking wallet…",
                        provisionalDisplayTotal = cart.provisionalTotal,
                        hasUnresolvedPending = pending != null,
                        pinDigits = "",
                    ),
            )
        }
        walletJob =
            scope.launch {
                try {
                    val walletResult = walletOperations.fetchWallet()
                    if (boundEventId != cart.eventId) return@launch
                    walletResult
                        .onSuccess { wallet ->
                            val status = wallet.status?.trim()?.uppercase()
                            when {
                                !wallet.exists -> {
                                    setWalletPayFailed(
                                        "Wallet is not activated. Activate it from Tips & wallet.",
                                        wallet,
                                    )
                                }
                                status == "DISABLED" -> {
                                    setWalletPayFailed("This tipper wallet is disabled.", wallet)
                                }
                                status == "LOCKED" -> {
                                    setWalletPayFailed(
                                        "Wallet PIN is temporarily locked. Try again later.",
                                        wallet,
                                    )
                                }
                                status != null && status != "ACTIVE" && status.isNotEmpty() -> {
                                    setWalletPayFailed(
                                        "Wallet is not ready for tipping (status: $status).",
                                        wallet,
                                    )
                                }
                                else -> {
                                    _state.update { s ->
                                        s.copy(
                                            walletPay =
                                                LiveTvWalletPayState(
                                                    phase = LiveTvWalletPayPhase.EnterPin,
                                                    message =
                                                        if (pending != null) {
                                                            WalletDebitSubmitMessages.PENDING_RECOVERY_HINT
                                                        } else {
                                                            "Enter your 4-digit wallet PIN to confirm."
                                                        },
                                                    walletBalance = wallet.balance,
                                                    walletExists = wallet.exists,
                                                    walletStatus = wallet.status,
                                                    provisionalDisplayTotal = cart.provisionalTotal,
                                                    hasUnresolvedPending = pending != null,
                                                    pinDigits = "",
                                                ),
                                        )
                                    }
                                }
                            }
                        }
                        .onFailure { e ->
                            if (e is CancellationException) throw e
                            onSessionAuthFailure(e)
                            setWalletPayFailed(WalletUserFacingErrors.messageFor(e))
                        }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    onSessionAuthFailure(e)
                    setWalletPayFailed(WalletUserFacingErrors.messageFor(e))
                }
            }
    }

    fun onWalletPinDigit(digit: Char) {
        if (digit !in '0'..'9') return
        _state.update { current ->
            if (current.walletPay.phase != LiveTvWalletPayPhase.EnterPin &&
                current.walletPay.phase != LiveTvWalletPayPhase.Failed &&
                current.walletPay.phase != LiveTvWalletPayPhase.UnconfirmedPending
            ) {
                return@update current
            }
            val next = (current.walletPay.pinDigits + digit).filter { it.isDigit() }.take(4)
            current.copy(
                walletPay =
                    current.walletPay.copy(
                        pinDigits = next,
                        phase =
                            if (current.walletPay.phase == LiveTvWalletPayPhase.Failed ||
                                current.walletPay.phase == LiveTvWalletPayPhase.UnconfirmedPending
                            ) {
                                LiveTvWalletPayPhase.EnterPin
                            } else {
                                current.walletPay.phase
                            },
                        message =
                            current.walletPay.message
                                ?.takeIf {
                                    current.walletPay.phase != LiveTvWalletPayPhase.Failed
                                }
                                ?: "Enter your 4-digit wallet PIN to confirm.",
                    ),
            )
        }
    }

    fun onWalletPinBackspace() {
        _state.update { current ->
            if (!current.walletPay.isActive) return@update current
            current.copy(
                walletPay = current.walletPay.copy(pinDigits = current.walletPay.pinDigits.dropLast(1)),
            )
        }
    }

    fun clearWalletPin() {
        _state.update { current ->
            current.copy(walletPay = current.walletPay.copy(pinDigits = ""))
        }
    }

    fun submitWalletCheckout() {
        val snapshot = _state.value
        val cart = payCartSnapshot ?: snapshot.cart
        if (cart.isEmpty) {
            setWalletPayFailed("Cart is empty.")
            return
        }
        if (LiveTvCartDebitMapper.cartLinesKey(cart) != LiveTvCartDebitMapper.cartLinesKey(snapshot.cart) ||
            cart.eventId != snapshot.cart.eventId
        ) {
            setWalletPayFailed("Cart changed during checkout. Review your tips and try again.")
            clearPayAttemptLocals()
            return
        }
        val pin = snapshot.walletPay.pinDigits
        if (!WalletTipValidation.isValidPin(pin)) {
            _state.update {
                it.copy(
                    walletPay =
                        it.walletPay.copy(
                            phase = LiveTvWalletPayPhase.EnterPin,
                            message = "Enter your 4-digit wallet PIN.",
                        ),
                )
            }
            return
        }
        if (!debitSubmitSession.tryBeginSubmit()) return

        cancelWalletJob()
        _state.update {
            it.copy(
                walletPay =
                    it.walletPay.copy(
                        phase = LiveTvWalletPayPhase.Submitting,
                        message = "Submitting tip…",
                    ),
            )
        }
        walletJob =
            scope.launch {
                try {
                    val amountAndQuote = resolveAmountAndQuote(cart)
                    if (amountAndQuote == null) {
                        debitSubmitSession.finishSubmit()
                        return@launch
                    }
                    val (amountGbp, tipQuoteId) = amountAndQuote
                    payAmountGbp = amountGbp
                    payQuoteId = tipQuoteId
                    val fingerprint =
                        LiveTvCartDebitMapper.fingerprint(cart, pin, amountGbp)
                    val plan =
                        try {
                            debitSubmitSession.planSubmit(fingerprint) {
                                LiveTvCartDebitMapper.buildRequest(
                                    cart = cart,
                                    pin = pin,
                                    amountGbp = amountGbp,
                                    tipQuoteId = tipQuoteId,
                                )
                            }
                        } catch (e: WalletDebitPendingConflictException) {
                            debitSubmitSession.finishSubmit()
                            setWalletPayFailed(e.message ?: WalletDebitSubmitMessages.PENDING_IDENTITY_LOCKED)
                            return@launch
                        }

                    _state.update {
                        it.copy(
                            walletPay =
                                it.walletPay.copy(
                                    quoteLockedTotalGbp = tipQuoteId?.let { amountGbp },
                                    provisionalDisplayTotal = cart.provisionalTotal,
                                    message = "Submitting tip…",
                                ),
                        )
                    }

                    walletOperations
                        .debitWallet(plan.request, plan.idempotencyKey)
                        .onSuccess { response ->
                            debitSubmitSession.clearAfterAuthoritativeSuccess()
                            clearPayAttemptLocals()
                            _state.update { current ->
                                val cleared = current.cart.clear()
                                current.copy(
                                    cart = cleared,
                                    checkout = TipCheckoutChoices.forCart(false, isLoggedIn()),
                                    walletPay =
                                        LiveTvWalletPayState(
                                            phase = LiveTvWalletPayPhase.Success,
                                            message =
                                                buildString {
                                                    append("Tip sent.")
                                                    response.newBalance?.let { bal ->
                                                        append(" Balance: £$bal")
                                                    }
                                                },
                                            newBalance = response.newBalance,
                                            transactionId = response.transactionId,
                                            walletBalance = response.newBalance,
                                            quoteLockedTotalGbp = amountGbp,
                                            provisionalDisplayTotal = cart.provisionalTotal,
                                        ),
                                )
                            }
                        }
                        .onFailure { e -> handleWalletDebitFailure(e) }
                } catch (e: CancellationException) {
                    _state.update {
                        it.copy(
                            walletPay =
                                it.walletPay.copy(
                                    phase = LiveTvWalletPayPhase.EnterPin,
                                    message = "Cancelled.",
                                    pinDigits = "",
                                ),
                        )
                    }
                    throw e
                } catch (e: Throwable) {
                    handleWalletDebitFailure(e)
                } finally {
                    debitSubmitSession.finishSubmit()
                }
            }
    }

    fun cancelWalletCheckout(keepPending: Boolean = true) {
        if (_state.value.walletPay.phase == LiveTvWalletPayPhase.Submitting) return
        cancelWalletJob()
        if (!keepPending) {
            debitSubmitSession.dismissUnresolvedPending()
        }
        clearPayAttemptLocals()
        _state.update {
            it.copy(walletPay = LiveTvWalletPayState(hasUnresolvedPending = debitSubmitSession.pendingWalletDebit != null))
        }
    }

    fun dismissWalletPendingRecovery() {
        debitSubmitSession.dismissUnresolvedPending()
        clearPayAttemptLocals()
        _state.update {
            it.copy(
                walletPay =
                    LiveTvWalletPayState(
                        phase = LiveTvWalletPayPhase.Idle,
                        message = WalletDebitSubmitMessages.PENDING_DISCARD_ACK,
                    ),
            )
        }
    }

    fun acknowledgeWalletSuccess() {
        clearPayAttemptLocals()
        _state.update { it.copy(walletPay = LiveTvWalletPayState()) }
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
        cancelWalletJob()
        clearPayAttemptLocals()
        boundEventId = ""
        catalogIds = emptySet()
        _state.value = LiveTvTipUiState()
    }

    private fun mutateQuantity(characterId: String, transform: (Int) -> Int) {
        if (_state.value.walletPay.blocksCartMutation) return
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

    private suspend fun resolveAmountAndQuote(cart: TipCartState): Pair<Double, String?>? {
        // Retry path: reuse frozen snapshot amount + tipQuoteId (same logical payment).
        val pending = debitSubmitSession.pendingWalletDebit
        if (pending != null &&
            pending.identity.eventId == cart.eventId &&
            pending.identity.characterId == LiveTvCartDebitMapper.cartLinesKey(cart)
        ) {
            return pending.identity.amount to pending.requestSnapshot.tipQuoteId
        }

        val tipConfigResult = walletOperations.fetchActiveTipConfig(cart.eventId)
        val tipConfig =
            tipConfigResult.getOrElse { e ->
                onSessionAuthFailure(e)
                setWalletPayFailed(WalletUserFacingErrors.messageFor(e))
                return null
            }
        when (val plan = LiveTvTipQuotePlanner.plan(cart, tipConfig)) {
            is LiveTvTipQuotePlanner.PlanResult.LegacyNoQuoteRequired -> {
                val amount = cart.provisionalTotal
                if (amount < 1.0) {
                    setWalletPayFailed("Tip amount must be at least £1.")
                    return null
                }
                return amount to null
            }
            is LiveTvTipQuotePlanner.PlanResult.Failed -> {
                setWalletPayFailed(plan.message)
                return null
            }
            is LiveTvTipQuotePlanner.PlanResult.Ready -> {
                val quoteResult = walletOperations.createTipQuote(plan.planned.request)
                val quote =
                    quoteResult.getOrElse { e ->
                        onSessionAuthFailure(e)
                        setWalletPayFailed(WalletUserFacingErrors.messageFor(e))
                        return null
                    }
                if (quote.quoteId.isBlank() || quote.totalMinor <= 0) {
                    setWalletPayFailed("Could not lock tip total with the server.")
                    return null
                }
                if (quote.totalMinor != plan.planned.expectedTotalMinor) {
                    setWalletPayFailed("Quoted tip total did not match the cart. Refresh and try again.")
                    return null
                }
                val amountGbp = quote.totalMinor / 100.0
                return amountGbp to quote.quoteId
            }
        }
    }

    private fun handleWalletDebitFailure(e: Throwable) {
        val classification = WalletDebitFailureClassifier.classify(e)
        if (classification is WalletDebitFailureClassification.Cancelled) return
        if (classification is WalletDebitFailureClassification.SessionAuth) {
            onSessionAuthFailure(e)
        }
        val message =
            WalletDebitSubmitMessages.messageFor(classification, e)
                ?: WalletUserFacingErrors.messageFor(e)
        val phase =
            when (classification) {
                WalletDebitFailureClassification.UnknownOutcome -> LiveTvWalletPayPhase.UnconfirmedPending
                else -> LiveTvWalletPayPhase.Failed
            }
        _state.update {
            it.copy(
                walletPay =
                    it.walletPay.copy(
                        phase = phase,
                        message = message,
                        pinDigits = "",
                        hasUnresolvedPending = debitSubmitSession.pendingWalletDebit != null,
                    ),
            )
        }
    }

    private fun setWalletPayFailed(
        message: String,
        wallet: com.example.telepse.data.model.WalletStatusResponse? = null,
    ) {
        _state.update {
            it.copy(
                walletPay =
                    LiveTvWalletPayState(
                        phase = LiveTvWalletPayPhase.Failed,
                        message = message,
                        walletBalance = wallet?.balance ?: it.walletPay.walletBalance,
                        walletExists = wallet?.exists ?: it.walletPay.walletExists,
                        walletStatus = wallet?.status ?: it.walletPay.walletStatus,
                        provisionalDisplayTotal =
                            it.walletPay.provisionalDisplayTotal ?: it.cart.provisionalTotal,
                        hasUnresolvedPending = debitSubmitSession.pendingWalletDebit != null,
                    ),
            )
        }
    }

    private fun cancelWalletJob() {
        walletJob?.cancel()
        walletJob = null
    }

    private fun clearPayAttemptLocals() {
        payCartSnapshot = null
        payQuoteId = null
        payAmountGbp = null
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
