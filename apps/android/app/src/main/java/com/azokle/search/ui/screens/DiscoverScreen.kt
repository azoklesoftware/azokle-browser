package com.azokle.search.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azokle.search.browser.ui.components.FaviconImage
import com.azokle.search.data.discover.DiscoverCategory
import com.azokle.search.data.discover.DiscoverItem
import com.azokle.search.ui.theme.AzokleTheme
import com.azokle.search.viewmodel.DiscoverViewModel

/**
 * Premium, Clean & Luxury Discover Hub Screen:
 *  - Pure solid background without muddy aura gradients or blotchy top glow.
 *  - Minimalist, confident header area with razor-sharp typography.
 *  - Refined, gradient-free search bar and solid high-contrast category pills.
 *  - Clean, luxury solid cards for Spotlight, Quick Access Dock, and Shelves.
 *  - Authentic high-resolution brand favicons (Poki, CrazyGames, MX Player, miniTV, etc.).
 */
@Composable
fun DiscoverScreen(
    viewModel: DiscoverViewModel,
    onOpenUrl: (url: String, title: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AzokleTheme.colors
    val haptic = LocalHapticFeedback.current
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // 1. Clean & Minimal Header & Search Bar
            DiscoverLuxuryHeader(
                searchQuery = uiState.searchQuery,
                onQueryChange = { viewModel.updateSearchQuery(it) }
            )

            // 2. Refined Solid Category Filter Pills (No Gradients)
            DiscoverCategorySelector(
                selectedCategory = uiState.selectedCategory,
                onSelectCategory = { cat ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.selectCategory(cat)
                }
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 3. Scrollable Content Body
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
            ) {
                // If in "All" view with no active search, show Editorial Spotlight, Quick Access Dock & Shelves
                if (uiState.selectedCategory == DiscoverCategory.ALL && uiState.searchQuery.isBlank()) {
                    // A. Editorial Spotlight Carousel (MX Player, MiniTV, Poki, CrazyGames, etc.)
                    item(key = "spotlight_carousel") {
                        SpotlightCarousel(
                            items = uiState.featuredItems,
                            onItemClick = { item ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onOpenUrl(item.url, item.title)
                            }
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // B. Quick Access 4-Column Springboard Dock (Top 8 Portals)
                    item(key = "quick_access_dock") {
                        LuxurySectionEyebrow(title = "QUICK ACCESS", count = uiState.topPickItems.size)
                        Spacer(modifier = Modifier.height(10.dp))
                        QuickAccessDock(
                            items = uiState.topPickItems,
                            onItemClick = { item ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onOpenUrl(item.url, item.title)
                            }
                        )
                        Spacer(modifier = Modifier.height(22.dp))
                    }

                    // C. Instant Web Games Shelf (Poki, CrazyGames, 1001Games, etc.)
                    item(key = "games_shelf") {
                        LuxuryShelfHeader(
                            title = "Web Games",
                            actionLabel = "See all",
                            onActionClick = { viewModel.selectCategory(DiscoverCategory.GAMES) }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalPortalShelf(
                            items = uiState.gameItems,
                            onItemClick = { item ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onOpenUrl(item.url, item.title)
                            }
                        )
                        Spacer(modifier = Modifier.height(22.dp))
                    }

                    // D. Free Streaming & MiniTV Shelf (MX Player, MiniTV, Disney+ Hotstar)
                    item(key = "streaming_shelf") {
                        LuxuryShelfHeader(
                            title = "Cinema & Shows",
                            actionLabel = "See all",
                            onActionClick = { viewModel.selectCategory(DiscoverCategory.STREAMING) }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalPortalShelf(
                            items = uiState.streamingItems,
                            onItemClick = { item ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onOpenUrl(item.url, item.title)
                            }
                        )
                        Spacer(modifier = Modifier.height(22.dp))
                    }

                    // E. All Curated Portals Eyebrow
                    item(key = "all_portals_eyebrow") {
                        LuxurySectionEyebrow(title = "ALL PORTALS", count = uiState.displayedItems.size)
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                // Portals Feed / Filtered Search Results
                items(
                    items = uiState.displayedItems,
                    key = { it.id }
                ) { item ->
                    val isFavorite = uiState.favoriteIds.contains(item.id)
                    LuxuryPortalCard(
                        item = item,
                        isFavorite = isFavorite,
                        onItemClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onOpenUrl(item.url, item.title)
                        },
                        onToggleFavorite = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.toggleFavorite(item.id)
                        },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DiscoverLuxuryHeader(
    searchQuery: String,
    onQueryChange: (String) -> Unit
) {
    val colors = AzokleTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Discover",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                letterSpacing = (-0.5).sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Clean Solid Search Bar (No blur, no messy border glow)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.cardBg)
                .border(0.8.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = colors.textSecondary.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            BasicTextField(
                value = searchQuery,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(colors.textPrimary),
                decorationBox = { innerTextField ->
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Search web games, streaming, music, AI...",
                            color = colors.textSecondary.copy(alpha = 0.6f),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                    innerTextField()
                },
                modifier = Modifier.weight(1f)
            )
            if (searchQuery.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DiscoverCategorySelector(
    selectedCategory: DiscoverCategory,
    onSelectCategory: (DiscoverCategory) -> Unit
) {
    val colors = AzokleTheme.colors

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(DiscoverCategory.values()) { category ->
            val isSelected = selectedCategory == category

            val bgModifier = if (isSelected) {
                Modifier.background(colors.textPrimary, RoundedCornerShape(20.dp))
            } else {
                Modifier
                    .background(colors.cardBg, RoundedCornerShape(20.dp))
                    .border(0.8.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .then(bgModifier)
                    .clickable { onSelectCategory(category) }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = category.label,
                    fontSize = 12.5.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) colors.background else colors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun SpotlightCarousel(
    items: List<DiscoverItem>,
    onItemClick: (DiscoverItem) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(end = 8.dp)
    ) {
        items(items, key = { "spotlight_${it.id}" }) { item ->
            SpotlightCard(
                item = item,
                onClick = { onItemClick(item) }
            )
        }
    }
}

@Composable
private fun SpotlightCard(
    item: DiscoverItem,
    onClick: () -> Unit
) {
    val colors = AzokleTheme.colors

    Box(
        modifier = Modifier
            .width(280.dp)
            .height(148.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.cardBg)
            .border(0.8.dp, colors.border.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: Brand Favicon & Domain & Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.background)
                            .border(0.8.dp, colors.border.copy(alpha = 0.6f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        FaviconImage(
                            url = item.url,
                            size = 20.dp
                        )
                    }
                    Spacer(modifier = Modifier.width(9.dp))
                    Text(
                        text = item.domain,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        color = colors.textSecondary
                    )
                }

                item.badge?.let { b ->
                    Box(
                        modifier = Modifier
                            .background(colors.border.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = b,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace,
                            color = colors.textPrimary,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            // Bottom Content & Launch Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.subtitle,
                        fontSize = 11.5.sp,
                        color = colors.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 15.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(colors.background)
                        .border(0.8.dp, colors.border.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickAccessDock(
    items: List<DiscoverItem>,
    onItemClick: (DiscoverItem) -> Unit
) {
    val colors = AzokleTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.cardBg)
            .border(0.8.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(vertical = 14.dp, horizontal = 8.dp)
    ) {
        val rows = items.chunked(4)
        rows.forEachIndexed { index, rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                rowItems.forEach { item ->
                    Column(
                        modifier = Modifier
                            .width(72.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onItemClick(item) }
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(colors.background)
                                .border(0.8.dp, colors.border.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            FaviconImage(
                                url = item.url,
                                size = 24.dp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = item.title,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            if (index < rows.size - 1) {
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun LuxuryShelfHeader(
    title: String,
    actionLabel: String,
    onActionClick: () -> Unit
) {
    val colors = AzokleTheme.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            letterSpacing = (-0.3).sp
        )

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onActionClick)
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Text(
                text = actionLabel,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textSecondary
            )
        }
    }
}

@Composable
private fun HorizontalPortalShelf(
    items: List<DiscoverItem>,
    onItemClick: (DiscoverItem) -> Unit
) {
    val colors = AzokleTheme.colors

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(end = 8.dp)
    ) {
        items(items, key = { "shelf_${it.id}" }) { item ->
            Column(
                modifier = Modifier
                    .width(132.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.cardBg)
                    .border(0.8.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .clickable { onItemClick(item) }
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.background)
                        .border(0.8.dp, colors.border.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    FaviconImage(
                        url = item.url,
                        size = 24.dp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = item.title,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = item.domain,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(colors.border.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.actionText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun LuxuryPortalCard(
    item: DiscoverItem,
    isFavorite: Boolean,
    onItemClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AzokleTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.cardBg)
            .border(0.8.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .clickable(onClick = onItemClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // High-res Favicon in squircle
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.background)
                .border(0.8.dp, colors.border.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            FaviconImage(
                url = item.url,
                size = 24.dp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title and Domain Information
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                item.badge?.let { b ->
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                colors.border.copy(alpha = 0.35f),
                                RoundedCornerShape(5.dp)
                            )
                            .padding(horizontal = 5.dp, vertical = 1.5.dp)
                    ) {
                        Text(
                            text = b,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace,
                            color = colors.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = item.subtitle,
                fontSize = 11.5.sp,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = item.domain,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = colors.textSecondary.copy(alpha = 0.75f)
            )
        }

        // Bookmark / Favorite Icon
        IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Favorite",
                tint = if (isFavorite) Color(0xFFEF4444) else colors.textSecondary.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun LuxurySectionEyebrow(
    title: String,
    count: Int? = null
) {
    val colors = AzokleTheme.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textSecondary,
            letterSpacing = 0.8.sp
        )
        if (count != null) {
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .background(colors.border.copy(alpha = 0.4f), CircleShape)
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            ) {
                Text(
                    text = count.toString(),
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textSecondary
                )
            }
        }
    }
}
