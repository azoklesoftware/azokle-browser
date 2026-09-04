package com.azokle.search.browser.bookmarks

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.azokle.search.R
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.azokle.search.data.vault.BookmarkItem
import com.azokle.search.data.vault.EncryptedVaultManager
import com.azokle.search.data.vault.QuickLinkItem
import com.azokle.search.ui.theme.AzokleRadius
import com.azokle.search.ui.theme.AzokleTheme
import com.azokle.search.ui.theme.adaptiveContentWidth

/**
 * Dedicated Sovereign Bookmarks & Quick Links Screen:
 *  - Pinned Quick Links / Shortcuts Carousel
 *  - Real-Time Searchable Bookmarks List
 *  - Add / Edit Bookmark & Quick Link Dialogs
 *  - Open in Browser or Tor
 */
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
            bookmarks.filter {
                it.title.contains(searchQuery.trim(), ignoreCase = true) ||
                        it.url.contains(searchQuery.trim(), ignoreCase = true)
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
                    .adaptiveContentWidth(680.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // 1. Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(colors.cardBg)
                                .border(1.dp, colors.border, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = null,
                                tint = colors.statusWarning,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "BOOKMARKS & SHORTCUTS",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = colors.textPrimary,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = "${bookmarks.size} Bookmarks • ${quickLinks.size} Pinned",
                                fontSize = 11.sp,
                                color = colors.textSecondary
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { showAddBookmarkDialog = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(colors.surface)
                                .border(1.dp, colors.border, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Bookmark",
                                tint = colors.textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(colors.surface)
                                .border(1.dp, colors.border, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // 2. Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search bookmarks or URLs...", fontSize = 13.sp, color = colors.textMuted) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = colors.cardBg,
                        unfocusedContainerColor = colors.cardBg,
                        focusedBorderColor = colors.statusAccent,
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    ),
                    shape = AzokleRadius.lg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                )

                // 3. Pinned Quick Links Section
                Text(
                    text = "PINNED QUICK LINKS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = colors.textSecondary,
                    letterSpacing = 0.6.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    items(quickLinks) { link ->
                        QuickLinkCard(
                            item = link,
                            onClick = {
                                onDismiss()
                                onNavigate(link.url)
                            },
                            onRemove = { vaultManager.removeQuickLink(link.id) },
                            colors = colors
                        )
                    }

                    item {
                        Box(
                            modifier = Modifier
                                .width(90.dp)
                                .height(82.dp)
                                .clip(AzokleRadius.lg)
                                .background(colors.surface)
                                .border(1.dp, colors.border, AzokleRadius.lg)
                                .clickable { showAddQuickLinkDialog = true }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Shortcut",
                                    tint = colors.statusAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Add Link",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }
                }

                // 4. Bookmarks List Section
                Text(
                    text = "SAVED BOOKMARKS (${filteredBookmarks.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = colors.textSecondary,
                    letterSpacing = 0.6.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Bookmarks LazyColumn
            if (filteredBookmarks.isEmpty()) {
                EmptyBookmarksView(searchQuery = searchQuery, colors = colors)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 48.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredBookmarks, key = { it.id }) { bookmark ->
                        BookmarkCard(
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
                },
                onDismiss = { bookmarkToEdit = null },
                colors = colors
            )
        }
    }
}

@Composable
private fun QuickLinkCard(
    item: QuickLinkItem,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    colors: com.azokle.search.ui.theme.AzokleThemeColors
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
            .width(90.dp)
            .height(82.dp)
            .clip(AzokleRadius.lg)
            .background(colors.cardBg)
            .border(1.dp, colors.border, AzokleRadius.lg)
            .clickable(onClick = onClick)
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(colors.surface)
                    .border(1.dp, colors.border, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(faviconUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
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

@Composable
private fun BookmarkCard(
    bookmark: BookmarkItem,
    isPinned: Boolean,
    onClick: () -> Unit,
    onOpenTor: () -> Unit,
    onTogglePin: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    colors: com.azokle.search.ui.theme.AzokleThemeColors
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
            .clip(AzokleRadius.lg)
            .background(colors.cardBg)
            .border(1.dp, colors.border, AzokleRadius.lg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(colors.surface)
                .border(1.dp, colors.border, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(faviconUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = bookmark.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = cleanHost,
                    fontSize = 11.sp,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (bookmark.dateAdded.isNotBlank()) {
                    Text(
                        text = " • ${bookmark.dateAdded}",
                        fontSize = 11.sp,
                        color = colors.textMuted
                    )
                }
            }
        }

        Box {
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
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
                    text = { Text("Open in Browser", color = colors.textPrimary) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = colors.statusAccent) },
                    onClick = {
                        showMenu = false
                        onClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Open in Tor Window", color = colors.statusTor) },
                    leadingIcon = {
                        Image(
                            painter = painterResource(id = R.drawable.ic_tor),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    onClick = {
                        showMenu = false
                        onOpenTor()
                    }
                )
                DropdownMenuItem(
                    text = { Text(if (isPinned) "Unpin from Quick Links" else "Pin to Quick Links", color = colors.textPrimary) },
                    leadingIcon = { Icon(Icons.Default.PushPin, null, tint = if (isPinned) colors.statusWarning else colors.textSecondary) },
                    onClick = {
                        showMenu = false
                        onTogglePin()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Copy Link", color = colors.textPrimary) },
                    leadingIcon = { Icon(Icons.Default.ContentCopy, null, tint = colors.textSecondary) },
                    onClick = {
                        showMenu = false
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val clipData = android.content.ClipData.newPlainText("URL", bookmark.url)
                        clipboard?.setPrimaryClip(clipData)
                        com.azokle.search.ui.components.ToastManager.showCopy("Link copied to clipboard")
                    }
                )
                DropdownMenuItem(
                    text = { Text("Edit Bookmark", color = colors.textPrimary) },
                    leadingIcon = { Icon(Icons.Default.Edit, null, tint = colors.textSecondary) },
                    onClick = {
                        showMenu = false
                        onEdit()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = colors.statusDestructive) },
                    leadingIcon = { Icon(Icons.Default.DeleteOutline, null, tint = colors.statusDestructive) },
                    onClick = {
                        showMenu = false
                        onDelete()
                        com.azokle.search.ui.components.ToastManager.show("Bookmark removed")
                    }
                )
            }
        }
    }
}

@Composable
private fun EmptyBookmarksView(
    searchQuery: String,
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
            text = if (searchQuery.isNotEmpty()) "No Matching Bookmarks" else "No Bookmarks Saved Yet",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (searchQuery.isNotEmpty()) "Try a different search query." else "Tap the bookmark icon on any webpage to save it here for quick access.",
            fontSize = 13.sp,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun AddBookmarkDialog(
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit,
    colors: com.azokle.search.ui.theme.AzokleThemeColors
) {
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("https://") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Bookmark", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL") },
                    singleLine = true,
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
                Text("Save", fontWeight = FontWeight.Bold, color = colors.statusAccent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textSecondary)
            }
        },
        containerColor = colors.modalBg
    )
}

@Composable
private fun AddQuickLinkDialog(
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit,
    colors: com.azokle.search.ui.theme.AzokleThemeColors
) {
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("https://") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Quick Link", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL") },
                    singleLine = true,
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
                Text("Add", fontWeight = FontWeight.Bold, color = colors.statusAccent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textSecondary)
            }
        },
        containerColor = colors.modalBg
    )
}

@Composable
private fun EditBookmarkDialog(
    item: BookmarkItem,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit,
    colors: com.azokle.search.ui.theme.AzokleThemeColors
) {
    var title by remember { mutableStateOf(item.title) }
    var url by remember { mutableStateOf(item.url) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Bookmark", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL") },
                    singleLine = true,
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
                Text("Update", fontWeight = FontWeight.Bold, color = colors.statusAccent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textSecondary)
            }
        },
        containerColor = colors.modalBg
    )
}
