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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azokle.search.permissions.SitePermissionState
import com.azokle.search.permissions.SitePermissionType
import com.azokle.search.permissions.SitePermissionsManager
import com.azokle.search.common.theme.AzokleRadius
import com.azokle.search.common.theme.AzokleTheme
import com.azokle.search.common.theme.adaptiveContentWidth
import com.azokle.search.common.util.UrlHelper

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
        containerColor = colors.surface,
        scrimColor = colors.scrim,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .adaptiveContentWidth(560.dp)
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drag Indicator
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(colors.border)
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(colors.statusAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = colors.statusAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Site Permissions",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Hardware & sensor access rules",
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }
                }

                if (allSites.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .sizeIn(minWidth = 38.dp, minHeight = 44.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = false, radius = 16.dp, color = colors.statusDestructive),
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
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(480.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Default Global Permissions
                item {
                    Text(
                        text = "GLOBAL DEFAULT BEHAVIOR",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.statusAccent,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(AzokleRadius.lg)
                            .background(colors.cardBg)
                            .border(1.dp, colors.border, AzokleRadius.lg)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            SitePermissionType.AUTOPLAY,
                            SitePermissionType.CAMERA,
                            SitePermissionType.MICROPHONE,
                            SitePermissionType.GEOLOCATION,
                            SitePermissionType.NOTIFICATIONS
                        ).forEachIndexed { idx, type ->
                            val defaultState = permManager.getGlobalDefault(type)
                            val isEnabled = if (type == SitePermissionType.AUTOPLAY) {
                                defaultState == SitePermissionState.ALLOW
                            } else {
                                defaultState == SitePermissionState.ASK
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
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
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = type.displayName,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.textPrimary
                                        )
                                        val subtitle = if (type == SitePermissionType.AUTOPLAY) {
                                            if (isEnabled) "Allow media playback (Default)" else "Block automatic playback"
                                        } else {
                                            if (isEnabled) "Ask before accessing (Recommended)" else "Block automatically"
                                        }
                                        Text(
                                            text = subtitle,
                                            fontSize = 11.sp,
                                            color = colors.textSecondary
                                        )
                                    }
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
                                        checkedTrackColor = colors.statusAccent,
                                        uncheckedThumbColor = colors.textSecondary,
                                        uncheckedTrackColor = colors.surfaceElevated
                                    )
                                )
                            }

                            if (idx < 4) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    color = colors.border.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }

                // Section 2: Configured Websites
                item {
                    Text(
                        text = "CUSTOMIZED WEBSITES (${allSites.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.statusAccent,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (allSites.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(AzokleRadius.md)
                                .background(colors.cardBg)
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No custom website permissions configured.\nSites will follow global default policies.",
                                fontSize = 12.sp,
                                color = colors.textSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    }
                } else {
                    items(allSites.keys.toList()) { host ->
                        val perms = allSites[host] ?: emptyMap()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(AzokleRadius.md)
                                .background(colors.cardBg)
                                .border(1.dp, colors.border, AzokleRadius.md)
                                .clickable { selectedSiteHost = host }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
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
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = permManager.getDisplayHost(host),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val summary = perms.entries.joinToString(", ") { "${it.key.displayName}: ${it.value.name}" }
                                    Text(
                                        text = summary,
                                        fontSize = 11.sp,
                                        color = colors.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .sizeIn(minWidth = 36.dp, minHeight = 40.dp)
                                    .clip(CircleShape)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(bounded = false, radius = 16.dp, color = colors.statusDestructive),
                                        onClick = {
                                            permManager.clearHostPermissions(host)
                                            Toast.makeText(context, "Permissions cleared for ${permManager.getDisplayHost(host)}", Toast.LENGTH_SHORT).show()
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
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Site Specific Detail Dialog
    selectedSiteHost?.let { host ->
        val sitePerms = permManager.getHostPermissions(host)
        AlertDialog(
            onDismissRequest = { selectedSiteHost = null },
            title = {
                Text(
                    text = permManager.getDisplayHost(host),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(
                        SitePermissionType.AUTOPLAY,
                        SitePermissionType.CAMERA,
                        SitePermissionType.MICROPHONE,
                        SitePermissionType.GEOLOCATION,
                        SitePermissionType.NOTIFICATIONS
                    ).forEach { type ->
                        val state = sitePerms[type] ?: permManager.getGlobalDefault(type)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = type.displayName,
                                fontSize = 13.sp,
                                color = colors.textPrimary
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (state == SitePermissionState.ALLOW) colors.statusSuccess.copy(alpha = 0.2f) else colors.surfaceElevated)
                                        .clickable {
                                            permManager.setPermission(host, type, SitePermissionState.ALLOW)
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Allow",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (state == SitePermissionState.ALLOW) colors.statusSuccess else colors.textSecondary
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (state == SitePermissionState.BLOCK) colors.statusDestructive.copy(alpha = 0.2f) else colors.surfaceElevated)
                                        .clickable {
                                            permManager.setPermission(host, type, SitePermissionState.BLOCK)
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Block",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (state == SitePermissionState.BLOCK) colors.statusDestructive else colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedSiteHost = null }) {
                    Text("Done", color = colors.statusAccent)
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
                    Text("Reset Site", color = colors.statusDestructive)
                }
            },
            containerColor = colors.surface,
            shape = AzokleRadius.xl
        )
    }

    // Confirm Clear All Dialog
    if (showClearAllConfirm) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirm = false },
            title = {
                Text("Clear All Site Permissions?", fontWeight = FontWeight.Bold, color = colors.textPrimary)
            },
            text = {
                Text(
                    "This will reset all customized website permissions to default settings.",
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
                    Text("Clear All", color = colors.statusDestructive, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirm = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface,
            shape = AzokleRadius.xl
        )
    }
}
