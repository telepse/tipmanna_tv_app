package com.example.telepse.ui.tv

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * TV-friendly vertical list: [Column] + scroll + [BringIntoViewSpec].
 *
 * Standard [androidx.compose.foundation.lazy.LazyColumn] does not wire D-pad focus
 * between sibling items on Android TV (focus stays on the first actionable item).
 * Short hub/control lists use a scrollable column instead.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TvLazyColumn(
    modifier: Modifier = Modifier,
    scrollable: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scrollState = rememberScrollState()
    val bringIntoViewSpec = remember { TvDefaultBringIntoViewSpec() }
    val columnModifier =
        if (scrollable) {
            modifier.verticalScroll(scrollState)
        } else {
            modifier
        }
    CompositionLocalProvider(LocalBringIntoViewSpec provides bringIntoViewSpec) {
        Column(
            modifier = columnModifier.focusGroup(),
            content = content,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TvLazyRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val scrollState = rememberScrollState()
    val bringIntoViewSpec = remember { TvDefaultBringIntoViewSpec() }
    CompositionLocalProvider(LocalBringIntoViewSpec provides bringIntoViewSpec) {
        Row(
            modifier = modifier.horizontalScroll(scrollState),
            content = content,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
internal class TvDefaultBringIntoViewSpec : BringIntoViewSpec {
    override fun calculateScrollDistance(
        offset: Float,
        size: Float,
        containerSize: Float,
    ): Float {
        val parentFraction = 0.3f
        val childFraction = 0f
        val initialTargetForLeadingEdge =
            parentFraction * containerSize - (childFraction * size)
        val targetForLeadingEdge =
            if (size <= containerSize && (containerSize - initialTargetForLeadingEdge) < size) {
                containerSize - size
            } else {
                initialTargetForLeadingEdge
            }
        return offset - targetForLeadingEdge
    }
}
