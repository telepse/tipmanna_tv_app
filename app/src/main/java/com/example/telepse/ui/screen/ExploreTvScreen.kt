package com.example.telepse.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import coil.compose.AsyncImage
import com.example.telepse.data.model.ExploreFeedItem
import com.example.telepse.player.YoutubeVideoIdUtil
import com.example.telepse.ui.moderation.ExploreModerationChipPolicy
import com.example.telepse.ui.moderation.TvModerationDialogOverlay
import com.example.telepse.ui.moderation.TvModerationToast
import com.example.telepse.ui.tv.TvAppColors
import com.example.telepse.ui.tv.TvLazyColumn
import com.example.telepse.ui.tv.TvLazyRow
import com.example.telepse.ui.tv.TvNavButton
import com.example.telepse.ui.tv.TvPlayerControlChip
import com.example.telepse.ui.tv.requestInitialTvFocus
import com.example.telepse.ui.viewmodel.ExploreUiPhase
import com.example.telepse.ui.viewmodel.ExploreViewModel
import com.example.telepse.ui.viewmodel.TvModerationViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ExploreTvScreen(
    onBack: () -> Unit,
    onOpenVideo: (videoId: String, title: String, eventId: String, channelId: String) -> Unit,
    viewModel: ExploreViewModel = hiltViewModel(),
    moderationViewModel: TvModerationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val moderationState by moderationViewModel.state.collectAsState()
    val backFocus = remember { FocusRequester() }
    val firstItemFocus = remember { FocusRequester() }
    val loadMoreFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        viewModel.loadInitial()
    }

    var initialListFocusDone by remember { mutableStateOf(false) }
    var focusedItem by remember { mutableStateOf<ExploreFeedItem?>(null) }

    fun openExploreItem(item: ExploreFeedItem) {
        val videoId = YoutubeVideoIdUtil.fromUrl(item.url).orEmpty()
        val eventId = item.event?.id?.trim().orEmpty()
        val channelId = item.id.trim()
        if (eventId.isEmpty() || channelId.isEmpty()) return
        viewModel.trackCardActivated(item)
        onOpenVideo(videoId, item.title, eventId, channelId)
    }

    val focusedChannelId = focusedItem?.id?.trim().orEmpty()
    val gridModerationEnabled = ExploreModerationChipPolicy.moderationEnabled(focusedChannelId)

    LaunchedEffect(uiState.phase) {
        when (uiState.phase) {
            ExploreUiPhase.Content ->
                if (uiState.items.isNotEmpty() && !initialListFocusDone) {
                    firstItemFocus.requestInitialTvFocus()
                    initialListFocusDone = true
                } else if (uiState.items.isEmpty()) {
                    backFocus.requestInitialTvFocus()
                }
            ExploreUiPhase.Empty,
            ExploreUiPhase.Error,
            -> backFocus.requestInitialTvFocus()
            else -> Unit
        }
    }

    Box(
        modifier =
            Modifier.fillMaxSize()
                .background(TvAppColors.ScreenBackground),
    ) {
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(horizontal = 40.dp, vertical = 28.dp),
        ) {
        Text(
            text = "Explore",
            color = TvAppColors.TextPrimary,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Trending on Tipmanna",
            color = TvAppColors.TextSecondary,
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
        )
        Text(
            text =
                "Select a video: use Actions (Report / Block) on this screen, or OK on the card to open the player.",
            color = TvAppColors.TextSecondary.copy(alpha = 0.85f),
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        when (uiState.phase) {
            ExploreUiPhase.Loading,
            ExploreUiPhase.Idle,
            -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TvAppColors.FocusRing)
                }
            }
            ExploreUiPhase.Empty -> {
                ExploreMessage(
                    title = "Nothing to explore",
                    body = uiState.errorMessage ?: "Try again later.",
                )
                TvNavButton(text = "Back", onClick = onBack, focusRequester = backFocus)
            }
            ExploreUiPhase.Error -> {
                ExploreMessage(
                    title = "Could not load feed",
                    body = uiState.errorMessage ?: "Check your connection and retry.",
                    isError = true,
                )
                TvNavButton(text = "Retry", onClick = viewModel::retry, focusRequester = backFocus)
                TvNavButton(
                    text = "Back",
                    onClick = onBack,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            ExploreUiPhase.Content -> {
                TvLazyColumn(
                    modifier = Modifier.weight(1f),
                    scrollable = true,
                ) {
                    uiState.items.forEachIndexed { index, item ->
                        ExploreFeedCard(
                            item = item,
                            onActivate = { openExploreItem(item) },
                            onFocusChanged = { isFocused ->
                                if (isFocused) focusedItem = item
                            },
                            focusRequester = if (index == 0) firstItemFocus else null,
                            modifier = Modifier.padding(bottom = 12.dp),
                        )
                    }
                    if (uiState.loadingMore) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(16.dp),
                            color = TvAppColors.FocusRing,
                        )
                    } else {
                        TvPlayerControlChip(
                            label = "Load more",
                            onClick = viewModel::loadMore,
                            focusRequester = loadMoreFocus,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                    uiState.errorMessage?.let { msg ->
                        Text(
                            text = msg,
                            color = Color(0xFFFF6B6B),
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
                if (focusedItem != null && gridModerationEnabled) {
                    val host =
                        focusedItem?.event?.hostName
                            ?: focusedItem?.event?.username
                            ?: "Host"
                    Text(
                        text = "Actions — $host",
                        color = TvAppColors.TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    )
                    TvLazyRow(modifier = Modifier.fillMaxWidth()) {
                        TvPlayerControlChip(
                            label = "Open player",
                            onClick = { focusedItem?.let(::openExploreItem) },
                            enabled = true,
                        )
                        TvPlayerControlChip(
                            label = "Report video",
                            onClick = { moderationViewModel.openReportVideoDialog(focusedChannelId) },
                            enabled = gridModerationEnabled,
                        )
                        TvPlayerControlChip(
                            label = "Report host",
                            onClick = { moderationViewModel.openReportHostDialog(focusedChannelId) },
                            enabled = gridModerationEnabled,
                        )
                        TvPlayerControlChip(
                            label = "Block host",
                            onClick = { moderationViewModel.openBlockHostDialog(focusedChannelId) },
                            enabled = gridModerationEnabled,
                        )
                    }
                }
                TvNavButton(
                    text = "Back",
                    onClick = onBack,
                    focusRequester = backFocus,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
        }
        moderationState.successMessage?.let { message ->
            TvModerationToast(
                message = message,
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 48.dp),
            )
        }
        if (focusedChannelId.isNotBlank()) {
            TvModerationDialogOverlay(
                channelId = focusedChannelId,
                state = moderationState,
                moderationViewModel = moderationViewModel,
            )
        }
    }
}

@Composable
private fun ExploreFeedCard(
    item: ExploreFeedItem,
    onActivate: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
) {
    val shape = RoundedCornerShape(14.dp)
    var focused by remember { mutableStateOf(false) }
    var activated by remember { mutableStateOf(false) }
    LaunchedEffect(activated) {
        if (activated) {
            delay(200)
            activated = false
        }
    }
    val borderColor by animateColorAsState(
        when {
            activated -> Color.White
            focused -> TvAppColors.FocusRing
            else -> TvAppColors.ButtonIdleBorder
        },
        label = "exploreCardBorder",
    )
    val videoId = YoutubeVideoIdUtil.fromUrl(item.url)
    val thumbUrl = videoId?.let { "https://img.youtube.com/vi/$it/hqdefault.jpg" }
    val host = item.event?.hostName ?: item.event?.username ?: "Tipmanna"
    val eventId = item.event?.id?.trim().orEmpty()
    val channelId = item.id.trim()
    val canActivate = eventId.isNotEmpty() && channelId.isNotEmpty()

    var rowModifier =
        modifier
            .fillMaxWidth()
            .height(120.dp)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocusChanged(true)
            }
    if (focusRequester != null) {
        rowModifier = rowModifier.focusRequester(focusRequester)
    }

    Row(
        modifier =
            rowModifier
                .focusable(enabled = canActivate)
                .onKeyEvent { event ->
                    if (!canActivate || event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    when (event.key) {
                        Key.DirectionCenter,
                        Key.Enter,
                        Key.NumPadEnter,
                        -> {
                            activated = true
                            onActivate()
                            true
                        }
                        else -> false
                    }
                }
                .border(
                    width = if (focused || activated) 4.dp else 2.dp,
                    color = borderColor,
                    shape = shape,
                )
                .background(Color(0xFF16161E), shape)
                .clip(shape)
                .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = thumbUrl,
            contentDescription = item.title,
            modifier =
                Modifier.size(140.dp, 96.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF22222E)),
            contentScale = ContentScale.Crop,
        )
        Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
            if (item.isLive) {
                Text(
                    text = "LIVE",
                    color = Color.Black,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier =
                        Modifier.background(TvAppColors.FocusRing, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
            Text(
                text = item.title,
                color = TvAppColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = host,
                color = TvAppColors.TextSecondary,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            item.event?.eventName?.let { eventName ->
                Text(
                    text = eventName,
                    color = TvAppColors.TextSecondary.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ExploreMessage(
    title: String,
    body: String,
    isError: Boolean = false,
) {
    Column(modifier = Modifier.padding(vertical = 24.dp)) {
        Text(
            text = title,
            color = if (isError) Color(0xFFFF6B6B) else TvAppColors.TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = body,
            color = TvAppColors.TextSecondary,
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
