package com.example.telepse

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import com.example.telepse.ui.tv.TvFocusBridge
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.example.telepse.data.repository.AuthRepository
import com.example.telepse.ui.navigation.Screen
import com.example.telepse.ui.screen.CodeInputScreen
import com.example.telepse.ui.screen.ExplorePlaceholderScreen
import com.example.telepse.ui.screen.FullDisplayScreen
import com.example.telepse.ui.screen.FundraisingMilestoneScreen
import com.example.telepse.ui.screen.JourneyHubScreen
import com.example.telepse.ui.screen.LiveTvFoundationScreen
import com.example.telepse.ui.screen.TipperLoginScreen
import com.example.telepse.ui.screen.YouTubePlaybackSpikeScreen
import com.example.telepse.ui.theme.TelepseTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var authRepository: AuthRepository

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
                TelepseApp(authRepository = authRepository)
            }
        }
    }
}

private const val NAV_LOG_TAG = "TelepseNav"

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TelepseApp(authRepository: AuthRepository) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.JourneyHub) }

    BackHandler(enabled = currentScreen !is Screen.JourneyHub) {
        currentScreen =
            when (currentScreen) {
                is Screen.FundraisingMilestone -> Screen.CodeInput
                is Screen.TipperLogin -> Screen.JourneyHub
                is Screen.CodeInput,
                is Screen.LiveTvFoundation,
                is Screen.ExplorePlaceholder,
                is Screen.YouTubePlaybackSpike,
                -> Screen.JourneyHub
                else -> Screen.JourneyHub
            }
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
                onSignIn = {
                    Log.d(NAV_LOG_TAG, "journey dest=TipperLogin returnTo=JourneyHub")
                    currentScreen = Screen.TipperLogin(returnTo = Screen.JourneyHub)
                },
                onYouTubeSpike = {
                    Log.d(NAV_LOG_TAG, "journey dest=YouTubePlaybackSpike")
                    currentScreen = Screen.YouTubePlaybackSpike
                },
            )
        }

        is Screen.CodeInput -> {
            CodeInputScreen(
                onCodeValidated = { },
                onLoadOverlay = { eventId ->
                    currentScreen = Screen.FundraisingMilestone(eventId)
                },
                onOpenYouTubeSpike = {
                    currentScreen = Screen.YouTubePlaybackSpike
                },
                onBackToHub = { currentScreen = Screen.JourneyHub },
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
            )
        }

        is Screen.FullDisplay -> {
            FullDisplayScreen(event = screen.event)
        }

        is Screen.FundraisingMilestone -> {
            FundraisingMilestoneScreen(
                eventId = screen.eventId,
                onBackClick = { currentScreen = Screen.CodeInput },
            )
        }
    }
}
