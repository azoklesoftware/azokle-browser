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

package com.azokle.search.adblock

import java.util.Locale
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azokle.search.adblock.AdBlockEngine
import com.azokle.search.adblock.AdBlockStats
import com.azokle.search.common.components.ToastManager
import com.azokle.search.common.theme.AzokleBreakpoints
import com.azokle.search.common.theme.AzokleRadius
import com.azokle.search.common.theme.AzokleTheme
import com.azokle.search.common.theme.AzokleThemeColors
import com.azokle.search.common.theme.adaptiveContentWidth

/**
 * Ultra-Minimalist Azokle Shield Ad & Tracker Blocker Sheet.
 * Designed with clean typography, compact flat cards, zero text clutter, and fast tactile controls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyShieldSheet(
    onDismiss: () -> Unit,
    onShieldToggled: () -> Unit
) {
    val colors = AzokleTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val stats by AdBlockStats.currentStats.collectAsState()
    val lifetimeStats by AdBlockStats.lifetimeStats.collectAsState()

    val animatedPageAds by animateIntAsState(
        targetValue = stats.adsBlockedCount,
        animationSpec = spring(stiffness = 400f),
        label = "AnimatedPageAds"
    )
    val animatedPageTrackers by animateIntAsState(
        targetValue = stats.trackersBlockedCount,
        animationSpec = spring(stiffness = 400f),
        label = "AnimatedPageTrackers"
    )
    val animatedLifetimeAds by animateIntAsState(
        targetValue = lifetimeStats.totalAdsBlocked.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
        animationSpec = spring(stiffness = 400f),
        label = "AnimatedLifetimeAds"
    )
    val animatedLifetimeTrackers by animateIntAsState(
        targetValue = lifetimeStats.trackersBlocked.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
        animationSpec = spring(stiffness = 400f),
        label = "AnimatedLifetimeTrackers"
    )

    var isBlockedListExpanded by remember { mutableStateOf(false) }

    val isShieldActive = stats.isShieldEnabled
    val statusColor by animateColorAsState(
        targetValue = if (isShieldActive) colors.statusSuccess else colors.textMuted,
        animationSpec = tween(durationMillis = 240),
        label = "ShieldStatusColor"
    )

    val cleanHost = remember(stats.host) {
        stats.host.removePrefix("www.").ifBlank { "Active Site" }
    }

    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        contentColor = colors.textPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 10.dp, bottom = 6.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(colors.border.copy(alpha = 0.6f))
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .adaptiveContentWidth(AzokleBreakpoints.MaxSheetWidth)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp, top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Azokle Shield",
                        fontSize = 17.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        letterSpacing = (-0.3).sp,
                        modifier = Modifier.semantics { heading() }
                    )

                    // Close Action
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
                                contentDescription = "Close Shield"
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

                // 2. Hero Card: Domain & Master Toggle
                val heroShape = AzokleRadius.lg
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(heroShape)
                        .background(colors.cardBg)
                        .border(1.dp, colors.borderSubtle, heroShape)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(statusColor.copy(alpha = 0.12f))
                                .border(0.8.dp, statusColor.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = cleanHost,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(5.5.dp)
                                        .clip(CircleShape)
                                        .background(statusColor)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (isShieldActive) "Active" else "Paused",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = statusColor
                                )
                            }
                        }
                    }

                    Switch(
                        checked = isShieldActive,
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
                            uncheckedTrackColor = colors.surfaceElevated,
                            uncheckedBorderColor = colors.borderSubtle
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Telemetry Counters Card (Ads & Trackers)
                val counterShape = AzokleRadius.lg
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(counterShape)
                        .background(colors.cardBg)
                        .border(1.dp, colors.borderSubtle, counterShape)
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MinimalCounter(
                            icon = Icons.Default.Block,
                            count = animatedPageAds,
                            label = "Ads Blocked",
                            tint = if (isShieldActive && animatedPageAds > 0) colors.statusSuccess else colors.textMuted,
                            colors = colors,
                            modifier = Modifier.weight(1f)
                        )

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(32.dp)
                                .background(colors.borderSubtle)
                        )

                        MinimalCounter(
                            icon = Icons.Default.Fingerprint,
                            count = animatedPageTrackers,
                            label = "Trackers Defused",
                            tint = if (isShieldActive && animatedPageTrackers > 0) colors.statusTor else colors.textMuted,
                            colors = colors,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = colors.borderSubtle, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "All-time: ${formatCompactNumber(animatedLifetimeAds.toLong())} ads • ${formatCompactNumber(animatedLifetimeTrackers.toLong())} trackers",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textMuted,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 4. Protection Features Card (Clean single-line rows)
                val featuresShape = AzokleRadius.lg
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(featuresShape)
                        .background(colors.cardBg)
                        .border(1.dp, colors.borderSubtle, featuresShape)
                        .padding(vertical = 4.dp)
                ) {
                    MinimalFeatureRow(
                        icon = Icons.Default.Block,
                        title = "Ad & Tracker Blocking",
                        isActive = isShieldActive,
                        colors = colors
                    )
                    HorizontalDivider(color = colors.borderSubtle, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 14.dp))
                    MinimalFeatureRow(
                        icon = Icons.Default.Lock,
                        title = "HTTPS Upgrade",
                        isActive = isShieldActive,
                        colors = colors
                    )
                    HorizontalDivider(color = colors.borderSubtle, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 14.dp))
                    MinimalFeatureRow(
                        icon = Icons.Default.Cookie,
                        title = "Cookie Partitioning",
                        isActive = isShieldActive,
                        colors = colors
                    )
                    HorizontalDivider(color = colors.borderSubtle, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 14.dp))
                    MinimalFeatureRow(
                        icon = Icons.Default.AutoAwesome,
                        title = "Global Privacy Control (GPC)",
                        isActive = isShieldActive,
                        colors = colors
                    )
                }

                // 5. Blocked Requests (Collapsible if any)
                val blockedList = stats.blockedDomainsList
                if (blockedList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))

                    val blockedShape = AzokleRadius.lg
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(blockedShape)
                            .background(colors.cardBg)
                            .border(1.dp, colors.borderSubtle, blockedShape)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true, color = colors.textPrimary.copy(alpha = 0.08f))
                                ) {
                                    isBlockedListExpanded = !isBlockedListExpanded
                                }
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Blocked Requests",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(colors.surfaceElevated)
                                        .padding(horizontal = 6.dp, vertical = 1.5.dp)
                                ) {
                                    Text(
                                        text = "${blockedList.size}",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textSecondary
                                    )
                                }
                            }

                            Icon(
                                imageVector = if (isBlockedListExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = colors.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        AnimatedVisibility(
                            visible = isBlockedListExpanded,
                            enter = expandVertically(animationSpec = spring()) + fadeIn(),
                            exit = shrinkVertically(animationSpec = spring()) + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp)
                                    .background(colors.surfaceElevated.copy(alpha = 0.35f))
                                    .padding(vertical = 4.dp)
                            ) {
                                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                    if (stats.blockedTrackers.isNotEmpty()) {
                                        items(stats.blockedTrackers) { record ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Block,
                                                        contentDescription = null,
                                                        tint = colors.statusDestructive.copy(alpha = 0.8f),
                                                        modifier = Modifier.size(11.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = record.entity.displayName.ifBlank { record.host },
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = colors.textPrimary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }

                                                Text(
                                                    text = record.entity.category.label,
                                                    fontSize = 10.sp,
                                                    color = colors.textMuted
                                                )
                                            }
                                        }
                                    } else {
                                        items(blockedList) { domain ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Block,
                                                    contentDescription = null,
                                                    tint = colors.statusDestructive.copy(alpha = 0.8f),
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = domain,
                                                    fontSize = 11.5.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = colors.textSecondary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------- Helper Components & Composables ----------------

@Composable
private fun MinimalCounter(
    icon: ImageVector,
    count: Int,
    label: String,
    tint: Color,
    colors: AzokleThemeColors,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(14.dp)
            )

            AnimatedContent(
                targetState = count,
                transitionSpec = {
                    (slideInVertically { height -> height / 2 } + fadeIn(tween(160)))
                        .togetherWith(slideOutVertically { height -> -height / 2 } + fadeOut(tween(120)))
                },
                label = "CounterAnimation"
            ) { value ->
                Text(
                    text = "$value",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (value > 0) colors.textPrimary else colors.textMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = label,
            fontSize = 11.5.sp,
            color = colors.textSecondary
        )
    }
}

/**
 * Composable that renders the minimal feature row UI component.
 */
@Composable
private fun MinimalFeatureRow(
    icon: ImageVector,
    title: String,
    isActive: Boolean,
    colors: AzokleThemeColors
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) colors.textPrimary else colors.textMuted,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (isActive) colors.textPrimary else colors.textMuted
            )
        }

        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = if (isActive) colors.statusSuccess else colors.borderSubtle,
            modifier = Modifier.size(15.dp)
        )
    }
}

/**
 * Formats compact number for display.
 */
private fun formatCompactNumber(count: Long): String {
    if (count <= 0L) return "0"
    if (count < 1_000L) return count.toString()
    if (count < 1_000_000L) {
        val value = count / 1_000.0
        return if (count % 1_000L < 100L) {
            "${count / 1_000L}K"
        } else {
            String.format(Locale.US, "%.1fK", value)
        }
    }
    val value = count / 1_000_000.0
    return String.format(Locale.US, "%.1fM", value)
}
