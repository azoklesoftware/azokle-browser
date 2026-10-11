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

package com.azokle.search.tabs

import android.net.Uri
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
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.azokle.search.R
import com.azokle.search.common.components.FaviconImage
import com.azokle.search.common.theme.AzokleRadius
import com.azokle.search.common.theme.AzokleTheme
import com.azokle.search.common.theme.adaptiveContentWidth
import com.azokle.search.settings.WallpaperManager

/**
 * Premium, Clean, and Minimalist Tab Group Popup Dialog.
 * Opens as a centered, floating popup card with dimmed backdrop.
 * Header side is stripped of clutter (no close button, no 3-dot menu).
 * Tab cards are optimized to the exact high-fidelity visual and tactile standard of ActiveTabsScreen.
 */
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
    val haptic = LocalHapticFeedback.current
    var showEditGroupDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showUngroupConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(tabs.isEmpty()) {
        if (tabs.isEmpty()) {
            onDismiss()
        }
    }

    // Dynamic grid height calculation for floating popup dialog matching ActiveTabsCard proportion
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val maxGridHeight = screenHeight * 0.60f

    val totalItems = tabs.size + 1 // Existing tabs + "New Tab in Group" card
    val rowCount = (totalItems + 1) / 2
    val cardHeight = 195.dp
    val spacing = 12.dp
    val calculatedGridHeight = (cardHeight * rowCount) + (spacing * (rowCount - 1).coerceAtLeast(0))
    val gridHeight = minOf(calculatedGridHeight, maxGridHeight)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            // Floating Popup Card Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .adaptiveContentWidth(560.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(colors.modalBg)
                    .border(1.dp, colors.border.copy(alpha = 0.55f), RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    // 1. Clean, Minimalist Header Bar (NO close button, NO 3-dot menu)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left: Group Title, Accent Color Dot & Tab Count Pill (Tappable to Edit)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(color = groupColor.copy(alpha = 0.15f)),
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        showEditGroupDialog = true
                                    }
                                )
                                .padding(vertical = 4.dp, horizontal = 4.dp)
                        ) {
                            // Halo Accent Color Indicator
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(groupColor)
                                    .border(2.dp, groupColor.copy(alpha = 0.35f), CircleShape)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = group.title,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            // Clean Tab Count Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(groupColor.copy(alpha = 0.14f))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${tabs.size} ${if (tabs.size == 1) "tab" else "tabs"}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = groupColor
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit group name and color",
                                tint = colors.textSecondary.copy(alpha = 0.5f),
                                modifier = Modifier.size(13.dp)
                            )
                        }

                        // Right: Only "+" Add Tab in Group Button
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(groupColor.copy(alpha = 0.15f))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true, radius = 17.dp, color = groupColor),
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onAddTabInGroup()
                                        onDismiss()
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New tab in group",
                                tint = groupColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Subtle Hairline Divider
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.6.dp)
                            .background(colors.border.copy(alpha = 0.3f))
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. Responsive Grid of Group Tabs (ActiveTabsCard Level)
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
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onTabSelected(tab)
                                    onDismiss()
                                },
                                onClose = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onCloseTab(tab.id)
                                }
                            )
                        }

                        // Interactive Add Tab Card within Group
                        item(key = "group_add_tab_card") {
                            GroupAddTabCard(
                                groupTitle = group.title,
                                groupColor = groupColor,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onAddTabInGroup()
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            }
        }

        // 3. Unified Edit Group Dialog (Title + Color Palette + Ungroup & Close Actions)
        if (showEditGroupDialog) {
            EditGroupDialog(
                currentTitle = group.title,
                currentColor = group.color,
                tabCount = tabs.size,
                onConfirm = { newTitle, newColor ->
                    TabManager.renameGroup(group.id, newTitle, newColor)
                    showEditGroupDialog = false
                },
                onUngroup = {
                    showEditGroupDialog = false
                    showUngroupConfirmDialog = true
                },
                onCloseGroup = {
                    showEditGroupDialog = false
                    showDeleteConfirmDialog = true
                },
                onDismiss = { showEditGroupDialog = false }
            )
        }

        // 4. Ungroup Confirmation Dialog
        if (showUngroupConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showUngroupConfirmDialog = false },
                shape = RoundedCornerShape(22.dp),
                title = {
                    Text(
                        text = "Ungroup Tabs?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = colors.textPrimary
                    )
                },
                text = {
                    Text(
                        text = "All ${tabs.size} tabs will remain open as standalone tabs.",
                        fontSize = 13.5.sp,
                        color = colors.textSecondary
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showUngroupConfirmDialog = false
                            TabManager.ungroupAll(group.id)
                            onDismiss()
                        }
                    ) {
                        Text("Ungroup", fontWeight = FontWeight.Bold, color = groupColor)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showUngroupConfirmDialog = false }) {
                        Text("Cancel", color = colors.textSecondary)
                    }
                },
                containerColor = colors.modalBg
            )
        }

        // 5. Close Group Confirmation Dialog
        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                shape = RoundedCornerShape(22.dp),
                title = {
                    Text(
                        text = "Close Group?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = colors.textPrimary
                    )
                },
                text = {
                    Text(
                        text = "This will close all ${tabs.size} tabs in \"${group.title}\".",
                        fontSize = 13.5.sp,
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

/**
 * Sovereign Tab Card matching the ActiveTabsScreen ActiveTabCard standard:
 * - RoundedCornerShape(18.dp) with tactile shadow and dynamic group accent borders.
 * - 38dp elevated header with Favicon chip and haptic close button.
 * - Hardware-accelerated cached thumbnail previews with bottom shadow gradient.
 * - Clean Active Tab indicator pill with monospace typography.
 */
@Composable
private fun GroupTabGridItem(
    tab: TabSession,
    isActive: Boolean,
    groupColor: Color,
    onClick: () -> Unit,
    onClose: () -> Unit
) {
    val colors = AzokleTheme.colors
    val shape = RoundedCornerShape(18.dp)
    val isHome = tab.url.isBlank() || tab.url == "about:home" || tab.url == "about:blank"
    val host = remember(tab.url) {
        try {
            Uri.parse(tab.url).host?.removePrefix("www.")?.ifBlank { tab.url } ?: tab.url
        } catch (e: Exception) {
            tab.url
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.965f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "groupCardPressScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(195.dp)
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .shadow(
                elevation = if (isActive) 5.dp else 1.dp,
                shape = shape,
                spotColor = if (isActive) groupColor.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.2f)
            )
            .clip(shape)
            .background(colors.cardBg)
            .border(
                width = if (isActive) 1.5.dp else 0.8.dp,
                color = if (isActive) groupColor else colors.border.copy(alpha = 0.55f),
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = groupColor.copy(alpha = 0.25f)),
                onClick = onClick
            )
            .semantics {
                role = Role.Button
                contentDescription = "Tab: ${tab.title.ifBlank { if (isHome) "New Tab" else host }}${if (isActive) ", Active" else ""}"
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar (Matching ActiveTabCard 38dp standard)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .background(colors.surfaceElevated.copy(alpha = 0.85f))
                    .padding(start = 10.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Favicon / Mode Icon Chip
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(AzokleRadius.sm)
                        .background(colors.surface)
                        .border(0.5.dp, colors.border.copy(alpha = 0.5f), AzokleRadius.sm),
                    contentAlignment = Alignment.Center
                ) {
                    if (isHome) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            tint = colors.actionPrimaryBg,
                            modifier = Modifier.size(12.dp)
                        )
                    } else if (tab.isTor) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_tor),
                            contentDescription = "Tor Tab",
                            modifier = Modifier.size(12.dp)
                        )
                    } else if (tab.isPrivate) {
                        Icon(
                            imageVector = Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = Color(0xFFA855F7),
                            modifier = Modifier.size(12.dp)
                        )
                    } else {
                        FaviconImage(
                            url = tab.url,
                            faviconUrl = tab.faviconUrl,
                            isTor = tab.isTor,
                            size = 13.dp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Group Color Accent Dot
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(groupColor)
                )

                Spacer(modifier = Modifier.width(5.dp))

                Text(
                    text = if (isHome) "New Tab" else tab.title.ifBlank { host },
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(4.dp))

                // Close Tab Button (Matching ActiveTabCard 26dp with haptic feedback)
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(colors.surface)
                        .border(0.5.dp, colors.border.copy(alpha = 0.4f), CircleShape)
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

            // Thumbnail / Content Preview Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(colors.surfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                val cachedThumbnail = TabThumbnailCache.get(tab.id)
                if (cachedThumbnail != null) {
                    val imageBitmap = remember(cachedThumbnail) { cachedThumbnail.asImageBitmap() }
                    Image(
                        bitmap = imageBitmap,
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
                                    listOf(
                                        Color.Transparent,
                                        Color(0x08000000),
                                        Color(0x40000000)
                                    )
                                )
                            )
                    )
                } else if (isHome) {
                    val context = LocalContext.current
                    val homeWallpaperModel = remember(tab.wallpaperResId, context) {
                        ImageRequest.Builder(context)
                            .data(tab.wallpaperResId ?: WallpaperManager.getCurrentWallpaper())
                            .crossfade(false)
                            .build()
                    }
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = homeWallpaperModel,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x33000000))
                        )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.align(Alignment.Center)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Home",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp)
                    ) {
                        FaviconImage(
                            url = tab.url,
                            faviconUrl = tab.faviconUrl,
                            isTor = tab.isTor,
                            size = 28.dp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = host,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Active Tab Indicator Badge (Matching ActiveTabCard standard)
        if (isActive) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(groupColor)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
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

/**
 * Clean & Minimalist "Add Tab" Card matching ActiveTabCard proportion & shadow.
 */
@Composable
private fun GroupAddTabCard(
    groupTitle: String,
    groupColor: Color,
    onClick: () -> Unit
) {
    val colors = AzokleTheme.colors
    val shape = RoundedCornerShape(18.dp)
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
            .height(195.dp)
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .shadow(
                elevation = 1.dp,
                shape = shape,
                spotColor = Color.Black.copy(alpha = 0.15f)
            )
            .clip(shape)
            .background(colors.surfaceElevated.copy(alpha = 0.4f))
            .border(
                BorderStroke(1.dp, groupColor.copy(alpha = 0.35f)),
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = groupColor.copy(alpha = 0.2f)),
                onClick = onClick
            )
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(groupColor.copy(alpha = 0.12f))
                    .border(1.2.dp, groupColor.copy(alpha = 0.4f), CircleShape),
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
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = "in $groupTitle",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Normal,
                color = groupColor.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Unified Edit Group Dialog: combines Title input, Color Palette selector, and Group Management (Ungroup & Close).
 */
@Composable
private fun EditGroupDialog(
    currentTitle: String,
    currentColor: Long,
    tabCount: Int,
    onConfirm: (String, Long) -> Unit,
    onUngroup: () -> Unit,
    onCloseGroup: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = AzokleTheme.colors
    var title by remember { mutableStateOf(currentTitle) }
    var selectedColor by remember { mutableLongStateOf(currentColor) }
    val activeColor = Color(selectedColor)

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
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(activeColor)
                        .border(2.dp, activeColor.copy(alpha = 0.35f), CircleShape)
                )
                Text(
                    text = "Edit Tab Group",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = colors.textPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Name Input Field
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    singleLine = true,
                    label = { Text("Group Name", fontSize = 13.sp) },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = activeColor,
                        unfocusedBorderColor = colors.border,
                        focusedLabelColor = activeColor,
                        cursorColor = activeColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Color Palette Label
                Text(
                    text = "Accent Color",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary
                )

                // 8-Color Palette Swatches
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TabGroup.GROUP_PALETTE.forEach { colorVal ->
                        val isSelected = selectedColor == colorVal
                        val itemColor = Color(colorVal)

                        val animatedSize by animateDpAsState(
                            targetValue = if (isSelected) 28.dp else 20.dp,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "paletteDotSize"
                        )

                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .clickable { selectedColor = colorVal },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, itemColor.copy(alpha = 0.65f), CircleShape)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(animatedSize)
                                    .clip(CircleShape)
                                    .background(itemColor),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    val iconTint = if (colorVal == 0xFFFBBF24 || colorVal == 0xFF34D399) Color.Black else Color.White
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected color",
                                        tint = iconTint,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Group Management Actions (Ungroup & Close)
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.6.dp)
                        .background(colors.border.copy(alpha = 0.35f))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Ungroup Action
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onUngroup)
                            .padding(vertical = 6.dp, horizontal = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LayersClear,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Ungroup",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textSecondary
                        )
                    }

                    // Close Group Action
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onCloseGroup)
                            .padding(vertical = 6.dp, horizontal = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = colors.statusDestructive,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Close Group",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.statusDestructive
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) onConfirm(title.trim(), selectedColor)
                }
            ) {
                Text("Save", fontWeight = FontWeight.Bold, color = activeColor)
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
