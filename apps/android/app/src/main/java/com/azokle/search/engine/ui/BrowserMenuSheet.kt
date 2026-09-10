package com.azokle.search.engine.ui

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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azokle.search.R
import com.azokle.search.common.theme.AzokleRadius
import com.azokle.search.common.theme.AzokleTheme
import com.azokle.search.common.theme.adaptiveContentWidth

/**
 * Premium Sovereign Browser Menu Sheet:
 *  - Unified, seamless, modern icon grid without cluttered category divisions.
 *  - Context-Aware Webpage Header with direct Shield access.
 *  - Ergonomic Quick Action Row (Reload, Share, Bookmark, Find, Desktop Mode).
 *  - Unified Browser Actions Grid (4 columns, balanced alignment, zero messy sub-headers).
 *  - Tactile haptic feedback and responsive tablet/phone adaptation.
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
    pwaEligibility: com.azokle.search.pwa.PwaInstallEligibility? = null,
    onInstallPwa: () -> Unit = {},
    onBurnData: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = AzokleTheme.colors
    val haptic = LocalHapticFeedback.current
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
                    .size(width = 38.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(colors.border.copy(alpha = 0.8f))
            )
        }
    ) {
        val isWebpage = isBrowserContext && url.isNotBlank() && url != "about:home" && url != "about:blank" && url != "about:tor"

        val menuActions = remember(isWebpage, pwaEligibility, blockedTrackersCount, colors) {
            listOfNotNull(
                // 1. Navigation & Isolation
                MenuActionItem(
                    label = "New Tab",
                    icon = Icons.Default.Add,
                    onClick = {
                        onDismiss()
                        onOpenNewTab()
                    }
                ),
                MenuActionItem(
                    label = "Private",
                    icon = Icons.Default.Security,
                    tint = colors.statusAccent,
                    iconBg = colors.statusAccent.copy(alpha = 0.12f),
                    onClick = {
                        onDismiss()
                        onOpenNewPrivateTab()
                    }
                ),
                MenuActionItem(
                    label = "Tor Window",
                    iconRes = R.drawable.ic_tor,
                    tint = colors.statusTor,
                    iconBg = colors.statusTor.copy(alpha = 0.14f),
                    badgeText = "Tor",
                    badgeColor = colors.statusTor,
                    onClick = {
                        onDismiss()
                        onOpenTorWindow()
                    }
                ),
                MenuActionItem(
                    label = "Shield",
                    icon = Icons.Default.Security,
                    tint = colors.statusSuccess,
                    iconBg = colors.statusSuccess.copy(alpha = 0.12f),
                    badgeText = if (blockedTrackersCount > 0) "$blockedTrackersCount" else null,
                    badgeColor = colors.statusSuccess,
                    onClick = {
                        onDismiss()
                        onOpenShield()
                    }
                ),
                // 2. Webpage Utilities (only on active websites)
                if (isWebpage) {
                    MenuActionItem(
                        label = "Reader",
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        onClick = {
                            onDismiss()
                            onOpenReaderMode()
                        }
                    )
                } else null,
                if (isWebpage) {
                    MenuActionItem(
                        label = "Save PDF",
                        icon = Icons.Default.Download,
                        onClick = {
                            onDismiss()
                            onSaveAsPdf()
                        }
                    )
                } else null,
                if (isWebpage && pwaEligibility != null) {
                    MenuActionItem(
                        label = "Add Home",
                        icon = Icons.Default.InstallMobile,
                        onClick = {
                            onDismiss()
                            onInstallPwa()
                        }
                    )
                } else null,
                // 3. Library & Data
                MenuActionItem(
                    label = "Bookmarks",
                    icon = Icons.Default.Star,
                    onClick = {
                        onDismiss()
                        onOpenBookmarks()
                    }
                ),
                MenuActionItem(
                    label = "History",
                    icon = Icons.Default.History,
                    onClick = {
                        onDismiss()
                        onOpenHistory()
                    }
                ),
                MenuActionItem(
                    label = "Downloads",
                    icon = Icons.Default.Download,
                    onClick = {
                        onDismiss()
                        onOpenDownloads()
                    }
                ),
                // 4. System & Security
                MenuActionItem(
                    label = "Settings",
                    icon = Icons.Default.Settings,
                    onClick = {
                        onDismiss()
                        onOpenSettings()
                    }
                ),
                MenuActionItem(
                    label = "About",
                    icon = Icons.Default.Info,
                    onClick = {
                        onDismiss()
                        onOpenAbout()
                    }
                ),
                MenuActionItem(
                    label = "Burn Session",
                    icon = Icons.Default.LocalFireDepartment,
                    isDestructive = true,
                    onClick = {
                        onDismiss()
                        onBurnData()
                    }
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .adaptiveContentWidth(540.dp)
                .padding(horizontal = 18.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Context Header Capsule
            if (isWebpage) {
                val host = remember(url) {
                    try {
                        android.net.Uri.parse(url).host?.removePrefix("www.") ?: url
                    } catch (e: Exception) {
                        url
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AzokleRadius.xl)
                        .background(colors.cardBg)
                        .border(1.dp, colors.border.copy(alpha = 0.45f), AzokleRadius.xl)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f).padding(end = 12.dp)
                    ) {
                        Text(
                            text = if (title.isNotBlank()) title else "Webpage",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = host,
                            fontSize = 11.sp,
                            color = colors.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Interactive Shield Status Capsule
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (blockedTrackersCount > 0) colors.statusSuccess.copy(alpha = 0.12f)
                                else colors.surfaceElevated
                            )
                            .border(
                                1.dp,
                                if (blockedTrackersCount > 0) colors.statusSuccess.copy(alpha = 0.30f)
                                else colors.border.copy(alpha = 0.5f),
                                CircleShape
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = false, radius = 24.dp, color = colors.statusSuccess),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onDismiss()
                                    onOpenShield()
                                }
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .semantics {
                                role = Role.Button
                                contentDescription = "Azokle Shield, $blockedTrackersCount trackers blocked"
                            }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = if (blockedTrackersCount > 0) colors.statusSuccess else colors.textMuted,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (blockedTrackersCount > 0) "$blockedTrackersCount blocked" else "Shield Active",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (blockedTrackersCount > 0) colors.statusSuccess else colors.textSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // 2. Primary Webpage Quick Actions (Reload, Share, Bookmark, Find, Desktop)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AzokleRadius.xl)
                    .background(colors.cardBg)
                    .border(1.dp, colors.border.copy(alpha = 0.45f), AzokleRadius.xl)
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reload
                MenuActionPill(
                    icon = Icons.Default.Refresh,
                    label = "Reload",
                    tint = colors.textPrimary,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onDismiss()
                        onReload()
                    }
                )

                // Share
                MenuActionPill(
                    icon = Icons.Default.Share,
                    label = "Share",
                    tint = if (isWebpage) colors.textPrimary else null,
                    enabled = isWebpage,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onDismiss()
                        onShare()
                    }
                )

                // Bookmark
                MenuActionPill(
                    icon = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    label = if (isBookmarked) "Saved" else "Bookmark",
                    tint = if (isBookmarked) colors.statusWarning else colors.textPrimary,
                    isActive = isBookmarked,
                    activeBorderColor = colors.statusWarning.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onToggleBookmark()
                        onDismiss()
                    }
                )

                // Find in Page
                MenuActionPill(
                    icon = Icons.Default.FindInPage,
                    label = "Find",
                    tint = if (isWebpage) colors.textPrimary else null,
                    enabled = isWebpage,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onDismiss()
                        onOpenFindInPage()
                    }
                )

                // Desktop Mode Toggle
                MenuActionPill(
                    icon = Icons.Default.Devices,
                    label = "Desktop",
                    tint = if (isDesktopMode) colors.actionPrimaryBg else colors.textPrimary,
                    isActive = isDesktopMode && isWebpage,
                    activeBorderColor = colors.actionPrimaryBg.copy(alpha = 0.5f),
                    enabled = isWebpage,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onToggleDesktopMode()
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Unified Browser Actions Grid (Seamless, clean, no cluttered category headers)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AzokleRadius.xl)
                    .background(colors.cardBg)
                    .border(1.dp, colors.border.copy(alpha = 0.45f), AzokleRadius.xl)
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                menuActions.chunked(4).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        rowItems.forEach { item ->
                            MenuActionPill(
                                label = item.label,
                                icon = item.icon,
                                iconRes = item.iconRes,
                                tint = item.tint,
                                iconBg = item.iconBg,
                                badgeText = item.badgeText,
                                badgeColor = item.badgeColor,
                                isDestructive = item.isDestructive,
                                enabled = item.enabled,
                                modifier = Modifier.weight(1f),
                                onClick = item.onClick
                            )
                        }
                        // Fill remaining spaces in the row to maintain vertical column alignment
                        repeat(4 - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Data representation of a browser menu action item
 */
private data class MenuActionItem(
    val label: String,
    val onClick: () -> Unit,
    val icon: ImageVector? = null,
    val iconRes: Int? = null,
    val tint: Color? = null,
    val iconBg: Color? = null,
    val badgeText: String? = null,
    val badgeColor: Color? = null,
    val isDestructive: Boolean = false,
    val enabled: Boolean = true
)

/**
 * Compact Circular Menu Action Pill matching the ergonomic Quick Action style:
 *  - 44dp circular icon container with subtle border & elevation
 *  - Support for Vector icons, Drawable resource icons, badges, active states & destructive accents
 *  - Single-line clear typography label underneath
 *  - Haptic feedback and subtle ripple on touch
 */
@Composable
private fun MenuActionPill(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconRes: Int? = null,
    tint: Color? = null,
    iconBg: Color? = null,
    enabled: Boolean = true,
    isActive: Boolean = false,
    activeBorderColor: Color? = null,
    badgeText: String? = null,
    badgeColor: Color? = null,
    isDestructive: Boolean = false
) {
    val colors = AzokleTheme.colors
    val haptic = LocalHapticFeedback.current

    val finalIconTint = when {
        !enabled -> colors.textMuted.copy(alpha = 0.35f)
        isDestructive -> colors.statusDestructive
        isActive -> activeBorderColor ?: colors.actionPrimaryBg
        tint != null -> tint
        else -> colors.textPrimary
    }

    val finalPillBg = when {
        !enabled -> colors.surfaceElevated.copy(alpha = 0.4f)
        isDestructive -> colors.statusDestructive.copy(alpha = 0.12f)
        isActive -> (activeBorderColor ?: colors.actionPrimaryBg).copy(alpha = 0.14f)
        iconBg != null -> iconBg
        else -> colors.surfaceElevated
    }

    val finalBorderColor = when {
        !enabled -> colors.border.copy(alpha = 0.2f)
        isDestructive -> colors.statusDestructive.copy(alpha = 0.35f)
        isActive -> activeBorderColor ?: colors.actionPrimaryBg
        iconBg != null -> (tint ?: colors.textPrimary).copy(alpha = 0.25f)
        else -> colors.border.copy(alpha = 0.4f)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(AzokleRadius.md)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(
                    bounded = false,
                    radius = 24.dp,
                    color = if (isDestructive) colors.statusDestructive else colors.textPrimary
                ),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            )
            .padding(horizontal = 2.dp, vertical = 4.dp)
            .semantics {
                role = Role.Button
                contentDescription = label
            }
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(finalPillBg)
                .border(
                    width = if (isActive) 1.5.dp else 0.5.dp,
                    color = finalBorderColor,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (iconRes != null) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = finalIconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (!badgeText.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 1.dp, end = 1.dp)
                        .clip(CircleShape)
                        .background(badgeColor ?: colors.actionPrimaryBg)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
            color = when {
                !enabled -> colors.textMuted.copy(alpha = 0.35f)
                isDestructive -> colors.statusDestructive
                isActive -> colors.textPrimary
                else -> colors.textSecondary
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
