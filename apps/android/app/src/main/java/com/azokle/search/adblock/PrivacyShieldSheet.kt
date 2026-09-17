package com.azokle.search.adblock

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShieldMoon
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Security
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azokle.search.adblock.AdBlockEngine
import com.azokle.search.adblock.AdBlockStats
import com.azokle.search.common.components.ToastManager
import com.azokle.search.common.theme.AzokleRadius
import com.azokle.search.common.theme.AzokleTheme
import com.azokle.search.common.theme.adaptiveContentWidth

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
        animationSpec = tween(durationMillis = 280),
        label = "ShieldStatusColor"
    )
    val shieldScale by animateFloatAsState(
        targetValue = if (isShieldActive) 1f else 0.94f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "ShieldScale"
    )

    val currentShieldState = remember(isShieldActive, stats.host) {
        if (stats.host.isNotBlank()) AdBlockEngine.instance.getShieldState(stats.host) else ShieldHostState.Enabled
    }

    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.cardBg,
        contentColor = colors.textPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 38.dp, height = 4.5.dp)
                    .clip(CircleShape)
                    .background(colors.borderSubtle.copy(alpha = 0.8f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .adaptiveContentWidth(500.dp)
                .navigationBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ==========================================
            // 1. HERO HEADER: BADGE, DOMAIN & MASTER TOGGLE
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    // Glowing Halo Badge
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .scale(shieldScale)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        statusColor.copy(alpha = if (isShieldActive) 0.22f else 0.10f),
                                        statusColor.copy(alpha = 0.04f)
                                    )
                                )
                            )
                            .border(
                                width = 1.dp,
                                color = statusColor.copy(alpha = if (isShieldActive) 0.35f else 0.15f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isShieldActive) Icons.Default.Shield else Icons.Default.ShieldMoon,
                            contentDescription = "Shield Status Icon",
                            tint = statusColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Azokle Shield",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                modifier = Modifier.semantics { heading() }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isShieldActive) colors.statusSuccess.copy(alpha = 0.12f)
                                        else colors.surfaceElevated
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isShieldActive) "ACTIVE" else "PAUSED",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 0.6.sp,
                                    color = if (isShieldActive) colors.statusSuccess else colors.textMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Verified Host Pill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(AzokleRadius.sm)
                                .background(colors.surfaceElevated.copy(alpha = 0.6f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = if (isShieldActive) Icons.Default.Lock else Icons.Outlined.Public,
                                contentDescription = null,
                                tint = if (isShieldActive) colors.statusSuccess.copy(alpha = 0.85f) else colors.textMuted,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (stats.host.isNotBlank()) stats.host else "Active Page Protection",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.Monospace,
                                color = colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // High-Quality Switch
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

            // ==========================================
            // 2. CONTEXTUAL STATUS BANNER
            // ==========================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AzokleRadius.md)
                    .background(
                        if (isShieldActive) colors.statusSuccess.copy(alpha = 0.07f)
                        else colors.surfaceElevated
                    )
                    .border(
                        width = 1.dp,
                        color = if (isShieldActive) colors.statusSuccess.copy(alpha = 0.18f) else colors.borderSubtle,
                        shape = AzokleRadius.md
                    )
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.5.dp)
                                .clip(CircleShape)
                                .background(if (isShieldActive) colors.statusSuccess else colors.statusDestructive)
                        )
                        Text(
                            text = when (currentShieldState) {
                                ShieldHostState.Enabled -> "Sovereign shielding enabled for this site"
                                ShieldHostState.DisabledUntilRelaunch -> "Shield paused until app relaunch"
                                ShieldHostState.DisabledPermanently -> "Shield permanently disabled for site"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isShieldActive) colors.textPrimary else colors.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (!isShieldActive && stats.host.isNotBlank()) {
                        Text(
                            text = if (currentShieldState == ShieldHostState.DisabledUntilRelaunch) "Make Permanent" else "Session Only",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.statusAccent,
                            modifier = Modifier
                                .clip(AzokleRadius.sm)
                                .clickable {
                                    if (currentShieldState == ShieldHostState.DisabledUntilRelaunch) {
                                        AdBlockEngine.instance.disableShieldPermanently(stats.host)
                                    } else {
                                        AdBlockEngine.instance.pauseShieldUntilRelaunch(stats.host)
                                    }
                                    onShieldToggled()
                                }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ==========================================
            // 3. UNIFIED METRIC MATRIX (PAGE + LIFETIME)
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AzokleRadius.lg)
                    .background(colors.surfaceElevated.copy(alpha = 0.7f))
                    .border(1.dp, colors.borderSubtle, AzokleRadius.lg)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                // Metric Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PROTECTION TELEMETRY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = colors.textMuted,
                        letterSpacing = 0.9.sp
                    )
                    Text(
                        text = "LIVE DEFENSES",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        color = if (isShieldActive) colors.statusSuccess else colors.textMuted
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Current Page Counter Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Page Ads Counter
                    MetricCounterItem(
                        icon = Icons.Default.Block,
                        iconTint = if (isShieldActive && animatedPageAds > 0) colors.statusSuccess else colors.textMuted,
                        badgeBg = if (isShieldActive && animatedPageAds > 0) colors.statusSuccess.copy(alpha = 0.12f) else colors.cardBg,
                        count = animatedPageAds,
                        label = "Ads Blocked",
                        sublabel = "This Page",
                        colors = colors,
                        modifier = Modifier.weight(1f)
                    )

                    // Vertical Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(38.dp)
                            .background(colors.borderSubtle.copy(alpha = 0.6f))
                    )

                    // Page Trackers Counter
                    MetricCounterItem(
                        icon = Icons.Default.Fingerprint,
                        iconTint = if (isShieldActive && animatedPageTrackers > 0) colors.statusTor else colors.textMuted,
                        badgeBg = if (isShieldActive && animatedPageTrackers > 0) colors.statusTor.copy(alpha = 0.12f) else colors.cardBg,
                        count = animatedPageTrackers,
                        label = "Trackers Defused",
                        sublabel = "This Page",
                        colors = colors,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                HorizontalDivider(
                    color = colors.borderSubtle.copy(alpha = 0.5f),
                    thickness = 0.8.dp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Lifetime Footnote Counters
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "All-Time Savings: ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textMuted
                        )
                        Text(
                            text = "${formatCompactNumber(animatedLifetimeAds.toLong())} ads",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = colors.textSecondary
                        )
                        Text(
                            text = " • ",
                            fontSize = 11.sp,
                            color = colors.textMuted
                        )
                        Text(
                            text = "${formatCompactNumber(animatedLifetimeTrackers.toLong())} trackers",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = colors.textSecondary
                        )
                    }

                    Icon(
                        imageVector = Icons.Outlined.Security,
                        contentDescription = null,
                        tint = colors.textMuted.copy(alpha = 0.6f),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ==========================================
            // 4. INTEGRATED PRIVACY PILLARS
            // ==========================================
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "ACTIVE DEFENSE PILLARS",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = colors.textMuted,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AzokleRadius.lg)
                        .background(colors.surfaceElevated.copy(alpha = 0.7f))
                        .border(1.dp, colors.borderSubtle, AzokleRadius.lg)
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    ProtectionPillarItem(
                        icon = Icons.Default.Block,
                        title = "Ad & Tracker Scriptlet Defusal",
                        desc = "Anti-fingerprinting, surrogates & network filters",
                        isActive = isShieldActive,
                        colors = colors
                    )

                    HorizontalDivider(
                        color = colors.borderSubtle.copy(alpha = 0.6f),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 36.dp)
                    )

                    ProtectionPillarItem(
                        icon = Icons.Default.Lock,
                        title = "Strict HTTPS Upgrade",
                        desc = "Forced secure encryption on all transports",
                        isActive = isShieldActive,
                        colors = colors
                    )

                    HorizontalDivider(
                        color = colors.borderSubtle.copy(alpha = 0.6f),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 36.dp)
                    )

                    ProtectionPillarItem(
                        icon = Icons.Default.Cookie,
                        title = "Cross-Site Cookie Isolation",
                        desc = "Partitioned storage prevents user profiling",
                        isActive = isShieldActive,
                        colors = colors
                    )

                    HorizontalDivider(
                        color = colors.borderSubtle.copy(alpha = 0.6f),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 36.dp)
                    )

                    ProtectionPillarItem(
                        icon = Icons.Default.AutoAwesome,
                        title = "Global Privacy Control (GPC)",
                        desc = "Broadcasts Do-Not-Track & opt-out headers",
                        isActive = isShieldActive,
                        colors = colors
                    )
                }
            }

            // ==========================================
            // 5. BLOCKED REQUESTS ACCORDION
            // ==========================================
            val blockedList = stats.blockedDomainsList
            if (blockedList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(AzokleRadius.md)
                            .background(colors.surfaceElevated.copy(alpha = 0.5f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = colors.textPrimary.copy(alpha = 0.08f))
                            ) {
                                isBlockedListExpanded = !isBlockedListExpanded
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "BLOCKED REQUESTS",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = colors.textMuted,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(colors.cardBg)
                                    .padding(horizontal = 7.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "${blockedList.size}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = colors.textPrimary
                                )
                            }
                        }

                        Icon(
                            imageVector = if (isBlockedListExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isBlockedListExpanded) "Collapse" else "Expand",
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
                                .heightIn(max = 220.dp)
                                .padding(top = 6.dp)
                                .clip(AzokleRadius.md)
                                .background(colors.surfaceElevated)
                                .border(1.dp, colors.borderSubtle, AzokleRadius.md)
                                .padding(vertical = 4.dp)
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (stats.blockedTrackers.isNotEmpty()) {
                                    items(stats.blockedTrackers) { record ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
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
                                                    tint = colors.statusDestructive.copy(alpha = 0.85f),
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(9.dp))
                                                Column {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = record.entity.displayName,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = colors.textPrimary,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(colors.cardBg)
                                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                                        ) {
                                                            Text(
                                                                text = record.entity.category.label,
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                color = colors.textMuted
                                                            )
                                                        }
                                                    }
                                                    Text(
                                                        text = record.host,
                                                        fontSize = 10.5.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = colors.textSecondary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }

                                            if (record.blockCount > 1) {
                                                Text(
                                                    text = "${record.blockCount}x",
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.textMuted,
                                                    modifier = Modifier.padding(start = 6.dp)
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    items(blockedList) { domain ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Block,
                                                contentDescription = null,
                                                tint = colors.statusDestructive.copy(alpha = 0.85f),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(9.dp))
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
            } else if (isShieldActive) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No invasive trackers or ads detected yet on this page.",
                    fontSize = 11.5.sp,
                    color = colors.textMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                )
            }

            // ==========================================
            // 6. SITE COMPATIBILITY / QUICK ACTION CARD
            // ==========================================
            if (stats.host.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AzokleRadius.md)
                        .background(colors.surfaceElevated.copy(alpha = 0.45f))
                        .border(1.dp, colors.borderSubtle.copy(alpha = 0.7f), AzokleRadius.md)
                        .clickable {
                            if (isShieldActive) {
                                AdBlockEngine.instance.pauseShieldUntilRelaunch(stats.host)
                                onShieldToggled()
                                ToastManager.show("Protections paused for this session")
                            } else {
                                AdBlockEngine.instance.enableShield(stats.host)
                                onShieldToggled()
                                ToastManager.show("Protections re-enabled")
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isShieldActive) "Website not working properly?" else "Protections paused for session",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = if (isShieldActive) "Tap to pause protections and reload cleanly" else "Tap to re-enable Azokle Shield",
                            fontSize = 11.sp,
                            color = colors.textMuted
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reload / Toggle",
                        tint = if (isShieldActive) colors.statusWarning else colors.statusSuccess,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricCounterItem(
    icon: ImageVector,
    iconTint: Color,
    badgeBg: Color,
    count: Int,
    label: String,
    sublabel: String,
    colors: com.azokle.search.common.theme.AzokleThemeColors,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "$label: $count"
        }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(badgeBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(14.dp)
                )
            }

            AnimatedContent(
                targetState = count,
                transitionSpec = {
                    (slideInVertically { height -> height / 2 } + fadeIn(tween(180)))
                        .togetherWith(slideOutVertically { height -> -height / 2 } + fadeOut(tween(140)))
                },
                label = "MetricCountText"
            ) { value ->
                Text(
                    text = "$value",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (value > 0) colors.textPrimary else colors.textMuted,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary
        )

        Text(
            text = sublabel,
            fontSize = 10.5.sp,
            color = colors.textMuted
        )
    }
}

@Composable
private fun ProtectionPillarItem(
    icon: ImageVector,
    title: String,
    desc: String,
    isActive: Boolean,
    colors: com.azokle.search.common.theme.AzokleThemeColors
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (isActive) colors.statusSuccess.copy(alpha = 0.12f) else colors.cardBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isActive) Icons.Default.Check else icon,
                contentDescription = null,
                tint = if (isActive) colors.statusSuccess else colors.textMuted,
                modifier = Modifier.size(14.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isActive) colors.textPrimary else colors.textMuted
            )
            Text(
                text = desc,
                fontSize = 10.5.sp,
                color = colors.textMuted,
                lineHeight = 13.5.sp
            )
        }
    }
}

private fun formatCompactNumber(count: Long): String {
    if (count <= 0L) return "0"
    if (count < 1_000L) return count.toString()
    if (count < 1_000_000L) {
        val value = count / 1_000.0
        return if (count % 1_000L < 100L) {
            "${count / 1_000L}K"
        } else {
            String.format(java.util.Locale.US, "%.1fK", value)
        }
    }
    if (count < 1_000_000_000L) {
        val value = count / 1_000_000.0
        return if (count % 1_000_000L < 100_000L) {
            "${count / 1_000_000L}M"
        } else {
            String.format(java.util.Locale.US, "%.1fM", value)
        }
    }
    val value = count / 1_000_000_000.0
    return String.format(java.util.Locale.US, "%.1fB", value)
}
