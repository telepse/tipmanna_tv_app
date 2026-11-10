package com.example.telepse.ui.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Non-focusable hub row; selection ring driven by parent [selected] state. */
@Composable
fun TvHubNavRow(
    text: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier =
            modifier
                .semantics {
                    contentDescription = text
                    selected = isSelected
                }
                .fillMaxWidth(0.62f)
                .height(52.dp)
                .border(
                    width = if (isSelected) 4.dp else 2.dp,
                    color = if (isSelected) TvAppColors.FocusRing else TvAppColors.ButtonIdleBorder,
                    shape = shape,
                )
                .padding(2.dp)
                .background(color = Color(0xFF1E1E28), shape = shape),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 20.dp),
            color = TvAppColors.TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
