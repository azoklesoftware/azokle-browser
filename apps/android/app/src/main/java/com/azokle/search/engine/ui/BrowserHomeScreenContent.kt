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

package com.azokle.search.engine.ui

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.azokle.search.R
import com.azokle.search.common.components.FaviconImage
import com.azokle.search.common.theme.AudiowideFontFamily
import com.azokle.search.common.theme.AzokleTheme
import com.azokle.search.common.theme.AzokleWhite
import com.azokle.search.common.theme.adaptiveContentWidth
import com.azokle.search.search.SearchBarCapsule
import com.azokle.search.settings.WallpaperManager
import com.azokle.search.tabs.TabSession

/**
 * Composable that renders the browser home screen content UI component.
 */
@Composable
fun BrowserHomeScreenContent(
    activeTab: TabSession? = null,
    onNavigate: (String) -> Unit,
    onOpenSearchSheet: () -> Unit,
    onVoiceClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedSearchEngine: String = "azokle",
    onSelectSearchEngine: (String) -> Unit = {},
    searchBarModifier: Modifier = Modifier,
    searchEngineModifier: Modifier = Modifier,
    shortcutsModifier: Modifier = Modifier
) {
    val colors = AzokleTheme.colors
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val screenHeightDp = configuration.screenHeightDp
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE || screenWidthDp > screenHeightDp

    // Dynamic Responsive Typography & Spacing based on viewport dimensions (Memoized)
    val metrics = remember(screenWidthDp, screenHeightDp, isLandscape) {
        HomeScreenMetrics(
            logoFontSize = when {
                isLandscape -> 34.sp
                screenHeightDp < 620 || screenWidthDp < 360 -> 38.sp
                screenHeightDp < 760 -> 46.sp
                else -> 52.sp
            },
            topSpacerHeight = when {
                isLandscape -> 8.dp
                screenHeightDp < 620 -> 12.dp
                screenHeightDp < 760 -> 18.dp
                else -> 24.dp
            },
            midSpacerHeight = when {
                isLandscape -> 10.dp
                screenHeightDp < 620 -> 14.dp
                screenHeightDp < 760 -> 20.dp
                else -> 28.dp
            },
            itemsPerRow = when {
                screenWidthDp >= 720 || (isLandscape && screenWidthDp >= 640) -> 8
                screenWidthDp >= 520 -> 6
                else -> 4
            },
            shortcutTileSize = when {
                isLandscape || screenHeightDp < 600 || screenWidthDp < 360 -> 48.dp
                screenHeightDp >= 840 || screenWidthDp >= 600 -> 58.dp
                else -> 54.dp
            },
            shortcutIconSize = when {
                isLandscape || screenHeightDp < 600 || screenWidthDp < 360 -> 26.dp
                screenHeightDp >= 840 || screenWidthDp >= 600 -> 32.dp
                else -> 30.dp
            },
            shortcutRowSpacing = when {
                isLandscape || screenHeightDp < 620 -> 10.dp
                else -> 16.dp
            },
            contentHorizontalPadding = when {
                screenWidthDp < 360 -> 12.dp
                screenWidthDp < 480 -> 18.dp
                else -> 24.dp
            },
            contentVerticalPadding = when {
                isLandscape -> 12.dp
                screenHeightDp < 620 -> 16.dp
                else -> 24.dp
            }
        )
    }

    // Cached gradient brushes to prevent allocation churn on scroll/recomposition
    val topScrimBrush = remember {
        Brush.verticalGradient(
            listOf(
                Color.Black.copy(alpha = 0.60f),
                Color.Black.copy(alpha = 0.20f),
                Color.Transparent
            )
        )
    }

    val bottomScrimBrush = remember {
        Brush.verticalGradient(
            listOf(
                Color.Transparent,
                Color.Black.copy(alpha = 0.35f),
                Color.Black.copy(alpha = 0.70f)
            )
        )
    }

    val donateBgBrush = remember {
        Brush.horizontalGradient(
            listOf(
                Color.Black.copy(alpha = 0.45f),
                Color.Black.copy(alpha = 0.30f)
            )
        )
    }

    val donateBorderBrush = remember {
        Brush.horizontalGradient(
            listOf(
                Color.White.copy(alpha = 0.25f),
                Color.White.copy(alpha = 0.10f)
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
        contentAlignment = Alignment.Center
    ) {
        // 1. High-Resolution Photographic Wallpaper Backdrop (Per-Tab Wallpaper)
        val globalWallpaper by WallpaperManager.currentWallpaper.collectAsState()
        val targetWallpaper = activeTab?.wallpaperResId ?: globalWallpaper

        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(targetWallpaper)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Top Status Bar Scrim
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .align(Alignment.TopCenter)
                .background(topScrimBrush)
        )

        // Bottom Navigation Scrim
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .align(Alignment.BottomCenter)
                .background(bottomScrimBrush)
        )

        // Scrollable Responsive Center Content Stack (Guarantees zero clipping on any phone screen height)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .adaptiveContentWidth(680.dp)
                    .padding(horizontal = metrics.contentHorizontalPadding, vertical = metrics.contentVerticalPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Audiowide Brand Logo in Lowercase (Exact match to Home Page)
                Text(
                    text = "azokle",
                    color = AzokleWhite,
                    fontFamily = AudiowideFontFamily,
                    fontSize = metrics.logoFontSize,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = (-1.0).sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(metrics.topSpacerHeight))

                /*
                // Exact Home Page Search Bar Capsule
                SearchBarCapsule(
                    query = "",
                    placeholderText = stringResource(R.string.search_placeholder),
                    onQueryChange = {},
                    onSearch = { onNavigate(it) },
                    onVoiceClick = onVoiceClick,
                    onCapsuleClick = onOpenSearchSheet,
                    isReadOnly = true,
                    selectedEngineId = selectedSearchEngine,
                    onEngineSelected = onSelectSearchEngine,
                    modifier = searchBarModifier,
                    searchEngineModifier = searchEngineModifier
                )

                Spacer(modifier = Modifier.height(metrics.midSpacerHeight))
                */

                // Quick Speed Dial Shortcuts (Wikipedia, YouTube, Amazon, Trip, Reddit, Azokle Auth, Kick, Azokle)
                val shortcuts = remember {
                    listOf(
                        BrowserShortcut("Wikipedia", "https://www.wikipedia.org", "https://www.google.com/s2/favicons?domain=wikipedia.org&sz=128"),
                        BrowserShortcut("YouTube", "https://www.youtube.com", "https://www.google.com/s2/favicons?domain=youtube.com&sz=128"),
                        BrowserShortcut(
                            label = "Amazon",
                            url = "https://amzn.to/4qTBnWI",
                            faviconUrl = "https://www.google.com/s2/favicons?domain=amazon.in&sz=128",
                            isSponsored = true
                        ),
                        BrowserShortcut(
                            label = "Trip",
                            url = "https://www.trip.com/?Allianceid=10452016&SID=330440797&trip_sub1=&trip_sub3=D19702202",
                            faviconUrl = "https://www.google.com/s2/favicons?domain=trip.com&sz=128",
                            isSponsored = true
                        ),
                        BrowserShortcut("Reddit", "https://www.reddit.com", "https://www.google.com/s2/favicons?domain=reddit.com&sz=128"),
                        BrowserShortcut(
                            label = "Azokle Auth",
                            url = "https://azokle.com/authenticator",
                            iconResId = R.drawable.ic_azokle_auth
                        ),
                        BrowserShortcut("Kick", "https://kick.com", "https://www.google.com/s2/favicons?domain=kick.com&sz=128"),
                        BrowserShortcut("Azokle", "https://azokle.com", "https://azokle.com/favicon.ico")
                    )
                }

                val chunkedShortcuts = remember(shortcuts, metrics.itemsPerRow) { shortcuts.chunked(metrics.itemsPerRow) }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(shortcutsModifier)
                        .padding(horizontal = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(metrics.shortcutRowSpacing)
                ) {
                    chunkedShortcuts.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            rowItems.forEach { item ->
                                ShortcutTile(
                                    item = item,
                                    onNavigate = onNavigate,
                                    tileSize = metrics.shortcutTileSize,
                                    iconSize = metrics.shortcutIconSize,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Bottom spacer to ensure shortcuts never collide with the docked Donate button when scrolled
                Spacer(modifier = Modifier.height(42.dp))
            }
        }

        // 4. Sovereign Azokle Foundation "Donate" Action (Docked at bottom-right, just above bottom navigation)
        val haptic = LocalHapticFeedback.current
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 14.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(donateBgBrush)
                .border(
                    width = 0.8.dp,
                    brush = donateBorderBrush,
                    shape = RoundedCornerShape(20.dp)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = Color.White.copy(alpha = 0.25f)),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNavigate("https://azokle.com/foundation")
                    }
                )
                .padding(horizontal = 11.dp, vertical = 6.5.dp)
                .semantics {
                    role = Role.Button
                    contentDescription = "Donate to Azokle Foundation"
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                tint = Color(0xFFFB7185),
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "Donate",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.95f),
                letterSpacing = 0.2.sp,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.8f),
                        offset = Offset(0f, 1.5f),
                        blurRadius = 4f
                    )
                )
            )
        }
    }
}

private data class HomeScreenMetrics(
    val logoFontSize: androidx.compose.ui.unit.TextUnit,
    val topSpacerHeight: Dp,
    val midSpacerHeight: Dp,
    val itemsPerRow: Int,
    val shortcutTileSize: Dp,
    val shortcutIconSize: Dp,
    val shortcutRowSpacing: Dp,
    val contentHorizontalPadding: Dp,
    val contentVerticalPadding: Dp
)

/**
 * Composable that renders the shortcut tile UI component.
 */
@Composable
fun ShortcutTile(
    item: BrowserShortcut,
    onNavigate: (String) -> Unit,
    tileSize: Dp = 54.dp,
    iconSize: Dp = 30.dp,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val tileBgBrush = remember {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.22f),
                Color.White.copy(alpha = 0.08f)
            )
        )
    }
    val tileBorderBrush = remember {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.40f),
                Color.White.copy(alpha = 0.12f)
            )
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 32.dp, color = Color.White.copy(alpha = 0.25f)),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onNavigate(item.url)
                }
            )
            .padding(vertical = 4.dp, horizontal = 2.dp)
            .semantics {
                role = Role.Button
                contentDescription = if (item.isSponsored) "${item.label} (Sponsored) shortcut" else "${item.label} shortcut"
            }
    ) {
        // Frosted Glassmorphic Squircle Tile
        Box(
            modifier = Modifier
                .size(tileSize)
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = Color.Black.copy(alpha = 0.35f),
                    spotColor = Color.Black.copy(alpha = 0.45f)
                )
                .clip(RoundedCornerShape(16.dp))
                .background(tileBgBrush)
                .border(
                    width = 1.dp,
                    brush = tileBorderBrush,
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (item.iconResId != null) {
                Image(
                    painter = painterResource(id = item.iconResId),
                    contentDescription = item.label,
                    modifier = Modifier.size(iconSize)
                )
            } else {
                FaviconImage(
                    url = item.url,
                    faviconUrl = item.faviconUrl,
                    size = iconSize
                )
            }

            if (item.isSponsored) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 3.dp, end = 3.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.30f), RoundedCornerShape(3.dp))
                        .padding(horizontal = 2.5.dp, vertical = 0.5.dp)
                ) {
                    Text(
                        text = "Ad",
                        fontSize = 6.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.85f),
                        letterSpacing = 0.1.sp,
                        lineHeight = 7.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = item.label,
            fontSize = if (tileSize < 50.dp) 11.sp else 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            style = TextStyle(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.75f),
                    offset = Offset(0f, 2f),
                    blurRadius = 6f
                )
            )
        )
    }
}

data class BrowserShortcut(
    val label: String,
    val url: String,
    val faviconUrl: String? = null,
    val iconResId: Int? = null,
    val isSponsored: Boolean = false
)
