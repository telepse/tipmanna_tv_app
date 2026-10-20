package com.example.telepse.ui.tv

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
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
import kotlinx.coroutines.delay

/** Compact D-pad control for full-bleed player overlays (LIVE TV, Explore actions). */
@Composable
fun TvPlayerControlChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    focusChainLeft: FocusRequester? = null,
    focusChainRight: FocusRequester? = null,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(12.dp)
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
        label = "chipBorder",
    )
    val backgroundColor by animateColorAsState(
        when {
            activated -> Color(0xFF2A4A5A)
            focused -> Color(0xFF1A2838)
            else -> Color(0xCC12121A)
        },
        label = "chipBg",
    )

    var chipModifier =
        modifier
            .height(52.dp)
            .padding(horizontal = 4.dp)
    if (focusRequester != null) {
        chipModifier = chipModifier.focusRequester(focusRequester)
    }

    Box(
        modifier =
            chipModifier
                .onFocusChanged { focused = it.isFocused }
                .focusable(enabled = enabled)
                .tvFocusChain(left = focusChainLeft, right = focusChainRight)
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
                .background(backgroundColor, shape)
                .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (enabled) TvAppColors.TextPrimary else TvAppColors.TextSecondary,
            fontSize = 16.sp,
            fontWeight = if (focused || activated) FontWeight.Bold else FontWeight.SemiBold,
        )
    }
}
