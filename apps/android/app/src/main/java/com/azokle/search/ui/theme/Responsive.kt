package com.azokle.search.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object AzokleBreakpoints {
    val CompactWidth = 360.dp
    val MediumWidth = 600.dp
    val ExpandedWidth = 840.dp
    val CompactHeight = 520.dp
    val MaxContentWidth = 680.dp
    val MaxSheetWidth = 560.dp
    val MaxDialogWidth = 480.dp
}

enum class WindowWidthSize {
    COMPACT,
    MEDIUM,
    EXPANDED
}

enum class WindowHeightSize {
    COMPACT,
    MEDIUM,
    EXPANDED
}

data class AzokleWindowSize(
    val width: WindowWidthSize,
    val height: WindowHeightSize,
    val widthDp: Int,
    val heightDp: Int,
    val isLandscape: Boolean,
    val isTabletOrExpanded: Boolean
)

@Composable
fun rememberWindowSize(): AzokleWindowSize {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp

    val widthSize = when {
        screenWidth < 600 -> WindowWidthSize.COMPACT
        screenWidth < 840 -> WindowWidthSize.MEDIUM
        else -> WindowWidthSize.EXPANDED
    }

    val heightSize = when {
        screenHeight < 520 -> WindowHeightSize.COMPACT
        screenHeight < 900 -> WindowHeightSize.MEDIUM
        else -> WindowHeightSize.EXPANDED
    }

    val isLandscape = screenWidth > screenHeight
    val isTabletOrExpanded = screenWidth >= 600

    return AzokleWindowSize(
        width = widthSize,
        height = heightSize,
        widthDp = screenWidth,
        heightDp = screenHeight,
        isLandscape = isLandscape,
        isTabletOrExpanded = isTabletOrExpanded
    )
}

/**
 * Bounds content to an optimal reading / interactive width and centers it on wide screens.
 */
fun Modifier.adaptiveContentWidth(maxWidth: Dp = AzokleBreakpoints.MaxContentWidth): Modifier {
    return this
        .fillMaxWidth()
        .widthIn(max = maxWidth)
}
