package com.azokle.search.browser.downloads

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azokle.search.ui.theme.AzokleRadius
import com.azokle.search.ui.theme.AzokleTheme
import com.azokle.search.ui.theme.adaptiveContentWidth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
                    // Back Button (48dp Touch Target)
                    Box(
                        modifier = Modifier
                            .sizeIn(minWidth = 44.dp, minHeight = 44.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = false, radius = 18.dp, color = colors.textPrimary),
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

                    Text(
                        text = "Downloads",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        modifier = Modifier.semantics { heading() }
                    )

                    if (downloadsList.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(colors.surfaceElevated)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${downloadsList.size}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = colors.textMuted
                            )
                        }
                    }
                }

                if (downloadsList.isNotEmpty()) {
                    // Clear All Action (48dp Touch Target)
                    Box(
                        modifier = Modifier
                            .sizeIn(minWidth = 44.dp, minHeight = 44.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = false, radius = 18.dp, color = colors.statusDestructive),
                                onClick = { showClearConfirmDialog = true }
                            )
                            .semantics {
                                role = Role.Button
                                contentDescription = "Clear all downloads"
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
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = colors.textMuted,
                                modifier = Modifier.size(17.dp)
                            )
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
                                    text = "Search downloaded files...",
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
                                contentDescription = "Clear search query",
                                tint = colors.textMuted,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            // 3. Category Filter Chips (Segmented Style)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Spacer(modifier = Modifier.width(8.dp)) }
                items(FileCategory.values()) { category ->
                    val isSelected = selectedCategory == category
                    val chipShape = AzokleRadius.full
                    Box(
                        modifier = Modifier
                            .clip(chipShape)
                            .background(if (isSelected) colors.actionPrimaryBg else colors.surfaceElevated)
                            .border(1.dp, if (isSelected) Color.Transparent else colors.border, chipShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = colors.textPrimary.copy(alpha = 0.15f)),
                                onClick = { selectedCategory = category }
                            )
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .semantics {
                                role = Role.Tab
                                contentDescription = "${category.name} filter"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = category.name.lowercase().replaceFirstChar { it.uppercase() },
                            color = if (isSelected) colors.actionPrimaryText else colors.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
                item { Spacer(modifier = Modifier.width(8.dp)) }
            }

            // 4. Downloads Content Area
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
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceElevated)
                                .border(1.dp, colors.border, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = colors.textMuted,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No matching downloads" else "No downloads yet",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "Try another search term" else "Files you download will securely appear here",
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
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "FILES (${filteredDownloads.size})",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMuted,
                                letterSpacing = 0.8.sp,
                                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                            )

                            // Grouped Card Container
                            val groupShape = AzokleRadius.lg
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(groupShape)
                                    .background(colors.cardBg)
                                    .border(1.dp, colors.border, groupShape)
                            ) {
                                filteredDownloads.forEachIndexed { index, item ->
                                    DownloadGroupedRow(
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
                                            color = colors.border.copy(alpha = 0.5f),
                                            thickness = 0.5.dp,
                                            modifier = Modifier.padding(start = 56.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            }
        }
    }

    // Confirmation Dialog for Clearing All History
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text(text = "Clear All Downloads?", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
            text = { Text(text = "This will remove all download records from Azokle. Downloaded files on your device will remain intact.", fontSize = 13.5.sp) },
            confirmButton = {
                TextButton(
                    onClick = {
                        DownloadManager.clearAllHistory(context)
                        showClearConfirmDialog = false
                    }
                ) {
                    Text(text = "Clear All", color = colors.statusDestructive, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text(text = "Cancel")
                }
            }
        )
    }

    // Confirmation Dialog for Single Item Delete
    if (itemToDelete != null) {
        val target = itemToDelete!!
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text(text = "Delete Download?", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
            text = { Text(text = "Do you want to delete '${target.filename}' from your device storage or just remove the record?", fontSize = 13.5.sp) },
            confirmButton = {
                TextButton(
                    onClick = {
                        DownloadManager.deleteDownload(context, target.id, deleteFile = true)
                        itemToDelete = null
                    }
                ) {
                    Text(text = "Delete File", color = colors.statusDestructive, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        DownloadManager.deleteDownload(context, target.id, deleteFile = false)
                        itemToDelete = null
                    }
                ) {
                    Text(text = "Remove Record Only")
                }
            }
        )
    }
}

@Composable
private fun DownloadGroupedRow(
    item: DownloadItem,
    colors: com.azokle.search.ui.theme.AzokleThemeColors,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit
) {
    val icon = remember(item.mimeType, item.filename) {
        val lower = item.filename.lowercase()
        val mime = item.mimeType.lowercase()
        when {
            mime.startsWith("image/") || lower.endsWith(".jpg") || lower.endsWith(".png") || lower.endsWith(".webp") -> Icons.Default.Image
            mime.startsWith("video/") || lower.endsWith(".mp4") || lower.endsWith(".mkv") -> Icons.Default.Movie
            mime.startsWith("audio/") || lower.endsWith(".mp3") || lower.endsWith(".wav") -> Icons.Default.MusicNote
            mime.contains("pdf") || lower.endsWith(".pdf") -> Icons.Default.PictureAsPdf
            mime.contains("zip") || mime.contains("tar") || lower.endsWith(".zip") || lower.endsWith(".apk") -> Icons.Default.FolderZip
            else -> Icons.AutoMirrored.Filled.InsertDriveFile
        }
    }

    val dateFormatter = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 56.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = colors.textPrimary.copy(alpha = 0.06f)),
                onClick = {
                    if (item.status == DownloadStatus.COMPLETED) {
                        onOpen()
                    }
                }
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .semantics {
                role = Role.Button
                contentDescription = "${item.filename}, Status: ${item.status.name}"
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // File Type Icon Medallion
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(colors.surfaceElevated)
                .border(1.dp, colors.border, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.textPrimary,
                modifier = Modifier.size(17.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Center Details & Progress
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.filename,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            when (item.status) {
                DownloadStatus.DOWNLOADING -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        LinearProgressIndicator(
                            progress = { item.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(1.5.dp)),
                            color = colors.actionPrimaryBg,
                            trackColor = colors.surfaceElevated
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = item.formattedProgress,
                            fontSize = 10.5.sp,
                            color = colors.actionPrimaryBg,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                DownloadStatus.PAUSED -> {
                    Text(
                        text = "Paused • ${item.formattedSize}",
                        fontSize = 11.sp,
                        color = colors.statusWarning,
                        fontWeight = FontWeight.Medium
                    )
                }
                DownloadStatus.COMPLETED -> {
                    Text(
                        text = "${item.formattedSize} • ${dateFormatter.format(Date(item.timestamp))}",
                        fontSize = 11.sp,
                        color = colors.textMuted
                    )
                }
                DownloadStatus.FAILED -> {
                    Text(
                        text = "Failed: ${item.errorMessage ?: "Network error"}",
                        fontSize = 11.sp,
                        color = colors.statusDestructive
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

        // Right Action Controls
        Row(verticalAlignment = Alignment.CenterVertically) {
            when (item.status) {
                DownloadStatus.DOWNLOADING -> {
                    Box(
                        modifier = Modifier
                            .sizeIn(minWidth = 32.dp, minHeight = 36.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onPause)
                            .semantics { role = Role.Button; contentDescription = "Pause download" },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(16.dp))
                    }
                    Box(
                        modifier = Modifier
                            .sizeIn(minWidth = 32.dp, minHeight = 36.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onCancel)
                            .semantics { role = Role.Button; contentDescription = "Cancel download" },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = colors.statusDestructive, modifier = Modifier.size(16.dp))
                    }
                }
                DownloadStatus.PAUSED -> {
                    Box(
                        modifier = Modifier
                            .sizeIn(minWidth = 32.dp, minHeight = 36.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onResume)
                            .semantics { role = Role.Button; contentDescription = "Resume download" },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = colors.actionPrimaryBg, modifier = Modifier.size(16.dp))
                    }
                    Box(
                        modifier = Modifier
                            .sizeIn(minWidth = 32.dp, minHeight = 36.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onCancel)
                            .semantics { role = Role.Button; contentDescription = "Cancel download" },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = colors.statusDestructive, modifier = Modifier.size(16.dp))
                    }
                }
                DownloadStatus.FAILED -> {
                    Box(
                        modifier = Modifier
                            .sizeIn(minWidth = 32.dp, minHeight = 36.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onRetry)
                            .semantics { role = Role.Button; contentDescription = "Retry download" },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = colors.actionPrimaryBg, modifier = Modifier.size(16.dp))
                    }
                    Box(
                        modifier = Modifier
                            .sizeIn(minWidth = 32.dp, minHeight = 36.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onDelete)
                            .semantics { role = Role.Button; contentDescription = "Delete download record" },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = colors.textMuted.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                    }
                }
                DownloadStatus.COMPLETED, DownloadStatus.CANCELLED -> {
                    if (item.status == DownloadStatus.COMPLETED) {
                        Box(
                            modifier = Modifier
                                .sizeIn(minWidth = 32.dp, minHeight = 36.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onShare)
                                .semantics { role = Role.Button; contentDescription = "Share file" },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(15.dp))
                        }
                    }
                    Box(
                        modifier = Modifier
                            .sizeIn(minWidth = 32.dp, minHeight = 36.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onDelete)
                            .semantics { role = Role.Button; contentDescription = "Delete download" },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = colors.textMuted.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
