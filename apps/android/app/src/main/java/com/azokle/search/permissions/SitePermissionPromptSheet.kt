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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azokle.search.permissions.SitePermissionRequest
import com.azokle.search.permissions.SitePermissionType
import com.azokle.search.common.theme.AzokleRadius
import com.azokle.search.common.theme.AzokleTheme
import com.azokle.search.common.theme.adaptiveContentWidth

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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.background,
        contentColor = colors.textPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .adaptiveContentWidth(560.dp)
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drag Indicator
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(colors.border)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Website Identity Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AzokleRadius.md)
                    .background(colors.surfaceElevated)
                    .border(1.dp, colors.border, AzokleRadius.md)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isHttps) colors.statusSuccess.copy(alpha = 0.15f) else colors.statusDestructive.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isHttps) Icons.Default.Lock else Icons.Default.Security,
                        contentDescription = null,
                        tint = if (isHttps) colors.statusSuccess else colors.statusDestructive,
                        modifier = Modifier.size(18.dp)
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
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isHttps) "Secure HTTPS Origin" else "Insecure Connection",
                        fontSize = 11.sp,
                        color = if (isHttps) colors.statusSuccess else colors.statusDestructive,
                        fontWeight = FontWeight.Medium
                    )
                }

                // 48dp Close Target
                Box(
                    modifier = Modifier
                        .sizeIn(minWidth = 38.dp, minHeight = 44.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = false, radius = 16.dp, color = colors.textSecondary),
                            onClick = onDismiss
                        )
                        .semantics {
                            role = Role.Button
                            contentDescription = "Close permission dialog"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            val isAutoplayOnly = request.requestedTypes.size == 1 && request.requestedTypes.first() == SitePermissionType.AUTOPLAY

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isAutoplayOnly) "Autoplay Permission" else "Permission Request",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isAutoplayOnly) {
                    "This website wants to automatically play audio or video."
                } else {
                    "$cleanHost wants to access your device capabilities:"
                },
                fontSize = 13.sp,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Requested Permission Items
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AzokleRadius.lg)
                    .background(colors.cardBg)
                    .border(1.dp, colors.border, AzokleRadius.lg)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                request.requestedTypes.forEachIndexed { index, perm ->
                    val permColor = when (perm) {
                        SitePermissionType.CAMERA -> colors.statusAccent
                        SitePermissionType.MICROPHONE -> colors.statusTor
                        SitePermissionType.GEOLOCATION -> colors.statusWarning
                        SitePermissionType.NOTIFICATIONS -> colors.statusAccent
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
                                .clip(AzokleRadius.sm)
                                .background(permColor.copy(alpha = 0.15f)),
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
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = perm.description,
                                fontSize = 11.sp,
                                color = colors.textSecondary
                            )
                        }
                    }

                    if (index < request.requestedTypes.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 2.dp),
                            color = colors.border.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Remember Choice Checkbox
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AzokleRadius.sm)
                    .clickable { rememberDecision = !rememberDecision }
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = rememberDecision,
                    onCheckedChange = { rememberDecision = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = colors.actionPrimaryBg,
                        checkmarkColor = colors.actionPrimaryText
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isAutoplayOnly) "Remember decision for $cleanHost" else "Remember my decision for this website",
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Allow
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
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
                        text = if (isAutoplayOnly) "Allow" else if (rememberDecision) "Allow While Visiting" else "Allow",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.actionPrimaryText
                    )
                }

                // 2. Block / Don't Allow
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(AzokleRadius.md)
                        .background(colors.statusDestructive.copy(alpha = 0.12f))
                        .border(1.dp, colors.statusDestructive.copy(alpha = 0.3f), AzokleRadius.md)
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
                        text = if (isAutoplayOnly) "Block" else "Don't Allow",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.statusDestructive
                    )
                }

                // 3. Ask Every Time / Allow This Time Only
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(AzokleRadius.md)
                        .background(colors.surfaceElevated)
                        .border(1.dp, colors.border, AzokleRadius.md)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = colors.textPrimary.copy(alpha = 0.1f)),
                            onClick = onAllowOnce
                        )
                        .semantics {
                            role = Role.Button
                            contentDescription = if (isAutoplayOnly) "Ask every time" else "Allow this time only"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isAutoplayOnly) "Ask Every Time" else "Allow This Time Only",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
