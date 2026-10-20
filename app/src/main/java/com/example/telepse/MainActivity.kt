package com.example.telepse

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import com.example.telepse.ui.tv.TvFocusBridge
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.example.telepse.data.display.ScreenDisplaySessionStore
import com.example.telepse.data.repository.AuthRepository
import com.example.telepse.ui.navigation.NavigationBackResolver
import com.example.telepse.ui.navigation.Screen
import com.example.telepse.ui.screen.CodeInputScreen
import com.example.telepse.ui.screen.CommunityRoomDisplayScreen
import com.example.telepse.ui.screen.CommunityRoomIdScreen
import com.example.telepse.ui.screen.ExplorePlaceholderScreen
import com.example.telepse.ui.screen.ExplorePlaybackScreen
import com.example.telepse.ui.screen.FullDisplayScreen
import com.example.telepse.ui.screen.FundraisingMilestoneScreen
import com.example.telepse.ui.screen.JourneyHubScreen
import com.example.telepse.ui.screen.LiveTvFoundationScreen
import com.example.telepse.ui.screen.TipperLoginScreen
import com.example.telepse.ui.screen.TipsWalletScreen
import com.example.telepse.ui.screen.YouTubePlaybackSpikeScreen
import com.example.telepse.ui.theme.TelepseTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var authRepository: AuthRepository

    @Inject lateinit var screenDisplaySessionStore: ScreenDisplaySessionStore

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    if (TvFocusBridge.onDpadDown != null) {
                        TvFocusBridge.onDpadDown?.invoke()
                        return true
                    }
                }
                KeyEvent.KEYCODE_DPAD_UP -> {
                    if (TvFocusBridge.onDpadUp != null) {
                        TvFocusBridge.onDpadUp?.invoke()
                        return true
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TelepseTheme {
                TelepseApp(
                    authRepository = authRepository,
                    screenDisplaySessionStore = screenDisplaySessionStore,
                )
            }
        }
    }
}

private const val NAV_LOG_TAG = "TelepseNav"

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TelepseApp(
    authRepository: AuthRepository,
    screenDisplaySessionStore: ScreenDisplaySessionStore,
) {
    var currentScreen by remember {
        mutableStateOf<Screen>(
            screenDisplaySessionStore.lastEventId()?.let { savedId ->
                Log.d(NAV_LOG_TAG, "resume Screen Display eventId=$savedId")
                Screen.FundraisingMilestone(savedId)
            } ?: Screen.JourneyHub,
        )
    }

    // After TV sleep/wake or process death that lands on Hub while a session is still
    // saved (transient pause ≠ explicit exit), restore Screen Display.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event != Lifecycle.Event.ON_START) return@LifecycleEventObserver
                val savedId = screenDisplaySessionStore.lastEventId() ?: return@LifecycleEventObserver
                if (currentScreen is Screen.JourneyHub) {
                    Log.d(NAV_LOG_TAG, "ON_START restore Screen Display eventId=$savedId")
                    currentScreen = Screen.FundraisingMilestone(savedId)
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler(enabled = currentScreen !is Screen.JourneyHub) {
        when (currentScreen) {
            is Screen.FundraisingMilestone,
            is Screen.CodeInput,
            -> screenDisplaySessionStore.clearLastEventId()
            else -> Unit
        }
        currentScreen = NavigationBackResolver.resolveBack(currentScreen)
    }

    fun requireAuth(destination: Screen, onAuthed: () -> Unit) {
        if (authRepository.isLoggedIn()) {
            onAuthed()
        } else {
            Log.d(NAV_LOG_TAG, "requireAuth redirect to TipperLogin returnTo=${destination.javaClass.simpleName}")
            currentScreen = Screen.TipperLogin(returnTo = destination)
        }
    }

    when (val screen = currentScreen) {
        is Screen.JourneyHub -> {
            JourneyHubScreen(
                onScreenDisplay = {
                    Log.d(NAV_LOG_TAG, "journey dest=CodeInput")
                    currentScreen = Screen.CodeInput
                },
                onLiveTv = {
                    Log.d(NAV_LOG_TAG, "journey dest=LiveTvFoundation (auth gated)")
                    requireAuth(Screen.LiveTvFoundation) {
                        Log.d(NAV_LOG_TAG, "journey dest=LiveTvFoundation (authed)")
                        currentScreen = Screen.LiveTvFoundation
                    }
                },
                onExplore = {
                    Log.d(NAV_LOG_TAG, "journey dest=ExplorePlaceholder (auth gated)")
                    requireAuth(Screen.ExplorePlaceholder) {
                        Log.d(NAV_LOG_TAG, "journey dest=ExplorePlaceholder (authed)")
                        currentScreen = Screen.ExplorePlaceholder
                    }
                },
                onTipsWallet = {
                    Log.d(NAV_LOG_TAG, "journey dest=TipsWallet (auth gated)")
                    requireAuth(Screen.TipsWallet) {
                        currentScreen = Screen.TipsWallet
                    }
                },
                onSignIn = {
                    Log.d(NAV_LOG_TAG, "journey dest=TipperLogin returnTo=JourneyHub")
                    currentScreen = Screen.TipperLogin(returnTo = Screen.JourneyHub)
                },
                onYouTubeSpike = {
                    Log.d(NAV_LOG_TAG, "journey dest=YouTubePlaybackSpike")
                    currentScreen = Screen.YouTubePlaybackSpike
                },
                onCommunityRoom = {
                    Log.d(NAV_LOG_TAG, "journey dest=CommunityRoomIdInput")
                    currentScreen = Screen.CommunityRoomIdInput
                },
            )
        }

        is Screen.CommunityRoomIdInput -> {
            CommunityRoomIdScreen(
                onBack = { currentScreen = Screen.JourneyHub },
                onCommunitySelected = { communityId ->
                    currentScreen = Screen.CommunityRoomDisplay(communityId)
                },
            )
        }

        is Screen.CommunityRoomDisplay -> {
            CommunityRoomDisplayScreen(
                communityId = screen.communityId,
                onBackClick = { currentScreen = Screen.CommunityRoomIdInput },
            )
        }

        is Screen.CodeInput -> {
            CodeInputScreen(
                onCodeValidated = { },
                onLoadOverlay = { eventId ->
                    screenDisplaySessionStore.saveLastEventId(eventId)
                    currentScreen = Screen.FundraisingMilestone(eventId)
                },
                onOpenYouTubeSpike = {
                    currentScreen = Screen.YouTubePlaybackSpike
                },
                onBackToHub = {
                    screenDisplaySessionStore.clearLastEventId()
                    currentScreen = Screen.JourneyHub
                },
            )
        }

        is Screen.YouTubePlaybackSpike -> {
            YouTubePlaybackSpikeScreen(
                onBack = { currentScreen = Screen.JourneyHub },
            )
        }

        is Screen.TipperLogin -> {
            TipperLoginScreen(
                onBack = { currentScreen = Screen.JourneyHub },
                onLoggedIn = { currentScreen = screen.returnTo },
            )
        }

        is Screen.LiveTvFoundation -> {
            LiveTvFoundationScreen(
                onBack = { currentScreen = Screen.JourneyHub },
            )
        }

        is Screen.ExplorePlaceholder -> {
            ExplorePlaceholderScreen(
                onBack = { currentScreen = Screen.JourneyHub },
                onOpenVideo = { videoId, title ->
                    currentScreen = Screen.ExplorePlayback(videoId = videoId, title = title)
                },
            )
        }

        is Screen.ExplorePlayback -> {
            ExplorePlaybackScreen(
                videoId = screen.videoId,
                title = screen.title,
                onBack = { currentScreen = Screen.ExplorePlaceholder },
            )
        }

        is Screen.TipsWallet -> {
            TipsWalletScreen(onBack = { currentScreen = Screen.JourneyHub })
        }

        is Screen.FullDisplay -> {
            FullDisplayScreen(event = screen.event)
        }

        is Screen.FundraisingMilestone -> {
            FundraisingMilestoneScreen(
                eventId = screen.eventId,
                onBackClick = {
                    screenDisplaySessionStore.clearLastEventId()
                    currentScreen = Screen.CodeInput
                },
            )
        }
    }
}
