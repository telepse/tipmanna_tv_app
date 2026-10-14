package com.example.telepse.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.example.telepse.ui.tv.TvLazyColumn
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.example.telepse.ui.tv.TvAppColors
import com.example.telepse.ui.tv.TvNavButton
import com.example.telepse.ui.tv.requestInitialTvFocus

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ExplorePlaceholderScreen(onBack: () -> Unit) {
    val backFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        backFocus.requestInitialTvFocus()
    }

    TvLazyColumn(
        modifier =
            Modifier.fillMaxSize()
                .background(TvAppColors.ScreenBackground)
                .padding(48.dp),
    ) {
        Text(text = "Explore", color = TvAppColors.TextPrimary, modifier = Modifier.padding(bottom = 12.dp))
        Text(
            text =
                "TV-native Explore feed is not implemented in this slice. Requires authenticated feed APIs and side-chick server enforcement.",
            color = TvAppColors.TextSecondary,
            modifier = Modifier.padding(bottom = 24.dp),
        )
        TvNavButton(
            text = "Back",
            onClick = onBack,
            focusRequester = backFocus,
        )
    }
}
