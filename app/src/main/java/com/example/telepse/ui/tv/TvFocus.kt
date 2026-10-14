package com.example.telepse.ui.tv

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** High-contrast TV focus ring — use on focusable controls (not on non-focusable containers). */
fun Modifier.tvFocusRing(
    cornerRadiusDp: Int = 10,
    enabled: Boolean = true,
): Modifier =
    if (!enabled) {
        this
    } else {
        composed {
            var focused by remember { mutableStateOf(false) }
            val shape = RoundedCornerShape(cornerRadiusDp.dp)
            onFocusChanged { focused = it.isFocused }
                .border(
                    width = if (focused) 4.dp else 2.dp,
                    color = if (focused) TvAppColors.FocusRing else TvAppColors.ButtonIdleBorder,
                    shape = shape,
                )
                .padding(2.dp)
        }
    }

fun Modifier.tvNotFocusable(): Modifier =
    focusProperties {
        canFocus = false
    }

/** Explicit D-pad up/down targets when spatial focus search fails (common on Android TV). */
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.tvFocusChainVertical(
    up: FocusRequester? = null,
    down: FocusRequester? = null,
): Modifier = tvFocusChain(up = up, down = down)

/** Explicit D-pad exit targets when spatial focus search fails (common on Android TV). */
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.tvFocusChain(
    up: FocusRequester? = null,
    down: FocusRequester? = null,
    left: FocusRequester? = null,
    right: FocusRequester? = null,
): Modifier =
    focusProperties {
        exit = { focusDirection ->
            when (focusDirection) {
                FocusDirection.Down -> down ?: FocusRequester.Default
                FocusDirection.Up -> up ?: FocusRequester.Default
                FocusDirection.Left -> left ?: FocusRequester.Default
                FocusDirection.Right -> right ?: FocusRequester.Default
                else -> FocusRequester.Default
            }
        }
    }

@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.onTvDpadVertical(
    onDown: (() -> Boolean)? = null,
    onUp: (() -> Boolean)? = null,
): Modifier =
    onKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
        when (event.key) {
            Key.DirectionDown -> onDown?.invoke() ?: false
            Key.DirectionUp -> onUp?.invoke() ?: false
            else -> false
        }
    }

suspend fun FocusRequester.requestInitialTvFocus(delayMs: Long = 120L) {
    delay(delayMs)
    try {
        requestFocus()
    } catch (_: IllegalStateException) {
        // Compose focus target not yet attached — caller may retry on next frame.
    }
}
