package com.example.telepse.ui.moderation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
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
import com.example.telepse.data.model.ContentReportReason
import com.example.telepse.ui.tv.TvAppColors
import com.example.telepse.ui.tv.TvLazyRow
import com.example.telepse.ui.tv.TvPlayerControlChip
import com.example.telepse.ui.tv.requestInitialTvFocus

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvContentReportDialog(
    title: String,
    subtitle: String,
    selectedReason: ContentReportReason,
    otherText: String,
    submitting: Boolean,
    errorMessage: String?,
    resolvedHostId: String?,
    onSelectReason: (ContentReportReason) -> Unit,
    onOtherTextChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val reasons = remember { ContentReportReason.entries }
    val reasonFocus = remember { List(reasons.size) { FocusRequester() } }
    val submitFocus = remember { FocusRequester() }
    val cancelFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        reasonFocus.first().requestInitialTvFocus()
    }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(Color(0xF0101010))
                .padding(28.dp),
    ) {
        Text(
            text = title,
            color = TvAppColors.TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = subtitle,
            color = TvAppColors.FocusRingSecondary,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 8.dp),
        )
        resolvedHostId?.let {
            Text(
                text = "Host identity is available to moderators via this channel record.",
                color = TvAppColors.FocusRingSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 6.dp, bottom = 8.dp),
            )
        }
        Text(
            text =
                "Select a reason and confirm.",
            color = TvAppColors.FocusRingSecondary,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
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
            reasons.forEachIndexed { index, reason ->
                TvPlayerControlChip(
                    label =
                        buildString {
                            append(reason.displayLabel)
                            if (reason == selectedReason) append(" ✓")
                        },
                    onClick = { onSelectReason(reason) },
                    focusRequester = reasonFocus[index],
                    focusChainLeft = reasonFocus.getOrNull(index - 1),
                    focusChainRight = reasonFocus.getOrNull(index + 1) ?: submitFocus,
                    enabled = !submitting,
                )
            }
        }
        if (selectedReason == ContentReportReason.Other) {
            OutlinedTextField(
                value = otherText,
                onValueChange = onOtherTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                label = { Text("Describe the issue") },
                singleLine = false,
                maxLines = 3,
            )
        }
        TvLazyRow(modifier = Modifier.padding(top = 16.dp)) {
            TvPlayerControlChip(
                label = if (submitting) "Submitting…" else "Submit report",
                onClick = onConfirm,
                focusRequester = submitFocus,
                focusChainLeft = reasonFocus.lastOrNull(),
                focusChainRight = cancelFocus,
                enabled = !submitting,
            )
            TvPlayerControlChip(
                label = "Cancel",
                onClick = onDismiss,
                focusRequester = cancelFocus,
                focusChainLeft = submitFocus,
                enabled = !submitting,
            )
        }
    }
}
