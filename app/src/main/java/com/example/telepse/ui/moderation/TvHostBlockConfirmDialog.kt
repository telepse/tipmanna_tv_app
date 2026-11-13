package com.example.telepse.ui.moderation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.example.telepse.ui.tv.TvAppColors
import com.example.telepse.ui.tv.TvLazyRow
import com.example.telepse.ui.tv.TvPlayerControlChip
import com.example.telepse.ui.tv.requestInitialTvFocus

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvHostBlockConfirmDialog(
    hostId: String?,
    submitting: Boolean,
    errorMessage: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val confirmFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        confirmFocus.requestInitialTvFocus()
    }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(Color(0xF0101010))
                .padding(28.dp),
    ) {
        Text(
            text = "Block host",
            color = TvAppColors.TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text =
                when {
                    hostId.isNullOrBlank() ->
                        "Host identity could not be resolved for this video."
                    submitting -> "Blocking host…"
                    else ->
                        "Hide this host's videos from LIVE TV and Explore on your account. " +
                            "You can unblock from Blocked hosts on the home screen."
                },
            color = TvAppColors.FocusRingSecondary,
            fontSize = 15.sp,
            modifier = Modifier.padding(top = 12.dp, bottom = 12.dp),
        )
        errorMessage?.let {
            Text(
                text = it,
                color = Color(0xFFFF8A80),
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }
        TvLazyRow {
            TvPlayerControlChip(
                label = if (submitting) "Blocking…" else "Block host",
                onClick = onConfirm,
                focusRequester = confirmFocus,
                enabled = !hostId.isNullOrBlank() && !submitting,
            )
            TvPlayerControlChip(
                label = "Cancel",
                onClick = onDismiss,
                enabled = !submitting,
            )
        }
    }
}
