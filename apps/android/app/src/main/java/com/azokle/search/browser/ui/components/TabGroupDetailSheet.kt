package com.azokle.search.browser.ui.components

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Brush
import com.azokle.search.ui.theme.AudiowideFontFamily
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.azokle.search.browser.tabs.TabGroup
import com.azokle.search.browser.tabs.TabManager
import com.azokle.search.browser.tabs.TabSession
import com.azokle.search.ui.theme.AzokleRadius
import com.azokle.search.ui.theme.AzokleTheme
import com.azokle.search.ui.theme.adaptiveContentWidth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabGroupDetailSheet(
    group: TabGroup,
    tabs: List<TabSession>,
    activeTabId: String?,
    onTabSelected: (TabSession) -> Unit,
    onAddTabInGroup: () -> Unit,
    onCloseTab: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = AzokleTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showRenameDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.modalBg,
        contentColor = colors.textPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 10.dp, bottom = 6.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(colors.border)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .adaptiveContentWidth(640.dp)
                .fillMaxHeight(0.75f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // 1. Group Header Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { showRenameDialog = true }
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(Color(group.color))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = group.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Rename",
                        tint = colors.textMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "(${tabs.size})",
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // "+" Add Tab in Group Button
                    IconButton(
                        onClick = {
                            onAddTabInGroup()
                            onDismiss()
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(colors.surface)
                            .border(1.dp, colors.border, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add tab in group",
                            tint = colors.statusAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Close Group / Delete Button
                    IconButton(
                        onClick = {
                            TabManager.closeGroup(group.id)
                            onDismiss()
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(colors.surface)
                            .border(1.dp, colors.border, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Close entire group",
                            tint = colors.statusDestructive,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // 2. Color Palette Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TabGroup.GROUP_PALETTE.forEach { colorVal ->
                    val isSelected = group.color == colorVal
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(colorVal))
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = if (isSelected) colors.textPrimary else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable {
                                TabManager.renameGroup(group.id, group.title, colorVal)
                            }
                    )
                }
            }

            // 3. Grid of Group Tabs
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(tabs, key = { it.id }) { tab ->
                    val isActive = tab.id == activeTabId
                    GroupTabGridItem(
                        tab = tab,
                        isActive = isActive,
                        groupColor = Color(group.color),
                        onClick = {
                            onTabSelected(tab)
                            onDismiss()
                        },
                        onClose = { onCloseTab(tab.id) }
                    )
                }

                // Interactive Add Tab Card within Group
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(AzokleRadius.lg)
                            .background(colors.surfaceElevated.copy(alpha = 0.5f))
                            .border(1.dp, colors.border, AzokleRadius.lg)
                            .clickable {
                                onAddTabInGroup()
                                onDismiss()
                            }
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(group.color).copy(alpha = 0.15f))
                                    .border(1.dp, Color(group.color).copy(alpha = 0.3f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Tab",
                                    tint = Color(group.color),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "New Tab",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "in ${group.title}",
                                fontSize = 11.sp,
                                color = colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        if (showRenameDialog) {
            RenameGroupDialog(
                currentTitle = group.title,
                onConfirm = { newTitle ->
                    TabManager.renameGroup(group.id, newTitle)
                    showRenameDialog = false
                },
                onDismiss = { showRenameDialog = false }
            )
        }
    }
}

@Composable
private fun GroupTabGridItem(
    tab: TabSession,
    isActive: Boolean,
    groupColor: Color,
    onClick: () -> Unit,
    onClose: () -> Unit
) {
    val colors = AzokleTheme.colors
    val isHome = tab.url.isBlank() || tab.url == "about:home" || tab.url == "about:blank"

    val host = remember(tab.url) {
        try {
            android.net.Uri.parse(tab.url).host?.removePrefix("www.")?.ifBlank { tab.url } ?: tab.url
        } catch (e: Exception) {
            tab.url
        }
    }

    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(shape)
            .background(colors.cardBg)
            .border(
                width = if (isActive) 2.dp else 1.dp,
                color = if (isActive) groupColor else colors.border.copy(alpha = 0.7f),
                shape = shape
            )
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = "Tab: ${tab.title.ifBlank { if (isHome) "New Tab" else host }}${if (isActive) ", Active" else ""}"
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Tab Card Top Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .background(colors.surfaceElevated)
                    .padding(start = 8.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isHome) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            tint = groupColor,
                            modifier = Modifier.size(14.dp)
                        )
                    } else if (tab.isTor) {
                        Text(text = "🧅", fontSize = 12.sp)
                    } else if (tab.isPrivate) {
                        Icon(
                            imageVector = Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = Color(0xFFA855F7),
                            modifier = Modifier.size(14.dp)
                        )
                    } else {
                        FaviconImage(
                            url = tab.url,
                            faviconUrl = tab.faviconUrl,
                            isTor = tab.isTor,
                            size = 14.dp
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = if (isHome) "New Tab" else tab.title.ifBlank { host },
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Close Tab Button
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = false, radius = 13.dp, color = colors.statusDestructive),
                            onClick = onClose
                        )
                        .semantics {
                            role = Role.Button
                            contentDescription = "Close tab: ${tab.title}"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            // 2. Webpage Live Screenshot or Miniature Wallpaper Home Screen
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(colors.surfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                if (tab.thumbnail != null) {
                    Image(
                        bitmap = tab.thumbnail.asImageBitmap(),
                        contentDescription = tab.title,
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.TopCenter,
                        modifier = Modifier.fillMaxSize()
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color(0x10000000), Color(0x55000000))
                                )
                            )
                    )
                } else if (isHome) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            painter = painterResource(id = tab.wallpaperResId ?: com.azokle.search.browser.wallpaper.WallpaperManager.getCurrentWallpaper()),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x40000000))
                        )
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "azokle",
                                fontFamily = AudiowideFontFamily,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .width(100.dp)
                                    .height(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x99000000))
                                    .border(0.5.dp, Color(0x66FFFFFF), CircleShape)
                                    .padding(horizontal = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = "Search",
                                        fontSize = 8.sp,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(colors.surfaceElevated, colors.cardBg)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(colors.surface)
                                    .border(1.dp, colors.border, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                FaviconImage(
                                    url = tab.url,
                                    faviconUrl = tab.faviconUrl,
                                    isTor = tab.isTor,
                                    size = 20.dp
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(colors.surface)
                                    .border(0.5.dp, colors.border, CircleShape)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = host,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Active Badge
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(groupColor)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "ACTIVE",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RenameGroupDialog(
    currentTitle: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = AzokleTheme.colors
    var title by remember { mutableStateOf(currentTitle) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Group", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Group Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) onConfirm(title.trim())
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
