package com.azokle.search.history

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azokle.search.history.HistoryItem
import com.azokle.search.history.HistoryManager
import com.azokle.search.common.components.FaviconImage
import com.azokle.search.common.theme.AzokleRadius
import com.azokle.search.common.theme.AzokleTheme
import com.azokle.search.common.theme.adaptiveContentWidth
import com.azokle.search.search.HomeViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class HistoryFilter {
    ALL,
    PAGES,
    SEARCHES
}

enum class GroupPosition {
    SINGLE,
    FIRST,
    MIDDLE,
    LAST
}

sealed class HistoryUiModel {
    data class Header(val title: String) : HistoryUiModel()
    data class Entry(val item: HistoryItem, val position: GroupPosition) : HistoryUiModel()
}

@Composable
fun HistoryScreen(
    viewModel: HomeViewModel,
    onHistoryQueryClick: (String) -> Unit,
    onOpenUrl: ((String) -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = AzokleTheme.colors
    val historyList by HistoryManager.history.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(HistoryFilter.ALL) }
    var showClearDialog by remember { mutableStateOf(false) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }

    BackHandler(enabled = isSelectionMode) {
        isSelectionMode = false
        selectedIds = emptySet()
    }

    val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    // Filter & Search
    val filteredHistory = remember(historyList, searchQuery, selectedFilter) {
        historyList.filter { item ->
            val matchesFilter = when (selectedFilter) {
                HistoryFilter.ALL -> true
                HistoryFilter.PAGES -> !item.isSearch
                HistoryFilter.SEARCHES -> item.isSearch
            }
            val matchesQuery = searchQuery.isBlank() ||
                    item.title.contains(searchQuery, ignoreCase = true) ||
                    item.url.contains(searchQuery, ignoreCase = true) ||
                    (item.searchQuery?.contains(searchQuery, ignoreCase = true) == true)
            matchesFilter && matchesQuery
        }
    }

    // Grouping by Date & Virtualized UI Models
    val uiItems = remember(filteredHistory) {
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val yesterdayStart = todayStart - (24 * 60 * 60 * 1000L)
        val lastWeekStart = todayStart - (7 * 24 * 60 * 60 * 1000L)

        val groups = linkedMapOf<String, MutableList<HistoryItem>>()
        for (item in filteredHistory) {
            val groupKey = when {
                item.timestamp >= todayStart -> "Today"
                item.timestamp >= yesterdayStart -> "Yesterday"
                item.timestamp >= lastWeekStart -> "Previous 7 Days"
                else -> "Older"
            }
            groups.getOrPut(groupKey) { mutableListOf() }.add(item)
        }

        val result = ArrayList<HistoryUiModel>(filteredHistory.size + groups.size)
        for ((dateGroup, items) in groups) {
            result.add(HistoryUiModel.Header(dateGroup))
            val size = items.size
            items.forEachIndexed { index, item ->
                val position = when {
                    size == 1 -> GroupPosition.SINGLE
                    index == 0 -> GroupPosition.FIRST
                    index == size - 1 -> GroupPosition.LAST
                    else -> GroupPosition.MIDDLE
                }
                result.add(HistoryUiModel.Entry(item, position))
            }
        }
        result
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .adaptiveContentWidth(680.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Clean Top Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onClose != null) {
                        Box(
                            modifier = Modifier
                                .sizeIn(minWidth = 44.dp, minHeight = 44.dp)
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = false, radius = 18.dp, color = colors.textPrimary),
                                    onClick = onClose
                                )
                                .semantics {
                                    role = Role.Button
                                    contentDescription = "Back"
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(colors.surfaceElevated)
                                    .border(1.dp, colors.border, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null,
                                    tint = colors.textPrimary,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    Text(
                        text = if (isSelectionMode) "${selectedIds.size} Selected" else "History",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        modifier = Modifier.semantics { heading() }
                    )

                    if (historyList.isNotEmpty() && !isSelectionMode) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(colors.surfaceElevated)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${historyList.size}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = colors.textMuted
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isSelectionMode) {
                        if (selectedIds.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .sizeIn(minWidth = 44.dp, minHeight = 44.dp)
                                    .clip(CircleShape)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(bounded = false, radius = 18.dp, color = colors.statusDestructive),
                                        onClick = {
                                            HistoryManager.deleteEntries(selectedIds.toList())
                                            selectedIds = emptySet()
                                            isSelectionMode = false
                                            Toast.makeText(context, "Deleted selected entries", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                    .semantics {
                                        role = Role.Button
                                        contentDescription = "Delete selected history"
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(colors.statusDestructive.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = colors.statusDestructive,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Box(
                            modifier = Modifier
                                .sizeIn(minWidth = 44.dp, minHeight = 44.dp)
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = false, radius = 18.dp, color = colors.textPrimary),
                                    onClick = {
                                        isSelectionMode = false
                                        selectedIds = emptySet()
                                    }
                                )
                                .semantics {
                                    role = Role.Button
                                    contentDescription = "Cancel selection"
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(colors.surfaceElevated)
                                    .border(1.dp, colors.border, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = colors.textPrimary,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    } else if (historyList.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .sizeIn(minWidth = 44.dp, minHeight = 44.dp)
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = false, radius = 18.dp, color = colors.statusDestructive),
                                    onClick = { showClearDialog = true }
                                )
                                .semantics {
                                    role = Role.Button
                                    contentDescription = "Clear all history"
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(colors.surfaceElevated)
                                    .border(1.dp, colors.border, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = null,
                                    tint = colors.textMuted,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Search Input Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(AzokleRadius.md)
                    .background(colors.surfaceElevated)
                    .border(1.dp, colors.border, AzokleRadius.md)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = colors.textMuted,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        textStyle = TextStyle(
                            color = colors.textPrimary,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(colors.textPrimary),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search browsing history...",
                                    color = colors.textMuted,
                                    fontSize = 13.5.sp
                                )
                            }
                            innerTextField()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .sizeIn(minWidth = 32.dp, minHeight = 32.dp)
                                .clip(CircleShape)
                                .clickable { searchQuery = "" },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = colors.textMuted,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            // 3. Filter Segmented Chips (All, Webpages, Searches)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Spacer(modifier = Modifier.width(8.dp)) }
                items(HistoryFilter.values()) { filter ->
                    val isSelected = selectedFilter == filter
                    val chipShape = AzokleRadius.full
                    val label = when (filter) {
                        HistoryFilter.ALL -> "All"
                        HistoryFilter.PAGES -> "Webpages"
                        HistoryFilter.SEARCHES -> "Searches"
                    }
                    Box(
                        modifier = Modifier
                            .clip(chipShape)
                            .background(if (isSelected) colors.actionPrimaryBg else colors.surfaceElevated)
                            .border(1.dp, if (isSelected) Color.Transparent else colors.border, chipShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = colors.textPrimary.copy(alpha = 0.15f)),
                                onClick = { selectedFilter = filter }
                            )
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .semantics {
                                role = Role.Tab
                                contentDescription = "$label history filter"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) colors.actionPrimaryText else colors.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
                item { Spacer(modifier = Modifier.width(8.dp)) }
            }

            // 4. Content Area
            if (filteredHistory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceElevated)
                                .border(1.dp, colors.border, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = colors.textMuted,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No matching history" else "No browsing history",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "Try a different search term" else "Searches and visited websites will securely appear here",
                            fontSize = 12.5.sp,
                            color = colors.textMuted
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                ) {
                    items(
                        items = uiItems,
                        key = { model ->
                            when (model) {
                                is HistoryUiModel.Header -> "hdr_${model.title}"
                                is HistoryUiModel.Entry -> model.item.id
                            }
                        },
                        contentType = { model ->
                            when (model) {
                                is HistoryUiModel.Header -> 0
                                is HistoryUiModel.Entry -> 1
                            }
                        }
                    ) { model ->
                        when (model) {
                            is HistoryUiModel.Header -> {
                                Text(
                                    text = model.title.uppercase(),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textMuted,
                                    letterSpacing = 0.8.sp,
                                    modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 6.dp)
                                )
                            }
                            is HistoryUiModel.Entry -> {
                                val item = model.item
                                val isSelected = selectedIds.contains(item.id)
                                val shape = when (model.position) {
                                    GroupPosition.SINGLE -> AzokleRadius.lg
                                    GroupPosition.FIRST -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                                    GroupPosition.MIDDLE -> RoundedCornerShape(0.dp)
                                    GroupPosition.LAST -> RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                                }
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(shape)
                                        .background(colors.cardBg)
                                        .historyCardBorder(model.position, colors.border)
                                ) {
                                    HistoryGroupedRow(
                                        item = item,
                                        isSelectionMode = isSelectionMode,
                                        isSelected = isSelected,
                                        timeFormatter = timeFormatter,
                                        colors = colors,
                                        onClick = {
                                            if (isSelectionMode) {
                                                selectedIds = if (isSelected) selectedIds - item.id else selectedIds + item.id
                                            } else {
                                                if (item.isSearch && item.searchQuery != null) {
                                                    onHistoryQueryClick(item.searchQuery)
                                                } else if (onOpenUrl != null) {
                                                    onOpenUrl(item.url)
                                                } else {
                                                    onHistoryQueryClick(item.url)
                                                }
                                            }
                                        },
                                        onLongClick = {
                                            isSelectionMode = true
                                            selectedIds = selectedIds + item.id
                                        },
                                        onDelete = {
                                            HistoryManager.deleteEntry(item.id)
                                        }
                                    )

                                    if (model.position == GroupPosition.FIRST || model.position == GroupPosition.MIDDLE) {
                                        HorizontalDivider(
                                            color = colors.border.copy(alpha = 0.5f),
                                            thickness = 0.5.dp,
                                            modifier = Modifier.padding(start = 56.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(96.dp)) }
                }
            }
        }
    }

    // Clear History Dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(text = "Clear Browsing History?", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
            text = { Text(text = "Choose whether to clear today's browsing history or purge all local history.", fontSize = 13.5.sp) },
            confirmButton = {
                TextButton(
                    onClick = {
                        HistoryManager.clearAll()
                        viewModel.clearRecentSearches()
                        showClearDialog = false
                        Toast.makeText(context, "All history purged", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(text = "Clear All", color = colors.statusDestructive, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            HistoryManager.clearToday()
                            showClearDialog = false
                            Toast.makeText(context, "Today's history cleared", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text(text = "Clear Today")
                    }
                    TextButton(onClick = { showClearDialog = false }) {
                        Text(text = "Cancel")
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryGroupedRow(
    item: HistoryItem,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    timeFormatter: SimpleDateFormat,
    colors: com.azokle.search.common.theme.AzokleThemeColors,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 54.dp)
            .background(if (isSelected) colors.actionPrimaryBg.copy(alpha = 0.08f) else Color.Transparent)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = colors.textPrimary.copy(alpha = 0.06f)),
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .semantics {
                role = Role.Button
                contentDescription = "${item.displayTitle}, ${item.cleanHost}"
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) colors.actionPrimaryBg else Color.Transparent)
                    .border(1.5.dp, if (isSelected) colors.actionPrimaryBg else colors.border, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = colors.actionPrimaryText,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
        }

        // Icon Favicon or Search Glyph
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(colors.surfaceElevated)
                .border(1.dp, colors.border, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (item.isSearch) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                FaviconImage(
                    url = item.url,
                    faviconUrl = item.faviconUrl,
                    size = 18.dp
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title & Metadata
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.displayTitle,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(1.5.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (item.isSearch) "Search query" else item.cleanHost,
                    fontSize = 11.sp,
                    color = colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "• ${timeFormatter.format(Date(item.timestamp))}",
                    fontSize = 10.5.sp,
                    color = colors.textMuted
                )
                if (item.visitCount > 1) {
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "(${item.visitCount})",
                        fontSize = 10.sp,
                        color = colors.textMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        if (!isSelectionMode) {
            Box(
                modifier = Modifier
                    .sizeIn(minWidth = 32.dp, minHeight = 36.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false, radius = 14.dp, color = colors.textMuted),
                        onClick = onDelete
                    )
                    .semantics {
                        role = Role.Button
                        contentDescription = "Delete entry"
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = colors.textMuted.copy(alpha = 0.7f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

private fun Modifier.historyCardBorder(position: GroupPosition, borderColor: Color): Modifier = when (position) {
    GroupPosition.SINGLE -> this.border(1.dp, borderColor, AzokleRadius.lg)
    GroupPosition.FIRST -> this.drawBehind {
        val strokeWidth = 1.dp.toPx()
        val halfStroke = strokeWidth / 2f
        val cornerRadius = 16.dp.toPx()
        val path = Path().apply {
            moveTo(halfStroke, size.height)
            lineTo(halfStroke, cornerRadius + halfStroke)
            arcTo(
                rect = Rect(halfStroke, halfStroke, halfStroke + cornerRadius * 2, halfStroke + cornerRadius * 2),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            lineTo(size.width - halfStroke - cornerRadius, halfStroke)
            arcTo(
                rect = Rect(size.width - halfStroke - cornerRadius * 2, halfStroke, size.width - halfStroke, halfStroke + cornerRadius * 2),
                startAngleDegrees = 270f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            lineTo(size.width - halfStroke, size.height)
        }
        drawPath(path, color = borderColor, style = Stroke(width = strokeWidth))
    }
    GroupPosition.MIDDLE -> this.drawBehind {
        val strokeWidth = 1.dp.toPx()
        val halfStroke = strokeWidth / 2f
        drawLine(borderColor, Offset(halfStroke, 0f), Offset(halfStroke, size.height), strokeWidth)
        drawLine(borderColor, Offset(size.width - halfStroke, 0f), Offset(size.width - halfStroke, size.height), strokeWidth)
    }
    GroupPosition.LAST -> this.drawBehind {
        val strokeWidth = 1.dp.toPx()
        val halfStroke = strokeWidth / 2f
        val cornerRadius = 16.dp.toPx()
        val path = Path().apply {
            moveTo(halfStroke, 0f)
            lineTo(halfStroke, size.height - halfStroke - cornerRadius)
            arcTo(
                rect = Rect(halfStroke, size.height - halfStroke - cornerRadius * 2, halfStroke + cornerRadius * 2, size.height - halfStroke),
                startAngleDegrees = 180f,
                sweepAngleDegrees = -90f,
                forceMoveTo = false
            )
            lineTo(size.width - halfStroke - cornerRadius, size.height - halfStroke)
            arcTo(
                rect = Rect(size.width - halfStroke - cornerRadius * 2, size.height - halfStroke - cornerRadius * 2, size.width - halfStroke, size.height - halfStroke),
                startAngleDegrees = 90f,
                sweepAngleDegrees = -90f,
                forceMoveTo = false
            )
            lineTo(size.width - halfStroke, 0f)
        }
        drawPath(path, color = borderColor, style = Stroke(width = strokeWidth))
    }
}
