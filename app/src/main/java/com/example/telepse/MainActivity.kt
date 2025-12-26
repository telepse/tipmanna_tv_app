package com.example.telepse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import com.example.telepse.data.model.Event
import com.example.telepse.ui.navigation.Screen
import com.example.telepse.ui.screen.CodeInputScreen
import com.example.telepse.ui.screen.FullDisplayScreen
import com.example.telepse.ui.screen.FundraisingMilestoneScreen
import com.example.telepse.ui.theme.TelepseTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TelepseTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape
                ) {
                    TelepseApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TelepseApp() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.CodeInput) }
    
    when (val screen = currentScreen) {
        is Screen.CodeInput -> {
            CodeInputScreen(
                onCodeValidated = { code ->
                    // Navigation handled within CodeInputScreen
                },
                onLoadOverlay = { eventId ->
                    currentScreen = Screen.FundraisingMilestone(eventId)
                }
            )
        }
        
        is Screen.FullDisplay -> {
            FullDisplayScreen(
                event = screen.event
            )
        }
        
        is Screen.FundraisingMilestone -> {
            FundraisingMilestoneScreen(
                eventId = screen.eventId,
                onBackClick = {
                    currentScreen = Screen.CodeInput
                }
            )
        }
    }
}