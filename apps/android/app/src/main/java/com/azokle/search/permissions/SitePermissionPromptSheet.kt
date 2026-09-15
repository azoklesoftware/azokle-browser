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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.azokle.search.common.theme.AzokleRadius
import com.azokle.search.common.theme.AzokleTheme
import com.azokle.search.common.theme.adaptiveContentWidth

/**
 * Premium, clean, and smooth Site Permission Request Bottom Sheet.
 * Displays domain identity, requested sensor/hardware capabilities, and explicit scope controls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SitePermissionPromptSheet(
    request: SitePermissionRequest,
    onAllowAlways: () -> Unit,
    onAllowOnce: () -> Unit,
    onDeny: (remember: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = AzokleTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var rememberDecision by remember { mutableStateOf(true) }

    val cleanHost = request.host.removePrefix("www.").ifBlank { "This website" }
    val isHttps = request.uri.startsWith("https://", ignoreCase = true)
    val isTor = request.isPrivate || request.uri.contains(".onion", ignoreCase = true)

    val primaryPerm = request.requestedTypes.firstOrNull() ?: SitePermissionType.AUTOPLAY
    val isAutoplayOnly = request.requestedTypes.size == 1 && primaryPerm == SitePermissionType.AUTOPLAY

    val heroAccentColor = when (primaryPerm) {
        SitePermissionType.CAMERA -> Color(0xFF38BDF8) // Electric Cyan
        SitePermissionType.MICROPHONE -> Color(0xFFA855F7) // Purple
        SitePermissionType.GEOLOCATION -> Color(0xFFF59E0B) // Amber
        SitePermissionType.NOTIFICATIONS -> Color(0xFF06B6D4) // Teal
        SitePermissionType.AUTOPLAY -> colors.statusSuccess
        else -> colors.statusSuccess
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.background,
        contentColor = colors.textPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(colors.border.copy(alpha = 0.8f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .adaptiveContentWidth(560.dp)
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top Close Button Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .sizeIn(minWidth = 40.dp, minHeight = 40.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = false, radius = 18.dp, color = colors.textSecondary),
                            onClick = onDismiss
                        )
                        .semantics {
                            role = Role.Button
                            contentDescription = "Close permission dialog"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceElevated)
                            .border(1.dp, colors.borderSubtle, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // 2. Hero Icon Badge Emblem
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(heroAccentColor.copy(alpha = 0.14f))
                    .border(1.5.dp, heroAccentColor.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = primaryPerm.icon,
                    contentDescription = null,
                    tint = heroAccentColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Website Origin & Permission Title
            Text(
                text = cleanHost,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Security Origin Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.surfaceElevated)
                    .border(0.8.dp, colors.borderSubtle, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Icon(
                    imageVector = when {
                        isTor -> Icons.Default.Security
                        isHttps -> Icons.Default.Lock
                        else -> Icons.Default.Security
                    },
                    contentDescription = null,
                    tint = if (isHttps) colors.statusSuccess else colors.textMuted,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = when {
                        isTor -> "Tor Onion Isolated"
                        isHttps -> "Secure HTTPS Origin"
                        else -> "Standard Web Origin"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isHttps) colors.statusSuccess else colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isAutoplayOnly) {
                    "Wants permission to automatically play media"
                } else {
                    "Requests access to the following device capabilities:"
                },
                fontSize = 13.sp,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Requested Capabilities List Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AzokleRadius.lg)
                    .background(colors.surfaceElevated)
                    .border(1.dp, colors.borderSubtle, AzokleRadius.lg)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                request.requestedTypes.forEachIndexed { index, perm ->
                    val permColor = when (perm) {
                        SitePermissionType.CAMERA -> Color(0xFF38BDF8)
                        SitePermissionType.MICROPHONE -> Color(0xFFA855F7)
                        SitePermissionType.GEOLOCATION -> Color(0xFFF59E0B)
                        SitePermissionType.NOTIFICATIONS -> Color(0xFF06B6D4)
                        SitePermissionType.AUTOPLAY -> colors.statusSuccess
                        else -> colors.statusSuccess
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(AzokleRadius.md)
                                .background(permColor.copy(alpha = 0.14f))
                                .border(1.dp, permColor.copy(alpha = 0.25f), AzokleRadius.md),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = perm.icon,
                                contentDescription = null,
                                tint = permColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = perm.displayName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = perm.description,
                                fontSize = 11.5.sp,
                                color = colors.textSecondary,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    if (index < request.requestedTypes.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = colors.borderSubtle
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Remember Choice Pill Card (With Emerald Green Switch)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AzokleRadius.md)
                    .background(colors.surfaceElevated)
                    .border(1.dp, colors.borderSubtle, AzokleRadius.md)
                    .clickable { rememberDecision = !rememberDecision }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = if (rememberDecision) colors.statusSuccess else colors.textMuted,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Remember for this website",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textPrimary
                        )
                        Text(
                            text = if (rememberDecision) "Choice saved permanently for $cleanHost" else "Permission expires when tab closes",
                            fontSize = 11.sp,
                            color = colors.textMuted
                        )
                    }
                }

                Switch(
                    checked = rememberDecision,
                    onCheckedChange = { rememberDecision = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = colors.statusSuccess,
                        checkedBorderColor = Color.Transparent,
                        uncheckedThumbColor = colors.textMuted,
                        uncheckedTrackColor = colors.cardBg,
                        uncheckedBorderColor = colors.borderSubtle
                    )
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 6. Action Buttons (3-Tier Visual Hierarchy)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Tier 1: Allow / While Visiting (Primary Emphasis)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(AzokleRadius.lg)
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
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.actionPrimaryText
                    )
                }

                // Tier 2: Allow This Time Only (Ephemeral 1-time grant)
                if (!isAutoplayOnly) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .clip(AzokleRadius.lg)
                            .background(colors.surfaceElevated)
                            .border(1.dp, colors.borderSubtle, AzokleRadius.lg)
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
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                    }
                }

                // Tier 3: Don't Allow (Block / Reject)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(AzokleRadius.lg)
                        .background(colors.statusDestructive.copy(alpha = 0.10f))
                        .border(1.dp, colors.statusDestructive.copy(alpha = 0.28f), AzokleRadius.lg)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = colors.statusDestructive.copy(alpha = 0.2f)),
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
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.statusDestructive
                    )
                }
            }
        }
    }
}
