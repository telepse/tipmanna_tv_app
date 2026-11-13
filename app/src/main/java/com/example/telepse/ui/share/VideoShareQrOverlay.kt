package com.example.telepse.ui.share

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.example.telepse.share.TipmannaShareUrl
import com.example.telepse.ui.livetv.LiveTvQrCodeImage
import com.example.telepse.ui.tv.TvAppColors
import com.example.telepse.ui.tv.tvNotFocusable

private const val SHARE_QR_MESSAGE = "Watch this video on your mobile"

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun VideoShareQrOverlay(
    eventId: String?,
    channelId: String?,
    modifier: Modifier = Modifier,
) {
    val url = TipmannaShareUrl.buildContentShareUrl(eventId, channelId) ?: return
    Column(
        modifier =
            modifier
                .tvNotFocusable()
                .background(Color(0xE612121A), RoundedCornerShape(12.dp))
                .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = SHARE_QR_MESSAGE,
            color = TvAppColors.TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
        LiveTvQrCodeImage(
            content = url,
            modifier = Modifier.padding(top = 10.dp),
            sizePx = 200,
            contentDescription = SHARE_QR_MESSAGE,
        )
    }
}
