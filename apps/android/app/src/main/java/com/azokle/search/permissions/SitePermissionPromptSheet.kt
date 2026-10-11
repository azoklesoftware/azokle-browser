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

package com.azokle.search.permissions

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.azokle.search.common.theme.AzokleRadius
import com.azokle.search.common.theme.AzokleTheme

/**
 * Minimalist, elegant, and flat Site Permission Request Popup Dialog.
 * Pure minimalist aesthetic: clean typography, flat surfaces, zero gradients or loud colors.
 */
@Composable
fun SitePermissionPromptSheet(
    request: SitePermissionRequest,
    onAllowAlways: () -> Unit,
    onAllowOnce: () -> Unit,
    onDeny: (remember: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    SitePermissionPromptPopup(
        request = request,
        onAllowAlways = onAllowAlways,
        onAllowOnce = onAllowOnce,
        onDeny = onDeny,
        onDismiss = onDismiss
    )
}

/**
 * Ultra-Minimalist Permission Popup Dialog.
 */
@Composable
fun SitePermissionPromptPopup(
    request: SitePermissionRequest,
    onAllowAlways: () -> Unit,
    onAllowOnce: () -> Unit,
    onDeny: (remember: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = AzokleTheme.colors
    var rememberDecision by remember { mutableStateOf(true) }

    val cleanHost = request.host.removePrefix("www.").ifBlank { "This website" }
    val isHttps = request.uri.startsWith("https://", ignoreCase = true)
    val isTor = request.isPrivate || request.uri.contains(".onion", ignoreCase = true)

    val primaryPerm = request.requestedTypes.firstOrNull() ?: SitePermissionType.AUTOPLAY
    val isAutoplayOnly = request.requestedTypes.size == 1 && primaryPerm == SitePermissionType.AUTOPLAY

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = true,
            dismissOnBackPress = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 360.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.cardBg)
                    .border(1.dp, colors.borderSubtle, RoundedCornerShape(20.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Top Header Row: Flat Icon Badge + Domain & Security + Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Flat Minimalist Icon Badge
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceElevated)
                                .border(1.dp, colors.borderSubtle, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = primaryPerm.icon,
                                contentDescription = null,
                                tint = colors.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = cleanHost,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.semantics { heading() }
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = when {
                                        isTor -> Icons.Default.Security
                                        isHttps -> Icons.Default.Lock
                                        else -> Icons.Default.Security
                                    },
                                    contentDescription = null,
                                    tint = colors.textMuted,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = when {
                                        isTor -> "Tor Isolated"
                                        isHttps -> "HTTPS Secure"
                                        else -> "Standard Web"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }

                    // Close Button
                    Box(
                        modifier = Modifier
                            .sizeIn(minWidth = 32.dp, minHeight = 32.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = false, radius = 16.dp, color = colors.textSecondary),
                                onClick = onDismiss
                            )
                            .semantics {
                                role = Role.Button
                                contentDescription = "Close"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceElevated)
                                .border(0.8.dp, colors.borderSubtle, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = colors.textSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Compact Prompt Description
                Text(
                    text = if (isAutoplayOnly) {
                        "Wants permission to play audio/video automatically"
                    } else if (request.requestedTypes.size == 1) {
                        "Wants to access your ${primaryPerm.displayName.lowercase()}"
                    } else {
                        "Wants to access device capabilities:"
                    },
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )

                // 3. Compact Requested Capabilities List (if multiple)
                if (request.requestedTypes.size > 1) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(AzokleRadius.md)
                            .background(colors.surfaceElevated)
                            .border(0.8.dp, colors.borderSubtle, AzokleRadius.md)
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        request.requestedTypes.forEachIndexed { index, perm ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = perm.icon,
                                    contentDescription = null,
                                    tint = colors.textPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = perm.displayName,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textPrimary
                                )
                            }
                            if (index < request.requestedTypes.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    color = colors.borderSubtle
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 4. Flat Minimalist Remember Choice Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AzokleRadius.sm)
                        .background(colors.surfaceElevated)
                        .border(0.8.dp, colors.borderSubtle, AzokleRadius.sm)
                        .clickable { rememberDecision = !rememberDecision }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Remember for this site",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Normal,
                        color = colors.textPrimary
                    )

                    Switch(
                        checked = rememberDecision,
                        onCheckedChange = { rememberDecision = it },
                        modifier = Modifier.scale(0.8f),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.actionPrimaryText,
                            checkedTrackColor = colors.actionPrimaryBg,
                            checkedBorderColor = Color.Transparent,
                            uncheckedThumbColor = colors.textMuted,
                            uncheckedTrackColor = colors.background,
                            uncheckedBorderColor = colors.borderSubtle
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 5. Flat Action Buttons Stack
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Primary Action Button (Allow)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .clip(AzokleRadius.md)
                            .background(colors.actionPrimaryBg)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = colors.actionPrimaryText.copy(alpha = 0.2f)),
                                onClick = {
                                    if (rememberDecision) {
                                        onAllowAlways()
                                    } else {
                                        onAllowOnce()
                                    }
                                }
                            )
                            .semantics {
                                role = Role.Button
                                contentDescription = if (isAutoplayOnly) "Allow autoplay" else if (rememberDecision) "Allow while visiting" else "Allow"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isAutoplayOnly) "Allow Autoplay" else if (rememberDecision) "Allow While Visiting" else "Allow",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.actionPrimaryText
                        )
                    }

                    // Secondary Action Button (Allow This Time Only - Ephemeral)
                    if (!isAutoplayOnly) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .clip(AzokleRadius.md)
                                .background(colors.surfaceElevated)
                                .border(0.8.dp, colors.borderSubtle, AzokleRadius.md)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true, color = colors.textPrimary.copy(alpha = 0.1f)),
                                    onClick = onAllowOnce
                                )
                            .semantics {
                                role = Role.Button
                                contentDescription = "Allow this time only"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Allow This Time Only",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textPrimary
                        )
                    }
                }

                    // Tertiary Action Button (Don't Allow)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .clip(AzokleRadius.md)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = colors.textSecondary.copy(alpha = 0.15f)),
                                onClick = { onDeny(rememberDecision) }
                            )
                            .semantics {
                                role = Role.Button
                                contentDescription = if (isAutoplayOnly) "Block autoplay" else "Don't allow"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isAutoplayOnly) "Block Autoplay" else "Don't Allow",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

