package com.example.telepse.ui.screen

import androidx.compose.runtime.Composable

/** Journey hub entry — implemented in [ExploreTvScreen]. */
@Composable
fun ExplorePlaceholderScreen(
    onBack: () -> Unit,
    onOpenVideo: (videoId: String, title: String) -> Unit,
) {
    ExploreTvScreen(onBack = onBack, onOpenVideo = onOpenVideo)
}
