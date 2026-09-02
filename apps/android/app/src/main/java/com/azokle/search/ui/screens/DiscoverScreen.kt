package com.azokle.search.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.azokle.search.data.news.DiscoverLayoutMode
import com.azokle.search.data.news.NewsArticle
import com.azokle.search.data.news.NewsCategory
import com.azokle.search.ui.components.DiscoverSettingsSheet
import com.azokle.search.ui.theme.AzokleRadius
import com.azokle.search.ui.theme.AzokleTheme
import com.azokle.search.ui.theme.adaptiveContentWidth
import com.azokle.search.viewmodel.DiscoverTabSection
import com.azokle.search.viewmodel.DiscoverViewModel
import kotlin.math.roundToInt

/**
 * Dedicated Sovereign Discover Screen:
 *  - Pull-Down Swipe to Refresh with Spring Overscroll
 *  - Endless Infinite Scroll Feed
 *  - Header Settings/Tune Button (replaces reload button)
 *  - Saved Reading List & Muted Publisher Filters
 */
@Composable
fun DiscoverScreen(
    viewModel: DiscoverViewModel,
    onTopicClick: (String) -> Unit = {},
    onNewsClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = AzokleTheme.colors
    val haptic = LocalHapticFeedback.current
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()

    var showSettingsSheet by remember { mutableStateOf(false) }

    // --- Pull Down to Refresh State ---
    var pullRefreshOffset by remember { mutableFloatStateOf(0f) }
    val isAtTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
        }
    }

    val animatedOffset by animateFloatAsState(
        targetValue = if (uiState.isNewsRefreshing) 80f else pullRefreshOffset,
        animationSpec = tween(durationMillis = 200),
        label = "pullOffset"
    )

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // If scrolling up while pulled down, consume to collapse
                if (pullRefreshOffset > 0 && available.y < 0) {
                    val consumed = available.y.coerceAtLeast(-pullRefreshOffset)
                    pullRefreshOffset += consumed
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (isAtTop && available.y > 0 && !uiState.isNewsRefreshing) {
                    val damping = 0.45f
                    pullRefreshOffset = (pullRefreshOffset + available.y * damping).coerceIn(0f, 220f)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: androidx.compose.ui.unit.Velocity): androidx.compose.ui.unit.Velocity {
                if (pullRefreshOffset >= 110f && !uiState.isNewsRefreshing) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.refreshNews()
                }
                pullRefreshOffset = 0f
                return androidx.compose.ui.unit.Velocity.Zero
            }
        }
    }

    // --- Endless Infinite Scroll Trigger ---
    val shouldLoadMore = remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 4 && lastVisible >= totalItems - 3
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && !uiState.isNewsLoading && !uiState.isNewsLoadingMore && uiState.currentSection == DiscoverTabSection.FEED) {
            viewModel.loadMoreNews()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "refreshSpin")
    val spinnerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .nestedScroll(nestedScrollConnection)
    ) {
        // Minimalist Ambient Radial Glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            colors.statusAccent.copy(alpha = if (colors.isDark) 0.08f else 0.03f),
                            Color.Transparent
                        ),
                        center = Offset.Unspecified,
                        radius = 900f
                    )
                )
        )

        Column(
            modifier = modifier
                .fillMaxSize()
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .adaptiveContentWidth(680.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Top Header Bar with Settings / Tune Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 14.dp, top = 6.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.semantics { heading() }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(colors.cardBg)
                                .border(1.dp, colors.border, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Explore,
                                contentDescription = null,
                                tint = colors.statusAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "DISCOVER",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = colors.textPrimary,
                                    letterSpacing = 0.8.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (uiState.isNewsOffline) Color(0xFFF59E0B) else colors.statusAccent)
                                )
                            }
                            Text(
                                text = if (uiState.isNewsOffline) "Offline Feed • Cached" else "Live News • Zero Tracking",
                                fontSize = 11.sp,
                                color = colors.textSecondary
                            )
                        }
                    }

                    // Top Action Buttons: [Saved Stories 🔖] & [Settings / Tune ⚙️]
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                val nextSection = if (uiState.currentSection == DiscoverTabSection.FEED)
                                    DiscoverTabSection.SAVED_STORIES else DiscoverTabSection.FEED
                                viewModel.selectSection(nextSection)
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (uiState.currentSection == DiscoverTabSection.SAVED_STORIES) colors.actionPrimaryBg else colors.surface)
                                .border(1.dp, colors.border, CircleShape)
                        ) {
                            Icon(
                                imageVector = if (uiState.currentSection == DiscoverTabSection.SAVED_STORIES) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Saved Reading List",
                                tint = if (uiState.currentSection == DiscoverTabSection.SAVED_STORIES) colors.actionPrimaryText else colors.textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Settings Button (Replaced reload button)
                        IconButton(
                            onClick = { showSettingsSheet = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(colors.surface)
                                .border(1.dp, colors.border, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Discover Settings",
                                tint = colors.textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // 2. Section Pills (Feed vs Saved) & Category Filter Carousel
                if (uiState.currentSection == DiscoverTabSection.FEED) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(NewsCategory.values()) { cat ->
                            val isSelected = uiState.selectedCategory == cat
                            val icon = getCategoryIcon(cat)

                            Box(
                                modifier = Modifier
                                    .clip(AzokleRadius.full)
                                    .background(
                                        if (isSelected) colors.textPrimary
                                        else colors.cardBg
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) colors.textPrimary else colors.border,
                                        AzokleRadius.full
                                    )
                                    .clickable { viewModel.selectCategory(cat) }
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) colors.background else colors.textSecondary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = cat.title,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) colors.background else colors.textPrimary
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = null,
                            tint = colors.statusAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SAVED READING LIST (${uiState.savedArticles.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = colors.textPrimary,
                            letterSpacing = 0.7.sp
                        )
                    }
                }

                // 3. Offline / Error Banner
                AnimatedVisibility(
                    visible = uiState.isNewsOffline && uiState.newsArticles.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clip(AzokleRadius.md)
                            .background(Color(0xFFF59E0B).copy(alpha = 0.12f))
                            .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.3f), AzokleRadius.md)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Offline cached stories • Pull down to refresh when connected",
                            fontSize = 11.sp,
                            color = colors.textPrimary
                        )
                    }
                }
            }

            // Pull to Refresh Floating Indicator
            if (animatedOffset > 10f || uiState.isNewsRefreshing) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(colors.cardBg)
                            .border(1.dp, colors.border, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = colors.statusAccent
                        )
                    }
                }
            }

            // 4. Content Feed Area
            if (uiState.currentSection == DiscoverTabSection.SAVED_STORIES) {
                if (uiState.savedArticles.isEmpty()) {
                    DiscoverEmptySavedState(colors = colors)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(uiState.savedArticles, key = { "saved_${it.id}" }) { article ->
                            YouTubeStyleFullBleedNewsCard(
                                article = article,
                                isSaved = true,
                                onClick = { onNewsClick(article.url) },
                                onToggleSave = { viewModel.toggleSaveArticle(article) },
                                onMutePublisher = { viewModel.mutePublisher(article.sourceName, article.sourceDomain) },
                                onShare = { shareArticle(context, article) },
                                colors = colors
                            )
                        }
                    }
                }
            } else if (uiState.isNewsLoading) {
                DiscoverSkeletonFeed(colors = colors)
            } else if (uiState.newsArticles.isEmpty()) {
                DiscoverEmptyState(
                    error = uiState.newsError,
                    onRetry = { viewModel.refreshNews() },
                    colors = colors
                )
            } else {
                val articles = uiState.newsArticles

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 14.dp,
                        end = 14.dp,
                        top = 2.dp,
                        bottom = 96.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Type 1: YouTube Home Full-Bleed Video-Style Cover (Lead Story)
                    if (articles.isNotEmpty()) {
                        item(key = "lead_hero_${articles[0].id}") {
                            if (uiState.layoutMode == DiscoverLayoutMode.COMPACT_EDITORIAL) {
                                EditorialSplitCard(
                                    article = articles[0],
                                    isSaved = viewModel.isArticleSaved(articles[0].id),
                                    onClick = { onNewsClick(articles[0].url) },
                                    onToggleSave = { viewModel.toggleSaveArticle(articles[0]) },
                                    onShare = { shareArticle(context, articles[0]) },
                                    colors = colors
                                )
                            } else {
                                YouTubeStyleFullBleedNewsCard(
                                    article = articles[0],
                                    isSaved = viewModel.isArticleSaved(articles[0].id),
                                    onClick = { onNewsClick(articles[0].url) },
                                    onToggleSave = { viewModel.toggleSaveArticle(articles[0]) },
                                    onMutePublisher = { viewModel.mutePublisher(articles[0].sourceName, articles[0].sourceDomain) },
                                    onShare = { shareArticle(context, articles[0]) },
                                    colors = colors
                                )
                            }
                        }
                    }

                    // Type 2: Trending Spotlight Horizontal Shelf
                    if (articles.size >= 4) {
                        item(key = "trending_shelf") {
                            TrendingStoriesShelf(
                                stories = articles.take(5),
                                onStoryClick = { onNewsClick(it.url) },
                                colors = colors
                            )
                        }
                    }

                    // Multi-Layout Endless Feed Distribution
                    val remainingArticles = if (articles.size > 1) articles.subList(1, articles.size) else emptyList()

                    itemsIndexed(
                        items = remainingArticles,
                        key = { _, item -> "feed_${item.id}" }
                    ) { index, article ->
                        val isSaved = viewModel.isArticleSaved(article.id)

                        if (uiState.layoutMode == DiscoverLayoutMode.COMPACT_EDITORIAL) {
                            EditorialSplitCard(
                                article = article,
                                isSaved = isSaved,
                                onClick = { onNewsClick(article.url) },
                                onToggleSave = { viewModel.toggleSaveArticle(article) },
                                onShare = { shareArticle(context, article) },
                                colors = colors
                            )
                        } else {
                            when {
                                index % 3 == 0 -> {
                                    YouTubeStyleFullBleedNewsCard(
                                        article = article,
                                        isSaved = isSaved,
                                        onClick = { onNewsClick(article.url) },
                                        onToggleSave = { viewModel.toggleSaveArticle(article) },
                                        onMutePublisher = { viewModel.mutePublisher(article.sourceName, article.sourceDomain) },
                                        onShare = { shareArticle(context, article) },
                                        colors = colors
                                    )
                                }
                                index % 4 == 0 -> {
                                    DeepDiveHighlightCard(
                                        article = article,
                                        onClick = { onNewsClick(article.url) },
                                        onShare = { shareArticle(context, article) },
                                        colors = colors
                                    )
                                }
                                else -> {
                                    EditorialSplitCard(
                                        article = article,
                                        isSaved = isSaved,
                                        onClick = { onNewsClick(article.url) },
                                        onToggleSave = { viewModel.toggleSaveArticle(article) },
                                        onShare = { shareArticle(context, article) },
                                        colors = colors
                                    )
                                }
                            }
                        }
                    }

                    // Endless Infinite Scroll Loading Footer
                    if (uiState.isNewsLoadingMore) {
                        item(key = "endless_loading_footer") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 18.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = colors.statusAccent
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Loading more stories...",
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Discover Settings Sheet
        if (showSettingsSheet) {
            DiscoverSettingsSheet(
                currentLayoutMode = uiState.layoutMode,
                currentRegionCode = uiState.regionCode,
                mutedPublishers = uiState.mutedPublishers,
                onSelectLayoutMode = { viewModel.setLayoutMode(it) },
                onSelectRegion = { viewModel.setNewsRegion(it) },
                onUnmutePublisher = { viewModel.unmutePublisher(it) },
                onClearCache = { viewModel.clearOfflineCache() },
                onDismiss = { showSettingsSheet = false }
            )
        }
    }
}

/**
 * 🎬 TYPE 1: YouTube Home-Style Full-Bleed Video/Story Card with Action Dropdown
 */
@Composable
private fun YouTubeStyleFullBleedNewsCard(
    article: NewsArticle,
    isSaved: Boolean = false,
    onClick: () -> Unit,
    onToggleSave: () -> Unit = {},
    onMutePublisher: () -> Unit = {},
    onShare: () -> Unit,
    colors: com.azokle.search.ui.theme.AzokleThemeColors,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(AzokleRadius.xl)
            .background(colors.cardBg)
            .border(1.dp, colors.border, AzokleRadius.xl)
            .clickable(onClick = onClick)
            .padding(bottom = 14.dp)
    ) {
        // 1. 16:9 Thumbnail Cover / Dynamic Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(getDynamicGradient(article.gradientThemeIndex, colors.isDark))
        ) {
            if (!article.thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(article.thumbnailUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = article.sourceName.take(3).uppercase(),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White.copy(alpha = 0.85f),
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = article.cleanHost,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Duration Overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.78f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${article.readingTimeMinutes} MIN READ",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
            }

            // Category Pill
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(AzokleRadius.full)
                    .background(colors.background.copy(alpha = 0.85f))
                    .border(1.dp, colors.border, AzokleRadius.full)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = article.category.uppercase(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = colors.textPrimary,
                    letterSpacing = 0.6.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Channel Avatar & Headline Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(colors.surface)
                    .border(1.dp, colors.border, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (article.faviconUrl.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(article.faviconUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Text(
                        text = article.sourceName.take(1).uppercase(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = article.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 21.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = article.sourceName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Verified",
                        tint = colors.statusAccent,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = " • ${article.timeAgoFormatted}",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                }
            }

            // 3-Dots Action Dropdown Menu
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Story Options",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(colors.modalBg)
                ) {
                    DropdownMenuItem(
                        text = { Text(if (isSaved) "Remove from Reading List" else "Save to Reading List", color = colors.textPrimary) },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                tint = colors.statusAccent
                            )
                        },
                        onClick = {
                            showMenu = false
                            onToggleSave()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Share Story", color = colors.textPrimary) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = colors.textPrimary
                            )
                        },
                        onClick = {
                            showMenu = false
                            onShare()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Mute ${article.sourceName}", color = colors.statusDestructive) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = null,
                                tint = colors.statusDestructive
                            )
                        },
                        onClick = {
                            showMenu = false
                            onMutePublisher()
                        }
                    )
                }
            }
        }
    }
}

/**
 * ⚡ TYPE 2: Trending Spotlight Horizontal Shelf
 */
@Composable
private fun TrendingStoriesShelf(
    stories: List<NewsArticle>,
    onStoryClick: (NewsArticle) -> Unit,
    colors: com.azokle.search.ui.theme.AzokleThemeColors,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(AzokleRadius.xl)
            .background(colors.cardBg.copy(alpha = 0.6f))
            .border(1.dp, colors.border, AzokleRadius.xl)
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                contentDescription = null,
                tint = colors.statusAccent,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "TRENDING SPOTLIGHT",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = colors.textPrimary,
                letterSpacing = 0.7.sp
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(stories) { story ->
                Box(
                    modifier = Modifier
                        .width(220.dp)
                        .height(130.dp)
                        .clip(AzokleRadius.lg)
                        .background(getDynamicGradient(story.gradientThemeIndex, colors.isDark))
                        .clickable { onStoryClick(story) }
                        .padding(12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (story.faviconUrl.isNotBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(story.faviconUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = story.sourceName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Text(
                            text = story.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 17.sp
                        )

                        Text(
                            text = story.timeAgoFormatted,
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 📰 TYPE 3: Sleek Editorial Split Card
 */
@Composable
private fun EditorialSplitCard(
    article: NewsArticle,
    isSaved: Boolean = false,
    onClick: () -> Unit,
    onToggleSave: () -> Unit = {},
    onShare: () -> Unit,
    colors: com.azokle.search.ui.theme.AzokleThemeColors,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(AzokleRadius.lg)
            .background(colors.cardBg)
            .border(1.dp, colors.border, AzokleRadius.lg)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (article.faviconUrl.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(article.faviconUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = article.sourceName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    text = " • ${article.timeAgoFormatted}",
                    fontSize = 11.sp,
                    color = colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = article.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 19.sp
            )

            if (!article.snippet.isNullOrBlank() && article.thumbnailUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = article.snippet,
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(AzokleRadius.md)
                .background(getDynamicGradient(article.gradientThemeIndex, colors.isDark)),
            contentAlignment = Alignment.Center
        ) {
            if (!article.thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(article.thumbnailUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = article.sourceName.take(2).uppercase(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * 💡 TYPE 4: Deep Dive Highlight Card
 */
@Composable
private fun DeepDiveHighlightCard(
    article: NewsArticle,
    onClick: () -> Unit,
    onShare: () -> Unit,
    colors: com.azokle.search.ui.theme.AzokleThemeColors,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(AzokleRadius.xl)
            .background(colors.surface)
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        colors.statusAccent.copy(alpha = 0.6f),
                        colors.border,
                        colors.cardBg
                    )
                ),
                AzokleRadius.xl
            )
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(colors.statusAccent)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "DEEP DIVE • ${article.sourceName.uppercase()}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = colors.statusAccent,
                    letterSpacing = 0.8.sp
                )
            }

            Text(
                text = article.timeAgoFormatted,
                fontSize = 11.sp,
                color = colors.textSecondary
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = article.title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            lineHeight = 22.sp
        )

        if (!article.snippet.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "\"${article.snippet}\"",
                fontSize = 13.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                color = colors.textSecondary,
                lineHeight = 18.sp
            )
        }
    }
}

/**
 * Animated Shimmer Skeleton Loading Feed.
 */
@Composable
private fun DiscoverSkeletonFeed(
    colors: com.azokle.search.ui.theme.AzokleThemeColors
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val shimmerColors = listOf(
        colors.surface,
        colors.cardBg.copy(alpha = 0.8f),
        colors.surface
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim, y = translateAnim)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(AzokleRadius.xl)
                .background(brush)
        )

        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(AzokleRadius.lg)
                    .background(brush)
            )
        }
    }
}

/**
 * Empty Saved Articles State.
 */
@Composable
private fun DiscoverEmptySavedState(
    colors: com.azokle.search.ui.theme.AzokleThemeColors
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(colors.surface)
                .border(1.dp, colors.border, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.BookmarkBorder,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No Saved Stories Yet",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Tap the bookmark icon or 3-dots menu on any story to save it for offline reading.",
            fontSize = 13.sp,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}

/**
 * Empty / Error State with Retry Button.
 */
@Composable
private fun DiscoverEmptyState(
    error: String?,
    onRetry: () -> Unit,
    colors: com.azokle.search.ui.theme.AzokleThemeColors
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(colors.surface)
                .border(1.dp, colors.border, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Newspaper,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Feed Temporarily Unavailable",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = error ?: "Check your connection and pull down to refresh news.",
            fontSize = 13.sp,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .clip(AzokleRadius.full)
                .background(colors.textPrimary)
                .clickable(onClick = onRetry)
                .padding(horizontal = 24.dp, vertical = 10.dp)
        ) {
            Text(
                text = "Retry",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = colors.background
            )
        }
    }
}

private fun getDynamicGradient(themeIndex: Int, isDark: Boolean): Brush {
    val gradients = if (isDark) {
        listOf(
            listOf(Color(0xFF1E1B4B), Color(0xFF312E81)),
            listOf(Color(0xFF064E3B), Color(0xFF047857)),
            listOf(Color(0xFF4C0519), Color(0xFF881337)),
            listOf(Color(0xFF164E63), Color(0xFF0E7490)),
            listOf(Color(0xFF3B0764), Color(0xFF6B21A8)),
            listOf(Color(0xFF18181B), Color(0xFF27272A))
        )
    } else {
        listOf(
            listOf(Color(0xFF6366F1), Color(0xFF818CF8)),
            listOf(Color(0xFF10B981), Color(0xFF34D399)),
            listOf(Color(0xFFF43F5E), Color(0xFFFB7185)),
            listOf(Color(0xFF06B6D4), Color(0xFF22D3EE)),
            listOf(Color(0xFF8B5CF6), Color(0xFFA78BFA)),
            listOf(Color(0xFF475569), Color(0xFF64748B))
        )
    }
    val colors = gradients[themeIndex % gradients.size]
    return Brush.linearGradient(colors)
}

private fun getCategoryIcon(category: NewsCategory): ImageVector {
    return when (category) {
        NewsCategory.TOP_STORIES -> Icons.AutoMirrored.Filled.TrendingUp
        NewsCategory.TECHNOLOGY -> Icons.Default.Science
        NewsCategory.BUSINESS -> Icons.Default.Business
        NewsCategory.SCIENCE -> Icons.Default.Explore
        NewsCategory.HEALTH -> Icons.Default.LocalHospital
        NewsCategory.ENTERTAINMENT -> Icons.Default.Movie
        NewsCategory.SPORTS -> Icons.Default.FitnessCenter
        NewsCategory.WORLD -> Icons.Default.Public
    }
}

private fun shareArticle(context: android.content.Context, article: NewsArticle) {
    try {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, article.title)
            putExtra(Intent.EXTRA_TEXT, "${article.title}\n\n${article.url}")
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share Article"))
    } catch (e: Exception) {
        // Ignore
    }
}
