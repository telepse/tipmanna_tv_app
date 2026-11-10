package com.example.telepse.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.example.telepse.tips.WalletDebitSubmitMessages
import com.example.telepse.ui.tv.TvAppColors
import com.example.telepse.ui.tv.TvLazyColumn
import com.example.telepse.ui.tv.TvNavButton
import com.example.telepse.ui.tv.requestInitialTvFocus
import com.example.telepse.ui.viewmodel.TipSubmitPhase
import com.example.telepse.ui.viewmodel.TipsWalletPhase
import com.example.telepse.ui.viewmodel.TipsWalletUiState
import com.example.telepse.ui.viewmodel.TipsWalletViewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TipsWalletScreen(
    onBack: () -> Unit,
    viewModel: TipsWalletViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val backFocus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        viewModel.refreshAll()
        backFocus.requestInitialTvFocus()
    }

    Column(
        modifier =
            Modifier.fillMaxSize()
                .background(TvAppColors.ScreenBackground)
                .padding(40.dp),
    ) {
        Text(
            text = "Tips & wallet",
            color = TvAppColors.TextPrimary,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Wallet balance and tips use verified Tipmanna APIs. Card/QR checkout is not on TV yet.",
            color = TvAppColors.TextSecondary,
            fontSize = 15.sp,
            modifier = Modifier.padding(top = 6.dp, bottom = 16.dp),
        )

        when (uiState.phase) {
            TipsWalletPhase.Loading -> {
                CircularProgressIndicator(color = TvAppColors.FocusRing)
            }
            TipsWalletPhase.Error -> {
                Text(text = uiState.errorMessage ?: "Error", color = Color(0xFFFF6B6B))
                TvNavButton(text = "Retry", onClick = viewModel::refreshAll)
                TvNavButton(text = "Back", onClick = onBack, modifier = Modifier.padding(top = 8.dp))
            }
            TipsWalletPhase.Ready -> {
                TvLazyColumn(modifier = Modifier.weight(1f), scrollable = true) {
                    Text(
                        text = "Balance: £${uiState.balance}",
                        color = TvAppColors.TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    uiState.walletStatus?.let {
                        Text(
                            text = "Status: $it",
                            color = TvAppColors.TextSecondary,
                            modifier = Modifier.padding(bottom = 12.dp),
                        )
                    }
                    TvNavButton(
                        text = "Refresh wallet",
                        onClick = viewModel::refreshAll,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )

                    if (uiState.hasUnresolvedPendingDebit) {
                        WalletPendingRecoveryBlock(
                            uiState = uiState,
                            onDiscard = viewModel::dismissUnresolvedPendingTip,
                        )
                    }
                    uiState.pendingDiscardAcknowledgement?.let { ack ->
                        Text(
                            text = ack,
                            color = TvAppColors.TextSecondary,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 12.dp),
                        )
                    }

                    Text(
                        text = "Send tip (wallet debit)",
                        color = TvAppColors.FocusRing,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    val tipIdentityLocked = uiState.hasUnresolvedPendingDebit
                    OutlinedTextField(
                        value = uiState.eventIdInput,
                        onValueChange = viewModel::onEventIdChange,
                        label = { Text("Event ID (UUID)") },
                        singleLine = true,
                        readOnly = tipIdentityLocked,
                        enabled = !tipIdentityLocked,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions =
                            KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus(force = true)
                                    viewModel.loadCharacterCatalog()
                                },
                            ),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        colors =
                            TextFieldDefaults.colors(
                                focusedTextColor = TvAppColors.TextPrimary,
                                unfocusedTextColor = TvAppColors.TextPrimary,
                                disabledTextColor = TvAppColors.TextSecondary,
                            ),
                    )
                    if (tipIdentityLocked) {
                        Text(
                            text = WalletDebitSubmitMessages.PENDING_IDENTITY_LOCKED,
                            color = TvAppColors.FocusRingSecondary,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                    TvNavButton(
                        text = if (uiState.loadingCatalog) "Loading…" else "Load tip items",
                        onClick = viewModel::loadCharacterCatalog,
                        enabled = !uiState.loadingCatalog,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    uiState.catalogError?.let {
                        Text(text = it, color = Color(0xFFFF6B6B), modifier = Modifier.padding(bottom = 8.dp))
                    }
                    uiState.catalogCharacters.forEach { character ->
                        val selected = character.id == uiState.selectedCharacterId
                        val characterSelectable =
                            !tipIdentityLocked || character.id == uiState.pendingDebitCharacterId
                        TvNavButton(
                            text = "${character.name} — £${character.price}${if (selected) " ✓" else ""}",
                            onClick = { viewModel.selectCharacter(character.id) },
                            enabled = characterSelectable,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                    }
                    OutlinedTextField(
                        value = uiState.pinInput,
                        onValueChange = viewModel::onPinChange,
                        label = { Text("Wallet PIN (4 digits)") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        colors =
                            TextFieldDefaults.colors(
                                focusedTextColor = TvAppColors.TextPrimary,
                                unfocusedTextColor = TvAppColors.TextPrimary,
                            ),
                    )
                    TvNavButton(
                        text =
                            when {
                                uiState.tipSubmitPhase == TipSubmitPhase.Submitting -> "Sending…"
                                uiState.hasUnresolvedPendingDebit -> "Retry pending wallet tip"
                                else -> "Confirm wallet tip"
                            },
                        onClick = viewModel::submitWalletTip,
                        enabled = uiState.tipSubmitPhase != TipSubmitPhase.Submitting,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    if (uiState.tipResultMessage != null && uiState.tipSubmitPhase != TipSubmitPhase.Idle) {
                        Text(
                            text = uiState.tipResultMessage!!,
                            color =
                                if (uiState.tipSubmitPhase == TipSubmitPhase.Success) {
                                    TvAppColors.FocusRing
                                } else {
                                    Color(0xFFFF6B6B)
                                },
                            modifier = Modifier.padding(bottom = 12.dp),
                        )
                    }

                    Text(
                        text = "Card pay & QR checkout",
                        color = TvAppColors.FocusRingSecondary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                    )
                    Text(
                        text =
                            "TV checkout sessions (`/c/{token}`) are not deployed in the backend yet " +
                                "(see docs/kotlin-tv-phase2-architecture-gate/03-QR-CHECKOUT-CONTRACT.md). " +
                                "Wallet top-up via Stripe PaymentIntent requires a phone or web client — not simulated on TV.",
                        color = TvAppColors.TextSecondary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )

                    if (uiState.transactions.isNotEmpty()) {
                        Text(
                            text = "Recent activity",
                            color = TvAppColors.TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        uiState.transactions.take(8).forEach { row ->
                            Text(
                                text = "${row.kind} £${row.amount} → £${row.balanceAfter}",
                                color = TvAppColors.TextSecondary,
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                        }
                    }
                }
                TvNavButton(text = "Back", onClick = onBack, focusRequester = backFocus)
            }
        }
    }
}

@Composable
private fun WalletPendingRecoveryBlock(
    uiState: TipsWalletUiState,
    onDiscard: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .border(2.dp, TvAppColors.FocusRingSecondary, RoundedCornerShape(8.dp))
                .background(TvAppColors.ScreenBackground)
                .padding(16.dp),
    ) {
        Text(
            text = WalletDebitSubmitMessages.PENDING_RECOVERY_TITLE,
            color = TvAppColors.FocusRingSecondary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Text(
            text = WalletDebitSubmitMessages.PENDING_RECOVERY_DETAIL,
            color = TvAppColors.TextPrimary,
            fontSize = 15.sp,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        uiState.pendingDebitAmount?.let { amount ->
            Text(
                text =
                    "Pending tip: event ${uiState.pendingDebitEventId.orEmpty()}, " +
                        "item ${uiState.pendingDebitCharacterId.orEmpty()}, £$amount",
                color = TvAppColors.TextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        Text(
            text = WalletDebitSubmitMessages.PENDING_DISCARD_EXPLAINER,
            color = TvAppColors.TextSecondary,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        TvNavButton(
            text = WalletDebitSubmitMessages.PENDING_DISCARD_BUTTON,
            onClick = onDiscard,
            modifier = Modifier.padding(bottom = 4.dp),
        )
    }
}
