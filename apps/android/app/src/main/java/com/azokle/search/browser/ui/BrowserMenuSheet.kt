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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserMenuSheet(
    url: String,
    title: String,
    isDesktopMode: Boolean,
    isBookmarked: Boolean,
    blockedTrackersCount: Int,
    onReload: () -> Unit,
    onToggleBookmark: () -> Unit,
    onShare: () -> Unit,
    onToggleDesktopMode: () -> Unit,
    onOpenReaderMode: () -> Unit,
    onOpenFindInPage: () -> Unit,
    onOpenDownloads: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onOpenShield: () -> Unit,
    onOpenTorWindow: () -> Unit = {},
    onEnterPictureInPicture: (() -> Unit)? = null,
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
            // 1. Top Quick Icons Bar (Reload, Bookmark, Share, Shield)
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
                // Reload
                QuickMenuIconButton(
                    icon = Icons.Default.Refresh,
                    label = "Reload",
                    tint = colors.textPrimary,
                    onClick = {
                        onReload()
                        onDismiss()
                    }
                )

                // Bookmark
                QuickMenuIconButton(
                    icon = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    label = if (isBookmarked) "Saved" else "Bookmark",
                    tint = if (isBookmarked) colors.statusWarning else colors.textPrimary,
                    onClick = {
                        onToggleBookmark()
                        onDismiss()
                    }
                )

                // Share
                QuickMenuIconButton(
                    icon = Icons.Default.Share,
                    label = "Share",
                    tint = colors.textPrimary,
                    onClick = {
                        onShare()
                        onDismiss()
                    }
                )

                // Shield
                QuickMenuIconButton(
                    icon = Icons.Default.Shield,
                    label = "Shield ($blockedTrackersCount)",
                    tint = colors.statusSuccess,
                    onClick = {
                        onDismiss()
                        onOpenShield()
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Action Items List
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AzokleRadius.lg)
                    .background(colors.cardBg)
                    .border(1.dp, colors.border, AzokleRadius.lg)
            ) {
                // New Tor Window
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

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.statusTor.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "TOR",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.statusTor
                        )
                    }
                }

                if (onEnterPictureInPicture != null) {
                    Box(modifier = Modifier.fillMaxWidth().height(0.6.dp).background(colors.border))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = colors.textPrimary.copy(alpha = 0.08f)),
                                onClick = {
                                    onEnterPictureInPicture()
                                    onDismiss()
                                }
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .semantics {
                                role = Role.Button
                                contentDescription = "Enter Picture-in-Picture mode"
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(id = com.azokle.search.R.drawable.ic_pip_enter),
                                contentDescription = null,
                                tint = colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = "Picture-in-Picture",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textPrimary
                            )
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().height(0.6.dp).background(colors.border))

                // Desktop Site Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = colors.textPrimary.copy(alpha = 0.08f)),
                            onClick = onToggleDesktopMode
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .semantics {
                            role = Role.Button
                            contentDescription = "Toggle desktop site mode"
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Devices,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp)
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
                            checkedTrackColor = colors.actionPrimaryBg,
                            uncheckedThumbColor = colors.textMuted,
                            uncheckedTrackColor = colors.surface
                        )
                    )
                }

                // Install PWA or Add to Home Screen
                if (pwaEligibility != null && pwaEligibility.isAddShortcutEligible) {
                    Box(modifier = Modifier.fillMaxWidth().height(0.6.dp).background(colors.border))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = colors.actionPrimaryBg.copy(alpha = 0.15f)),
                                onClick = {
                                    onDismiss()
                                    onInstallPwa()
                                }
                            )
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .semantics {
                                role = Role.Button
                                contentDescription = if (pwaEligibility.isInstallablePwa) "Install ${pwaEligibility.title} app" else "Add webpage to home screen"
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (pwaEligibility.isInstallablePwa) Icons.Default.InstallMobile else Icons.Default.Download,
                                contentDescription = null,
                                tint = if (pwaEligibility.isInstallablePwa) colors.actionPrimaryBg else colors.textSecondary,
                                modifier = Modifier.size(19.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = if (pwaEligibility.isInstallablePwa) "Install ${pwaEligibility.title.take(18)}" else "Add to Home Screen",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = if (pwaEligibility.isInstallablePwa) "Standalone app • Fast launcher access" else "Pin webpage shortcut to home screen",
                                    fontSize = 11.sp,
                                    color = colors.textMuted
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (pwaEligibility.isInstallablePwa) colors.actionPrimaryBg.copy(alpha = 0.15f) else colors.surface)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (pwaEligibility.isInstallablePwa) "INSTALL" else "SHORTCUT",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (pwaEligibility.isInstallablePwa) colors.actionPrimaryBg else colors.textMuted
                            )
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().height(0.6.dp).background(colors.border))

                // Reader Mode
                MenuRowItem(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    title = "Distraction-Free Reader Mode",
                    onClick = {
                        onDismiss()
                        onOpenReaderMode()
                    }
                )

                Box(modifier = Modifier.fillMaxWidth().height(0.6.dp).background(colors.border))

                // Find in Page
                MenuRowItem(
                    icon = Icons.Default.FindInPage,
                    title = "Find in Webpage",
                    onClick = {
                        onDismiss()
                        onOpenFindInPage()
                    }
                )

                Box(modifier = Modifier.fillMaxWidth().height(0.6.dp).background(colors.border))

                // Downloads
                MenuRowItem(
                    icon = Icons.Default.Download,
                    title = "Downloads",
                    onClick = {
                        onDismiss()
                        onOpenDownloads()
                    }
                )

                Box(modifier = Modifier.fillMaxWidth().height(0.6.dp).background(colors.border))

                // History
                MenuRowItem(
                    icon = Icons.Default.History,
                    title = "History",
                    onClick = {
                        onDismiss()
                        onOpenHistory()
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Incinerator Burn Button
            val burnShape = AzokleRadius.lg
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(burnShape)
                    .background(colors.statusDestructive.copy(alpha = 0.12f))
                    .border(1.dp, colors.statusDestructive.copy(alpha = 0.3f), burnShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, color = colors.statusDestructive.copy(alpha = 0.2f)),
                        onClick = {
                            onBurnData()
                            onDismiss()
                        }
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .semantics {
                        role = Role.Button
                        contentDescription = "Burn browsing data"
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = colors.statusDestructive,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Burn Browsing Data",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.statusDestructive
                        )
                        Text(
                            text = "Close tabs, purge cookies & clear cache",
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
    tint: Color = AzokleTheme.colors.textPrimary,
    onClick: () -> Unit
) {
    val colors = AzokleTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .sizeIn(minWidth = 56.dp, minHeight = 56.dp)
            .clip(AzokleRadius.md)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = colors.textPrimary.copy(alpha = 0.1f)),
                onClick = onClick
            )
            .padding(vertical = 4.dp, horizontal = 4.dp)
            .semantics {
                role = Role.Button
                contentDescription = label
            }
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(colors.surface)
                .border(1.dp, colors.border, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            color = colors.textSecondary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun MenuRowItem(
    icon: ImageVector,
    title: String,
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
            .padding(horizontal = 16.dp, vertical = 14.dp)
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
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textPrimary
        )
    }
}
