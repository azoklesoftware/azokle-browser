package com.azokle.search.browser.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azokle.search.ui.theme.AzokleRadius
import com.azokle.search.ui.theme.AzokleTheme
import com.azokle.search.ui.theme.adaptiveContentWidth

/**
 * Sovereign Shared Menu Sheet:
 *  - Context-Aware: In browser shows live webpage tools (Reader Mode, Find in Page, Desktop switch).
 *    In Home mode shows app-level tools (New Tab, New Private Tab, New Tor, Bookmarks), hiding page-only actions.
 *  - Excludes Picture-in-Picture completely.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserMenuSheet(
    url: String,
    title: String,
    isDesktopMode: Boolean = false,
    isBookmarked: Boolean = false,
    blockedTrackersCount: Int = 0,
    isBrowserContext: Boolean = true,
    onReload: () -> Unit = {},
    onToggleBookmark: () -> Unit = {},
    onShare: () -> Unit = {},
    onToggleDesktopMode: () -> Unit = {},
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
    onOpenAbout: () -> Unit = {},
    pwaEligibility: com.azokle.search.browser.pwa.PwaInstallEligibility? = null,
    onInstallPwa: () -> Unit = {},
    onBurnData: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = AzokleTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                .adaptiveContentWidth(560.dp)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top Quick Action Icons Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AzokleRadius.lg)
                    .background(colors.cardBg)
                    .border(1.dp, colors.border, AzokleRadius.lg)
                    .padding(vertical = 12.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isBrowserContext) {
                    // Reload Current Webpage
                    QuickMenuIconButton(
                        icon = Icons.Default.Refresh,
                        label = "Reload",
                        tint = colors.textPrimary,
                        onClick = {
                            onReload()
                            onDismiss()
                        }
                    )

                    // Bookmark Current Webpage
                    QuickMenuIconButton(
                        icon = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        label = if (isBookmarked) "Saved" else "Bookmark",
                        tint = if (isBookmarked) colors.statusWarning else colors.textPrimary,
                        onClick = {
                            onToggleBookmark()
                            onDismiss()
                        }
                    )

                    // Share Webpage URL
                    QuickMenuIconButton(
                        icon = Icons.Default.Share,
                        label = "Share",
                        tint = colors.textPrimary,
                        onClick = {
                            onShare()
                            onDismiss()
                        }
                    )

                    // Shield HUD
                    QuickMenuIconButton(
                        icon = Icons.Default.Shield,
                        label = "Shield ($blockedTrackersCount)",
                        tint = colors.statusSuccess,
                        onClick = {
                            onDismiss()
                            onOpenShield()
                        }
                    )
                } else {
                    // Home Context 1: New Tab
                    QuickMenuIconButton(
                        icon = Icons.Default.Add,
                        label = "New Tab",
                        tint = colors.textPrimary,
                        onClick = {
                            onDismiss()
                            onOpenNewTab()
                        }
                    )

                    // Home Context 2: New Private Tab
                    QuickMenuIconButton(
                        icon = Icons.Default.Shield,
                        label = "Private",
                        tint = colors.statusAccent,
                        onClick = {
                            onDismiss()
                            onOpenNewPrivateTab()
                        }
                    )

                    // Home Context 3: New Tor
                    QuickMenuIconButton(
                        icon = Icons.Default.Public,
                        label = "New Tor",
                        tint = colors.statusTor,
                        onClick = {
                            onDismiss()
                            onOpenTorWindow()
                        }
                    )

                    // Home Context 4: Bookmarks
                    QuickMenuIconButton(
                        icon = Icons.Default.Bookmark,
                        label = "Bookmarks",
                        tint = colors.statusWarning,
                        onClick = {
                            onDismiss()
                            onOpenBookmarks()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Main Action Items Group
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AzokleRadius.lg)
                    .background(colors.cardBg)
                    .border(1.dp, colors.border, AzokleRadius.lg)
            ) {
                // New Tor Private Window
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = colors.statusTor.copy(alpha = 0.15f)),
                            onClick = {
                                onDismiss()
                                onOpenTorWindow()
                            }
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .semantics {
                            role = Role.Button
                            contentDescription = "Open new Tor private window"
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(colors.statusTor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "🧅", fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "New Tor Window",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.statusTor
                            )
                            Text(
                                text = "Isolated 3-hop onion circuit • .onion support",
                                fontSize = 11.sp,
                                color = colors.textMuted
                            )
                        }
                    }
                }

                HorizontalDivider(color = colors.border, thickness = 1.dp)

                // Webpage Only Tools: Distraction-Free Reader Mode & Find in Page & Desktop Site
                if (isBrowserContext) {
                    // Distraction-Free Reader Mode
                    MenuActionRow(
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        title = "Distraction-Free Reader Mode",
                        subtitle = "Clean typography without ads, trackers or clutter",
                        onClick = {
                            onDismiss()
                            onOpenReaderMode()
                        }
                    )

                    HorizontalDivider(color = colors.border, thickness = 1.dp)

                    // Find in Webpage
                    MenuActionRow(
                        icon = Icons.Default.FindInPage,
                        title = "Find in Webpage",
                        subtitle = "Search text occurrences across the live page",
                        onClick = {
                            onDismiss()
                            onOpenFindInPage()
                        }
                    )

                    HorizontalDivider(color = colors.border, thickness = 1.dp)

                    // Desktop Site Toggle Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = colors.textPrimary.copy(alpha = 0.08f)),
                                onClick = onToggleDesktopMode
                            )
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .semantics {
                                role = Role.Switch
                                contentDescription = "Desktop site, currently ${if (isDesktopMode) "enabled" else "disabled"}"
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Devices,
                                contentDescription = null,
                                tint = colors.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Desktop Site",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "Request desktop user-agent view",
                                    fontSize = 11.sp,
                                    color = colors.textMuted
                                )
                            }
                        }

                        Switch(
                            checked = isDesktopMode,
                            onCheckedChange = { onToggleDesktopMode() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = colors.statusAccent,
                                uncheckedThumbColor = colors.textSecondary,
                                uncheckedTrackColor = colors.surface
                            )
                        )
                    }

                    HorizontalDivider(color = colors.border, thickness = 1.dp)
                }

                // Bookmarks & Quick Links
                MenuActionRow(
                    icon = Icons.Default.Star,
                    title = "Bookmarks & Quick Links",
                    subtitle = "Manage saved links, shortcuts and reading list",
                    onClick = {
                        onDismiss()
                        onOpenBookmarks()
                    }
                )

                HorizontalDivider(color = colors.border, thickness = 1.dp)

                // Downloads
                MenuActionRow(
                    icon = Icons.Default.Download,
                    title = "Downloads",
                    subtitle = "Manage offline files and saved resources",
                    onClick = {
                        onDismiss()
                        onOpenDownloads()
                    }
                )

                HorizontalDivider(color = colors.border, thickness = 1.dp)

                // Browsing History
                MenuActionRow(
                    icon = Icons.Default.History,
                    title = "History",
                    subtitle = "Encrypted local browsing activity",
                    onClick = {
                        onDismiss()
                        onOpenHistory()
                    }
                )

                HorizontalDivider(color = colors.border, thickness = 1.dp)

                // Settings
                MenuActionRow(
                    icon = Icons.Default.Settings,
                    title = "Settings",
                    subtitle = "Search engines, DNS, security & appearance",
                    onClick = {
                        onDismiss()
                        onOpenSettings()
                    }
                )

                if (!isBrowserContext) {
                    HorizontalDivider(color = colors.border, thickness = 1.dp)

                    // About
                    MenuActionRow(
                        icon = Icons.Default.Info,
                        title = "About Azokle",
                        subtitle = "v-1.0.9 • Sovereign Privacy Engine",
                        onClick = {
                            onDismiss()
                            onOpenAbout()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Cinema-Grade Fire Incineration Destruction Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AzokleRadius.lg)
                    .background(colors.cardBg)
                    .border(1.dp, colors.statusDestructive.copy(alpha = 0.4f), AzokleRadius.lg)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, color = colors.statusDestructive.copy(alpha = 0.2f)),
                        onClick = {
                            onDismiss()
                            onBurnData()
                        }
                    )
                    .padding(16.dp)
                    .semantics {
                        role = Role.Button
                        contentDescription = "Burn all tabs and browsing data"
                    }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(colors.statusDestructive.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = colors.statusDestructive,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Burn Browsing Session",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.statusDestructive
                        )
                        Text(
                            text = "Instantly wipe all ephemeral tabs, cache & recent history",
                            fontSize = 11.sp,
                            color = colors.textMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun QuickMenuIconButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    val colors = AzokleTheme.colors

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .sizeIn(minWidth = 54.dp)
            .clip(AzokleRadius.md)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 22.dp, color = colors.textPrimary),
                onClick = onClick
            )
            .padding(vertical = 4.dp, horizontal = 6.dp)
            .semantics {
                role = Role.Button
                contentDescription = label
            }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textSecondary
        )
    }
}

@Composable
private fun MenuActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val colors = AzokleTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = colors.textPrimary.copy(alpha = 0.08f)),
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 13.dp)
            .semantics {
                role = Role.Button
                contentDescription = title
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = colors.textMuted
            )
        }
    }
}
