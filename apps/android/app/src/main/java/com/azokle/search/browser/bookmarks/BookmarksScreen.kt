package com.azokle.search.browser.bookmarks

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.azokle.search.R
import com.azokle.search.common.components.ToastManager
import com.azokle.search.common.theme.AzokleBlack
import com.azokle.search.common.theme.AzokleRadius
import com.azokle.search.common.theme.AzokleTheme
import com.azokle.search.common.theme.AzokleWhite
import com.azokle.search.common.theme.adaptiveContentWidth
import com.azokle.search.vault.BookmarkItem
import com.azokle.search.vault.EncryptedVaultManager
import com.azokle.search.vault.QuickLinkItem

@Composable
fun BookmarksScreen(
    onDismiss: () -> Unit,
    onNavigate: (String) -> Unit,
    onOpenTor: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = AzokleTheme.colors
    val vaultManager = remember { EncryptedVaultManager.getInstance(context) }

    val bookmarks by vaultManager.bookmarksFlow.collectAsState()
    val quickLinks by vaultManager.quickLinksFlow.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showAddBookmarkDialog by remember { mutableStateOf(false) }
    var showAddQuickLinkDialog by remember { mutableStateOf(false) }
    var bookmarkToEdit by remember { mutableStateOf<BookmarkItem?>(null) }

    val filteredBookmarks = remember(bookmarks, searchQuery) {
        if (searchQuery.isBlank()) {
            bookmarks
        } else {
            val query = searchQuery.trim().lowercase()
            bookmarks.filter {
                it.title.lowercase().contains(query) || it.url.lowercase().contains(query)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .adaptiveContentWidth(640.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // 1. Minimal Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.semantics { heading() }
                    ) {
                        Text(
                            text = "Bookmarks",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                        if (bookmarks.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(colors.surfaceElevated)
                                    .border(0.8.dp, colors.borderSubtle, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "${bookmarks.size}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Add Button
                        Box(
                            modifier = Modifier
                                .sizeIn(minWidth = 36.dp, minHeight = 36.dp)
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = false, radius = 18.dp, color = colors.textSecondary),
                                    onClick = { showAddBookmarkDialog = true }
                                )
                                .semantics {
                                    role = Role.Button
                                    contentDescription = "Add bookmark"
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
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = colors.textPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Close Button
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
                                    contentDescription = "Close bookmarks"
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
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = colors.textPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Compact Search Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
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
                                    text = "Search bookmarks...",
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

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Shortcuts / Quick Links (if any or minimal)
                if (quickLinks.isNotEmpty()) {
                    Text(
                        text = "SHORTCUTS (${quickLinks.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textMuted,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(start = 2.dp, bottom = 6.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                    ) {
                        items(quickLinks, key = { it.id }) { link ->
                            QuickLinkTile(
                                item = link,
                                onClick = {
                                    onDismiss()
                                    onNavigate(link.url)
                                },
                                colors = colors
                            )
                        }

                        item {
                            Box(
                                modifier = Modifier
                                    .width(76.dp)
                                    .height(68.dp)
                                    .clip(AzokleRadius.md)
                                    .background(colors.surfaceElevated)
                                    .border(0.8.dp, colors.borderSubtle, AzokleRadius.md)
                                    .clickable { showAddQuickLinkDialog = true }
                                    .padding(6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add Shortcut",
                                        tint = colors.textMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "Add",
                                        fontSize = 10.sp,
                                        color = colors.textMuted
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Saved Bookmarks Header
                if (filteredBookmarks.isNotEmpty()) {
                    Text(
                        text = "SAVED (${filteredBookmarks.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textMuted,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(start = 2.dp, bottom = 6.dp)
                    )
                }
            }

            // Bookmarks List
            if (filteredBookmarks.isEmpty()) {
                EmptyBookmarksView(searchQuery = searchQuery, colors = colors)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .adaptiveContentWidth(640.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp)
                ) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(AzokleRadius.lg)
                                .background(colors.surfaceElevated)
                                .border(0.8.dp, colors.borderSubtle, AzokleRadius.lg)
                        ) {
                            filteredBookmarks.forEachIndexed { index, bookmark ->
                                BookmarkItemRow(
                                    bookmark = bookmark,
                                    isPinned = vaultManager.isQuickLinkPinned(bookmark.url),
                                    onClick = {
                                        onDismiss()
                                        onNavigate(bookmark.url)
                                    },
                                    onOpenTor = {
                                        onDismiss()
                                        onOpenTor(bookmark.url)
                                    },
                                    onTogglePin = {
                                        if (vaultManager.isQuickLinkPinned(bookmark.url)) {
                                            val ql = quickLinks.find { it.url == bookmark.url }
                                            if (ql != null) vaultManager.removeQuickLink(ql.id)
                                        } else {
                                            vaultManager.addQuickLink(bookmark.title, bookmark.url)
                                        }
                                    },
                                    onEdit = { bookmarkToEdit = bookmark },
                                    onDelete = { vaultManager.removeBookmark(bookmark.id) },
                                    colors = colors
                                )

                                if (index < filteredBookmarks.size - 1) {
                                    HorizontalDivider(
                                        color = colors.borderSubtle,
                                        thickness = 0.5.dp,
                                        modifier = Modifier.padding(start = 52.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add Bookmark Dialog
        if (showAddBookmarkDialog) {
            AddBookmarkDialog(
                onConfirm = { title, url ->
                    vaultManager.addBookmark(title, url)
                    showAddBookmarkDialog = false
                    ToastManager.show("Bookmark saved")
                },
                onDismiss = { showAddBookmarkDialog = false },
                colors = colors
            )
        }

        // Add Quick Link Dialog
        if (showAddQuickLinkDialog) {
            AddQuickLinkDialog(
                onConfirm = { title, url ->
                    vaultManager.addQuickLink(title, url)
                    showAddQuickLinkDialog = false
                    ToastManager.show("Shortcut added")
                },
                onDismiss = { showAddQuickLinkDialog = false },
                colors = colors
            )
        }

        // Edit Bookmark Dialog
        bookmarkToEdit?.let { item ->
            EditBookmarkDialog(
                item = item,
                onConfirm = { newTitle, newUrl ->
                    vaultManager.updateBookmark(item.id, newTitle, newUrl)
                    bookmarkToEdit = null
                    ToastManager.show("Bookmark updated")
                },
                onDismiss = { bookmarkToEdit = null },
                colors = colors
            )
        }
    }
}

@Composable
private fun QuickLinkTile(
    item: QuickLinkItem,
    onClick: () -> Unit,
    colors: com.azokle.search.common.theme.AzokleThemeColors
) {
    val context = LocalContext.current
    val host = try {
        java.net.URI(item.url).host?.removePrefix("www.") ?: item.url
    } catch (e: Exception) {
        item.url
    }
    val faviconUrl = "https://www.google.com/s2/favicons?domain=$host&sz=64"

    Box(
        modifier = Modifier
            .width(76.dp)
            .height(68.dp)
            .clip(AzokleRadius.md)
            .background(colors.surfaceElevated)
            .border(0.8.dp, colors.borderSubtle, AzokleRadius.md)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = colors.textPrimary.copy(alpha = 0.08f)),
                onClick = onClick
            )
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(colors.cardBg)
                    .border(0.8.dp, colors.borderSubtle, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(faviconUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(15.dp)
                        .clip(CircleShape)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
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

@Composable
private fun BookmarkItemRow(
    bookmark: BookmarkItem,
    isPinned: Boolean,
    onClick: () -> Unit,
    onOpenTor: () -> Unit,
    onTogglePin: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    colors: com.azokle.search.common.theme.AzokleThemeColors
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    val cleanHost = try {
        java.net.URI(bookmark.url).host?.removePrefix("www.") ?: bookmark.url
    } catch (e: Exception) {
        bookmark.url
    }
    val faviconUrl = "https://www.google.com/s2/favicons?domain=$cleanHost&sz=64"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 52.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = colors.textPrimary.copy(alpha = 0.08f)),
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Favicon badge
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(AzokleRadius.sm)
                    .background(colors.cardBg)
                    .border(0.8.dp, colors.borderSubtle, AzokleRadius.sm),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(faviconUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = bookmark.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = cleanHost,
                    fontSize = 11.5.sp,
                    color = colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box {
            Box(
                modifier = Modifier
                    .sizeIn(minWidth = 32.dp, minHeight = 32.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false, radius = 16.dp, color = colors.textSecondary),
                        onClick = { showMenu = true }
                    )
                    .semantics {
                        role = Role.Button
                        contentDescription = "Bookmark options for ${bookmark.title}"
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = null,
                    tint = colors.textMuted,
                    modifier = Modifier.size(16.dp)
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier
                    .background(colors.cardBg)
                    .border(0.8.dp, colors.borderSubtle, RoundedCornerShape(12.dp))
            ) {
                DropdownMenuItem(
                    text = { Text("Open", fontSize = 13.sp, color = colors.textPrimary) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = colors.statusAccent, modifier = Modifier.size(16.dp)) },
                    onClick = {
                        showMenu = false
                        onClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Open in Tor", fontSize = 13.sp, color = colors.statusTor) },
                    leadingIcon = {
                        Image(
                            painter = painterResource(id = R.drawable.ic_tor),
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                    },
                    onClick = {
                        showMenu = false
                        onOpenTor()
                    }
                )
                DropdownMenuItem(
                    text = { Text(if (isPinned) "Unpin Shortcut" else "Pin Shortcut", fontSize = 13.sp, color = colors.textPrimary) },
                    leadingIcon = { Icon(Icons.Default.PushPin, null, tint = if (isPinned) colors.statusWarning else colors.textMuted, modifier = Modifier.size(16.dp)) },
                    onClick = {
                        showMenu = false
                        onTogglePin()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Copy Link", fontSize = 13.sp, color = colors.textPrimary) },
                    leadingIcon = { Icon(Icons.Default.ContentCopy, null, tint = colors.textMuted, modifier = Modifier.size(16.dp)) },
                    onClick = {
                        showMenu = false
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val clipData = android.content.ClipData.newPlainText("URL", bookmark.url)
                        clipboard?.setPrimaryClip(clipData)
                        ToastManager.showCopy("Link copied")
                    }
                )
                DropdownMenuItem(
                    text = { Text("Edit", fontSize = 13.sp, color = colors.textPrimary) },
                    leadingIcon = { Icon(Icons.Default.Edit, null, tint = colors.textMuted, modifier = Modifier.size(16.dp)) },
                    onClick = {
                        showMenu = false
                        onEdit()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete", fontSize = 13.sp, color = colors.statusDestructive) },
                    leadingIcon = { Icon(Icons.Default.DeleteOutline, null, tint = colors.statusDestructive, modifier = Modifier.size(16.dp)) },
                    onClick = {
                        showMenu = false
                        onDelete()
                        ToastManager.show("Bookmark removed")
                    }
                )
            }
        }
    }
}

@Composable
private fun EmptyBookmarksView(
    searchQuery: String,
    colors: com.azokle.search.common.theme.AzokleThemeColors
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
                .size(44.dp)
                .clip(CircleShape)
                .background(colors.surfaceElevated)
                .border(0.8.dp, colors.borderSubtle, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.BookmarkBorder,
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (searchQuery.isNotEmpty()) "No results" else "No bookmarks",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (searchQuery.isNotEmpty()) "Try a different search query" else "Saved pages will appear here",
            fontSize = 12.sp,
            color = colors.textMuted,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AddBookmarkDialog(
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit,
    colors: com.azokle.search.common.theme.AzokleThemeColors
) {
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("https://") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Bookmark",
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = colors.textPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title", fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.actionPrimaryBg,
                        unfocusedBorderColor = colors.borderSubtle,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    ),
                    shape = AzokleRadius.md,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL", fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.actionPrimaryBg,
                        unfocusedBorderColor = colors.borderSubtle,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    ),
                    shape = AzokleRadius.md,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (url.isNotBlank()) onConfirm(title.ifBlank { url }, url)
                }
            ) {
                Text("Save", fontWeight = FontWeight.SemiBold, color = colors.statusAccent, fontSize = 13.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textSecondary, fontSize = 13.sp)
            }
        },
        containerColor = colors.cardBg,
        shape = AzokleRadius.xl
    )
}

@Composable
private fun AddQuickLinkDialog(
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit,
    colors: com.azokle.search.common.theme.AzokleThemeColors
) {
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("https://") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Shortcut",
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = colors.textPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title", fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.actionPrimaryBg,
                        unfocusedBorderColor = colors.borderSubtle,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    ),
                    shape = AzokleRadius.md,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL", fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.actionPrimaryBg,
                        unfocusedBorderColor = colors.borderSubtle,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    ),
                    shape = AzokleRadius.md,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (url.isNotBlank()) onConfirm(title.ifBlank { url }, url)
                }
            ) {
                Text("Add", fontWeight = FontWeight.SemiBold, color = colors.statusAccent, fontSize = 13.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textSecondary, fontSize = 13.sp)
            }
        },
        containerColor = colors.cardBg,
        shape = AzokleRadius.xl
    )
}

@Composable
private fun EditBookmarkDialog(
    item: BookmarkItem,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit,
    colors: com.azokle.search.common.theme.AzokleThemeColors
) {
    var title by remember { mutableStateOf(item.title) }
    var url by remember { mutableStateOf(item.url) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Bookmark",
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = colors.textPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title", fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.actionPrimaryBg,
                        unfocusedBorderColor = colors.borderSubtle,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    ),
                    shape = AzokleRadius.md,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL", fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.actionPrimaryBg,
                        unfocusedBorderColor = colors.borderSubtle,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    ),
                    shape = AzokleRadius.md,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (url.isNotBlank()) onConfirm(title.ifBlank { url }, url)
                }
            ) {
                Text("Update", fontWeight = FontWeight.SemiBold, color = colors.statusAccent, fontSize = 13.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textSecondary, fontSize = 13.sp)
            }
        },
        containerColor = colors.cardBg,
        shape = AzokleRadius.xl
    )
}
