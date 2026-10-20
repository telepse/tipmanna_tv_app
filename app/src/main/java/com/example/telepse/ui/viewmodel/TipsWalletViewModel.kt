package com.example.telepse.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.telepse.data.model.TipCharacterCatalogItem
import com.example.telepse.data.model.WalletDebitCharacterLine
import com.example.telepse.data.model.WalletDebitRequest
import com.example.telepse.data.model.WalletLedgerRow
import com.example.telepse.analytics.TvAnalyticsClient
import com.example.telepse.analytics.TvAnalyticsEventFactory
import com.example.telepse.analytics.TvAnalyticsSurfaces
import com.example.telepse.data.auth.SessionExpiryHandler
import com.example.telepse.data.repository.WalletOperations
import com.example.telepse.tips.WalletDebitFailureClassifier
import com.example.telepse.tips.WalletDebitFailureClassification
import com.example.telepse.tips.WalletDebitFingerprint
import com.example.telepse.tips.WalletDebitSubmitMessages
import com.example.telepse.tips.WalletDebitPendingConflictException
import com.example.telepse.tips.WalletDebitSubmitSession
import com.example.telepse.tips.WalletTipValidation
import com.example.telepse.tips.WalletUserFacingErrors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

enum class TipsWalletPhase {
    Loading,
    Ready,
    Error,
}

enum class TipSubmitPhase {
    Idle,
    Submitting,
    Success,
    Error,
}

data class TipsWalletUiState(
    val phase: TipsWalletPhase = TipsWalletPhase.Loading,
    val balance: String = "—",
    val walletExists: Boolean = false,
    val walletStatus: String? = null,
    val transactions: List<WalletLedgerRow> = emptyList(),
    val eventIdInput: String = "",
    val pinInput: String = "",
    val catalogCharacters: List<TipCharacterCatalogItem> = emptyList(),
    val selectedCharacterId: String? = null,
    val catalogError: String? = null,
    val loadingCatalog: Boolean = false,
    val tipSubmitPhase: TipSubmitPhase = TipSubmitPhase.Idle,
    val tipResultMessage: String? = null,
    val errorMessage: String? = null,
    /** True while [WalletDebitSubmitSession] holds a durable unresolved debit. */
    val hasUnresolvedPendingDebit: Boolean = false,
    val pendingDebitEventId: String? = null,
    val pendingDebitCharacterId: String? = null,
    val pendingDebitAmount: Double? = null,
    /** Shown briefly after explicit discard (local-only). */
    val pendingDiscardAcknowledgement: String? = null,
)

@HiltViewModel
class TipsWalletViewModel @Inject constructor(
    private val walletOperations: WalletOperations,
    private val sessionExpiryHandler: SessionExpiryHandler,
    private val analytics: TvAnalyticsClient,
    private val debitSubmitSession: WalletDebitSubmitSession,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TipsWalletUiState())
    val uiState: StateFlow<TipsWalletUiState> = _uiState.asStateFlow()

    private var catalogInFlight = false

    init {
        analytics.setContext(surface = TvAnalyticsSurfaces.KOTLIN_TV)
        applyRestoredPendingToUi()
    }

    fun refreshAll() {
        _uiState.update { it.copy(phase = TipsWalletPhase.Loading, errorMessage = null) }
        viewModelScope.launch {
            val walletResult = walletOperations.fetchWallet()
            val txResult = walletOperations.fetchTransactions()
            walletResult
                .onSuccess { wallet ->
                    _uiState.update {
                        it.copy(
                            phase = TipsWalletPhase.Ready,
                            balance = wallet.balance,
                            walletExists = wallet.exists,
                            walletStatus = wallet.status,
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            phase = TipsWalletPhase.Error,
                            errorMessage = mapError(e),
                        )
                    }
                }
            txResult.onSuccess { tx ->
                _uiState.update { it.copy(transactions = tx.items) }
            }
        }
    }

    fun onEventIdChange(value: String) {
        val pending = debitSubmitSession.pendingWalletDebit
        val normalized = WalletTipValidation.normalizeEventId(value)
        if (pending != null && normalized != null && normalized != pending.identity.eventId) {
            _uiState.update {
                syncPendingRecovery(
                    it.copy(
                        eventIdInput = pending.identity.eventId,
                        tipSubmitPhase = TipSubmitPhase.Error,
                        tipResultMessage = WalletDebitSubmitMessages.PENDING_IDENTITY_LOCKED,
                    ),
                )
            }
            return
        }
        _uiState.update { syncPendingRecovery(it.copy(eventIdInput = value, catalogError = null)) }
    }

    fun onPinChange(value: String) {
        val digits = value.filter { it.isDigit() }.take(4)
        _uiState.update { it.copy(pinInput = digits) }
    }

    fun loadCharacterCatalog() {
        if (catalogInFlight) return
        val eventId = WalletTipValidation.normalizeEventId(_uiState.value.eventIdInput)
        if (eventId == null) {
            _uiState.update { it.copy(catalogError = "Enter a valid event UUID.") }
            return
        }
        catalogInFlight = true
        _uiState.update { it.copy(loadingCatalog = true, catalogError = null, catalogCharacters = emptyList()) }
        viewModelScope.launch {
            walletOperations
                .fetchCharacterCatalog(eventId)
                .onSuccess { catalog ->
                    trackWalletEvent("catalog_loaded", eventId)
                    val flat =
                        catalog.characterSets.flatMap { set ->
                            set.characters
                        }
                    val pending = debitSubmitSession.pendingWalletDebit
                    val selectedId =
                        pending?.identity?.characterId?.takeIf { id -> flat.any { ch -> ch.id == id } }
                            ?: flat.firstOrNull()?.id
                    _uiState.update {
                        syncPendingRecovery(
                            it.copy(
                                loadingCatalog = false,
                                catalogCharacters = flat,
                                selectedCharacterId = selectedId,
                                catalogError =
                                    if (flat.isEmpty()) {
                                        "No tip characters configured for this event."
                                    } else {
                                        null
                                    },
                            ),
                        )
                    }
                }
                .onFailure { e ->
                    trackWalletEvent("catalog_failed", eventId)
                    _uiState.update {
                        it.copy(
                            loadingCatalog = false,
                            catalogError = mapError(e),
                        )
                    }
                }
            catalogInFlight = false
        }
    }

    fun selectCharacter(characterId: String) {
        val pending = debitSubmitSession.pendingWalletDebit
        if (pending != null && characterId != pending.identity.characterId) {
            _uiState.update {
                syncPendingRecovery(
                    it.copy(
                        selectedCharacterId = pending.identity.characterId,
                        tipSubmitPhase = TipSubmitPhase.Error,
                        tipResultMessage = WalletDebitSubmitMessages.PENDING_IDENTITY_LOCKED,
                    ),
                )
            }
            return
        }
        _uiState.update { syncPendingRecovery(it.copy(selectedCharacterId = characterId)) }
    }

    fun submitWalletTip() {
        if (!debitSubmitSession.tryBeginSubmit()) return
        val snapshot = _uiState.value
        val eventId = WalletTipValidation.normalizeEventId(snapshot.eventIdInput)
        val character =
            snapshot.catalogCharacters.find { it.id == snapshot.selectedCharacterId }
        when {
            eventId == null -> {
                debitSubmitSession.finishSubmit()
                _uiState.update { it.copy(tipSubmitPhase = TipSubmitPhase.Error, tipResultMessage = "Invalid event ID.") }
                return
            }
            character == null -> {
                debitSubmitSession.finishSubmit()
                _uiState.update { it.copy(tipSubmitPhase = TipSubmitPhase.Error, tipResultMessage = "Select a tip item.") }
                return
            }
            !WalletTipValidation.isValidPin(snapshot.pinInput) -> {
                debitSubmitSession.finishSubmit()
                _uiState.update { it.copy(tipSubmitPhase = TipSubmitPhase.Error, tipResultMessage = "Enter your 4-digit wallet PIN.") }
                return
            }
        }
        val fingerprint =
            WalletDebitFingerprint(
                eventId = eventId,
                characterId = character.id,
                amount = character.price,
                pin = snapshot.pinInput,
            )
        val plan =
            try {
                debitSubmitSession.planSubmit(fingerprint) {
                    val timeLabel =
                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                    WalletDebitRequest(
                        pin = snapshot.pinInput,
                        amount = character.price,
                        eventId = eventId,
                        characters =
                            listOf(
                                WalletDebitCharacterLine(
                                    name = character.name,
                                    time = timeLabel,
                                    price = character.price,
                                    characterId = character.id,
                                ),
                            ),
                    )
                }
            } catch (e: WalletDebitPendingConflictException) {
                debitSubmitSession.finishSubmit()
                _uiState.update {
                    syncPendingRecovery(
                        it.copy(
                            tipSubmitPhase = TipSubmitPhase.Error,
                            tipResultMessage = e.message,
                        ),
                    )
                }
                return
            }
        _uiState.update {
            syncPendingRecovery(
                it.copy(tipSubmitPhase = TipSubmitPhase.Submitting, tipResultMessage = null, pendingDiscardAcknowledgement = null),
            )
        }
        viewModelScope.launch {
            try {
                walletOperations
                    .debitWallet(plan.request, plan.idempotencyKey)
                    .onSuccess { response ->
                        debitSubmitSession.clearAfterAuthoritativeSuccess()
                        _uiState.update {
                            syncPendingRecovery(
                                it.copy(
                                    tipSubmitPhase = TipSubmitPhase.Success,
                                    tipResultMessage =
                                        buildString {
                                            append("Tip sent. ")
                                            response.newBalance?.let { bal -> append("Balance: £$bal") }
                                        },
                                    balance = response.newBalance ?: it.balance,
                                    pinInput = "",
                                ),
                            )
                        }
                        refreshAll()
                    }
                    .onFailure { e -> handleDebitFailure(e) }
            } catch (e: CancellationException) {
                _uiState.update {
                    syncPendingRecovery(
                        it.copy(tipSubmitPhase = TipSubmitPhase.Idle, tipResultMessage = null),
                    )
                }
                throw e
            } finally {
                debitSubmitSession.finishSubmit()
            }
        }
    }

    fun dismissUnresolvedPendingTip() {
        debitSubmitSession.dismissUnresolvedPending()
        _uiState.update {
            syncPendingRecovery(
                it.copy(
                    tipResultMessage = null,
                    tipSubmitPhase = TipSubmitPhase.Idle,
                    pendingDiscardAcknowledgement = WalletDebitSubmitMessages.PENDING_DISCARD_ACK,
                    pinInput = "",
                ),
            )
        }
    }

    private fun applyRestoredPendingToUi() {
        val pending = debitSubmitSession.pendingWalletDebit ?: return
        _uiState.update {
            syncPendingRecovery(
                it.copy(
                    eventIdInput = pending.identity.eventId,
                    selectedCharacterId = pending.identity.characterId,
                    pinInput = "",
                    tipSubmitPhase = TipSubmitPhase.Idle,
                    tipResultMessage = null,
                ),
            )
        }
    }

    private fun syncPendingRecovery(state: TipsWalletUiState): TipsWalletUiState {
        val pending = debitSubmitSession.pendingWalletDebit
        return if (pending == null) {
            state.copy(
                hasUnresolvedPendingDebit = false,
                pendingDebitEventId = null,
                pendingDebitCharacterId = null,
                pendingDebitAmount = null,
            )
        } else {
            state.copy(
                hasUnresolvedPendingDebit = true,
                pendingDebitEventId = pending.identity.eventId,
                pendingDebitCharacterId = pending.identity.characterId,
                pendingDebitAmount = pending.identity.amount,
            )
        }
    }

    private fun handleDebitFailure(e: Throwable) {
        val classification = WalletDebitFailureClassifier.classify(e)
        if (classification is WalletDebitFailureClassification.Cancelled) {
            return
        }
        if (classification is WalletDebitFailureClassification.SessionAuth) {
            sessionExpiryHandler.invalidateSessionIfNeeded(e)
        }
        val message =
            WalletDebitSubmitMessages.messageFor(classification, e)
                ?: mapError(e)
        _uiState.update {
            syncPendingRecovery(
                it.copy(
                    tipSubmitPhase = TipSubmitPhase.Error,
                    tipResultMessage = message,
                    pinInput = if (debitSubmitSession.pendingWalletDebit != null) "" else it.pinInput,
                ),
            )
        }
    }

    fun clearTipResult() {
        _uiState.update {
            syncPendingRecovery(it.copy(tipSubmitPhase = TipSubmitPhase.Idle, tipResultMessage = null))
        }
    }

    private fun mapError(e: Throwable): String {
        sessionExpiryHandler.invalidateSessionIfNeeded(e)
        return WalletUserFacingErrors.messageFor(e)
    }

    private fun trackWalletEvent(outcome: String, eventId: String?) {
        analytics.track(
            TvAnalyticsEventFactory.userInteraction(
                ts = System.currentTimeMillis(),
                kind = "click",
                detail = "wallet_$outcome${eventId?.let { ":$it" } ?: ""}",
            ),
        )
    }

    override fun onCleared() {
        analytics.flushAsync()
        super.onCleared()
    }
}
