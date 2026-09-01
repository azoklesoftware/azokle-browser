package com.azokle.search.browser.ui

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azokle.search.ui.theme.AzokleRadius
import com.azokle.search.ui.theme.AzokleTheme

@Composable
fun BrowserOmnibar(
    url: String,
    title: String,
    blockedCount: Int,
    isShieldEnabled: Boolean,
    isLoading: Boolean,
    progress: Float,
    onSecurityLockClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onShieldClick: () -> Unit,
    onReaderModeClick: () -> Unit,
    onSettingsClick: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    onUrlBarClick: () -> Unit
) {
    val colors = AzokleTheme.colors
    val haptic = LocalHapticFeedback.current

    val uri = try {
        Uri.parse(url)
    } catch (e: Exception) {
        null
    }

    val isHttp = url.startsWith("http://", ignoreCase = true)
    val isInternal = url.startsWith("about:", ignoreCase = true) || url.isBlank() || url == "about:blank"

    val rawHost = uri?.host ?: url
    val isOnion = rawHost.endsWith(".onion", ignoreCase = true) || url.contains(".onion", ignoreCase = true)

    // Parse domain parts for Anti-Spoofing Visual Hierarchy
    val displayHost = when {
        isInternal || rawHost.equals("about:blank", ignoreCase = true) -> {
            if (title.isNotBlank() && !title.equals("about:blank", ignoreCase = true)) title else "Search or type URL"
        }
        else -> rawHost
    }
    val path = if (!isInternal && !rawHost.equals("about:blank", ignoreCase = true)) (uri?.path ?: "") else ""

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
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.cardBg)
            .statusBarsPadding()
    ) {
        val windowSize = com.azokle.search.ui.theme.rememberWindowSize()
        val showExtendedActions = windowSize.widthDp >= 360

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .padding(start = if (windowSize.widthDp < 360) 6.dp else 12.dp, end = if (windowSize.widthDp < 360) 4.dp else 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Center Address Capsule (Brave / DDG Style)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(AzokleRadius.full)
                    .background(colors.surface)
                    .border(1.dp, colors.border, AzokleRadius.full)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = colors.textPrimary.copy(alpha = 0.1f)),
                        onClick = onUrlBarClick
                    )
                    .padding(start = 4.dp, end = 4.dp)
                    .semantics {
                        role = Role.Button
                        contentDescription = "Address bar, current address: $displayHost"
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                // SSL Padlock / Certificate Viewer Button (Accessible 48dp Touch Delegate)
                Box(
                    modifier = Modifier
                        .sizeIn(minWidth = 40.dp, minHeight = 44.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = false, radius = 18.dp, color = sslTint),
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
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(sslTint.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = sslIcon,
                            contentDescription = null,
                            tint = sslTint,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Domain & Path with Anti-Spoofing Hierarchy
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayHost,
                        color = colors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
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

                // Shield Live Counter Badge (Accessible Touch Target)
                Box(
                    modifier = Modifier
                        .clip(AzokleRadius.full)
                        .background(if (isShieldEnabled && blockedCount > 0) colors.statusSuccess.copy(alpha = 0.15f) else colors.surfaceElevated)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = colors.statusSuccess),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onShieldClick()
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                        .semantics {
                            role = Role.Button
                            contentDescription = if (isShieldEnabled) "Privacy Shield Active, $blockedCount trackers blocked" else "Privacy Shield Disabled"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (isShieldEnabled) (if (blockedCount > 0) colors.statusSuccess else colors.textPrimary) else colors.textMuted,
                            modifier = Modifier.size(13.dp)
                        )
                        if (blockedCount > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$blockedCount",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (isShieldEnabled) colors.statusSuccess else colors.textMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(2.dp))

                // Refresh / Stop Action Icon (WCAG 48dp Touch Target)
                Box(
                    modifier = Modifier
                        .sizeIn(minWidth = 40.dp, minHeight = 44.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = false, radius = 18.dp, color = colors.textPrimary),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onRefreshClick()
                            }
                        )
                        .semantics {
                            role = Role.Button
                            contentDescription = if (isLoading) "Stop loading" else "Reload page"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isLoading) Icons.Default.Close else Icons.Default.Refresh,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (showExtendedActions) {
                // Reader Mode Icon Button (48dp Touch Target)
                Box(
                    modifier = Modifier
                        .sizeIn(minWidth = 44.dp, minHeight = 48.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = false, radius = 20.dp, color = colors.textPrimary),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onReaderModeClick()
                            }
                        )
                        .semantics {
                            role = Role.Button
                            contentDescription = "Reader Mode"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(21.dp)
                    )
                }

                // Settings Action Trigger (48dp Touch Target)
                Box(
                    modifier = Modifier
                        .sizeIn(minWidth = 44.dp, minHeight = 48.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = false, radius = 20.dp, color = colors.textPrimary),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSettingsClick()
                            }
                        )
                        .semantics {
                            role = Role.Button
                            contentDescription = "Settings"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = colors.textPrimary,
                        modifier = Modifier.size(21.dp)
                    )
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
