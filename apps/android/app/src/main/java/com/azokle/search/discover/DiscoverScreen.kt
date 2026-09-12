package com.azokle.search.discover

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
import com.azokle.search.common.components.FaviconImage
import com.azokle.search.common.theme.AzokleTheme

/**
 * Premium, Clean & Streamlined Discover Screen:
 *  - Slide section removed (no clunky horizontal carousels or sliding shelves).
 *  - Top Quick Access Dock featuring ChatGPT, Spotify, Amazon.in as #1, #2, #3.
 *  - Curated All Portals section directly accessible with authentic high-res favicons.
 *  - Minimalist search bar, crisp category pills, and zero text emojis.
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
            // 1. Clean & Minimal Header with Integrated Search Bar
            DiscoverLuxuryHeader(
                searchQuery = uiState.searchQuery,
                onQueryChange = { viewModel.updateSearchQuery(it) }
            )

            // 2. Refined Solid Category Filter Pills (No Emojis, High Contrast)
            DiscoverCategorySelector(
                selectedCategory = uiState.selectedCategory,
                onSelectCategory = { cat ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.selectCategory(cat)
                }
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 3. Main Content: Quick Access Dock + All Portals Feed
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
            ) {
                // When in "All" view with no search, show the Quick Access Dock first
                if (uiState.selectedCategory == DiscoverCategory.ALL && uiState.searchQuery.isBlank()) {
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
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    item(key = "all_portals_eyebrow") {
                        LuxurySectionEyebrow(title = "ALL PORTALS", count = uiState.displayedItems.size)
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                } else if (uiState.searchQuery.isNotBlank()) {
                    item(key = "search_results_eyebrow") {
                        LuxurySectionEyebrow(title = "SEARCH RESULTS", count = uiState.displayedItems.size)
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                } else {
                    item(key = "category_eyebrow") {
                        LuxurySectionEyebrow(title = uiState.selectedCategory.label.uppercase(), count = uiState.displayedItems.size)
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                // Empty State if no portals match filter or search
                if (uiState.displayedItems.isEmpty()) {
                    item(key = "empty_state") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = colors.textSecondary.copy(alpha = 0.4f),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No portals found",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Try searching for games, streaming, music, AI or tools",
                                    fontSize = 12.5.sp,
                                    color = colors.textSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // All Portals Feed
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

        // Clean Solid Search Bar
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
                            text = "Search portals, AI, games, music, shopping...",
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

/**
 * Top Quick Access Dock:
 * Clean 4-column grid featuring top services (ChatGPT, Spotify, Amazon.in at the top positions #1, #2, #3).
 */
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
                                url = item.domain.ifBlank { item.url },
                                faviconUrl = item.faviconUrl ?: "https://www.google.com/s2/favicons?domain=${item.domain}&sz=128",
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
                if (rowItems.size < 4) {
                    repeat(4 - rowItems.size) {
                        Spacer(modifier = Modifier.width(72.dp))
                    }
                }
            }
            if (index < rows.size - 1) {
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

/**
 * Clean & Luxury Portal Card:
 * Full-width card with high-resolution favicon, crisp titles, domain, category badge, and bookmark toggle.
 */
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
                url = item.domain.ifBlank { item.url },
                faviconUrl = item.faviconUrl,
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
