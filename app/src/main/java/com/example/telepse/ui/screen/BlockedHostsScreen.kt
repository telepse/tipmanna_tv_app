package com.example.telepse.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.example.telepse.ui.tv.TvAppColors
import com.example.telepse.ui.tv.TvLazyRow
import com.example.telepse.ui.tv.TvPlayerControlChip
import com.example.telepse.ui.viewmodel.BlockedHostsViewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun BlockedHostsScreen(
    onBack: () -> Unit,
    viewModel: BlockedHostsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Column(
        modifier =
            Modifier.fillMaxSize()
                .background(TvAppColors.ScreenBackground)
                .padding(horizontal = 48.dp, vertical = 40.dp),
    ) {
        Text(
            text = "Blocked hosts",
            color = TvAppColors.TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Text(
            text = "Unblock to show this host in LIVE TV and Explore again.",
            color = TvAppColors.TextSecondary,
            fontSize = 15.sp,
            modifier = Modifier.padding(bottom = 24.dp),
        )
        state.error?.let {
            Text(text = it, color = TvAppColors.FocusRingSecondary, modifier = Modifier.padding(bottom = 12.dp))
        }
        state.message?.let {
            Text(text = it, color = TvAppColors.FocusRingSecondary, modifier = Modifier.padding(bottom = 12.dp))
        }
        if (state.loading) {
            Text(text = "Loading…", color = TvAppColors.TextSecondary)
        } else if (state.hostIds.isEmpty()) {
            Text(text = "No blocked hosts.", color = TvAppColors.TextSecondary, modifier = Modifier.padding(bottom = 16.dp))
        } else {
            state.hostIds.forEach { hostId ->
                TvLazyRow(modifier = Modifier.padding(bottom = 12.dp)) {
                    TvPlayerControlChip(
                        label = "Unblock $hostId",
                        onClick = { viewModel.unblock(hostId) },
                    )
                }
            }
        }
        TvLazyRow {
            TvPlayerControlChip(label = "Back to home", onClick = onBack)
        }
    }
}
