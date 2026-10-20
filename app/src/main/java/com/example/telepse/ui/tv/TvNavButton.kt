package com.example.telepse.ui.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.animateColorAsState
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Single focus-target hub/list row for Android TV D-pad navigation. */
@Composable
fun TvNavButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    focusChainUp: FocusRequester? = null,
    focusChainDown: FocusRequester? = null,
    focusChainLeft: FocusRequester? = null,
    focusChainRight: FocusRequester? = null,
    onFocused: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(10.dp)
    var focused by remember { mutableStateOf(false) }
    var activated by remember { mutableStateOf(false) }
    LaunchedEffect(activated) {
        if (activated) {
            delay(220)
            activated = false
        }
    }
    val borderColor by animateColorAsState(
        when {
            activated -> Color.White
            focused -> TvAppColors.FocusRing
            else -> TvAppColors.ButtonIdleBorder
        },
        label = "navBorder",
    )
    val backgroundColor by animateColorAsState(
        when {
            activated -> Color(0xFF2A4A5A)
            focused -> Color(0xFF252532)
            else -> if (enabled) Color(0xFF1E1E28) else Color(0xFF14141A)
        },
        label = "navBg",
    )
    var focusModifier =
        Modifier
            .fillMaxWidth(0.62f)
            .height(52.dp)
            .then(modifier)
    if (focusRequester != null) {
        focusModifier = focusModifier.focusRequester(focusRequester)
    }
    focusModifier =
        focusModifier.onFocusChanged { state ->
            focused = state.isFocused
            if (state.isFocused) onFocused?.invoke()
        }
    Box(
        modifier =
            focusModifier
                .focusable(enabled = enabled, interactionSource = interactionSource)
                .tvFocusChain(
                    up = focusChainUp,
                    down = focusChainDown,
                    left = focusChainLeft,
                    right = focusChainRight,
                )
                .onKeyEvent { event ->
                    if (!enabled || event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    when (event.key) {
                        Key.DirectionCenter,
                        Key.Enter,
                        Key.NumPadEnter,
                        -> {
                            activated = true
                            onClick()
                            true
                        }
                        else -> false
                    }
                }
                .semantics(mergeDescendants = true) { role = Role.Button }
                .border(
                    width = if (focused || activated) 4.dp else 2.dp,
                    color = borderColor,
                    shape = shape,
                )
                .background(color = backgroundColor, shape = shape),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 20.dp),
            color = if (enabled) TvAppColors.TextPrimary else TvAppColors.TextSecondary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
