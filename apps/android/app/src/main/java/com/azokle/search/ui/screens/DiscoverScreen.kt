package com.azokle.search.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.graphics.Brush
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
 *  - Ethereal ambient lighting & deep glassmorphism.
 *  - Authentic high-resolution brand favicons & typography (no tacky emojis).
 *  - High-impact Editorial Spotlight Carousel for MX Player, MiniTV, Poki & CrazyGames.
 *  - 4-Column Quick Access Dock for immediate one-tap launching.
 *  - Dedicated shelves for Instant Web Games and Free Streaming.
 */
@OptIn(ExperimentalLayoutApi::class)
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
        // Luxury Ambient Aura Gradient (Deep Indigo / Violet Ethereal Glow)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF4F46E5).copy(alpha = 0.14f),
                            Color(0xFF7C3AED).copy(alpha = 0.07f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // 1. Luxury Header & Search Bar
            DiscoverLuxuryHeader(
                searchQuery = uiState.searchQuery,
                onQueryChange = { viewModel.updateSearchQuery(it) }
            )

            // 2. Refined Category Filter Pills
            DiscoverCategorySelector(
                selectedCategory = uiState.selectedCategory,
                onSelectCategory = { cat ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.selectCategory(cat)
                }
            )

            // 3. Scrollable Content Body
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 100.dp)
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
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // B. Quick Access 4-Column Springboard Dock (Top 8 Portals)
                    item(key = "quick_access_dock") {
                        LuxurySectionEyebrow(title = "QUICK ACCESS", count = uiState.topPickItems.size)
                        Spacer(modifier = Modifier.height(12.dp))
                        QuickAccessDock(
                            items = uiState.topPickItems,
                            onItemClick = { item ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onOpenUrl(item.url, item.title)
                            }
                        )
                        Spacer(modifier = Modifier.height(26.dp))
                    }

                    // C. Instant Web Games Shelf (Poki, CrazyGames, GameSnacks, etc.)
                    item(key = "games_shelf") {
                        LuxuryShelfHeader(
                            eyebrow = "INSTANT PLAY • 0 INSTALL",
                            title = "Web Games",
                            actionLabel = "See All",
                            onActionClick = { viewModel.selectCategory(DiscoverCategory.GAMES) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalPortalShelf(
                            items = uiState.gameItems,
                            onItemClick = { item ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onOpenUrl(item.url, item.title)
                            }
                        )
                        Spacer(modifier = Modifier.height(26.dp))
                    }

                    // D. Free Streaming & MiniTV Shelf (MX Player, MiniTV, JioCinema)
                    item(key = "streaming_shelf") {
                        LuxuryShelfHeader(
                            eyebrow = "FREE STREAMING • NO SUBSCRIPTION",
                            title = "Cinema & Shows",
                            actionLabel = "See All",
                            onActionClick = { viewModel.selectCategory(DiscoverCategory.STREAMING) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalPortalShelf(
                            items = uiState.streamingItems,
                            onItemClick = { item ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onOpenUrl(item.url, item.title)
                            }
                        )
                        Spacer(modifier = Modifier.height(26.dp))
                    }

                    // E. All Curated Portals Eyebrow
                    item(key = "all_portals_eyebrow") {
                        LuxurySectionEyebrow(title = "CURATED PORTALS", count = uiState.displayedItems.size)
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
                        modifier = Modifier.padding(vertical = 5.dp)
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
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(Color(0xFF818CF8), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "EXPLORE & ENTERTAINMENT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF818CF8),
                        letterSpacing = 1.6.sp
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Discover",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.textPrimary,
                    letterSpacing = (-0.6).sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Frosted Glass Search Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.cardBg.copy(alpha = 0.85f))
                .border(0.8.dp, colors.border.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = colors.textSecondary.copy(alpha = 0.8f),
                modifier = Modifier.size(19.dp)
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
                cursorBrush = SolidColor(Color(0xFF818CF8)),
                decorationBox = { innerTextField ->
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Search web games, streaming, music, AI...",
                            color = colors.textSecondary.copy(alpha = 0.65f),
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
                    modifier = Modifier.size(26.dp)
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
            .padding(vertical = 6.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(DiscoverCategory.values()) { category ->
            val isSelected = selectedCategory == category

            val bgModifier = if (isSelected) {
                Modifier
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF4338CA), Color(0xFF6366F1))
                        ),
                        RoundedCornerShape(22.dp)
                    )
                    .border(0.8.dp, Color(0xFF818CF8).copy(alpha = 0.6f), RoundedCornerShape(22.dp))
            } else {
                Modifier
                    .background(colors.cardBg.copy(alpha = 0.6f), RoundedCornerShape(22.dp))
                    .border(0.6.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(22.dp))
                    .then(bgModifier)
                    .clickable { onSelectCategory(category) }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .background(Color.White, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = category.label,
                    fontSize = 12.5.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else colors.textSecondary
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
        horizontalArrangement = Arrangement.spacedBy(14.dp),
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
    val gradient = Brush.linearGradient(
        colors = listOf(
            Color(item.primaryColorHex).copy(alpha = 0.92f),
            Color(item.secondaryColorHex).copy(alpha = 0.82f)
        )
    )

    Box(
        modifier = Modifier
            .width(290.dp)
            .height(154.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(gradient)
            .border(0.8.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: Brand Favicon & Domain Pill & Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(Color.White.copy(alpha = 0.95f)),
                        contentAlignment = Alignment.Center
                    ) {
                        FaviconImage(
                            url = item.url,
                            size = 22.dp
                        )
                    }
                    Spacer(modifier = Modifier.width(9.dp))
                    Text(
                        text = item.domain,
                        fontSize = 11.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }

                item.badge?.let { b ->
                    Box(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.38f), RoundedCornerShape(8.dp))
                            .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.5.dp)
                    ) {
                        Text(
                            text = b,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White,
                            letterSpacing = 0.8.sp
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
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = item.subtitle,
                        fontSize = 11.5.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 15.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
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
            .clip(RoundedCornerShape(18.dp))
            .background(colors.cardBg.copy(alpha = 0.75f))
            .border(0.8.dp, colors.border.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
            .padding(vertical = 14.dp, horizontal = 8.dp)
    ) {
        // Chunk items into rows of 4 for an Apple-style luxury springboard dock
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
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .background(colors.background)
                                .border(0.8.dp, colors.border.copy(alpha = 0.7f), RoundedCornerShape(15.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            FaviconImage(
                                url = item.url,
                                size = 26.dp
                            )
                        }

                        Spacer(modifier = Modifier.height(7.dp))

                        Text(
                            text = item.title,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
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
    eyebrow: String,
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
        Column {
            Text(
                text = eyebrow,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF818CF8),
                letterSpacing = 1.3.sp
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.textPrimary,
                letterSpacing = (-0.3).sp
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onActionClick)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = actionLabel,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF818CF8)
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
                    .width(136.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.cardBg.copy(alpha = 0.8f))
                    .border(0.8.dp, colors.border.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .clickable { onItemClick(item) }
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(colors.background)
                        .border(0.8.dp, colors.border.copy(alpha = 0.6f), RoundedCornerShape(13.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    FaviconImage(
                        url = item.url,
                        size = 26.dp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = item.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = item.domain,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = colors.textSecondary.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF4F46E5).copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.actionText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF818CF8)
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
            .clip(RoundedCornerShape(16.dp))
            .background(colors.cardBg.copy(alpha = 0.8f))
            .border(0.8.dp, colors.border.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .clickable(onClick = onItemClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // High-res Favicon in squircle
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(colors.background)
                .border(0.8.dp, colors.border.copy(alpha = 0.6f), RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center
        ) {
            FaviconImage(
                url = item.url,
                size = 25.dp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title and Domain Information
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.title,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                item.badge?.let { b ->
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                Color(0xFF4F46E5).copy(alpha = 0.15f),
                                RoundedCornerShape(5.dp)
                            )
                            .padding(horizontal = 5.dp, vertical = 1.5.dp)
                    ) {
                        Text(
                            text = b,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF818CF8)
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
                fontSize = 10.5.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF818CF8).copy(alpha = 0.85f)
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
                tint = if (isFavorite) Color(0xFFEF4444) else colors.textSecondary.copy(alpha = 0.5f),
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
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = colors.textSecondary.copy(alpha = 0.8f),
            letterSpacing = 1.5.sp
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
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = colors.textSecondary
                )
            }
        }
    }
}
