package com.example.telepse.ui.screen

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.example.telepse.ui.tv.TvAppColors
import com.example.telepse.ui.tv.TvFocusBridge
import com.example.telepse.ui.tv.TvHubNavRow
import com.example.telepse.ui.tv.TvLazyColumn
import com.example.telepse.ui.tv.TvVerticalFocusIndex
import com.example.telepse.ui.tv.requestInitialTvFocus
import com.example.telepse.ui.viewmodel.JourneyHubViewModel

private const val JOURNEY_HUB_LOG_TAG = "JourneyHub"

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun JourneyHubScreen(
    onScreenDisplay: () -> Unit,
    onLiveTv: () -> Unit,
    onExplore: () -> Unit,
    onSignIn: () -> Unit,
    onYouTubeSpike: () -> Unit,
    viewModel: JourneyHubViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val listFocus = remember { FocusRequester() }
    var selectedActionIndex by remember { mutableIntStateOf(0) }

    val labels =
        remember(uiState.loggedIn) {
            buildList {
                add("Event screen display")
                add("Watch LIVE TV")
                add("Explore")
                add(if (uiState.loggedIn) "Sign out" else "Sign in (tipper)")
                add("YouTube spike (QA)")
            }
        }
    val selectedIndexState = rememberUpdatedState(selectedActionIndex)
    val labelsState = rememberUpdatedState(labels)

    val actions: List<() -> Unit> =
        remember(uiState.loggedIn) {
            buildList {
                add(onScreenDisplay)
                add(onLiveTv)
                add(onExplore)
                add(if (uiState.loggedIn) ({ viewModel.signOut() }) else onSignIn)
                add(onYouTubeSpike)
            }
        }

    LaunchedEffect(Unit) {
        viewModel.refresh()
        selectedActionIndex = 0
        listFocus.requestInitialTvFocus()
    }

    DisposableEffect(labelsState) {
        TvFocusBridge.onDpadDown = {
            val current = selectedIndexState.value
            val next =
                TvVerticalFocusIndex.nextIndex(current, labelsState.value.lastIndex)
            if (next != null) selectedActionIndex = next
        }
        TvFocusBridge.onDpadUp = {
            val current = selectedIndexState.value
            val prev = TvVerticalFocusIndex.previousIndex(current)
            if (prev != null) selectedActionIndex = prev
        }
        onDispose {
            TvFocusBridge.onDpadDown = null
            TvFocusBridge.onDpadUp = null
        }
    }

    TvLazyColumn(
        modifier =
            Modifier.fillMaxSize()
                .background(TvAppColors.ScreenBackground)
                .padding(horizontal = 48.dp, vertical = 40.dp)
                .focusRequester(listFocus)
                .focusable()
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    when (event.key) {
                        Key.DirectionDown -> {
                            val next =
                                TvVerticalFocusIndex.nextIndex(
                                    selectedActionIndex,
                                    labels.lastIndex,
                                )
                            if (next != null) {
                                selectedActionIndex = next
                                true
                            } else {
                                false
                            }
                        }
                        Key.DirectionUp -> {
                            val prev = TvVerticalFocusIndex.previousIndex(selectedActionIndex)
                            if (prev != null) {
                                selectedActionIndex = prev
                                true
                            } else {
                                false
                            }
                        }
                        Key.DirectionCenter,
                        Key.Enter,
                        Key.NumPadEnter,
                        -> {
                            val label = labels.getOrNull(selectedActionIndex) ?: "?"
                            Log.d(
                                JOURNEY_HUB_LOG_TAG,
                                "activate index=$selectedActionIndex label=$label",
                            )
                            actions.getOrNull(selectedActionIndex)?.invoke()
                            true
                        }
                        else -> false
                    }
                },
    ) {
        Text(
            text = "Tipmanna TV",
            color = TvAppColors.TextPrimary,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Text(
            text =
                if (uiState.loggedIn) {
                    "Signed in as ${uiState.tipperEmail ?: "tipper"}"
                } else {
                    "Sign in for LIVE TV and Explore"
                },
            color = TvAppColors.TextSecondary,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 24.dp),
        )
        labels.forEachIndexed { index, label ->
            TvHubNavRow(
                text = label,
                isSelected = index == selectedActionIndex,
                modifier = Modifier.padding(bottom = if (index < labels.lastIndex) 12.dp else 0.dp),
            )
        }
    }
}
