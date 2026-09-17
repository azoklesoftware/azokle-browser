package com.azokle.search.downloads

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Security
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
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azokle.search.common.theme.AzokleRadius
import com.azokle.search.common.theme.AzokleTheme
import com.azokle.search.common.theme.adaptiveContentWidth
import java.text.SimpleDateFormat
import java.util.Calendar
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

    val activeCount = remember(downloadsList) {
        downloadsList.count { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.PAUSED }
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
            // ==========================================
            // 1. SLEEK TOP APP BAR
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    // Back Navigation Action
                    Box(
                        modifier = Modifier
                            .sizeIn(minWidth = 44.dp, minHeight = 44.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = false, radius = 20.dp, color = colors.textPrimary),
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
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceElevated.copy(alpha = 0.8f))
                                .border(1.dp, colors.borderSubtle, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                tint = colors.textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Downloads",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                modifier = Modifier.semantics { heading() }
                            )

                            if (downloadsList.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(colors.surfaceElevated)
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
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
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = colors.textSecondary
                                        )
                                    }
                                }
                            }
                        }

                        if (activeCount > 0) {
                            Text(
                                text = "$activeCount active transfer${if (activeCount > 1) "s" else ""}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.statusSuccess
                            )
                        }
                    }
                }

                if (downloadsList.isNotEmpty()) {
                    // Clear History Action Button
                    Box(
                        modifier = Modifier
                            .sizeIn(minWidth = 44.dp, minHeight = 44.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = false, radius = 20.dp, color = colors.statusDestructive),
                                onClick = { showClearConfirmDialog = true }
                            )
                            .semantics {
                                role = Role.Button
                                contentDescription = "Clear download records"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceElevated.copy(alpha = 0.8f))
                                .border(1.dp, colors.borderSubtle, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = colors.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // ==========================================
            // 2. SEARCH INPUT FIELD
            // ==========================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(AzokleRadius.md)
                    .background(colors.surfaceElevated.copy(alpha = 0.7f))
                    .border(1.dp, colors.borderSubtle, AzokleRadius.md)
                    .padding(horizontal = 14.dp, vertical = 9.dp)
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
                    Spacer(modifier = Modifier.width(10.dp))
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
                                    text = "Search downloads by file name or URL...",
                                    color = colors.textMuted,
                                    fontSize = 13.sp
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

            // ==========================================
            // 3. HORIZONTAL CATEGORY FILTER PILLS
            // ==========================================
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Spacer(modifier = Modifier.width(8.dp)) }
                items(FileCategory.values()) { category ->
                    val isSelected = selectedCategory == category
                    val chipBg by animateColorAsState(
                        targetValue = if (isSelected) colors.actionPrimaryBg else colors.surfaceElevated.copy(alpha = 0.6f),
                        animationSpec = tween(durationMillis = 200),
                        label = "CategoryChipBg"
                    )
                    val chipTextColor by animateColorAsState(
                        targetValue = if (isSelected) colors.actionPrimaryText else colors.textSecondary,
                        animationSpec = tween(durationMillis = 200),
                        label = "CategoryChipText"
                    )

                    val categoryCount = remember(downloadsList, category) {
                        when (category) {
                            FileCategory.ALL -> downloadsList.size
                            else -> downloadsList.count { it.category == category }
                        }
                    }

                    val chipShape = AzokleRadius.full
                    Box(
                        modifier = Modifier
                            .clip(chipShape)
                            .background(chipBg)
                            .border(
                                1.dp,
                                if (isSelected) Color.Transparent else colors.borderSubtle,
                                chipShape
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = colors.textPrimary.copy(alpha = 0.15f)),
                                onClick = { selectedCategory = category }
                            )
                            .padding(horizontal = 14.dp, vertical = 6.5.dp)
                            .semantics {
                                role = Role.Tab
                                contentDescription = "${category.name} filter, $categoryCount items"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = when (category) {
                                    FileCategory.ALL -> "All"
                                    FileCategory.ACTIVE -> "Active"
                                    FileCategory.IMAGES -> "Images"
                                    FileCategory.MEDIA -> "Media"
                                    FileCategory.DOCUMENTS -> "Documents"
                                    FileCategory.OTHER -> "Other"
                                },
                                color = chipTextColor,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )

                            if (categoryCount > 0) {
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "$categoryCount",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) chipTextColor.copy(alpha = 0.8f) else colors.textMuted
                                )
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.width(8.dp)) }
            }

            // ==========================================
            // 4. DOWNLOADS CONTENT AREA / LIST
            // ==========================================
            if (filteredDownloads.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 24.dp)
                    ) {
                        // Ambient Glowing Icon Badge
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            colors.surfaceElevated,
                                            colors.surfaceElevated.copy(alpha = 0.4f)
                                        )
                                    )
                                )
                                .border(1.dp, colors.borderSubtle, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (searchQuery.isNotBlank()) Icons.Default.Search else Icons.Outlined.FileDownload,
                                contentDescription = null,
                                tint = colors.textMuted,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (searchQuery.isNotBlank()) "No Matching Downloads" else "No Downloads Yet",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (searchQuery.isNotBlank())
                                "Try searching with a different filename keyword."
                            else
                                "Files downloaded via standard, private, or Tor sessions appear here securely.",
                            fontSize = 12.5.sp,
                            color = colors.textMuted,
                            textAlign = TextAlign.Center,
                            lineHeight = 17.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 4.dp, bottom = 6.dp, end = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "TRANSFERS & FILES (${filteredDownloads.size})",
                                    fontSize = 10.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textMuted,
                                    letterSpacing = 0.8.sp
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Security,
                                        contentDescription = null,
                                        tint = colors.textMuted.copy(alpha = 0.7f),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Isolated Storage",
                                        fontSize = 10.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = colors.textMuted
                                    )
                                }
                            }

                            // Grouped Card Container
                            val groupShape = AzokleRadius.lg
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(groupShape)
                                    .background(colors.cardBg)
                                    .border(1.dp, colors.borderSubtle, groupShape)
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
                                            color = colors.borderSubtle.copy(alpha = 0.5f),
                                            thickness = 0.6.dp,
                                            modifier = Modifier.padding(start = 58.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(28.dp)) }
                }
            }
        }
    }

    // Confirmation Dialog: Clear All History
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text(text = "Clear Download History?", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
            text = { Text(text = "This removes all download records from the Azokle browser history. Downloaded files stored on your device will not be deleted.", fontSize = 13.5.sp) },
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
                    Text(text = "Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    // Confirmation Dialog: Single Item Delete
    if (itemToDelete != null) {
        val target = itemToDelete!!
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text(text = "Delete Download Record?", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
            text = { Text(text = "Do you want to delete '${target.filename}' from your device storage or remove only the record?", fontSize = 13.5.sp) },
            confirmButton = {
                TextButton(
                    onClick = {
                        DownloadManager.deleteDownload(context, target.id, deleteFile = true)
                        itemToDelete = null
                    }
                ) {
                    Text(text = "Delete File & Record", color = colors.statusDestructive, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        DownloadManager.deleteDownload(context, target.id, deleteFile = false)
                        itemToDelete = null
                    }
                ) {
                    Text(text = "Record Only", color = colors.textSecondary)
                }
            }
        )
    }
}

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
    // Determine Color-coded Category Medallion & Icon
    val (icon, iconTint, badgeBg) = remember(item.mimeType, item.filename) {
        val lower = item.filename.lowercase()
        val mime = item.mimeType.lowercase()
        when {
            mime.startsWith("image/") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp") || lower.endsWith(".gif") || lower.endsWith(".svg") ->
                Triple(Icons.Default.Image, Color(0xFFEC4899), Color(0xFFEC4899).copy(alpha = 0.12f))
            mime.startsWith("video/") || lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".webm") ->
                Triple(Icons.Default.Movie, Color(0xFF8B5CF6), Color(0xFF8B5CF6).copy(alpha = 0.12f))
            mime.startsWith("audio/") || lower.endsWith(".mp3") || lower.endsWith(".wav") || lower.endsWith(".m4a") ->
                Triple(Icons.Default.MusicNote, Color(0xFF06B6D4), Color(0xFF06B6D4).copy(alpha = 0.12f))
            mime.contains("pdf") || lower.endsWith(".pdf") ->
                Triple(Icons.Default.PictureAsPdf, Color(0xFFEF4444), Color(0xFFEF4444).copy(alpha = 0.12f))
            lower.endsWith(".apk") ->
                Triple(Icons.Default.Android, Color(0xFF22C55E), Color(0xFF22C55E).copy(alpha = 0.12f))
            mime.contains("zip") || mime.contains("tar") || lower.endsWith(".zip") || lower.endsWith(".rar") || lower.endsWith(".7z") ->
                Triple(Icons.Default.FolderZip, Color(0xFFF59E0B), Color(0xFFF59E0B).copy(alpha = 0.12f))
            else ->
                Triple(Icons.AutoMirrored.Filled.InsertDriveFile, colors.textPrimary, colors.surfaceElevated)
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
            .sizeIn(minHeight = 60.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = colors.textPrimary.copy(alpha = 0.05f)),
                onClick = {
                    if (item.status == DownloadStatus.COMPLETED) {
                        onOpen()
                    }
                }
            )
            .padding(horizontal = 14.dp, vertical = 11.dp)
            .semantics {
                role = Role.Button
                contentDescription = "${item.filename}, Status: ${item.status.name}"
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // File Type Medallion
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(badgeBg)
                .border(1.dp, iconTint.copy(alpha = 0.20f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Center Details & Progress
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.filename,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.5.dp))

            when (item.status) {
                DownloadStatus.DOWNLOADING -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.5.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = colors.actionPrimaryBg,
                            trackColor = colors.surfaceElevated
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = item.formattedProgress,
                            fontSize = 10.5.sp,
                            color = colors.actionPrimaryBg,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                DownloadStatus.PAUSED -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(colors.statusWarning.copy(alpha = 0.12f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "PAUSED",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = colors.statusWarning
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = item.formattedSize,
                            fontSize = 11.sp,
                            color = colors.textMuted
                        )
                    }
                }
                DownloadStatus.COMPLETED -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (item.isTor) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(colors.statusTor.copy(alpha = 0.12f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "TOR",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = colors.statusTor
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        } else if (item.isPrivate) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(colors.statusSuccess.copy(alpha = 0.12f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "PRIVATE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = colors.statusSuccess
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        Text(
                            text = "${item.formattedSize} • ${formatHumanTimestamp(item.timestamp)}",
                            fontSize = 11.sp,
                            color = colors.textMuted
                        )
                    }
                }
                DownloadStatus.FAILED -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(colors.statusDestructive.copy(alpha = 0.12f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "FAILED",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = colors.statusDestructive
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = item.errorMessage ?: "Network error",
                            fontSize = 11.sp,
                            color = colors.statusDestructive,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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
                            .sizeIn(minWidth = 36.dp, minHeight = 36.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onPause)
                            .semantics { role = Role.Button; contentDescription = "Pause download" },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(17.dp))
                    }
                    Box(
                        modifier = Modifier
                            .sizeIn(minWidth = 36.dp, minHeight = 36.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onCancel)
                            .semantics { role = Role.Button; contentDescription = "Cancel download" },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = colors.statusDestructive, modifier = Modifier.size(17.dp))
                    }
                }
                DownloadStatus.PAUSED -> {
                    Box(
                        modifier = Modifier
                            .sizeIn(minWidth = 36.dp, minHeight = 36.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onResume)
                            .semantics { role = Role.Button; contentDescription = "Resume download" },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = colors.actionPrimaryBg, modifier = Modifier.size(17.dp))
                    }
                    Box(
                        modifier = Modifier
                            .sizeIn(minWidth = 36.dp, minHeight = 36.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onCancel)
                            .semantics { role = Role.Button; contentDescription = "Cancel download" },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = colors.statusDestructive, modifier = Modifier.size(17.dp))
                    }
                }
                DownloadStatus.FAILED -> {
                    Box(
                        modifier = Modifier
                            .sizeIn(minWidth = 36.dp, minHeight = 36.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onRetry)
                            .semantics { role = Role.Button; contentDescription = "Retry download" },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = colors.actionPrimaryBg, modifier = Modifier.size(17.dp))
                    }
                    Box(
                        modifier = Modifier
                            .sizeIn(minWidth = 36.dp, minHeight = 36.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onDelete)
                            .semantics { role = Role.Button; contentDescription = "Delete download record" },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = colors.textMuted.copy(alpha = 0.7f), modifier = Modifier.size(17.dp))
                    }
                }
                DownloadStatus.COMPLETED, DownloadStatus.CANCELLED -> {
                    if (item.status == DownloadStatus.COMPLETED) {
                        Box(
                            modifier = Modifier
                                .sizeIn(minWidth = 36.dp, minHeight = 36.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onShare)
                                .semantics { role = Role.Button; contentDescription = "Share file" },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                    Box(
                        modifier = Modifier
                            .sizeIn(minWidth = 36.dp, minHeight = 36.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onDelete)
                            .semantics { role = Role.Button; contentDescription = "Delete download" },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = colors.textMuted.copy(alpha = 0.7f), modifier = Modifier.size(17.dp))
                    }
                }
            }
        }
    }
}

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
            "Today, ${timeFormat.format(Date(timestamp))}"
        }
        calNow.get(Calendar.YEAR) == calTime.get(Calendar.YEAR) &&
                calNow.get(Calendar.DAY_OF_YEAR) - calTime.get(Calendar.DAY_OF_YEAR) == 1 -> {
            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            "Yesterday, ${timeFormat.format(Date(timestamp))}"
        }
        else -> {
            val fullFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            fullFormat.format(Date(timestamp))
        }
    }
}
