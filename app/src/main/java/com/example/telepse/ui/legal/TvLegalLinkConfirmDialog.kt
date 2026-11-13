package com.example.telepse.ui.legal

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
import com.example.telepse.legal.LegalDocument
import com.example.telepse.ui.tv.TvAppColors
import com.example.telepse.ui.tv.TvLazyRow
import com.example.telepse.ui.tv.TvPlayerControlChip
import com.example.telepse.ui.tv.requestInitialTvFocus

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvLegalLinkConfirmDialog(
    document: LegalDocument,
    url: String,
    onConfirmOpen: () -> Unit,
    onDismiss: () -> Unit,
) {
    val openFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        openFocus.requestInitialTvFocus()
    }
    val body =
        when (document) {
            LegalDocument.PrivacyPolicy ->
                "Opens the Tipmanna privacy policy in your browser.\n\n$url"
            LegalDocument.TermsOfUse ->
                "Opens the Tipmanna terms (cookie policy page used on tipmanna.tv sign-up) in your browser.\n\n$url"
            LegalDocument.AccountDeletion ->
                "Opens the privacy policy section on account deletion on tipmanna.tv. " +
                    "To delete a signed-in tipper account, use the Tipmanna website while signed in.\n\n$url"
        }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(Color(0xF0101010))
                .padding(28.dp),
    ) {
        Text(
            text = document.hubLabel,
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
                label = "Open in browser",
                onClick = onConfirmOpen,
                focusRequester = openFocus,
            )
            TvPlayerControlChip(
                label = "Cancel",
                onClick = onDismiss,
            )
        }
    }
}
