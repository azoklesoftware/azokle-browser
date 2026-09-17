package com.azokle.search.engine.ui

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import com.azokle.search.R
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azokle.search.common.theme.AzokleRadius
import com.azokle.search.common.theme.AzokleTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BrowserOmnibar(
    url: String,
    title: String,
    blockedCount: Int,
    isShieldEnabled: Boolean,
    isLoading: Boolean,
    progress: Float,
    onSecurityLockClick: () -> Unit = {},
    onRefreshClick: () -> Unit = {},
    onShieldClick: () -> Unit = {},
    onReaderModeClick: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    onQrClick: () -> Unit = {},
    onVoiceClick: () -> Unit = {},
    onUrlBarClick: () -> Unit,
    showOverflowMenu: Boolean = false,
    openTabsCount: Int = 1,
    onHomeClick: () -> Unit = {},
    onDiscoverClick: () -> Unit = {},
    onTabsClick: () -> Unit = {},
    onBurnDataClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    includeStatusBarPadding: Boolean = true,
    selectedSearchEngine: String = "azokle",
    onSelectSearchEngine: (String) -> Unit = {},
    omnibarCapsuleModifier: Modifier = Modifier,
    searchEngineModifier: Modifier = Modifier
) {
    val colors = AzokleTheme.colors
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val uri = remember(url) {
        try {
            Uri.parse(url)
        } catch (e: Exception) {
            null
        }
    }

    val isHttp = url.startsWith("http://", ignoreCase = true)
    val isInternal = url.startsWith("about:", ignoreCase = true) || url.isBlank() || url == "about:blank"

    // RFC 3986 Anti-Spoofing Host Extraction:
    // Strips embedded credentials/user-info prefixes (e.g. https://brand.com@evil.com -> evil.com)
    val cleanHost = remember(url, uri) {
        try {
            val extracted = uri?.host?.removePrefix("www.")?.trim()
            if (!extracted.isNullOrBlank()) {
                extracted
            } else {
                url.removePrefix("https://")
                    .removePrefix("http://")
                    .substringBefore("/")
                    .substringAfterLast("@")
                    .removePrefix("www.")
                    .trim()
            }
        } catch (e: Exception) {
            url
        }
    }

    val isOnion = cleanHost.endsWith(".onion", ignoreCase = true) || url.contains(".onion", ignoreCase = true)

    // Parse domain parts for Anti-Spoofing Visual Hierarchy
    val displayHost = when {
        isInternal || cleanHost.equals("about:blank", ignoreCase = true) || cleanHost.equals("about:tor", ignoreCase = true) || cleanHost.isBlank() -> {
            "Search or type URL"
        }
        else -> cleanHost
    }
    val path = if (!isInternal && !cleanHost.equals("about:blank", ignoreCase = true)) (uri?.path ?: "") else ""

    val sslIcon = when {
        isOnion -> Icons.Default.Shield
        isHttp -> Icons.Default.Warning
        else -> Icons.Default.Lock
    }
    val sslTint = when {
        isOnion -> colors.statusTor
        isHttp -> colors.statusDestructive
        else -> colors.statusSuccess
    }
    val sslDesc = when {
        isOnion -> "Tor Onion Circuit Encrypted"
        isHttp -> "Connection is not secure"
        else -> "Connection is secure"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.cardBg)
            .then(if (includeStatusBarPadding) Modifier.statusBarsPadding() else Modifier)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Center Address Capsule (Clean, Minimalist Apple / Brave Style)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .then(omnibarCapsuleModifier)
                    .clip(AzokleRadius.md)
                    .background(colors.surfaceElevated)
                    .border(1.dp, colors.border, AzokleRadius.md)
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = colors.textPrimary.copy(alpha = 0.08f)),
                        onClick = onUrlBarClick,
                        onLongClick = {
                            if (url.isNotBlank() && !isInternal) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                val clipData = android.content.ClipData.newPlainText("URL", url)
                                clipboard?.setPrimaryClip(clipData)
                                com.azokle.search.common.components.ToastManager.showCopy("URL copied to clipboard")
                            }
                        }
                    )
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // SSL Padlock / Certificate Viewer (Only shown for external websites)
                if (!isInternal && cleanHost.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .sizeIn(minWidth = 32.dp, minHeight = 36.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = false, radius = 16.dp, color = sslTint),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSecurityLockClick()
                                }
                            )
                            .semantics {
                                role = Role.Button
                                contentDescription = sslDesc
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isOnion) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_tor),
                                contentDescription = sslDesc,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Icon(
                                imageVector = sslIcon,
                                contentDescription = null,
                                tint = sslTint,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))
                } else {
                    // Search Engine Selector Icon on Home Page (Azokle, DuckDuckGo, Brave, Startpage)
                    com.azokle.search.search.SearchEngineIconDropdown(
                        selectedEngineId = selectedSearchEngine,
                        onEngineSelected = onSelectSearchEngine,
                        iconSize = 16.dp,
                        showDropdownArrow = true,
                        modifier = Modifier
                            .padding(start = 0.dp, end = 4.dp)
                            .then(searchEngineModifier)
                    )
                }

                // Domain & Path with Anti-Spoofing Hierarchy
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayHost,
                        color = colors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (path.isNotEmpty() && path != "/") {
                        Text(
                            text = path,
                            color = colors.textMuted,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Shield Live Counter (Clean, Zero Background Glow Circle - Only on active web pages)
                if (!isInternal && cleanHost.isNotBlank()) {
                    val isShieldActive = isShieldEnabled && blockedCount > 0
                    val shieldTint = when {
                        isShieldActive -> colors.statusSuccess
                        isShieldEnabled -> colors.textPrimary
                        else -> colors.textMuted
                    }

                    Box(
                        modifier = Modifier
                            .sizeIn(minWidth = 32.dp, minHeight = 36.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = false, radius = 16.dp, color = shieldTint),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onShieldClick()
                                }
                            )
                            .padding(horizontal = 4.dp)
                            .semantics {
                                role = Role.Button
                                contentDescription = if (isShieldActive) "Privacy Shield Active, $blockedCount trackers blocked" else if (isShieldEnabled) "Privacy Shield Protected" else "Privacy Shield Disabled"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = shieldTint,
                                modifier = Modifier.size(16.5.dp)
                            )
                            if (isShieldActive && blockedCount > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$blockedCount",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = shieldTint
                                )
                            }
                        }
                    }
                }

                // QR Scanner & Voice Search Triggers in Omnibar (when on home/blank tab)
                if (isInternal || cleanHost.isBlank()) {
                    // QR Code Scanner Trigger (Before Mic)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = false, radius = 18.dp, color = colors.statusAccent),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onQrClick()
                                }
                            )
                            .semantics {
                                role = Role.Button
                                contentDescription = "Scan QR code"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(2.dp))

                    // Voice Search Trigger
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = false, radius = 18.dp, color = colors.statusAccent),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onVoiceClick()
                                }
                            )
                            .semantics {
                                role = Role.Button
                                contentDescription = "Voice search"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Responsive Tabs, Fire Burn & 3-Dot Overflow Menu (Shown when Bottom Bar is hidden in Landscape / Tablet mode)
            if (showOverflowMenu) {
                // 1. Dedicated Tabs Counter Button in Omnibar (Before Fire Burn & 3-Dot Menu)
                Spacer(modifier = Modifier.width(6.dp))

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = false, radius = 21.dp, color = colors.actionPrimaryBg),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onTabsClick()
                            }
                        )
                        .semantics {
                            role = Role.Button
                            contentDescription = "Active Tabs, $openTabsCount open"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(colors.surfaceElevated)
                            .border(1.dp, colors.border, RoundedCornerShape(9.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (openTabsCount > 99) "99+" else "$openTabsCount",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = colors.textPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // 2. Dedicated Fire Burn Button in Omnibar (After Tabs Button)
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceElevated)
                        .border(1.dp, colors.border, CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = false, radius = 21.dp, color = colors.statusDestructive.copy(alpha = 0.25f)),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onBurnDataClick()
                            }
                        )
                        .semantics {
                            role = Role.Button
                            contentDescription = "Clear all tabs and browsing data"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = colors.statusDestructive,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // 3. Responsive 3-Dot Overflow Menu (Home, Discover, Menu)
                var overflowMenuExpanded by remember { mutableStateOf(false) }

                Box {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceElevated)
                            .border(1.dp, colors.border, CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = false, radius = 21.dp, color = colors.actionPrimaryBg),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    overflowMenuExpanded = true
                                }
                            )
                            .semantics {
                                role = Role.Button
                                contentDescription = "Navigation Menu"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = null,
                            tint = colors.textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Luxury Glassmorphic Dropdown Menu
                    DropdownMenu(
                        expanded = overflowMenuExpanded,
                        onDismissRequest = { overflowMenuExpanded = false },
                        shape = RoundedCornerShape(18.dp),
                        containerColor = colors.cardBg,
                        tonalElevation = 6.dp,
                        shadowElevation = 16.dp,
                        border = BorderStroke(1.dp, colors.border),
                        modifier = Modifier
                            .widthIn(min = 210.dp, max = 240.dp)
                            .padding(vertical = 4.dp)
                    ) {
                        // 1. Home
                        OmnibarDropdownActionItem(
                            title = "Home",
                            icon = Icons.Default.Home,
                            iconBgColor = colors.surfaceElevated,
                            iconTint = colors.textPrimary,
                            onClick = {
                                overflowMenuExpanded = false
                                onHomeClick()
                            }
                        )

                        // 2. Discover
                        OmnibarDropdownActionItem(
                            title = "Discover",
                            icon = Icons.Default.Explore,
                            iconBgColor = colors.surfaceElevated,
                            iconTint = colors.textPrimary,
                            onClick = {
                                overflowMenuExpanded = false
                                onDiscoverClick()
                            }
                        )

                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = colors.border.copy(alpha = 0.6f),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )

                        // 3. Browser Menu Sheet
                        OmnibarDropdownActionItem(
                            title = "Menu",
                            icon = Icons.Default.Menu,
                            iconBgColor = colors.surfaceElevated,
                            iconTint = colors.textPrimary,
                            onClick = {
                                overflowMenuExpanded = false
                                onMenuClick()
                            }
                        )
                    }
                }
            }
        }

        // Smooth Progress Indicator
        val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
            targetValue = progress,
            animationSpec = androidx.compose.animation.core.tween(durationMillis = 180, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            label = "omnibarProgress"
        )

        if (isLoading && animatedProgress < 1.0f) {
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp),
                color = colors.textPrimary,
                trackColor = colors.surface
            )
        } else {
            HorizontalDivider(
                thickness = 0.5.dp,
                color = colors.border
            )
        }
    }
}

/**
 * Premium Luxury Action Item for Responsive Omnibar Dropdown Menu:
 * Features a frosted squircle icon badge, crisp typography, live badge counter, and tactile haptic feedback.
 */
@Composable
private fun OmnibarDropdownActionItem(
    title: String,
    icon: ImageVector,
    iconTint: Color = AzokleTheme.colors.textPrimary,
    iconBgColor: Color = AzokleTheme.colors.surface,
    textColor: Color = AzokleTheme.colors.textPrimary,
    badgeCount: Int? = null,
    onClick: () -> Unit
) {
    val colors = AzokleTheme.colors
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = colors.actionPrimaryBg.copy(alpha = 0.12f)),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Frosted Squircle Icon Badge
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBgColor)
                .border(0.5.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Action Label
        Text(
            text = title,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        // Pill Badge (for active tabs count)
        if (badgeCount != null && badgeCount > 0) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.actionPrimaryBg)
                    .padding(horizontal = 7.dp, vertical = 2.5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (badgeCount > 99) "99+" else "$badgeCount",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = colors.actionPrimaryText
                )
            }
        }
    }
}
