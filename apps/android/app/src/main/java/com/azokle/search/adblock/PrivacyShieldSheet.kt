package com.azokle.search.adblock

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateIntAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azokle.search.adblock.AdBlockEngine
import com.azokle.search.adblock.AdBlockStats
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
        animationSpec = tween(durationMillis = 350),
        label = "AnimatedPageAds"
    )
    val animatedPageTrackers by animateIntAsState(
        targetValue = stats.trackersBlockedCount,
        animationSpec = tween(durationMillis = 350),
        label = "AnimatedPageTrackers"
    )
    val animatedLifetimeAds by animateIntAsState(
        targetValue = lifetimeStats.totalAdsBlocked.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
        animationSpec = tween(durationMillis = 350),
        label = "AnimatedLifetimeAds"
    )
    val animatedLifetimeTrackers by animateIntAsState(
        targetValue = lifetimeStats.trackersBlocked.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
        animationSpec = tween(durationMillis = 350),
        label = "AnimatedLifetimeTrackers"
    )

    var isBlockedListExpanded by remember { mutableStateOf(false) }

    val isShieldActive = stats.isShieldEnabled
    val statusColor by animateColorAsState(
        targetValue = if (isShieldActive) colors.statusSuccess else colors.textMuted,
        animationSpec = tween(durationMillis = 250),
        label = "ShieldStatusColor"
    )

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
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ==========================================
            // 1. HEADER: BRAND, HOST & TOGGLE SWITCH
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = if (isShieldActive) 0.12f else 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isShieldActive) Icons.Default.Shield else Icons.Default.ShieldMoon,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = "Azokle Shield",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            modifier = Modifier.semantics { heading() }
                        )
                        Text(
                            text = if (stats.host.isNotBlank()) stats.host else "Active Page Protection",
                            fontSize = 12.sp,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
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

            Spacer(modifier = Modifier.height(14.dp))

            // ==========================================
            // 2. PRIMARY PROTECTION STATUS BANNER
            // ==========================================
            val currentShieldState = remember(isShieldActive, stats.host) {
                if (stats.host.isNotBlank()) AdBlockEngine.instance.getShieldState(stats.host) else ShieldHostState.Enabled
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AzokleRadius.md)
                    .background(if (isShieldActive) colors.statusSuccess.copy(alpha = 0.08f) else colors.surfaceElevated)
                    .border(
                        1.dp,
                        if (isShieldActive) colors.statusSuccess.copy(alpha = 0.22f) else colors.borderSubtle,
                        AzokleRadius.md
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isShieldActive) colors.statusSuccess else colors.statusDestructive)
                        )
                        Text(
                            text = when (currentShieldState) {
                                ShieldHostState.Enabled -> "Shield active for this site"
                                ShieldHostState.DisabledUntilRelaunch -> "Shield paused until app relaunch"
                                ShieldHostState.DisabledPermanently -> "Shield permanently disabled for site"
                            },
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isShieldActive) colors.textPrimary else colors.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (!isShieldActive && stats.host.isNotBlank()) {
                        Text(
                            text = if (currentShieldState == ShieldHostState.DisabledUntilRelaunch) "Permanent?" else "Session only?",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.statusAccent,
                            modifier = Modifier
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
            // 3. CORE METRICS: THIS SITE & ALL-TIME TOTAL
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AzokleRadius.lg)
                    .background(colors.surfaceElevated)
                    .border(1.dp, colors.borderSubtle, AzokleRadius.lg)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                // Section 1 Header: ON THIS SITE
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ON THIS SITE",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = colors.textMuted,
                        letterSpacing = 0.8.sp
                    )
                    if (stats.host.isNotBlank()) {
                        Text(
                            text = stats.host,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Current Site Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Site Ads Blocked
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .semantics(mergeDescendants = true) {
                                contentDescription = "Ads blocked on this site: ${stats.adsBlockedCount}"
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isShieldActive && animatedPageAds > 0)
                                            colors.statusSuccess.copy(alpha = 0.12f)
                                        else
                                            colors.cardBg
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Block,
                                    contentDescription = null,
                                    tint = if (isShieldActive && animatedPageAds > 0) colors.statusSuccess else colors.textMuted,
                                    modifier = Modifier.size(13.dp)
                                )
                            }

                            AnimatedContent(
                                targetState = animatedPageAds,
                                transitionSpec = {
                                    (slideInVertically { height -> height / 2 } + fadeIn(tween(180)))
                                        .togetherWith(slideOutVertically { height -> -height / 2 } + fadeOut(tween(140)))
                                },
                                label = "SiteAdsCountText"
                            ) { count ->
                                Text(
                                    text = "$count",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isShieldActive && animatedPageAds > 0) colors.textPrimary else colors.textMuted,
                                    maxLines = 1
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Ads Blocked",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textSecondary
                        )
                    }

                    // Vertical Separator
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(30.dp)
                            .background(colors.borderSubtle)
                    )

                    // Site Trackers Blocked
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .semantics(mergeDescendants = true) {
                                contentDescription = "Trackers blocked on this site: ${stats.trackersBlockedCount}"
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isShieldActive && animatedPageTrackers > 0)
                                            colors.statusTor.copy(alpha = 0.12f)
                                        else
                                            colors.cardBg
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    tint = if (isShieldActive && animatedPageTrackers > 0) colors.statusTor else colors.textMuted,
                                    modifier = Modifier.size(13.dp)
                                )
                            }

                            AnimatedContent(
                                targetState = animatedPageTrackers,
                                transitionSpec = {
                                    (slideInVertically { height -> height / 2 } + fadeIn(tween(180)))
                                        .togetherWith(slideOutVertically { height -> -height / 2 } + fadeOut(tween(140)))
                                },
                                label = "SiteTrackersCountText"
                            ) { count ->
                                Text(
                                    text = "$count",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isShieldActive && animatedPageTrackers > 0) colors.textPrimary else colors.textMuted,
                                    maxLines = 1
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Trackers Blocked",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Subtle divider separating site stats from all-time totals
                HorizontalDivider(
                    color = colors.borderSubtle.copy(alpha = 0.6f),
                    thickness = 0.8.dp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Section 2 Header: ALL-TIME TOTAL
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ALL-TIME PROTECTION",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = colors.textMuted,
                        letterSpacing = 0.8.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Lifetime Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Total Ads Blocked
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .semantics(mergeDescendants = true) {
                                contentDescription = "Total Ads Blocked: ${lifetimeStats.totalAdsBlocked}"
                            }
                    ) {
                        AnimatedContent(
                            targetState = formatCompactNumber(animatedLifetimeAds.toLong()),
                            transitionSpec = {
                                (slideInVertically { height -> height / 2 } + fadeIn(tween(180)))
                                    .togetherWith(slideOutVertically { height -> -height / 2 } + fadeOut(tween(140)))
                            },
                            label = "LifetimeAdsCountText"
                        ) { formattedCount ->
                            Text(
                                text = formattedCount,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (isShieldActive && animatedLifetimeAds > 0) colors.textPrimary else colors.textMuted,
                                maxLines = 1
                            )
                        }

                        Spacer(modifier = Modifier.height(1.dp))

                        Text(
                            text = "Total Ads Blocked",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Vertical Separator
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(24.dp)
                            .background(colors.borderSubtle)
                    )

                    // Total Trackers Blocked
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .semantics(mergeDescendants = true) {
                                contentDescription = "Total Trackers Blocked: ${lifetimeStats.trackersBlocked}"
                            }
                    ) {
                        AnimatedContent(
                            targetState = formatCompactNumber(animatedLifetimeTrackers.toLong()),
                            transitionSpec = {
                                (slideInVertically { height -> height / 2 } + fadeIn(tween(180)))
                                    .togetherWith(slideOutVertically { height -> -height / 2 } + fadeOut(tween(140)))
                            },
                            label = "LifetimeTrackersCountText"
                        ) { formattedCount ->
                            Text(
                                text = formattedCount,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (isShieldActive && animatedLifetimeTrackers > 0) colors.textPrimary else colors.textMuted,
                                maxLines = 1
                            )
                        }

                        Spacer(modifier = Modifier.height(1.dp))

                        Text(
                            text = "Total Trackers Blocked",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ==========================================
            // 4. ACTIVE PRIVACY PROTECTIONS
            // ==========================================
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "ACTIVE PROTECTIONS",
                    fontSize = 11.sp,
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
                        .background(colors.surfaceElevated)
                        .border(1.dp, colors.borderSubtle, AzokleRadius.lg)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    ProtectionFeatureItem(
                        icon = Icons.Default.Lock,
                        title = "HTTPS Enforcement",
                        desc = "Encrypted transport forced on all requests",
                        isActive = isShieldActive,
                        colors = colors
                    )

                    HorizontalDivider(
                        color = colors.borderSubtle,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 38.dp)
                    )

                    ProtectionFeatureItem(
                        icon = Icons.Default.Cookie,
                        title = "3rd-Party Cookie Isolation",
                        desc = "Cross-site advertising cookies dropped",
                        isActive = isShieldActive,
                        colors = colors
                    )

                    HorizontalDivider(
                        color = colors.borderSubtle,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 38.dp)
                    )

                    ProtectionFeatureItem(
                        icon = Icons.Default.AutoAwesome,
                        title = "Global Privacy Control (GPC)",
                        desc = "Do-Not-Track & opt-out signals broadcasted",
                        isActive = isShieldActive,
                        colors = colors
                    )
                }
            }

            // ==========================================
            // 5. BLOCKED DOMAINS BREAKDOWN
            // ==========================================
            val blockedList = stats.blockedDomainsList
            if (blockedList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(AzokleRadius.sm)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = colors.textPrimary.copy(alpha = 0.08f))
                            ) {
                                isBlockedListExpanded = !isBlockedListExpanded
                            }
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "BLOCKED REQUESTS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = colors.textMuted,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(colors.surfaceElevated)
                                    .padding(horizontal = 7.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "${blockedList.size}",
                                    fontSize = 10.5.sp,
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

                    Spacer(modifier = Modifier.height(6.dp))

                    AnimatedVisibility(
                        visible = isBlockedListExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
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
                                                .padding(horizontal = 12.dp, vertical = 7.dp),
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
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
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
                                                .padding(horizontal = 12.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Block,
                                                contentDescription = null,
                                                tint = colors.statusDestructive.copy(alpha = 0.8f),
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = domain,
                                                fontSize = 12.sp,
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
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "No invasive trackers or ads detected yet on this page.",
                    fontSize = 11.5.sp,
                    color = colors.textMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                )
            }

            if (stats.host.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = colors.borderSubtle, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AzokleRadius.md)
                        .background(colors.surfaceElevated.copy(alpha = 0.5f))
                        .clickable {
                            if (isShieldActive) {
                                AdBlockEngine.instance.pauseShieldUntilRelaunch(stats.host)
                                onShieldToggled()
                                com.azokle.search.common.components.ToastManager.show("Protections paused for this session 🛡️")
                            } else {
                                AdBlockEngine.instance.enableShield(stats.host)
                                onShieldToggled()
                                com.azokle.search.common.components.ToastManager.show("Protections re-enabled 🛡️")
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isShieldActive) "Website not working properly?" else "Protections paused for this session",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = if (isShieldActive) "Tap to pause protections for this session and reload" else "Tap to re-enable Azokle Shield",
                            fontSize = 11.sp,
                            color = colors.textMuted
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Fix Site",
                        tint = if (isShieldActive) colors.statusWarning else colors.statusSuccess,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProtectionFeatureItem(
    icon: ImageVector,
    title: String,
    desc: String,
    isActive: Boolean,
    colors: com.azokle.search.common.theme.AzokleThemeColors
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
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
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
            Text(
                text = desc,
                fontSize = 11.sp,
                color = colors.textMuted,
                lineHeight = 14.sp
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
