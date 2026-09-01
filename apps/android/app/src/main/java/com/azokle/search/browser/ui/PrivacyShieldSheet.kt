package com.azokle.search.browser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azokle.search.browser.adblock.AdBlockEngine
import com.azokle.search.browser.adblock.AdBlockStats
import com.azokle.search.ui.theme.AzokleRadius
import com.azokle.search.ui.theme.AzokleTheme
import com.azokle.search.ui.theme.adaptiveContentWidth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyShieldSheet(
    onDismiss: () -> Unit,
    onShieldToggled: () -> Unit
) {
    val colors = AzokleTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val stats by AdBlockStats.currentStats.collectAsState()

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
                    .background(colors.border)
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .adaptiveContentWidth(560.dp)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (stats.isShieldEnabled) colors.statusSuccess.copy(alpha = 0.2f) else colors.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = if (stats.isShieldEnabled) colors.statusSuccess else colors.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "AZOKLE SHIELD",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = colors.textPrimary
                            )
                            Text(
                                text = if (stats.host.isNotBlank()) stats.host else "Active Page Protection",
                                fontSize = 12.sp,
                                color = colors.textSecondary,
                                maxLines = 1
                            )
                        }
                    }

                    Switch(
                        checked = stats.isShieldEnabled,
                        onCheckedChange = { isChecked ->
                            if (stats.host.isNotBlank()) {
                                AdBlockEngine.instance.toggleShield(stats.host)
                                AdBlockStats.toggleShield(isChecked)
                                onShieldToggled()
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = colors.statusSuccess,
                            uncheckedThumbColor = colors.textMuted,
                            uncheckedTrackColor = colors.surface
                        )
                    )
                }
            }

            // Status Metric Banner
            item {
                val bannerShape = AzokleRadius.lg
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(bannerShape)
                        .background(colors.cardBg)
                        .border(1.dp, colors.border, bannerShape)
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${stats.adsBlockedCount}",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = if (stats.isShieldEnabled) (if (stats.adsBlockedCount > 0) colors.statusSuccess else colors.textPrimary) else colors.textMuted
                            )
                            Text(
                                text = "Total Ads Blocked",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textSecondary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(36.dp)
                                .background(colors.border)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${stats.trackersBlockedCount}",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = if (stats.isShieldEnabled) (if (stats.trackersBlockedCount > 0) colors.statusTor else colors.textPrimary) else colors.textMuted
                            )
                            Text(
                                text = "Trackers Intercepted",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textSecondary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (stats.isShieldEnabled) "Real-Time Quantum Shield Active" else "Shield Paused for this Site",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textMuted
                    )
                }
            }

            // Security Protections Checklist
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AzokleRadius.lg)
                        .background(colors.cardBg)
                        .border(1.dp, colors.border, AzokleRadius.lg)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ProtectionFeatureRow(
                        icon = Icons.Default.Lock,
                        title = "HTTPS Enforcement",
                        desc = "Secure encrypted transport forced",
                        colors = colors
                    )
                    ProtectionFeatureRow(
                        icon = Icons.Default.Cookie,
                        title = "3rd-Party Cookie Isolation",
                        desc = "Cross-site advertising cookies dropped",
                        colors = colors
                    )
                    ProtectionFeatureRow(
                        icon = Icons.Default.AutoAwesome,
                        title = "Global Privacy Control (GPC)",
                        desc = "Do-Not-Track signals broadcasted",
                        colors = colors
                    )
                }
            }

            // List of Blocked Domains on Current Page
            if (stats.blockedDomainsList.isNotEmpty()) {
                item {
                    Text(
                        text = "BLOCKED REQUESTS (${stats.blockedDomainsList.size})",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMuted,
                        letterSpacing = 0.5.sp
                    )
                }

                items(stats.blockedDomainsList) { domain ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(AzokleRadius.sm)
                            .background(colors.cardBg)
                            .border(0.5.dp, colors.border, AzokleRadius.sm)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = null,
                            tint = colors.statusDestructive,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = domain,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = colors.textSecondary,
                            maxLines = 1
                        )
                    }
                }
            } else if (stats.isShieldEnabled) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(AzokleRadius.md)
                            .background(colors.cardBg)
                            .border(0.5.dp, colors.border, AzokleRadius.md)
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No invasive trackers or ads detected yet on this page.",
                            fontSize = 12.sp,
                            color = colors.textMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ProtectionFeatureRow(
    icon: ImageVector,
    title: String,
    desc: String,
    colors: com.azokle.search.ui.theme.AzokleThemeColors
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.textPrimary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
            Text(
                text = desc,
                fontSize = 11.sp,
                color = colors.textMuted
            )
        }
    }
}
