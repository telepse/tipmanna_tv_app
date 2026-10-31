package com.example.telepse.ui.livetv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.telepse.tips.LiveTvQrPayPhase
import com.example.telepse.tips.LiveTvTipUiState
import com.example.telepse.tips.LiveTvWalletPayPhase
import com.example.telepse.tips.TipQrCheckoutAvailability
import com.example.telepse.tips.TipWalletCheckoutAvailability
import com.example.telepse.ui.tv.TvAppColors
import com.example.telepse.ui.tv.TvPlayerControlChip
import com.example.telepse.ui.tv.requestInitialTvFocus

@Composable
fun LiveTvTipDrawer(
    tip: LiveTvTipUiState,
    onClose: () -> Unit,
    onIncrement: (String) -> Unit,
    onDecrement: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
    onStartWalletCheckout: () -> Unit,
    onStartQrCheckout: () -> Unit,
    onWalletPinDigit: (Char) -> Unit,
    onWalletPinBackspace: () -> Unit,
    onSubmitWalletCheckout: () -> Unit,
    onCancelWalletCheckout: () -> Unit,
    onDismissWalletPending: () -> Unit,
    onCancelQrCheckout: () -> Unit,
    onAcknowledgeWalletSuccess: () -> Unit,
    onAcknowledgeQrSuccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val closeFocus = remember { FocusRequester() }
    val payFocus = remember { FocusRequester() }
    val walletPay = tip.walletPay
    val qrPay = tip.qrPay
    val showWalletPayUi = walletPay.isActive
    val showQrPayUi = qrPay.isActive && !showWalletPayUi
    val showPayUi = showWalletPayUi || showQrPayUi

    LaunchedEffect(showPayUi) {
        if (showPayUi) {
            payFocus.requestInitialTvFocus()
        } else {
            closeFocus.requestInitialTvFocus()
        }
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color(0x99000000)),
    ) {
        Column(
            modifier =
                Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .fillMaxWidth(0.42f)
                    .background(Color(0xF012121A), RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
        ) {
            when {
                showWalletPayUi -> {
                    LiveTvWalletPayPanel(
                        tip = tip,
                        payFocus = payFocus,
                        onPinDigit = onWalletPinDigit,
                        onPinBackspace = onWalletPinBackspace,
                        onSubmit = onSubmitWalletCheckout,
                        onCancel = onCancelWalletCheckout,
                        onDismissPending = onDismissWalletPending,
                        onAcknowledgeSuccess = onAcknowledgeWalletSuccess,
                    )
                }
                showQrPayUi -> {
                    LiveTvQrPayPanel(
                        tip = tip,
                        payFocus = payFocus,
                        onCancel = onCancelQrCheckout,
                        onAcknowledgeSuccess = onAcknowledgeQrSuccess,
                    )
                }
                else -> {
                    LiveTvTipCartPanel(
                        tip = tip,
                        closeFocus = closeFocus,
                        onClose = onClose,
                        onIncrement = onIncrement,
                        onDecrement = onDecrement,
                        onRemove = onRemove,
                        onClear = onClear,
                        onStartWalletCheckout = onStartWalletCheckout,
                        onStartQrCheckout = onStartQrCheckout,
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveTvTipCartPanel(
    tip: LiveTvTipUiState,
    closeFocus: FocusRequester,
    onClose: () -> Unit,
    onIncrement: (String) -> Unit,
    onDecrement: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
    onStartWalletCheckout: () -> Unit,
    onStartQrCheckout: () -> Unit,
) {
    Text(
        text = "Tip cart",
        color = TvAppColors.TextPrimary,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
    )
    Text(
        text =
            if (tip.eventId.isNotBlank()) {
                "Event ${tip.eventId.take(8)}…"
            } else {
                "No event"
            },
        color = TvAppColors.TextSecondary,
        fontSize = 13.sp,
        modifier = Modifier.padding(bottom = 12.dp),
    )

    if (tip.cart.isEmpty) {
        Text(
            text = "Cart is empty. Select a tip from the strip.",
            color = TvAppColors.TextSecondary,
            modifier = Modifier.padding(bottom = 16.dp),
        )
    } else {
        tip.cart.lines.forEach { line ->
            val setName =
                tip.drawerSets
                    .firstOrNull { set -> set.characters.any { it.id == line.character.id } }
                    ?.name
            Column(modifier = Modifier.padding(bottom = 12.dp)) {
                Text(
                    text = line.character.name,
                    color = TvAppColors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (setName != null) {
                    Text(
                        text = setName,
                        color = TvAppColors.TextSecondary,
                        fontSize = 12.sp,
                    )
                }
                Text(
                    text =
                        "${formatTipPrice(line.character.price)} × ${line.quantity} = " +
                            formatTipPrice(line.lineTotal),
                    color = TvAppColors.TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                Row {
                    TvPlayerControlChip(
                        label = "−",
                        onClick = { onDecrement(line.character.id) },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    TvPlayerControlChip(
                        label = "+",
                        onClick = { onIncrement(line.character.id) },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    TvPlayerControlChip(
                        label = "Remove",
                        onClick = { onRemove(line.character.id) },
                    )
                }
            }
        }

        Text(
            text = "Items: ${tip.cart.itemCount}",
            color = TvAppColors.TextSecondary,
            fontSize = 14.sp,
        )
        Text(
            text = "Provisional total: ${formatTipPrice(tip.cart.provisionalTotal)}",
            color = TvAppColors.FocusRingSecondary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Text(
            text = "Final amount is confirmed by the server at payment.",
            color = TvAppColors.TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        TvPlayerControlChip(
            label = "Clear cart",
            onClick = onClear,
            modifier = Modifier.padding(bottom = 16.dp),
        )
    }

    Text(
        text = "Checkout",
        color = TvAppColors.TextPrimary,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp),
    )

    val walletLabel =
        when {
            tip.cart.isEmpty -> "Wallet — add tips first"
            tip.checkout.wallet == TipWalletCheckoutAvailability.Available -> "Pay with wallet"
            else -> "Wallet — sign in required"
        }
    TvPlayerControlChip(
        label = walletLabel,
        onClick = onStartWalletCheckout,
        enabled = tip.checkout.canEnterWalletCheckout,
        modifier = Modifier.padding(bottom = 8.dp),
    )
    if (tip.checkout.wallet == TipWalletCheckoutAvailability.RequiresSignIn &&
        !tip.cart.isEmpty
    ) {
        Text(
            text = "Sign in as a tipper to use wallet debit.",
            color = TvAppColors.TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 8.dp),
        )
    }

    val qrLabel =
        when {
            tip.cart.isEmpty -> "QR / card — add tips first"
            tip.checkout.qrCard == TipQrCheckoutAvailability.Available -> "Pay with QR / card"
            else -> "QR / card — sign in required"
        }
    TvPlayerControlChip(
        label = qrLabel,
        onClick = onStartQrCheckout,
        enabled = tip.checkout.canEnterQrCheckout,
        modifier = Modifier.padding(bottom = 8.dp),
    )
    if (tip.checkout.qrCard == TipQrCheckoutAvailability.RequiresSignIn &&
        !tip.cart.isEmpty
    ) {
        Text(
            text = "Sign in as a tipper to create a checkout session on this TV.",
            color = TvAppColors.TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 16.dp),
        )
    }

    TvPlayerControlChip(
        label = "Close",
        onClick = onClose,
        focusRequester = closeFocus,
    )
}

@Composable
private fun LiveTvWalletPayPanel(
    tip: LiveTvTipUiState,
    payFocus: FocusRequester,
    onPinDigit: (Char) -> Unit,
    onPinBackspace: () -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
    onDismissPending: () -> Unit,
    onAcknowledgeSuccess: () -> Unit,
) {
    val pay = tip.walletPay
    Text(
        text = "Wallet checkout",
        color = TvAppColors.TextPrimary,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
    )
    Text(
        text =
            when (pay.phase) {
                LiveTvWalletPayPhase.CheckingPrerequisites -> "Checking wallet…"
                LiveTvWalletPayPhase.EnterPin -> "Confirm with PIN"
                LiveTvWalletPayPhase.Submitting -> "Submitting…"
                LiveTvWalletPayPhase.Success -> "Tip confirmed"
                LiveTvWalletPayPhase.Failed -> "Payment failed"
                LiveTvWalletPayPhase.UnconfirmedPending -> "Outcome unconfirmed"
                LiveTvWalletPayPhase.Idle -> "Wallet"
            },
        color = TvAppColors.TextSecondary,
        fontSize = 14.sp,
        modifier = Modifier.padding(bottom = 12.dp),
    )

    pay.provisionalDisplayTotal?.let { total ->
        Text(
            text = "Cart display total: ${formatTipPrice(total)}",
            color = TvAppColors.TextSecondary,
            fontSize = 13.sp,
        )
    }
    pay.quoteLockedTotalGbp?.let { locked ->
        Text(
            text = "Server-locked total: ${formatTipPrice(locked)}",
            color = TvAppColors.FocusRingSecondary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp),
        )
    }
    pay.walletBalance?.let { bal ->
        Text(
            text = "Wallet balance: £$bal",
            color = TvAppColors.TextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 8.dp),
        )
    }
    pay.message?.let { msg ->
        Text(
            text = msg,
            color =
                when (pay.phase) {
                    LiveTvWalletPayPhase.Success -> TvAppColors.FocusRingSecondary
                    LiveTvWalletPayPhase.Failed,
                    LiveTvWalletPayPhase.UnconfirmedPending,
                    -> Color(0xFFFFB4A8)
                    else -> TvAppColors.TextPrimary
                },
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 12.dp),
        )
    }

    if (pay.phase == LiveTvWalletPayPhase.EnterPin ||
        pay.phase == LiveTvWalletPayPhase.Failed ||
        pay.phase == LiveTvWalletPayPhase.UnconfirmedPending
    ) {
        Text(
            text = "PIN: " + "•".repeat(pay.pinDigits.length) + "–".repeat(4 - pay.pinDigits.length),
            color = TvAppColors.TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        val digits = listOf("123", "456", "789")
        digits.forEach { row ->
            Row(modifier = Modifier.padding(bottom = 8.dp)) {
                row.forEachIndexed { index, ch ->
                    TvPlayerControlChip(
                        label = ch.toString(),
                        onClick = { onPinDigit(ch) },
                        modifier = Modifier.padding(end = if (index < 2) 8.dp else 0.dp),
                        focusRequester = if (row == "123" && index == 0) payFocus else null,
                    )
                }
            }
        }
        Row(modifier = Modifier.padding(bottom = 12.dp)) {
            TvPlayerControlChip(
                label = "0",
                onClick = { onPinDigit('0') },
                modifier = Modifier.padding(end = 8.dp),
            )
            TvPlayerControlChip(
                label = "⌫",
                onClick = onPinBackspace,
            )
        }
        TvPlayerControlChip(
            label =
                if (pay.hasUnresolvedPending || pay.phase == LiveTvWalletPayPhase.UnconfirmedPending) {
                    "Retry wallet tip"
                } else {
                    "Confirm wallet tip"
                },
            onClick = onSubmit,
            enabled = pay.pinDigits.length == 4 && pay.phase != LiveTvWalletPayPhase.Submitting,
            modifier = Modifier.padding(bottom = 8.dp),
        )
    }

    if (pay.phase == LiveTvWalletPayPhase.Success) {
        TvPlayerControlChip(
            label = "Done",
            onClick = onAcknowledgeSuccess,
            focusRequester = payFocus,
            modifier = Modifier.padding(bottom = 8.dp),
        )
    }

    if (pay.hasUnresolvedPending &&
        pay.phase != LiveTvWalletPayPhase.Submitting &&
        pay.phase != LiveTvWalletPayPhase.Success
    ) {
        TvPlayerControlChip(
            label = "Discard local recovery only",
            onClick = onDismissPending,
            modifier = Modifier.padding(bottom = 8.dp),
        )
    }

    if (pay.phase != LiveTvWalletPayPhase.Submitting && pay.phase != LiveTvWalletPayPhase.Success) {
        TvPlayerControlChip(
            label = "Back to cart",
            onClick = onCancel,
            focusRequester = if (pay.phase == LiveTvWalletPayPhase.CheckingPrerequisites) payFocus else null,
        )
    }
}

@Composable
private fun LiveTvQrPayPanel(
    tip: LiveTvTipUiState,
    payFocus: FocusRequester,
    onCancel: () -> Unit,
    onAcknowledgeSuccess: () -> Unit,
) {
    val pay = tip.qrPay
    Text(
        text = "QR / card checkout",
        color = TvAppColors.TextPrimary,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
    )
    Text(
        text =
            when (pay.phase) {
                LiveTvQrPayPhase.CreatingSession -> "Creating checkout…"
                LiveTvQrPayPhase.AwaitingPhonePayment -> "Waiting for phone payment"
                LiveTvQrPayPhase.Paid -> "Payment confirmed"
                LiveTvQrPayPhase.Failed -> "Checkout failed"
                LiveTvQrPayPhase.Expired -> "Checkout expired"
                LiveTvQrPayPhase.Cancelled -> "Checkout cancelled"
                LiveTvQrPayPhase.Idle -> "QR checkout"
            },
        color = TvAppColors.TextSecondary,
        fontSize = 14.sp,
        modifier = Modifier.padding(bottom = 12.dp),
    )
    pay.message?.let { msg ->
        Text(
            text = msg,
            color =
                when (pay.phase) {
                    LiveTvQrPayPhase.Paid -> TvAppColors.FocusRingSecondary
                    LiveTvQrPayPhase.Failed,
                    LiveTvQrPayPhase.Expired,
                    -> Color(0xFFFFB4A8)
                    else -> TvAppColors.TextPrimary
                },
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 12.dp),
        )
    }
    pay.expiresAtIso?.let { exp ->
        Text(
            text = "Session expires: $exp",
            color = TvAppColors.TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 8.dp),
        )
    }
    val url = pay.checkoutUrl
    if (pay.phase == LiveTvQrPayPhase.AwaitingPhonePayment && !url.isNullOrBlank()) {
        LiveTvQrCodeImage(
            content = url,
            modifier =
                Modifier
                    .padding(vertical = 12.dp)
                    .fillMaxWidth(0.85f),
            sizePx = 400,
        )
        Text(
            text = "Scan with your phone camera or QR app.",
            color = TvAppColors.TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 12.dp),
        )
    }
    if (pay.phase == LiveTvQrPayPhase.Paid) {
        TvPlayerControlChip(
            label = "Done",
            onClick = onAcknowledgeSuccess,
            focusRequester = payFocus,
            modifier = Modifier.padding(bottom = 8.dp),
        )
    }
    if (pay.phase != LiveTvQrPayPhase.CreatingSession && pay.phase != LiveTvQrPayPhase.Paid) {
        TvPlayerControlChip(
            label = "Back to cart",
            onClick = onCancel,
            focusRequester = if (pay.phase == LiveTvQrPayPhase.AwaitingPhonePayment) payFocus else null,
        )
    }
}
