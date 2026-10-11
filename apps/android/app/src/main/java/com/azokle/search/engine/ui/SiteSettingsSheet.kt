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

package com.azokle.search.engine.ui

import android.widget.Toast
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azokle.search.common.theme.AzokleRadius
import com.azokle.search.common.theme.AzokleTheme
import com.azokle.search.common.theme.adaptiveContentWidth
import com.azokle.search.permissions.SitePermissionState
import com.azokle.search.permissions.SitePermissionType
import com.azokle.search.permissions.SitePermissionsManager

/**
 * Composable that renders the site settings sheet UI component.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiteSettingsSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colors = AzokleTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val permManager = remember { SitePermissionsManager.getInstance(context) }
    val updateTick by permManager.updatesFlow.collectAsState()

    val allSites = remember(updateTick) { permManager.getAllCustomizedSites() }
    var selectedSiteHost by remember { mutableStateOf<String?>(null) }
    var showClearAllConfirm by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.cardBg,
        contentColor = colors.textPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 10.dp, bottom = 6.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(colors.borderSubtle)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .adaptiveContentWidth(560.dp)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Site Permissions",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    modifier = Modifier.semantics { heading() }
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (allSites.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .sizeIn(minWidth = 36.dp, minHeight = 36.dp)
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = false, radius = 18.dp, color = colors.statusDestructive),
                                    onClick = { showClearAllConfirm = true }
                                )
                                .semantics {
                                    role = Role.Button
                                    contentDescription = "Clear all site permissions"
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = null,
                                tint = colors.statusDestructive,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    Box(
                        modifier = Modifier
                            .sizeIn(minWidth = 36.dp, minHeight = 36.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = false, radius = 18.dp, color = colors.textSecondary),
                                onClick = onDismiss
                            )
                            .semantics {
                                role = Role.Button
                                contentDescription = "Close site permissions"
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
                                tint = colors.textPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Section 1: Default Global Permissions
                item {
                    Text(
                        text = "DEFAULT PERMISSIONS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textMuted,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(start = 2.dp, bottom = 6.dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(AzokleRadius.lg)
                            .background(colors.surfaceElevated)
                            .border(0.8.dp, colors.borderSubtle, AzokleRadius.lg)
                    ) {
                        val permTypes = listOf(
                            SitePermissionType.CAMERA,
                            SitePermissionType.MICROPHONE,
                            SitePermissionType.GEOLOCATION,
                            SitePermissionType.NOTIFICATIONS,
                            SitePermissionType.AUTOPLAY
                        )

                        permTypes.forEachIndexed { idx, type ->
                            val defaultState = permManager.getGlobalDefault(type)
                            val isEnabled = if (type == SitePermissionType.AUTOPLAY) {
                                defaultState == SitePermissionState.ALLOW
                            } else {
                                defaultState == SitePermissionState.ASK
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .sizeIn(minHeight = 48.dp)
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = type.icon,
                                        contentDescription = null,
                                        tint = colors.textPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = type.displayName,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = colors.textPrimary
                                    )
                                }

                                Switch(
                                    checked = isEnabled,
                                    onCheckedChange = { checked ->
                                        if (type == SitePermissionType.AUTOPLAY) {
                                            permManager.setGlobalDefault(
                                                type,
                                                if (checked) SitePermissionState.ALLOW else SitePermissionState.BLOCK
                                            )
                                        } else {
                                            permManager.setGlobalDefault(
                                                type,
                                                if (checked) SitePermissionState.ASK else SitePermissionState.BLOCK
                                            )
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = colors.statusSuccess,
                                        checkedBorderColor = Color.Transparent,
                                        uncheckedThumbColor = colors.textSecondary,
                                        uncheckedTrackColor = colors.cardBg,
                                        uncheckedBorderColor = colors.borderSubtle
                                    )
                                )
                            }

                            if (idx < permTypes.size - 1) {
                                HorizontalDivider(
                                    color = colors.borderSubtle,
                                    thickness = 0.5.dp,
                                    modifier = Modifier.padding(start = 40.dp)
                                )
                            }
                        }
                    }
                }

                // Section 2: Configured Websites
                item {
                    Text(
                        text = "CUSTOM SITES (${allSites.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textMuted,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(start = 2.dp, bottom = 6.dp)
                    )

                    if (allSites.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(AzokleRadius.md)
                                .background(colors.surfaceElevated)
                                .border(0.8.dp, colors.borderSubtle, AzokleRadius.md)
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No custom site permissions",
                                fontSize = 12.sp,
                                color = colors.textMuted
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(AzokleRadius.lg)
                                .background(colors.surfaceElevated)
                                .border(0.8.dp, colors.borderSubtle, AzokleRadius.lg)
                        ) {
                            val siteKeys = allSites.keys.toList()
                            siteKeys.forEachIndexed { idx, host ->
                                val perms = allSites[host] ?: emptyMap()
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .sizeIn(minHeight = 48.dp)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = ripple(bounded = true, color = colors.textPrimary.copy(alpha = 0.08f)),
                                            onClick = { selectedSiteHost = host }
                                        )
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Public,
                                            contentDescription = null,
                                            tint = colors.statusAccent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = permManager.getDisplayHost(host),
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = colors.textPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${perms.size} custom rule${if (perms.size != 1) "s" else ""}",
                                                fontSize = 11.sp,
                                                color = colors.textMuted
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .sizeIn(minWidth = 32.dp, minHeight = 32.dp)
                                            .clip(CircleShape)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = ripple(bounded = false, radius = 16.dp, color = colors.statusDestructive),
                                                onClick = {
                                                    permManager.clearHostPermissions(host)
                                                    Toast.makeText(context, "Reset ${permManager.getDisplayHost(host)}", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                            .semantics {
                                                role = Role.Button
                                                contentDescription = "Delete permissions for $host"
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = colors.textMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                if (idx < siteKeys.size - 1) {
                                    HorizontalDivider(
                                        color = colors.borderSubtle,
                                        thickness = 0.5.dp,
                                        modifier = Modifier.padding(start = 40.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Site Specific Detail Dialog
    selectedSiteHost?.let { host ->
        val sitePerms = remember(host, updateTick) { permManager.getHostPermissions(host) }
        AlertDialog(
            onDismissRequest = { selectedSiteHost = null },
            title = {
                Text(
                    text = permManager.getDisplayHost(host),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        SitePermissionType.CAMERA,
                        SitePermissionType.MICROPHONE,
                        SitePermissionType.GEOLOCATION,
                        SitePermissionType.NOTIFICATIONS,
                        SitePermissionType.AUTOPLAY
                    ).forEach { type ->
                        val state = sitePerms[type] ?: permManager.getGlobalDefault(type)
                        val isAllow = state == SitePermissionState.ALLOW
                        val isBlock = state == SitePermissionState.BLOCK

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = type.icon,
                                    contentDescription = null,
                                    tint = when {
                                        isAllow -> colors.statusSuccess
                                        isBlock -> colors.statusDestructive
                                        else -> colors.textPrimary
                                    },
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = type.displayName,
                                    fontSize = 13.sp,
                                    color = colors.textPrimary
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isAllow) colors.statusSuccess.copy(alpha = 0.18f) else colors.cardBg)
                                        .border(
                                            0.8.dp,
                                            if (isAllow) colors.statusSuccess.copy(alpha = 0.5f) else colors.borderSubtle,
                                            RoundedCornerShape(6.dp)
                                        )
                                        .clickable {
                                            permManager.setPermission(host, type, SitePermissionState.ALLOW)
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Allow",
                                        fontSize = 11.sp,
                                        fontWeight = if (isAllow) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isAllow) colors.statusSuccess else colors.textSecondary
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isBlock) colors.statusDestructive.copy(alpha = 0.18f) else colors.cardBg)
                                        .border(
                                            0.8.dp,
                                            if (isBlock) colors.statusDestructive.copy(alpha = 0.5f) else colors.borderSubtle,
                                            RoundedCornerShape(6.dp)
                                        )
                                        .clickable {
                                            permManager.setPermission(host, type, SitePermissionState.BLOCK)
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Block",
                                        fontSize = 11.sp,
                                        fontWeight = if (isBlock) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isBlock) colors.statusDestructive else colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedSiteHost = null }) {
                    Text("Done", color = colors.statusAccent, fontSize = 13.sp)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        permManager.clearHostPermissions(host)
                        selectedSiteHost = null
                        Toast.makeText(context, "Permissions reset for $host", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Reset", color = colors.statusDestructive, fontSize = 13.sp)
                }
            },
            containerColor = colors.cardBg,
            shape = AzokleRadius.xl
        )
    }

    // Confirm Clear All Dialog
    if (showClearAllConfirm) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirm = false },
            title = {
                Text("Clear All Permissions?", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = colors.textPrimary)
            },
            text = {
                Text(
                    "Reset all site-specific rules to default.",
                    color = colors.textSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        permManager.clearAllPermissions()
                        showClearAllConfirm = false
                        Toast.makeText(context, "All site permissions cleared", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Clear All", color = colors.statusDestructive, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirm = false }) {
                    Text("Cancel", color = colors.textSecondary, fontSize = 13.sp)
                }
            },
            containerColor = colors.cardBg,
            shape = AzokleRadius.xl
        )
    }
}
