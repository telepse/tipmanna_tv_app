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
import com.example.telepse.tips.LiveTvTipUiState
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
    modifier: Modifier = Modifier,
) {
    val closeFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        closeFocus.requestInitialTvFocus()
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
                    tip.checkout.wallet == TipWalletCheckoutAvailability.Available ->
                        "Wallet checkout (next task)"
                    else -> "Wallet — sign in required"
                }
            TvPlayerControlChip(
                label = walletLabel,
                onClick = { /* Payment integration is Task 8 — entry only. */ },
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

            TvPlayerControlChip(
                label = "QR / card checkout — unavailable",
                onClick = { },
                enabled = tip.checkout.canEnterQrCheckout,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            if (tip.checkout.qrCard == TipQrCheckoutAvailability.BackendUnavailable) {
                Text(
                    text =
                        "TV tip checkout sessions (/c/{token}) are not deployed on the backend. " +
                            "No placeholder QR is shown.",
                    color = Color(0xFFFFB4A8),
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
    }
}
