package com.example.telepse.ui.screen

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.telepse.data.community.communityShowProgressBarAmounts
import com.example.telepse.data.community.toDisplayContent
import com.example.telepse.ui.background.EventBackgroundMedia
import com.example.telepse.ui.background.resolveCommunityRoomBackgroundMedia
import com.example.telepse.ui.tv.TvAppColors
import com.example.telepse.ui.viewmodel.CommunityRoomDisplayViewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CommunityRoomIdScreen(
    onBack: () -> Unit,
    onCommunitySelected: (String) -> Unit,
) {
    var communityId by remember { mutableStateOf("") }
    Column(
        modifier =
            Modifier.fillMaxSize()
                .background(TvAppColors.ScreenBackground)
                .padding(32.dp),
    ) {
        Text(text = "Community room display", color = TvAppColors.TextPrimary)
        Text(
            text = "Enter the community ID from the host dashboard URL.",
            color = TvAppColors.TextSecondary,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        androidx.compose.material3.OutlinedTextField(
            value = communityId,
            onValueChange = { communityId = it },
            label = { Text("Community ID") },
            modifier = Modifier.padding(vertical = 8.dp),
        )
        androidx.compose.material3.Button(
            onClick = { onCommunitySelected(communityId.trim()) },
            enabled = communityId.trim().isNotEmpty(),
        ) {
            Text("Open community room")
        }
        androidx.compose.material3.TextButton(onClick = onBack) {
            Text("Back")
        }
    }
}

@UnstableApi
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CommunityRoomDisplayScreen(
    communityId: String,
    onBackClick: () -> Unit,
    viewModel: CommunityRoomDisplayViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(communityId) {
        viewModel.loadCommunity(communityId)
    }

    DisposableEffect(communityId) {
        onDispose {
            viewModel.onLeaveCommunityRoom()
        }
    }

    val settings = uiState.settings
    if (uiState.isLoading && settings == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    uiState.error?.let { err ->
        if (settings == null) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black).padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = err, color = Color(0xFFFF6B6B))
                    androidx.compose.material3.TextButton(onClick = onBackClick) {
                        Text("Back")
                    }
                }
            }
            return
        }
    }

    val display = settings ?: return
    val backgroundMedia =
        remember(display.background, display.backgroundType) {
            resolveCommunityRoomBackgroundMedia(display.background, display.backgroundType)
        }
    val showAmounts = communityShowProgressBarAmounts(display)
    val displayContent = remember(display) { display.toDisplayContent() }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val media = backgroundMedia) {
            is EventBackgroundMedia.LocalRaw -> {
                val uri =
                    Uri.parse("android.resource://${context.packageName}/${media.resourceId}")
                val playerManager = remember(communityId, media.resourceId) {
                    ExoPlayerManager(context)
                }
                DisposableEffect(communityId, media.resourceId) {
                    onDispose { playerManager.release() }
                }
                VideoBackground(
                    uri = uri,
                    resourceId = media.resourceId,
                    playerManager = playerManager,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            is EventBackgroundMedia.RemoteImage -> {
                CommunityRoomRemoteImageBackground(
                    url = media.url,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        Column(
            modifier =
                Modifier.align(Alignment.BottomStart)
                    .fillMaxWidth(0.48f)
                    .padding(48.dp),
        ) {
            CommunityRoomDisplaySettingsPanel(
                display = display,
                content = displayContent,
                showAmounts = showAmounts,
                errorMessage = uiState.error,
            )
            androidx.compose.material3.TextButton(
                onClick = onBackClick,
                modifier = Modifier.padding(top = 12.dp),
            ) {
                Text("Back", color = Color.White)
            }
        }
    }
}

@Composable
private fun CommunityRoomRemoteImageBackground(
    url: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var loadFailed by remember(url) { mutableStateOf(false) }
    if (loadFailed) {
        Box(modifier = modifier.background(Color.Black))
        return
    }
    AsyncImage(
        model =
            ImageRequest.Builder(context)
                .data(url)
                .crossfade(true)
                .listener(onError = { _, _ -> loadFailed = true })
                .build(),
        contentDescription = null,
        modifier = modifier,
    )
}
