package com.example.telepse.ui.moderation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.example.telepse.ui.tv.TvAppColors
import com.example.telepse.ui.viewmodel.TvModerationDialog
import com.example.telepse.ui.viewmodel.TvModerationUiState
import com.example.telepse.ui.viewmodel.TvModerationViewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvModerationToast(message: String, modifier: Modifier = Modifier) {
    Text(
        text = message,
        color = TvAppColors.FocusRingSecondary,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        modifier =
            modifier
                .background(Color(0xCC000000), RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvModerationDialogOverlay(
    channelId: String,
    state: TvModerationUiState,
    moderationViewModel: TvModerationViewModel,
) {
    when (state.dialog) {
        TvModerationDialog.ReportVideo ->
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(Color(0x99000000)),
                contentAlignment = Alignment.Center,
            ) {
                TvContentReportDialog(
                    title = "Report video",
                    subtitle = "Reports the EventChannel (video row) you are watching.",
                    selectedReason = state.selectedReason,
                    otherText = state.otherText,
                    submitting = state.submitting,
                    errorMessage = state.errorMessage,
                    resolvedHostId = state.resolvedHostId,
                    onSelectReason = moderationViewModel::selectReason,
                    onOtherTextChange = moderationViewModel::updateOtherText,
                    onConfirm = { moderationViewModel.submitVideoReport(channelId) },
                    onDismiss = moderationViewModel::dismissDialog,
                )
            }
        TvModerationDialog.ReportHost ->
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(Color(0x99000000)),
                contentAlignment = Alignment.Center,
            ) {
                TvContentReportDialog(
                    title = "Report host",
                    subtitle = "Reports the host account tied to this video.",
                    selectedReason = state.selectedReason,
                    otherText = state.otherText,
                    submitting = state.submitting,
                    errorMessage = state.errorMessage,
                    resolvedHostId = state.resolvedHostId,
                    onSelectReason = moderationViewModel::selectReason,
                    onOtherTextChange = moderationViewModel::updateOtherText,
                    onConfirm = moderationViewModel::submitHostReport,
                    onDismiss = moderationViewModel::dismissDialog,
                )
            }
        TvModerationDialog.BlockHostConfirm ->
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(Color(0x99000000)),
                contentAlignment = Alignment.Center,
            ) {
                TvHostBlockConfirmDialog(
                    hostId = state.resolvedHostId,
                    submitting = state.submitting,
                    errorMessage = state.errorMessage,
                    onConfirm = moderationViewModel::confirmBlockHost,
                    onDismiss = moderationViewModel::dismissDialog,
                )
            }
        TvModerationDialog.None -> Unit
    }
}
