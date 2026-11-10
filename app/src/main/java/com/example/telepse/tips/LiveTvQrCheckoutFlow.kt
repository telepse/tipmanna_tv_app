package com.example.telepse.tips

import com.example.telepse.data.model.CreateTvTipCheckoutSessionRequest
import com.example.telepse.data.model.TvTipCheckoutSessionStatusResponse
import com.example.telepse.data.repository.WalletOperations
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * TV QR/card checkout: create backend session, show QR URL, poll authoritative status.
 */
class LiveTvQrCheckoutFlow(
    private val walletOperations: WalletOperations,
    private val scope: CoroutineScope,
    private val onSessionAuthFailure: (Throwable) -> Unit,
    private val resolveAmountAndQuote: suspend (TipCartState) -> Pair<Double, String?>?,
) {
    var state: LiveTvQrPayState = LiveTvQrPayState()
        private set

    private var pollJob: Job? = null
    private var activeSessionId: String? = null
    private var idempotencyKey: String? = null

    fun reset() {
        pollJob?.cancel()
        pollJob = null
        activeSessionId = null
        idempotencyKey = null
        state = LiveTvQrPayState()
    }

    fun start(
        cart: TipCartState,
        onState: (LiveTvQrPayState) -> Unit,
        onPaid: (TvTipCheckoutSessionStatusResponse) -> Unit,
    ) {
        if (cart.isEmpty || cart.eventId.isBlank()) return
        pollJob?.cancel()
        state =
            LiveTvQrPayState(
                phase = LiveTvQrPayPhase.CreatingSession,
                message = "Creating secure checkout…",
            )
        onState(state)
        if (idempotencyKey == null) idempotencyKey = UUID.randomUUID().toString()
        pollJob =
            scope.launch {
                try {
                    val amountQuote = resolveAmountAndQuote(cart) ?: return@launch
                    val (amountGbp, tipQuoteId) = amountQuote
                    val request =
                        CreateTvTipCheckoutSessionRequest(
                            eventId = cart.eventId.trim(),
                            amount = amountGbp,
                            characters = LiveTvCartDebitMapper.expandCharacterLines(cart),
                            tipQuoteId = tipQuoteId,
                            idempotencyKey = idempotencyKey,
                        )
                    walletOperations
                        .createTvTipCheckoutSession(request)
                        .onSuccess { response ->
                            if (response.checkoutUrl.isBlank() || response.sessionId.isBlank()) {
                                state =
                                    LiveTvQrPayState(
                                        phase = LiveTvQrPayPhase.Failed,
                                        message = "Checkout session response was incomplete.",
                                    )
                                onState(state)
                                return@onSuccess
                            }
                            activeSessionId = response.sessionId
                            state =
                                LiveTvQrPayState(
                                    phase = LiveTvQrPayPhase.AwaitingPhonePayment,
                                    message = "Scan with your phone to pay by card.",
                                    checkoutUrl = response.checkoutUrl,
                                    sessionId = response.sessionId,
                                    expiresAtIso = response.expiresAt,
                                )
                            onState(state)
                            pollUntilTerminal(response.sessionId, onState, onPaid)
                        }
                        .onFailure { e ->
                            if (e is CancellationException) throw e
                            onSessionAuthFailure(e)
                            state =
                                LiveTvQrPayState(
                                    phase = LiveTvQrPayPhase.Failed,
                                    message = WalletUserFacingErrors.messageFor(e),
                                )
                            onState(state)
                        }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    onSessionAuthFailure(e)
                    state =
                        LiveTvQrPayState(
                            phase = LiveTvQrPayPhase.Failed,
                            message = WalletUserFacingErrors.messageFor(e),
                        )
                    onState(state)
                }
            }
    }

    fun cancel(onState: (LiveTvQrPayState) -> Unit) {
        if (state.phase == LiveTvQrPayPhase.CreatingSession) return
        pollJob?.cancel()
        pollJob = null
        val sessionId = activeSessionId
        if (sessionId != null) {
            scope.launch {
                walletOperations.cancelTvTipCheckoutSession(sessionId)
            }
        }
        activeSessionId = null
        state = LiveTvQrPayState()
        onState(state)
    }

    fun acknowledgePaid(onState: (LiveTvQrPayState) -> Unit) {
        reset()
        onState(state)
    }

    private suspend fun pollUntilTerminal(
        sessionId: String,
        onState: (LiveTvQrPayState) -> Unit,
        onPaid: (TvTipCheckoutSessionStatusResponse) -> Unit,
    ) {
        while (scope.coroutineContext.isActive) {
            delay(3_000)
            val result = walletOperations.fetchTvTipCheckoutSessionStatus(sessionId)
            result
                .onSuccess { status ->
                    when (status.status.lowercase()) {
                        "paid" -> {
                            state =
                                LiveTvQrPayState(
                                    phase = LiveTvQrPayPhase.Paid,
                                    message = "Payment confirmed.",
                                    sessionId = sessionId,
                                    transactionId = status.transactionId,
                                )
                            onState(state)
                            onPaid(status)
                            pollJob?.cancel()
                            return
                        }
                        "expired" -> {
                            state =
                                LiveTvQrPayState(
                                    phase = LiveTvQrPayPhase.Expired,
                                    message = "Checkout expired. Start again from the TV cart.",
                                    sessionId = sessionId,
                                )
                            onState(state)
                            return
                        }
                        "cancelled" -> {
                            state =
                                LiveTvQrPayState(
                                    phase = LiveTvQrPayPhase.Cancelled,
                                    message = "Checkout cancelled.",
                                    sessionId = sessionId,
                                )
                            onState(state)
                            return
                        }
                        "failed" -> {
                            state =
                                LiveTvQrPayState(
                                    phase = LiveTvQrPayPhase.Failed,
                                    message = "Card payment failed.",
                                    sessionId = sessionId,
                                )
                            onState(state)
                            return
                        }
                        else -> Unit
                    }
                }
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    // Keep polling on transient errors; surface only if session gone.
                    if (e.message?.contains("404", ignoreCase = true) == true) {
                        state =
                            LiveTvQrPayState(
                                phase = LiveTvQrPayPhase.Failed,
                                message = WalletUserFacingErrors.messageFor(e),
                            )
                        onState(state)
                        return
                    }
                }
        }
    }
}
