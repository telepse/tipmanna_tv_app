package com.example.telepse.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.example.telepse.ui.tv.TvAppColors
import com.example.telepse.ui.tv.TvLazyColumn
import com.example.telepse.ui.tv.TvNavButton
import com.example.telepse.ui.tv.requestInitialTvFocus
import com.example.telepse.ui.tv.tvFocusRing
import com.example.telepse.ui.viewmodel.LoginStep
import com.example.telepse.ui.viewmodel.TipperLoginViewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TipperLoginScreen(
    onBack: () -> Unit,
    onLoggedIn: () -> Unit,
    viewModel: TipperLoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val emailFocus = remember { FocusRequester() }

    LaunchedEffect(uiState.loginSucceeded) {
        if (uiState.loginSucceeded) {
            onLoggedIn()
        }
    }

    LaunchedEffect(Unit) {
        emailFocus.requestInitialTvFocus()
    }

    TvLazyColumn(
        modifier =
            Modifier.fillMaxSize()
                .background(TvAppColors.ScreenBackground)
                .padding(48.dp),
    ) {
        Text(
            text = "Tipper sign in",
            color = TvAppColors.TextPrimary,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        OutlinedTextField(
            value = uiState.email,
            onValueChange = viewModel::onEmailChange,
            label = { Text("Email") },
            enabled = uiState.step == LoginStep.EMAIL && !uiState.loading,
            singleLine = true,
            modifier =
                Modifier.focusRequester(emailFocus)
                    .tvFocusRing(cornerRadiusDp = 8)
                    .padding(bottom = 12.dp),
        )
        if (uiState.step == LoginStep.CODE) {
            OutlinedTextField(
                value = uiState.code,
                onValueChange = viewModel::onCodeChange,
                label = { Text("Verification code") },
                enabled = !uiState.loading,
                singleLine = true,
                modifier =
                    Modifier.tvFocusRing(cornerRadiusDp = 8)
                        .padding(bottom = 12.dp),
            )
        }
        uiState.message?.let { msg ->
            Text(text = msg, color = Color(0xFF7DFFB3), modifier = Modifier.padding(bottom = 8.dp))
        }
        uiState.error?.let { err ->
            Text(text = err, color = Color(0xFFFF6B6B), modifier = Modifier.padding(bottom = 8.dp))
        }
        if (uiState.loading) {
            CircularProgressIndicator(modifier = Modifier.padding(8.dp))
        }
        when (uiState.step) {
            LoginStep.EMAIL ->
                TvNavButton(
                    text = "Send code",
                    onClick = viewModel::sendCode,
                    enabled = !uiState.loading,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            LoginStep.CODE ->
                TvNavButton(
                    text = "Verify and sign in",
                    onClick = viewModel::verifyCode,
                    enabled = !uiState.loading,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
        }
        TvNavButton(text = "Back", onClick = onBack)
    }
}
