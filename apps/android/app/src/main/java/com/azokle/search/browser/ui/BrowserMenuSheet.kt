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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azokle.search.ui.theme.AzokleRadius
import com.azokle.search.ui.theme.AzokleTheme
import com.azokle.search.ui.theme.adaptiveContentWidth

/**
 * Premium Sovereign Browser Menu Sheet:
 *  - Ultra-clean, distraction-free iOS / Arc-style grouped design.
 *  - Context-Aware: Webpage tools (Desktop switch, Reader View, Find in Page, Save PDF) when browsing.
 *  - Core browser actions: Bookmarks, History, Downloads, New Tor Window, Settings, Burn Session.
 *  - Zero redundant paragraph bloat or billboard warning cards.
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
                .adaptiveContentWidth(540.dp)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Optional Page Header Capsule (when on a live webpage)
            val isWebpage = isBrowserContext && url.isNotBlank() && url != "about:home" && url != "about:blank" && url != "about:tor"
            if (isWebpage) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (title.isNotBlank()) title else "Webpage",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = try {
                            val host = android.net.Uri.parse(url).host?.removePrefix("www.")
                            host ?: url
                        } catch (e: Exception) {
                            url
                        },
                        fontSize = 11.sp,
                        color = colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 1. Top Quick Action Buttons Row (Share, Bookmark, Find in Page, Save PDF)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AzokleRadius.xl)
                    .background(colors.cardBg)
                    .border(1.dp, colors.border.copy(alpha = 0.6f), AzokleRadius.xl)
                    .padding(vertical = 12.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Share
                QuickActionPill(
                    icon = Icons.Default.Share,
                    label = "Share",
                    tint = colors.textPrimary,
                    onClick = {
                        onDismiss()
                        onShare()
                    }
                )

                // Bookmark
                QuickActionPill(
                    icon = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    label = if (isBookmarked) "Saved" else "Bookmark",
                    tint = if (isBookmarked) colors.statusWarning else colors.textPrimary,
                    onClick = {
                        onToggleBookmark()
                        onDismiss()
                    }
                )

                // Find in Page
                QuickActionPill(
                    icon = Icons.Default.FindInPage,
                    label = "Find",
                    tint = colors.textPrimary,
                    onClick = {
                        onDismiss()
                        onOpenFindInPage()
                    }
                )

                // Save as PDF
                QuickActionPill(
                    icon = Icons.Default.Download,
                    label = "Save PDF",
                    tint = colors.textPrimary,
                    onClick = {
                        onDismiss()
                        onSaveAsPdf()
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Webpage Controls Card (Desktop Site & Reader Mode & PWA)
            if (isWebpage) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AzokleRadius.lg)
                        .background(colors.cardBg)
                        .border(1.dp, colors.border.copy(alpha = 0.6f), AzokleRadius.lg)
                ) {
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
                            MenuIconBadge(
                                icon = Icons.Default.Devices,
                                tint = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = "Desktop Site",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textPrimary
                            )
                        }

                        Switch(
                            checked = isDesktopMode,
                            onCheckedChange = { onToggleDesktopMode() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = colors.statusAccent,
                                uncheckedThumbColor = colors.textSecondary,
                                uncheckedTrackColor = colors.surfaceElevated,
                                uncheckedBorderColor = colors.border
                            )
                        )
                    }

                    HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 0.5.dp)

                    // Distraction-Free Reader Mode
                    CleanMenuRow(
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        title = "Reader View",
                        onClick = {
                            onDismiss()
                            onOpenReaderMode()
                        }
                    )

                    // PWA Install (if available)
                    if (pwaEligibility != null) {
                        HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 0.5.dp)
                        CleanMenuRow(
                            icon = Icons.Default.InstallMobile,
                            title = "Add to Home Screen",
                            onClick = {
                                onDismiss()
                                onInstallPwa()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // 3. Navigation & Library Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AzokleRadius.lg)
                    .background(colors.cardBg)
                    .border(1.dp, colors.border.copy(alpha = 0.6f), AzokleRadius.lg)
            ) {
                // New Tor Window
                CleanMenuRow(
                    iconText = "🧅",
                    title = "New Tor Window",
                    titleColor = colors.statusTor,
                    iconBg = colors.statusTor.copy(alpha = 0.14f),
                    trailingTint = colors.statusTor,
                    onClick = {
                        onDismiss()
                        onOpenTorWindow()
                    }
                )

                HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 0.5.dp)

                // Bookmarks
                CleanMenuRow(
                    icon = Icons.Default.Star,
                    title = "Bookmarks",
                    onClick = {
                        onDismiss()
                        onOpenBookmarks()
                    }
                )

                HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 0.5.dp)

                // History
                CleanMenuRow(
                    icon = Icons.Default.History,
                    title = "History",
                    onClick = {
                        onDismiss()
                        onOpenHistory()
                    }
                )

                HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 0.5.dp)

                // Downloads
                CleanMenuRow(
                    icon = Icons.Default.Download,
                    title = "Downloads",
                    onClick = {
                        onDismiss()
                        onOpenDownloads()
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Settings & Burn Browsing Session Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AzokleRadius.lg)
                    .background(colors.cardBg)
                    .border(1.dp, colors.border.copy(alpha = 0.6f), AzokleRadius.lg)
            ) {
                // Settings
                CleanMenuRow(
                    icon = Icons.Default.Settings,
                    title = "Settings",
                    onClick = {
                        onDismiss()
                        onOpenSettings()
                    }
                )

                HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 0.5.dp)

                // Burn Browsing Session (Clean Destructive Item)
                CleanMenuRow(
                    icon = Icons.Default.LocalFireDepartment,
                    title = "Burn Browsing Session",
                    titleColor = colors.statusDestructive,
                    iconTint = colors.statusDestructive,
                    iconBg = colors.statusDestructive.copy(alpha = 0.12f),
                    trailingTint = colors.statusDestructive,
                    onClick = {
                        onDismiss()
                        onBurnData()
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Sleek Top Quick Action Pill (Share, Bookmark, Find, PDF)
 */
@Composable
private fun QuickActionPill(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    val colors = AzokleTheme.colors

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(AzokleRadius.md)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 24.dp, color = colors.textPrimary),
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .semantics {
                role = Role.Button
                contentDescription = label
            }
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(colors.surfaceElevated)
                .border(0.5.dp, colors.border.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textSecondary
        )
    }
}

/**
 * Premium, Single-Line Menu Action Row with Leading Icon & Subtle Trailing Chevron
 */
@Composable
private fun CleanMenuRow(
    title: String,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    iconText: String? = null,
    titleColor: Color? = null,
    iconTint: Color? = null,
    iconBg: Color? = null,
    trailingTint: Color? = null
) {
    val colors = AzokleTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = colors.textPrimary.copy(alpha = 0.08f)),
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics {
                role = Role.Button
                contentDescription = title
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            if (iconText != null) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(AzokleRadius.sm)
                        .background(iconBg ?: colors.surfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = iconText, fontSize = 16.sp)
                }
            } else if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(AzokleRadius.sm)
                        .background(iconBg ?: colors.surfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint ?: colors.textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = titleColor ?: colors.textPrimary
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = trailingTint ?: colors.textMuted.copy(alpha = 0.5f),
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * Rounded icon badge container for leading menu row icons
 */
@Composable
private fun MenuIconBadge(
    icon: ImageVector,
    tint: Color
) {
    val colors = AzokleTheme.colors
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(AzokleRadius.sm)
            .background(colors.surfaceElevated),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
    }
}
