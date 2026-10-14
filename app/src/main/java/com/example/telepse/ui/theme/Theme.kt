package com.example.telepse.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TelepseTheme(
    isInDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme =
        darkColorScheme(
            primary = com.example.telepse.ui.tv.TvAppColors.FocusRing,
            onPrimary = com.example.telepse.ui.tv.TvAppColors.ScreenBackground,
            secondary = com.example.telepse.ui.tv.TvAppColors.FocusRingSecondary,
            onSecondary = com.example.telepse.ui.tv.TvAppColors.ScreenBackground,
            background = com.example.telepse.ui.tv.TvAppColors.ScreenBackground,
            onBackground = com.example.telepse.ui.tv.TvAppColors.TextPrimary,
            surface = com.example.telepse.ui.tv.TvAppColors.ScreenBackground,
            onSurface = com.example.telepse.ui.tv.TvAppColors.TextPrimary,
        )
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}