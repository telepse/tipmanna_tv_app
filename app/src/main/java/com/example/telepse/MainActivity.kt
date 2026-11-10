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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.example.telepse.data.display.ScreenDisplaySessionStore
import com.example.telepse.data.repository.AuthRepository
import com.example.telepse.ui.navigation.NavigationBackResolver
import com.example.telepse.ui.navigation.Screen
import com.example.telepse.ui.navigation.TvMvpNavigation
import com.example.telepse.ui.navigation.initialAppScreen
import com.example.telepse.ui.navigation.screenDisplayRestoreTarget
import com.example.telepse.ui.navigation.shouldScheduleScreenDisplayRestoreOnStart
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
    var currentScreen by remember { mutableStateOf<Screen>(initialAppScreen()) }

    var deferScreenDisplayRestore by remember { mutableStateOf(false) }
    var isFirstOnStartSinceProcessLaunched by remember { mutableStateOf(true) }

    // After TV sleep/wake (not cold process start), restore Screen Display — after first frame.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event != Lifecycle.Event.ON_START) return@LifecycleEventObserver
                val savedId = screenDisplaySessionStore.lastEventId()
                val firstStart = isFirstOnStartSinceProcessLaunched
                if (firstStart) {
                    isFirstOnStartSinceProcessLaunched = false
                }
                if (
                    !TvMvpNavigation.deferScreenDisplayAutoRestore &&
                    shouldScheduleScreenDisplayRestoreOnStart(
                        isFirstOnStartSinceProcessLaunched = firstStart,
                        savedEventId = savedId,
                        currentScreen = currentScreen,
                    )
                ) {
                    Log.d(NAV_LOG_TAG, "ON_START schedule Screen Display restore eventId=$savedId")
                    deferScreenDisplayRestore = true
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(deferScreenDisplayRestore) {
        if (!deferScreenDisplayRestore) return@LaunchedEffect
        deferScreenDisplayRestore = false
        withFrameMillis { }
        val target =
            screenDisplayRestoreTarget(
                screenDisplaySessionStore.lastEventId(),
                currentScreen,
            )
        if (target != null) {
            Log.d(NAV_LOG_TAG, "deferred restore Screen Display eventId=${(target as Screen.FundraisingMilestone).eventId}")
            currentScreen = target
        }
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
                onLiveTv = {
                    Log.d(NAV_LOG_TAG, "journey dest=LiveTvFoundation (auth gated)")
                    requireAuth(Screen.LiveTvFoundation) {
                        Log.d(NAV_LOG_TAG, "journey dest=LiveTvFoundation (authed)")
                        currentScreen = Screen.LiveTvFoundation
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
