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

package com.azokle.search.engine.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azokle.search.R
import com.azokle.search.common.theme.AzokleTheme
import com.azokle.search.pwa.PwaInstallEligibility

/**
 * Sovereign Luxury Browser Dropup Menu:
 *  - Expands upward comfortably up to the top Omnibar area for an expansive, spacious experience.
 *  - Frosted surface elevation with 26dp soft curvature and subtle border glow.
 *  - Live context-aware Webpage Capsule with dynamic Shield counter.
 *  - Ergonomic Quick Action Toolbar (Reload, Share, Bookmark, Find in Page, Desktop Mode).
 *  - Organized grouped actions (Privacy & Isolation, Webpage Utilities, Library, System & Danger).
 *  - Tactile haptic feedback and smooth responsive vertical scrolling.
 */
@Composable
fun BrowserMenuDropup(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    url: String,
    title: String,
    isDesktopMode: Boolean = false,
    isBookmarked: Boolean = false,
    blockedTrackersCount: Int = 0,
    isBrowserContext: Boolean = true,
    showHomeAndBookmarks: Boolean = false,
    onHomeClick: () -> Unit = {},
    onReload: () -> Unit = {},
    onToggleBookmark: () -> Unit = {},
    onShare: () -> Unit = {},
    onToggleDesktopMode: () -> Unit = {},
    onSaveAsPdf: () -> Unit = {},
    onOpenReaderMode: () -> Unit = {},
    onOpenFindInPage: () -> Unit = {},
    onOpenNewTab: () -> Unit = {},
    onOpenNewPrivateTab: () -> Unit = {},
    onOpenBookmarks: () -> Unit = {},
    onOpenDownloads: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onOpenShield: () -> Unit = {},
    onOpenTorWindow: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    pwaEligibility: PwaInstallEligibility? = null,
    onInstallPwa: () -> Unit = {},
    onBurnData: () -> Unit = {},
    offset: DpOffset = DpOffset(0.dp, (-6).dp),
    modifier: Modifier = Modifier
) {
    val colors = AzokleTheme.colors
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val configuration = LocalConfiguration.current
    // Expansive height reaching up to just below the top Omnibar
    val maxAvailableHeight = (configuration.screenHeightDp - 108).dp.coerceAtLeast(480.dp)

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        offset = offset,
        shape = RoundedCornerShape(26.dp),
        containerColor = colors.cardBg,
        tonalElevation = 8.dp,
        shadowElevation = 24.dp,
        border = BorderStroke(1.dp, colors.border.copy(alpha = 0.55f)),
        modifier = modifier
            .widthIn(min = 310.dp, max = 350.dp)
            .heightIn(max = maxAvailableHeight)
            .padding(vertical = 4.dp)
    ) {
        val isWebpage = isBrowserContext && url.isNotBlank() && url != "about:home" && url != "about:blank" && url != "about:tor"

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxAvailableHeight)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 0. Optional Home & Bookmarks Shortcuts (Tablet / Omnibar mode)
            if (showHomeAndBookmarks) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.surfaceElevated)
                        .border(1.dp, colors.border.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DropupQuickActionPill(
                        icon = Icons.Default.Home,
                        label = "Home",
                        tint = colors.textPrimary,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onDismissRequest()
                            onHomeClick()
                        }
                    )
                    DropupQuickActionPill(
                        icon = Icons.Default.BookmarkBorder,
                        label = "Bookmarks",
                        tint = colors.textPrimary,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onDismissRequest()
                            onOpenBookmarks()
                        }
                    )
                }

                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = colors.border.copy(alpha = 0.35f),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }


            // 2. Ergonomic Quick Action Toolbar (Reload, Share, Bookmark, Find, Desktop)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surfaceElevated)
                    .border(1.dp, colors.border.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 4.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reload
                DropupQuickActionPill(
                    icon = Icons.Default.Refresh,
                    label = "Reload",
                    tint = colors.textPrimary,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onDismissRequest()
                        onReload()
                    }
                )

                // Share
                DropupQuickActionPill(
                    icon = Icons.Default.Share,
                    label = "Share",
                    tint = if (isWebpage) colors.textPrimary else colors.textMuted.copy(alpha = 0.35f),
                    enabled = isWebpage,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onDismissRequest()
                        onShare()
                    }
                )

                // Bookmark
                DropupQuickActionPill(
                    icon = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    label = if (isBookmarked) "Saved" else "Bookmark",
                    tint = colors.textPrimary,
                    isActive = isBookmarked,
                    activeBorderColor = colors.textPrimary,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onDismissRequest()
                        onToggleBookmark()
                    }
                )

                // Find in Page
                DropupQuickActionPill(
                    icon = Icons.Default.FindInPage,
                    label = "Find",
                    tint = if (isWebpage) colors.textPrimary else colors.textMuted.copy(alpha = 0.35f),
                    enabled = isWebpage,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onDismissRequest()
                        onOpenFindInPage()
                    }
                )

                // Desktop / Mobile Mode Toggle
                DropupQuickActionPill(
                    icon = if (isDesktopMode) Icons.Default.PhoneAndroid else Icons.Default.Devices,
                    label = if (isDesktopMode) "Mobile" else "Desktop",
                    tint = if (isWebpage) colors.textPrimary else colors.textMuted.copy(alpha = 0.35f),
                    isActive = isDesktopMode && isWebpage,
                    activeBorderColor = colors.textPrimary,
                    enabled = isWebpage,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onToggleDesktopMode()
                    }
                )
            }

            HorizontalDivider(
                thickness = 0.5.dp,
                color = colors.border.copy(alpha = 0.35f),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )

            // 3. Navigation & Privacy Section (Pure Minimalist UI)
            DropupMenuItemRow(
                title = "New Tab",
                icon = Icons.Default.Add,
                iconTint = colors.textPrimary,
                iconBg = colors.surfaceElevated,
                onClick = {
                    onDismissRequest()
                    onOpenNewTab()
                }
            )

            DropupMenuItemRow(
                title = "New Private Tab",
                icon = Icons.Default.Security,
                iconTint = colors.textPrimary,
                iconBg = colors.surfaceElevated,
                badge = "Private",
                badgeColor = colors.textSecondary,
                onClick = {
                    onDismissRequest()
                    onOpenNewPrivateTab()
                }
            )

            DropupMenuItemRow(
                title = "New Tor Window",
                iconRes = R.drawable.ic_tor,
                iconTint = colors.textPrimary,
                iconBg = colors.surfaceElevated,
                badge = "Tor Onion",
                badgeColor = colors.textSecondary,
                onClick = {
                    onDismissRequest()
                    onOpenTorWindow()
                }
            )

            DropupMenuItemRow(
                title = "Azokle Shield",
                icon = Icons.Default.Security,
                iconTint = colors.textPrimary,
                iconBg = colors.surfaceElevated,
                badge = if (blockedTrackersCount > 0) "$blockedTrackersCount Blocked" else "Protected",
                badgeColor = colors.textSecondary,
                onClick = {
                    onDismissRequest()
                    onOpenShield()
                }
            )

            // 4. Webpage Utilities Section (only available when on active webpage)
            if (isWebpage) {
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = colors.border.copy(alpha = 0.35f),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )

                DropupMenuItemRow(
                    title = "Reader Mode",
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    iconTint = colors.textPrimary,
                    iconBg = colors.surfaceElevated,
                    onClick = {
                        onDismissRequest()
                        onOpenReaderMode()
                    }
                )

                DropupMenuItemRow(
                    title = "Save as PDF",
                    icon = Icons.Default.Download,
                    iconTint = colors.textPrimary,
                    iconBg = colors.surfaceElevated,
                    onClick = {
                        onDismissRequest()
                        onSaveAsPdf()
                    }
                )

                val installedPwas by com.azokle.search.pwa.PwaStorageManager.getInstance(context).installedPwasFlow.collectAsState()
                val installedPwa = remember(url, installedPwas) {
                    if (url.isNotBlank()) {
                        com.azokle.search.pwa.PwaStorageManager.getInstance(context).findPwaForUrl(url)
                    } else null
                }

                if (installedPwa != null) {
                    DropupMenuItemRow(
                        title = "Open in ${installedPwa.title}",
                        icon = Icons.Default.OpenInBrowser,
                        iconTint = colors.textPrimary,
                        iconBg = colors.surfaceElevated,
                        badge = "PWA",
                        badgeColor = colors.textSecondary,
                        onClick = {
                            onDismissRequest()
                            com.azokle.search.pwa.PwaShortcutManager.launchPwa(context, installedPwa)
                        }
                    )
                    DropupMenuItemRow(
                        title = "Uninstall ${installedPwa.title}",
                        icon = Icons.Default.DeleteOutline,
                        iconTint = colors.statusDestructive,
                        iconBg = colors.statusDestructive.copy(alpha = 0.12f),
                        badge = "Remove",
                        badgeColor = colors.statusDestructive,
                        onClick = {
                            onDismissRequest()
                            com.azokle.search.pwa.PwaStorageManager.getInstance(context).uninstallPwa(installedPwa.id)
                            android.widget.Toast.makeText(context, "${installedPwa.title} uninstalled", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                } else if (pwaEligibility != null) {
                    DropupMenuItemRow(
                        title = if (pwaEligibility.isInstallablePwa) "Install Web App" else "Add to Home screen",
                        icon = Icons.Default.InstallMobile,
                        iconTint = colors.textPrimary,
                        iconBg = colors.surfaceElevated,
                        badge = if (pwaEligibility.isInstallablePwa) "App" else null,
                        badgeColor = colors.textSecondary,
                        onClick = {
                            onDismissRequest()
                            onInstallPwa()
                        }
                    )
                }
            }

            HorizontalDivider(
                thickness = 0.5.dp,
                color = colors.border.copy(alpha = 0.35f),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )

            // 5. Library & Data Section
            DropupMenuItemRow(
                title = "Bookmarks",
                icon = Icons.Default.Star,
                iconTint = colors.textPrimary,
                iconBg = colors.surfaceElevated,
                onClick = {
                    onDismissRequest()
                    onOpenBookmarks()
                }
            )

            DropupMenuItemRow(
                title = "History",
                icon = Icons.Default.History,
                iconTint = colors.textPrimary,
                iconBg = colors.surfaceElevated,
                onClick = {
                    onDismissRequest()
                    onOpenHistory()
                }
            )

            DropupMenuItemRow(
                title = "Downloads",
                icon = Icons.Default.Download,
                iconTint = colors.textPrimary,
                iconBg = colors.surfaceElevated,
                onClick = {
                    onDismissRequest()
                    onOpenDownloads()
                }
            )

            HorizontalDivider(
                thickness = 0.5.dp,
                color = colors.border.copy(alpha = 0.35f),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )

            // 6. Settings & Security Incinerator Reset
            DropupMenuItemRow(
                title = "Settings",
                icon = Icons.Default.Settings,
                iconTint = colors.textPrimary,
                iconBg = colors.surfaceElevated,
                onClick = {
                    onDismissRequest()
                    onOpenSettings()
                }
            )

            DropupMenuItemRow(
                title = "Burn Browsing Data",
                icon = Icons.Default.LocalFireDepartment,
                iconTint = colors.statusDestructive,
                iconBg = colors.statusDestructive.copy(alpha = 0.12f),
                isDestructive = true,
                badge = "Clear",
                badgeColor = colors.statusDestructive,
                onClick = {
                    onDismissRequest()
                    onBurnData()
                }
            )
        }
    }
}

/**
 * Compact circular quick action pill button in the dropup toolbar
 */
@Composable
private fun DropupQuickActionPill(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    enabled: Boolean = true,
    isActive: Boolean = false,
    activeBorderColor: Color? = null
) {
    val colors = AzokleTheme.colors
    val haptic = LocalHapticFeedback.current

    val finalTint = when {
        !enabled -> colors.textMuted.copy(alpha = 0.35f)
        isActive -> activeBorderColor ?: colors.actionPrimaryBg
        tint != null -> tint
        else -> colors.textPrimary
    }

    val finalBg = when {
        !enabled -> Color.Transparent
        isActive -> (activeBorderColor ?: colors.actionPrimaryBg).copy(alpha = 0.14f)
        else -> colors.cardBg
    }

    val finalBorder = when {
        !enabled -> colors.border.copy(alpha = 0.2f)
        isActive -> activeBorderColor ?: colors.actionPrimaryBg
        else -> colors.border.copy(alpha = 0.45f)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 22.dp, color = colors.actionPrimaryBg),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            )
            .padding(vertical = 4.dp, horizontal = 2.dp)
            .semantics {
                role = Role.Button
                contentDescription = label
            }
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(finalBg)
                .border(
                    width = if (isActive) 1.5.dp else 0.5.dp,
                    color = finalBorder,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = finalTint,
                modifier = Modifier.size(19.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.5.dp))

        Text(
            text = label,
            fontSize = 10.5.sp,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
            color = when {
                !enabled -> colors.textMuted.copy(alpha = 0.35f)
                isActive -> colors.textPrimary
                else -> colors.textSecondary
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Standard menu item row with squircle icon, clear typography, optional badge and subtle border
 */
@Composable
private fun DropupMenuItemRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconRes: Int? = null,
    iconTint: Color? = null,
    iconBg: Color? = null,
    badge: String? = null,
    badgeColor: Color? = null,
    isDestructive: Boolean = false
) {
    val colors = AzokleTheme.colors
    val haptic = LocalHapticFeedback.current

    val finalIconTint = when {
        isDestructive -> colors.statusDestructive
        iconTint != null -> iconTint
        else -> colors.textPrimary
    }

    val finalIconBg = when {
        isDestructive -> colors.statusDestructive.copy(alpha = 0.12f)
        iconBg != null -> iconBg
        else -> colors.surfaceElevated
    }

    val finalTitleColor = if (isDestructive) colors.statusDestructive else colors.textPrimary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(
                    bounded = true,
                    color = if (isDestructive) colors.statusDestructive.copy(alpha = 0.15f) else colors.actionPrimaryBg.copy(alpha = 0.12f)
                ),
                onClick = {
                    if (isDestructive) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    } else {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    onClick()
                }
            )
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon squircle badge
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(finalIconBg)
                .border(0.5.dp, colors.border.copy(alpha = 0.45f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (iconRes != null) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(19.dp)
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = finalIconTint,
                    modifier = Modifier.size(19.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = title,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = finalTitleColor,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (!badge.isNullOrBlank()) {
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(colors.surfaceElevated)
                    .border(0.8.dp, colors.borderSubtle, CircleShape)
                    .padding(horizontal = 8.dp, vertical = 2.5.dp)
            ) {
                Text(
                    text = badge,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = badgeColor ?: colors.textSecondary
                )
            }
        }
    }
}
