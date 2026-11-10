package com.example.telepse.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.example.telepse.data.community.CommunityRoomDisplayContent
import com.example.telepse.data.community.formatCommunityRoomEntryLine
import com.example.telepse.data.model.CommunityRoomDisplaySettings

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CommunityRoomDisplaySettingsPanel(
    display: CommunityRoomDisplaySettings,
    content: CommunityRoomDisplayContent,
    showAmounts: Boolean,
    errorMessage: String? = null,
    modifier: Modifier = Modifier,
) {
    val scroll = rememberScrollState()
    val panelShape = RoundedCornerShape(12.dp)
    val titleColor = Color.White
    val bodyColor = Color.White.copy(alpha = 0.92f)
    val mutedColor = Color.White.copy(alpha = 0.72f)

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.62f), panelShape)
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .verticalScroll(scroll),
    ) {
        Text(
            text = display.moneyFor.ifBlank { "Community room" },
            color = titleColor,
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold,
        )

        if (content.performers.isNotEmpty()) {
            Text(
                text = "Performers",
                color = titleColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 16.dp),
            )
            content.performers.forEach { entry ->
                Text(
                    text = formatCommunityRoomEntryLine(entry),
                    color = bodyColor,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }

        if (content.projects.isNotEmpty()) {
            Text(
                text = "Projects",
                color = titleColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 16.dp),
            )
            content.projects.forEach { entry ->
                Text(
                    text = formatCommunityRoomEntryLine(entry),
                    color = bodyColor,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }

        content.tipUsageDisclosureText?.let { disclosure ->
            Text(
                text = disclosure,
                color = bodyColor,
                fontSize = 17.sp,
                lineHeight = 22.sp,
                modifier = Modifier.padding(top = 16.dp),
            )
        }

        if (display.showLatest) {
            Text(
                text = "Show latest amount: on",
                color = mutedColor,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 14.dp),
            )
        }
        Text(
            text =
                if (showAmounts) {
                    "Progress amounts: visible"
                } else {
                    "Progress amounts: hidden"
                },
            color = mutedColor,
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 4.dp),
        )

        errorMessage?.let {
            Text(
                text = it,
                color = Color(0xFFFF6B6B),
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}
