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
fun TvModerationInfoDialog(
    title: String,
    body: String,
    onDismiss: () -> Unit,
) {
    val okFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        okFocus.requestInitialTvFocus()
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
            text = body,
            color = TvAppColors.FocusRingSecondary,
            fontSize = 15.sp,
            modifier = Modifier.padding(top = 12.dp, bottom = 20.dp),
        )
        TvLazyRow {
            TvPlayerControlChip(
                label = "OK",
                onClick = onDismiss,
                focusRequester = okFocus,
            )
        }
    }
}
