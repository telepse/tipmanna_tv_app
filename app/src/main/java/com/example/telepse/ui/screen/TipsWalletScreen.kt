package com.example.telepse.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.example.telepse.ui.tv.TvAppColors
import com.example.telepse.ui.tv.TvLazyColumn
import com.example.telepse.ui.tv.TvNavButton
import com.example.telepse.ui.tv.requestInitialTvFocus
import com.example.telepse.ui.viewmodel.TipsWalletPhase
import com.example.telepse.ui.viewmodel.TipsWalletViewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TipsWalletScreen(
    onBack: () -> Unit,
    viewModel: TipsWalletViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val backFocus = remember { FocusRequester() }

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
            text = "Wallet balance & activity",
            color = TvAppColors.TextPrimary,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text =
                "Your Tipmanna wallet and ledger are shared with web and other clients. " +
                    "Tip on LIVE TV. Fund your wallet through existing Tipmanna channels (not on TV).",
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
                        Text(
                            text =
                                "A wallet tip is pending from LIVE TV. Open Watch LIVE TV to retry or discard there.",
                            color = TvAppColors.FocusRingSecondary,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(bottom = 16.dp),
                        )
                    }

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
