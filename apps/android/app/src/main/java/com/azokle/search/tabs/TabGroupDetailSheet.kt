package com.azokle.search.tabs

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import com.azokle.search.R
import com.azokle.search.common.components.FaviconImage
import com.azokle.search.common.theme.AudiowideFontFamily
import com.azokle.search.common.theme.AzokleTheme
import com.azokle.search.common.theme.adaptiveContentWidth

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
    val groupColor = Color(group.color)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showRenameDialog by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Dynamic grid height calculation: eliminates dead empty space while allowing smooth scroll for many tabs
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val maxGridHeight = screenHeight * 0.58f

    val totalItems = tabs.size + 1 // Existing tabs + "New Tab in Group" card
    val rowCount = (totalItems + 1) / 2
    val cardHeight = 200.dp
    val spacing = 12.dp
    val calculatedGridHeight = (cardHeight * rowCount) + (spacing * (rowCount - 1).coerceAtLeast(0))
    val gridHeight = minOf(calculatedGridHeight, maxGridHeight)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.modalBg,
        contentColor = colors.textPrimary,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            // Frosted Pill Drag Handle centered without status bar padding gap
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 10.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(colors.border.copy(alpha = 0.7f))
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .adaptiveContentWidth(680.dp)
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp)
            ) {
                // 1. Group Header Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Group Title & Pill Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .height(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(colors.surfaceElevated.copy(alpha = 0.9f))
                            .border(1.dp, groupColor.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
                            .clickable { showRenameDialog = true }
                            .padding(horizontal = 10.dp)
                    ) {
                        // Halo Color Dot
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(groupColor)
                                .border(2.dp, groupColor.copy(alpha = 0.4f), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                        Text(
                            text = group.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(${tabs.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = groupColor.copy(alpha = 0.9f)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Rename group",
                            tint = colors.textSecondary.copy(alpha = 0.7f),
                            modifier = Modifier.size(12.dp)
                        )
                    }

                    // Action Buttons: Add Tab, Group Options Menu, and Close Sheet (Strictly sized, zero overlap)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. "+" Add Tab in Group Button
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(groupColor.copy(alpha = 0.18f))
                                .border(1.dp, groupColor.copy(alpha = 0.45f), CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true, radius = 17.dp, color = groupColor),
                                    onClick = {
                                        onAddTabInGroup()
                                        onDismiss()
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add tab in group",
                                tint = groupColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // 2. More Options (Rename Group / Close Group)
                        Box(contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(colors.surfaceElevated.copy(alpha = 0.85f))
                                    .border(1.dp, colors.border.copy(alpha = 0.6f), CircleShape)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(bounded = true, radius = 17.dp, color = colors.textSecondary),
                                        onClick = { showOptionsMenu = true }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Group options",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(17.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showOptionsMenu,
                                onDismissRequest = { showOptionsMenu = false },
                                modifier = Modifier.background(colors.surfaceElevated)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Rename Group", color = colors.textPrimary) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Edit, contentDescription = null, tint = groupColor, modifier = Modifier.size(18.dp))
                                    },
                                    onClick = {
                                        showOptionsMenu = false
                                        showRenameDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Close Group", color = colors.statusDestructive) },
                                    leadingIcon = {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = colors.statusDestructive, modifier = Modifier.size(18.dp))
                                    },
                                    onClick = {
                                        showOptionsMenu = false
                                        showDeleteConfirmDialog = true
                                    }
                                )
                            }
                        }

                        // 3. Close Sheet Button
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceElevated.copy(alpha = 0.85f))
                                .border(1.dp, colors.border.copy(alpha = 0.6f), CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true, radius = 17.dp, color = colors.textSecondary),
                                    onClick = onDismiss
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close sheet",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }

                // 2. Animated Color Palette Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TabGroup.GROUP_PALETTE.forEach { colorVal ->
                        val isSelected = group.color == colorVal
                        val itemColor = Color(colorVal)

                        val animatedSize by animateDpAsState(
                            targetValue = if (isSelected) 30.dp else 22.dp,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "colorSwatchSize"
                        )

                        Box(
                            modifier = Modifier.size(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Outer glowing ring for selected state
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, itemColor.copy(alpha = 0.6f), CircleShape)
                                )
                            }

                            // Color Dot
                            Box(
                                modifier = Modifier
                                    .size(animatedSize)
                                    .clip(CircleShape)
                                    .background(itemColor)
                                    .clickable {
                                        TabManager.renameGroup(group.id, group.title, colorVal)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    val iconTint = if (colorVal == 0xFFFBBF24.toLong() || colorVal == 0xFF34D399.toLong()) Color.Black else Color.White
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected color",
                                        tint = iconTint,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Clean subtle theme divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(colors.border.copy(alpha = 0.35f))
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Grid of Group Tabs (Cleanly sized without dead space)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(gridHeight)
                ) {
                    items(tabs, key = { it.id }) { tab ->
                        val isActive = tab.id == activeTabId
                        GroupTabGridItem(
                            tab = tab,
                            isActive = isActive,
                            groupColor = groupColor,
                            onClick = {
                                onTabSelected(tab)
                                onDismiss()
                            },
                            onClose = { onCloseTab(tab.id) }
                        )
                    }

                    // Interactive Add Tab Card within Group
                    item {
                        GroupAddTabCard(
                            groupTitle = group.title,
                            groupColor = groupColor,
                            onClick = {
                                onAddTabInGroup()
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }

        if (showRenameDialog) {
            RenameGroupDialog(
                currentTitle = group.title,
                groupColor = groupColor,
                onConfirm = { newTitle ->
                    TabManager.renameGroup(group.id, newTitle)
                    showRenameDialog = false
                },
                onDismiss = { showRenameDialog = false }
            )
        }

        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                shape = RoundedCornerShape(24.dp),
                title = {
                    Text(
                        text = "Close Group?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = colors.textPrimary
                    )
                },
                text = {
                    Text(
                        text = "This will close all ${tabs.size} tabs in \"${group.title}\".",
                        fontSize = 14.sp,
                        color = colors.textSecondary
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteConfirmDialog = false
                            TabManager.closeGroup(group.id)
                            onDismiss()
                        }
                    ) {
                        Text("Close Group", fontWeight = FontWeight.Bold, color = colors.statusDestructive)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = false }) {
                        Text("Cancel", color = colors.textSecondary)
                    }
                },
                containerColor = colors.modalBg
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

    val cardShape = RoundedCornerShape(18.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.965f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cardPressScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .clip(cardShape)
            .background(colors.cardBg)
            .border(
                BorderStroke(
                    width = if (isActive) 2.dp else 1.dp,
                    color = if (isActive) groupColor else colors.border.copy(alpha = 0.65f)
                ),
                shape = cardShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = groupColor.copy(alpha = 0.2f)),
                onClick = onClick
            )
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
                    .background(colors.surfaceElevated.copy(alpha = 0.95f))
                    .padding(start = 10.dp, end = 6.dp),
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
                            modifier = Modifier.size(15.dp)
                        )
                    } else if (tab.isTor) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_tor),
                            contentDescription = "Tor Tab",
                            modifier = Modifier.size(15.dp)
                        )
                    } else if (tab.isPrivate) {
                        Icon(
                            imageVector = Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = Color(0xFFA855F7),
                            modifier = Modifier.size(15.dp)
                        )
                    } else {
                        FaviconImage(
                            url = tab.url,
                            faviconUrl = tab.faviconUrl,
                            isTor = tab.isTor,
                            size = 15.dp
                        )
                    }

                    Spacer(modifier = Modifier.width(7.dp))

                    Text(
                        text = if (isHome) "New Tab" else tab.title.ifBlank { host },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Close Tab Button with comfortable touch area and chip background
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(colors.surface.copy(alpha = 0.55f))
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

            // Hairline separator below card header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(colors.border.copy(alpha = 0.4f))
            )

            // 2. Webpage Live Screenshot or Miniature Wallpaper Home Screen
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(colors.surfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                val cachedThumbnail = TabThumbnailCache.get(tab.id)
                if (cachedThumbnail != null) {
                    Image(
                        bitmap = cachedThumbnail.asImageBitmap(),
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
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(tab.wallpaperResId ?: com.azokle.search.settings.WallpaperManager.getCurrentWallpaper())
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x45000000))
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
                                    .width(108.dp)
                                    .height(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x99000000))
                                    .border(0.5.dp, Color(0x66FFFFFF), CircleShape)
                                    .padding(horizontal = 8.dp),
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
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = "Search",
                                        fontSize = 8.5.sp,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(11.dp)
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
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(colors.surface)
                                    .border(1.dp, colors.border.copy(alpha = 0.7f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                FaviconImage(
                                    url = tab.url,
                                    faviconUrl = tab.faviconUrl,
                                    isTor = tab.isTor,
                                    size = 22.dp
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(colors.surface)
                                    .border(0.5.dp, colors.border.copy(alpha = 0.6f), CircleShape)
                                    .padding(horizontal = 10.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = host,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // High-Contrast Active Badge Pill with group-colored background
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(groupColor)
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                            Text(
                                text = "ACTIVE",
                                fontSize = 8.5.sp,
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
}

@Composable
private fun GroupAddTabCard(
    groupTitle: String,
    groupColor: Color,
    onClick: () -> Unit
) {
    val colors = AzokleTheme.colors
    val cardShape = RoundedCornerShape(18.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.965f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "addCardPressScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .clip(cardShape)
            .background(colors.surfaceElevated.copy(alpha = 0.45f))
            .border(
                BorderStroke(1.2.dp, groupColor.copy(alpha = 0.45f)),
                shape = cardShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = groupColor.copy(alpha = 0.2f)),
                onClick = onClick
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(groupColor.copy(alpha = 0.15f))
                    .border(1.5.dp, groupColor.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Tab",
                    tint = groupColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "New Tab",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "in $groupTitle",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = groupColor.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RenameGroupDialog(
    currentTitle: String,
    groupColor: Color,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = AzokleTheme.colors
    var title by remember { mutableStateOf(currentTitle) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(groupColor)
                )
                Text(
                    text = "Rename Group",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = colors.textPrimary
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "Give this tab group a distinct name:",
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = groupColor,
                        unfocusedBorderColor = colors.border,
                        focusedLabelColor = groupColor,
                        cursorColor = groupColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) onConfirm(title.trim())
                }
            ) {
                Text("Save", fontWeight = FontWeight.Bold, color = groupColor)
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
