/*
 * Copyright (c) 2026 The Azokle Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.azokle.search.common.theme

import android.content.res.Configuration
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
) {
    /**
     * Determines whether the bottom navigation bar should be hidden due to horizontal rotation or tablet screen size.
     */
    val shouldHideBottomBar: Boolean
        get() = isLandscape || isTabletOrExpanded

    /**
     * Determines whether the Omnibar 3-dot overflow menu should be visible to replace the bottom navigation bar.
     */
    val showOmnibarOverflowMenu: Boolean
        get() = shouldHideBottomBar
}

/**
 * Composable that renders the remember window size UI component.
 */
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

    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE || screenWidth > screenHeight
    val isTabletOrExpanded = configuration.smallestScreenWidthDp >= 600 || screenWidth >= 600

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
