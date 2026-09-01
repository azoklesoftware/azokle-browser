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
    onSecurityLockClick: () -> Unit = {},
    onRefreshClick: () -> Unit = {},
    onShieldClick: () -> Unit = {},
    onReaderModeClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    onUrlBarClick: () -> Unit
) {
    val colors = AzokleTheme.colors
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
        isInternal || cleanHost.equals("about:blank", ignoreCase = true) || cleanHost.isBlank() -> {
            if (title.isNotBlank() && !title.equals("about:blank", ignoreCase = true)) title else "Search or type URL"
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
            // Center Address Capsule (Clean, Minimalist Apple / Brave Style)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(CircleShape)
                    .background(colors.surface)
                    .border(1.dp, colors.border.copy(alpha = 0.6f), CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = colors.textPrimary.copy(alpha = 0.08f)),
                        onClick = onUrlBarClick
                    )
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // SSL Padlock / Certificate Viewer (Clean, Zero Glow Bubble)
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
                    Icon(
                        imageVector = sslIcon,
                        contentDescription = null,
                        tint = sslTint,
                        modifier = Modifier.size(15.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

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

                // Shield Live Counter (Clean, Zero Background Glow Circle)
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
                            contentDescription = if (isShieldEnabled) "Privacy Shield Active, $blockedCount trackers blocked" else "Privacy Shield Disabled"
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
                        if (blockedCount > 0) {
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

            if (showExtendedActions) {
                // Settings Action Trigger (Clean 40dp Touch Target)
                Box(
                    modifier = Modifier
                        .sizeIn(minWidth = 40.dp, minHeight = 44.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = false, radius = 18.dp, color = colors.textPrimary),
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
                        tint = colors.textSecondary,
                        modifier = Modifier.size(20.dp)
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
