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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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

    // Dynamic Responsive Typography & Spacing based on viewport dimensions
    val logoFontSize = when {
        isLandscape -> 34.sp
        screenHeightDp < 620 || screenWidthDp < 360 -> 38.sp
        screenHeightDp < 760 -> 46.sp
        else -> 52.sp
    }

    val topSpacerHeight = when {
        isLandscape -> 8.dp
        screenHeightDp < 620 -> 12.dp
        screenHeightDp < 760 -> 18.dp
        else -> 24.dp
    }

    val midSpacerHeight = when {
        isLandscape -> 10.dp
        screenHeightDp < 620 -> 14.dp
        screenHeightDp < 760 -> 20.dp
        else -> 28.dp
    }

    val itemsPerRow = when {
        screenWidthDp >= 720 || (isLandscape && screenWidthDp >= 640) -> 8
        screenWidthDp >= 520 -> 6
        else -> 4
    }

    val shortcutTileSize = when {
        isLandscape || screenHeightDp < 600 || screenWidthDp < 360 -> 48.dp
        screenHeightDp >= 840 || screenWidthDp >= 600 -> 58.dp
        else -> 54.dp
    }

    val shortcutIconSize = when {
        isLandscape || screenHeightDp < 600 || screenWidthDp < 360 -> 26.dp
        screenHeightDp >= 840 || screenWidthDp >= 600 -> 32.dp
        else -> 30.dp
    }

    val shortcutRowSpacing = when {
        isLandscape || screenHeightDp < 620 -> 10.dp
        else -> 16.dp
    }

    val contentHorizontalPadding = when {
        screenWidthDp < 360 -> 12.dp
        screenWidthDp < 480 -> 18.dp
        else -> 24.dp
    }

    val contentVerticalPadding = when {
        isLandscape -> 12.dp
        screenHeightDp < 620 -> 16.dp
        else -> 24.dp
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
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.60f),
                            Color.Black.copy(alpha = 0.20f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Bottom Navigation Scrim
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.35f),
                            Color.Black.copy(alpha = 0.70f)
                        )
                    )
                )
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
                    .padding(horizontal = contentHorizontalPadding, vertical = contentVerticalPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Audiowide Brand Logo in Lowercase (Exact match to Home Page)
                Text(
                    text = "azokle",
                    color = AzokleWhite,
                    fontFamily = AudiowideFontFamily,
                    fontSize = logoFontSize,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = (-1.0).sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(topSpacerHeight))

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

                Spacer(modifier = Modifier.height(midSpacerHeight))

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

                val chunkedShortcuts = remember(shortcuts, itemsPerRow) { shortcuts.chunked(itemsPerRow) }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(shortcutsModifier)
                        .padding(horizontal = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(shortcutRowSpacing)
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
                                    tileSize = shortcutTileSize,
                                    iconSize = shortcutIconSize,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShortcutTile(
    item: BrowserShortcut,
    onNavigate: (String) -> Unit,
    tileSize: Dp = 54.dp,
    iconSize: Dp = 30.dp,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

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
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.22f),
                            Color.White.copy(alpha = 0.08f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.40f),
                            Color.White.copy(alpha = 0.12f)
                        )
                    ),
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
