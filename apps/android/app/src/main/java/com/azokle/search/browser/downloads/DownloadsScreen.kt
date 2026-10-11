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

package com.azokle.search.downloads

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azokle.search.common.theme.AzokleBlack
import com.azokle.search.common.theme.AzokleRadius
import com.azokle.search.common.theme.AzokleTheme
import com.azokle.search.common.theme.AzokleWhite
import com.azokle.search.common.theme.adaptiveContentWidth
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Composable that renders the downloads screen UI component.
 */
@Composable
fun DownloadsScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = AzokleTheme.colors
    val downloadsList by DownloadManager.downloads.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(FileCategory.ALL) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<DownloadItem?>(null) }

    val filteredDownloads = remember(downloadsList, searchQuery, selectedCategory) {
        downloadsList.filter { item ->
            val matchesCategory = when (selectedCategory) {
                FileCategory.ALL -> true
                else -> item.category == selectedCategory
            }
            val matchesQuery = searchQuery.isBlank() ||
                    item.filename.contains(searchQuery.trim(), ignoreCase = true) ||
                    item.url.contains(searchQuery.trim(), ignoreCase = true)
            matchesCategory && matchesQuery
        }
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
                .adaptiveContentWidth(640.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    // Back button
                    Box(
                        modifier = Modifier
                            .sizeIn(minWidth = 36.dp, minHeight = 36.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = false, radius = 18.dp, color = colors.textSecondary),
                                onClick = onDismiss
                            )
                            .semantics {
                                role = Role.Button
                                contentDescription = "Back"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceElevated)
                                .border(0.8.dp, colors.borderSubtle, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                tint = colors.textPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = "Downloads",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                        modifier = Modifier.semantics { heading() }
                    )

                    if (downloadsList.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(colors.surfaceElevated)
                                .border(0.8.dp, colors.borderSubtle, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            AnimatedContent(
                                targetState = downloadsList.size,
                                transitionSpec = {
                                    (slideInVertically { height -> height / 2 } + fadeIn(tween(180)))
                                        .togetherWith(slideOutVertically { height -> -height / 2 } + fadeOut(tween(140)))
                                },
                                label = "DownloadsCountBadge"
                            ) { count ->
                                Text(
                                    text = "$count",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }
                }

                if (downloadsList.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .sizeIn(minWidth = 36.dp, minHeight = 36.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = false, radius = 18.dp, color = colors.statusDestructive),
                                onClick = { showClearConfirmDialog = true }
                            )
                            .semantics {
                                role = Role.Button
                                contentDescription = "Clear downloads"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceElevated)
                                .border(0.8.dp, colors.borderSubtle, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = colors.textMuted,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            // 2. Compact Search Input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .height(42.dp)
                    .clip(AzokleRadius.md)
                    .background(colors.surfaceElevated)
                    .border(0.8.dp, colors.borderSubtle, AzokleRadius.md)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = colors.textMuted,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    textStyle = TextStyle(
                        color = colors.textPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    cursorBrush = SolidColor(if (colors.isDark) AzokleWhite else AzokleBlack),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search downloads...",
                                color = colors.textMuted,
                                fontSize = 13.5.sp
                            )
                        }
                        innerTextField()
                    }
                )
                if (searchQuery.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear search",
                        tint = colors.textMuted,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = false, radius = 12.dp, color = colors.textMuted),
                                onClick = { searchQuery = "" }
                            )
                    )
                }
            }

            // 3. Category Filter Pills
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(FileCategory.values()) { category ->
                    val isSelected = selectedCategory == category
                    val chipBg by animateColorAsState(
                        targetValue = if (isSelected) colors.actionPrimaryBg else colors.surfaceElevated,
                        animationSpec = tween(durationMillis = 180),
                        label = "CategoryChipBg"
                    )
                    val chipTextColor by animateColorAsState(
                        targetValue = if (isSelected) colors.actionPrimaryText else colors.textSecondary,
                        animationSpec = tween(durationMillis = 180),
                        label = "CategoryChipText"
                    )

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(chipBg)
                            .border(
                                width = 0.8.dp,
                                color = if (isSelected) Color.Transparent else colors.borderSubtle,
                                shape = CircleShape
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, radius = 16.dp, color = colors.textPrimary.copy(alpha = 0.1f)),
                                onClick = { selectedCategory = category }
                            )
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                            .semantics {
                                role = Role.Tab
                                contentDescription = category.name
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (category) {
                                FileCategory.ALL -> "All"
                                FileCategory.ACTIVE -> "Active"
                                FileCategory.IMAGES -> "Images"
                                FileCategory.MEDIA -> "Media"
                                FileCategory.DOCUMENTS -> "Docs"
                                FileCategory.OTHER -> "Other"
                            },
                            color = chipTextColor,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }

            // 4. Downloads Content List
            if (filteredDownloads.isEmpty()) {
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
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceElevated)
                                .border(0.8.dp, colors.borderSubtle, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (searchQuery.isNotBlank()) Icons.Default.Search else Icons.Outlined.FileDownload,
                                contentDescription = null,
                                tint = colors.textMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = if (searchQuery.isNotBlank()) "No matching downloads" else "No downloads",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (searchQuery.isNotBlank()) "Try a different keyword" else "Downloaded files will appear here",
                            fontSize = 12.sp,
                            color = colors.textMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(AzokleRadius.lg)
                                .background(colors.surfaceElevated)
                                .border(0.8.dp, colors.borderSubtle, AzokleRadius.lg)
                        ) {
                            filteredDownloads.forEachIndexed { index, item ->
                                DownloadItemRow(
                                    item = item,
                                    colors = colors,
                                    onOpen = { DownloadManager.openFile(context, item) },
                                    onShare = { DownloadManager.shareFile(context, item) },
                                    onPause = { DownloadManager.pauseDownload(context, item.id) },
                                    onResume = { DownloadManager.resumeDownload(context, item.id) },
                                    onCancel = { DownloadManager.cancelDownload(context, item.id) },
                                    onRetry = { DownloadManager.retryDownload(context, item.id) },
                                    onDelete = { itemToDelete = item }
                                )

                                if (index < filteredDownloads.size - 1) {
                                    HorizontalDivider(
                                        color = colors.borderSubtle,
                                        thickness = 0.5.dp,
                                        modifier = Modifier.padding(start = 50.dp)
                                    )
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(20.dp)) }
                }
            }
        }
    }

    // Confirmation Dialog: Clear All History
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text(text = "Clear Downloads?", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = colors.textPrimary) },
            text = { Text(text = "Remove all download records from history.", fontSize = 13.sp, color = colors.textSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        DownloadManager.clearAllHistory(context)
                        showClearConfirmDialog = false
                    }
                ) {
                    Text(text = "Clear All", color = colors.statusDestructive, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text(text = "Cancel", color = colors.textSecondary, fontSize = 13.sp)
                }
            },
            containerColor = colors.cardBg,
            shape = AzokleRadius.xl
        )
    }

    // Confirmation Dialog: Single Item Delete
    if (itemToDelete != null) {
        val target = itemToDelete!!
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text(text = "Delete Download?", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = colors.textPrimary) },
            text = { Text(text = target.filename, fontSize = 13.sp, color = colors.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            confirmButton = {
                TextButton(
                    onClick = {
                        DownloadManager.deleteDownload(context, target.id, deleteFile = true)
                        itemToDelete = null
                    }
                ) {
                    Text(text = "Delete File", color = colors.statusDestructive, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        DownloadManager.deleteDownload(context, target.id, deleteFile = false)
                        itemToDelete = null
                    }
                ) {
                    Text(text = "Record Only", color = colors.textSecondary, fontSize = 13.sp)
                }
            },
            containerColor = colors.cardBg,
            shape = AzokleRadius.xl
        )
    }
}

/**
 * Composable that renders the download item row UI component.
 */
@Composable
private fun DownloadItemRow(
    item: DownloadItem,
    colors: com.azokle.search.common.theme.AzokleThemeColors,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit
) {
    val (icon, iconTint) = remember(item.mimeType, item.filename) {
        val lower = item.filename.lowercase()
        val mime = item.mimeType.lowercase()
        when {
            mime.startsWith("image/") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp") || lower.endsWith(".gif") || lower.endsWith(".svg") ->
                Pair(Icons.Default.Image, Color(0xFFEC4899))
            mime.startsWith("video/") || lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".webm") ->
                Pair(Icons.Default.Movie, Color(0xFF8B5CF6))
            mime.startsWith("audio/") || lower.endsWith(".mp3") || lower.endsWith(".wav") || lower.endsWith(".m4a") ->
                Pair(Icons.Default.MusicNote, Color(0xFF06B6D4))
            mime.contains("pdf") || lower.endsWith(".pdf") ->
                Pair(Icons.Default.PictureAsPdf, Color(0xFFEF4444))
            lower.endsWith(".apk") ->
                Pair(Icons.Default.Android, Color(0xFF22C55E))
            mime.contains("zip") || mime.contains("tar") || lower.endsWith(".zip") || lower.endsWith(".rar") || lower.endsWith(".7z") ->
                Pair(Icons.Default.FolderZip, Color(0xFFF59E0B))
            else ->
                Pair(Icons.AutoMirrored.Filled.InsertDriveFile, colors.textPrimary)
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = item.progress,
        animationSpec = spring(stiffness = 500f),
        label = "DownloadProgress"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 52.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = colors.textPrimary.copy(alpha = 0.08f)),
                onClick = {
                    if (item.status == DownloadStatus.COMPLETED) {
                        onOpen()
                    }
                }
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .semantics {
                role = Role.Button
                contentDescription = "${item.filename}, ${item.status.name}"
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Badge
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(AzokleRadius.sm)
                .background(colors.cardBg)
                .border(0.8.dp, colors.borderSubtle, AzokleRadius.sm),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Center Details
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.filename,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(1.dp))

            when (item.status) {
                DownloadStatus.DOWNLOADING -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = colors.actionPrimaryBg,
                            trackColor = colors.cardBg
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.formattedProgress,
                            fontSize = 10.sp,
                            color = colors.actionPrimaryBg,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                DownloadStatus.PAUSED -> {
                    Text(
                        text = "Paused • ${item.formattedSize}",
                        fontSize = 11.sp,
                        color = colors.statusWarning
                    )
                }
                DownloadStatus.COMPLETED -> {
                    Text(
                        text = "${item.formattedSize} • ${formatHumanTimestamp(item.timestamp)}",
                        fontSize = 11.sp,
                        color = colors.textMuted
                    )
                }
                DownloadStatus.FAILED -> {
                    Text(
                        text = "Failed • ${item.errorMessage ?: "Error"}",
                        fontSize = 11.sp,
                        color = colors.statusDestructive,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                DownloadStatus.CANCELLED -> {
                    Text(
                        text = "Cancelled",
                        fontSize = 11.sp,
                        color = colors.textMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Action Buttons
        Row(verticalAlignment = Alignment.CenterVertically) {
            when (item.status) {
                DownloadStatus.DOWNLOADING -> {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onPause),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = "Pause", tint = colors.textPrimary, modifier = Modifier.size(15.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onCancel),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", tint = colors.statusDestructive, modifier = Modifier.size(15.dp))
                    }
                }
                DownloadStatus.PAUSED -> {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onResume),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Resume", tint = colors.actionPrimaryBg, modifier = Modifier.size(15.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onCancel),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", tint = colors.statusDestructive, modifier = Modifier.size(15.dp))
                    }
                }
                DownloadStatus.FAILED -> {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onRetry),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = colors.actionPrimaryBg, modifier = Modifier.size(15.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onDelete),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = colors.textMuted, modifier = Modifier.size(15.dp))
                    }
                }
                DownloadStatus.COMPLETED, DownloadStatus.CANCELLED -> {
                    if (item.status == DownloadStatus.COMPLETED) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onShare),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = colors.textSecondary, modifier = Modifier.size(14.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onDelete),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = colors.textMuted, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }
    }
}

/**
 * Formats human timestamp for display.
 */
private fun formatHumanTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val calNow = Calendar.getInstance()
    val calTime = Calendar.getInstance().apply { timeInMillis = timestamp }

    return when {
        diff < 60_000L -> "Just now"
        calNow.get(Calendar.YEAR) == calTime.get(Calendar.YEAR) &&
                calNow.get(Calendar.DAY_OF_YEAR) == calTime.get(Calendar.DAY_OF_YEAR) -> {
            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            timeFormat.format(Date(timestamp))
        }
        calNow.get(Calendar.YEAR) == calTime.get(Calendar.YEAR) &&
                calNow.get(Calendar.DAY_OF_YEAR) - calTime.get(Calendar.DAY_OF_YEAR) == 1 -> {
            "Yesterday"
        }
        else -> {
            val fullFormat = SimpleDateFormat("MMM d", Locale.getDefault())
            fullFormat.format(Date(timestamp))
        }
    }
}
