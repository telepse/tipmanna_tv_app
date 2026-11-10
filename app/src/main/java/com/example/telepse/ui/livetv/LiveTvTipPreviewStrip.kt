package com.example.telepse.ui.livetv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import com.example.telepse.ui.tv.tvFocusChainVertical
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.telepse.data.model.TipCharacterCatalogItem
import com.example.telepse.tips.LiveTvTipCatalogPhase
import com.example.telepse.tips.LiveTvTipUiState
import com.example.telepse.ui.tv.TvAppColors
import com.example.telepse.ui.tv.TvLazyRow
import com.example.telepse.ui.tv.TvPlayerControlChip
import java.util.Locale

@Composable
fun LiveTvTipPreviewStrip(
    tip: LiveTvTipUiState,
    onSelectCharacter: (String) -> Unit,
    onOpenCart: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    firstChipFocusRequester: FocusRequester? = null,
    tipsControlFocusRequester: FocusRequester? = null,
) {
    if (!tip.showStrip) return

    Column(modifier = modifier.padding(horizontal = 20.dp)) {
        Text(
            text = "Tips",
            color = TvAppColors.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        when (tip.catalogPhase) {
            LiveTvTipCatalogPhase.Loading -> {
                Box(modifier = Modifier.padding(vertical = 8.dp)) {
                    CircularProgressIndicator(
                        color = TvAppColors.FocusRing,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
            LiveTvTipCatalogPhase.NoEvent,
            LiveTvTipCatalogPhase.Empty,
            LiveTvTipCatalogPhase.Error,
            -> {
                Text(
                    text = tip.statusMessage ?: "Tips unavailable",
                    color =
                        if (tip.catalogPhase == LiveTvTipCatalogPhase.Error) {
                            Color(0xFFFF6B6B)
                        } else {
                            TvAppColors.TextSecondary
                        },
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                if (tip.catalogPhase == LiveTvTipCatalogPhase.Error) {
                    TvPlayerControlChip(label = "Retry tips", onClick = onRetry)
                }
            }
            LiveTvTipCatalogPhase.Ready -> {
                TvLazyRow {
                    tip.stripCharacters.forEachIndexed { index, character ->
                        TipPreviewChip(
                            character = character,
                            quantityInCart = tip.cart.quantityOf(character.id),
                            onClick = { onSelectCharacter(character.id) },
                            focusRequester = if (index == 0) firstChipFocusRequester else null,
                            focusChainDown =
                                if (index == 0) tipsControlFocusRequester else null,
                        )
                    }
                    if (!tip.cart.isEmpty) {
                        TvPlayerControlChip(
                            label = "Cart (${tip.cart.itemCount})",
                            onClick = onOpenCart,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TipPreviewChip(
    character: TipCharacterCatalogItem,
    quantityInCart: Int,
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null,
    focusChainDown: FocusRequester? = null,
) {
    val shape = RoundedCornerShape(12.dp)
    var focused by remember { mutableStateOf(false) }
    val context = LocalContext.current

    var chipModifier =
        Modifier
            .padding(end = 10.dp)
            .widthIn(max = 88.dp)
    if (focusRequester != null) {
        chipModifier = chipModifier.focusRequester(focusRequester)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            chipModifier
                .border(
                    width = if (focused) 2.dp else 1.dp,
                    color =
                        when {
                            focused -> TvAppColors.FocusRing
                            quantityInCart > 0 -> TvAppColors.FocusRingSecondary
                            else -> TvAppColors.ButtonIdleBorder
                        },
                    shape = shape,
                )
                .background(Color(0xCC12121A), shape)
                .onFocusChanged { focused = it.isFocused }
                .focusable(true)
                .tvFocusChainVertical(down = focusChainDown)
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    when (event.key) {
                        Key.DirectionCenter,
                        Key.Enter,
                        Key.NumPadEnter,
                        -> {
                            onClick()
                            true
                        }
                        else -> false
                    }
                }
                .padding(8.dp),
    ) {
        AsyncImage(
            model =
                ImageRequest.Builder(context)
                    .data(character.iconUrl)
                    .crossfade(true)
                    .build(),
            contentDescription = character.name,
            contentScale = ContentScale.Fit,
            modifier =
                Modifier
                    .size(48.dp)
                    .background(Color(0xFF1C1C28), RoundedCornerShape(8.dp))
                    .padding(4.dp),
        )
        Text(
            text = character.name,
            color = TvAppColors.TextPrimary,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            text = formatTipPrice(character.price),
            color = TvAppColors.TextSecondary,
            fontSize = 11.sp,
        )
        if (quantityInCart > 0) {
            Text(
                text = "×$quantityInCart",
                color = TvAppColors.FocusRingSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

internal fun formatTipPrice(price: Double): String =
    String.format(Locale.UK, "£%.2f", price)
