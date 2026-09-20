package com.azokle.search.tabs

import com.azokle.search.common.components.FaviconImage
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.azokle.search.common.theme.AzokleTheme
import com.azokle.search.common.theme.adaptiveContentWidth

/**
 * High-Performance Sovereign Tab Group Bottom Switcher Strip:
 *  - Floats smoothly right above the bottom navigation bar when the active tab belongs to a group.
 *  - Glassmorphic top-rounded container with ambient group color accentuation.
 *  - Auto-scrolling horizontal favicon carousel with instant active tab centering.
 *  - Brave/Chrome-style active indicator pill & spring physics.
 *  - Integrated tactile haptics, quick close badge, and one-tap group overview expansion.
 */
@Composable
fun TabGroupBottomStrip(
    group: TabGroup?,
    groupTabs: List<TabSession>,
    activeTabId: String?,
    onTabSelected: (TabSession) -> Unit,
    onAddTabInGroup: () -> Unit,
    onCloseTab: (String) -> Unit,
    onOpenGroupOverview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isVisible = group != null && groupTabs.isNotEmpty()
    val colors = AzokleTheme.colors
    val haptic = LocalHapticFeedback.current
    val listState = rememberLazyListState()

    // Smoothly glide to the active tab whenever selection changes
    LaunchedEffect(activeTabId) {
        val activeIndex = groupTabs.indexOfFirst { it.id == activeTabId }
        if (activeIndex in groupTabs.indices) {
            runCatching {
                listState.animateScrollToItem(activeIndex)
            }
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + fadeIn(animationSpec = tween(200)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(180, easing = FastOutLinearInEasing)
        ) + fadeOut(animationSpec = tween(160)),
        modifier = modifier
    ) {
        val groupColor = remember(group) {
            if (group != null) Color(group.color) else colors.statusAccent
        }

        val stripShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        val bgBrush = Brush.verticalGradient(
            colors = listOf(
                colors.surfaceElevated.copy(alpha = 0.98f),
                colors.surface.copy(alpha = 0.98f)
            )
        )
        val borderBrush = Brush.verticalGradient(
            colors = listOf(
                groupColor.copy(alpha = 0.45f),
                colors.border.copy(alpha = 0.55f)
            )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(stripShape)
                .background(bgBrush)
                .border(width = 1.dp, brush = borderBrush, shape = stripShape),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .adaptiveContentWidth(580.dp)
                    .height(52.dp)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. Group Overview Trigger Chip (Interactive Pill with Group Accent)
                val groupInteractionSource = remember { MutableInteractionSource() }
                val isGroupPressed by groupInteractionSource.collectIsPressedAsState()
                val groupScale by animateFloatAsState(
                    targetValue = if (isGroupPressed) 0.92f else 1.0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "groupScale"
                )

                Box(
                    modifier = Modifier
                        .scale(groupScale)
                        .clip(RoundedCornerShape(10.dp))
                        .background(groupColor.copy(alpha = 0.16f))
                        .border(1.dp, groupColor.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                        .clickable(
                            interactionSource = groupInteractionSource,
                            indication = ripple(bounded = true, color = groupColor),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onOpenGroupOverview()
                            }
                        )
                        .semantics {
                            role = Role.Button
                            contentDescription = "Group Overview: ${group?.title ?: "Group"}"
                        }
                        .padding(horizontal = 7.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Group Color Dot
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(groupColor)
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = null,
                            tint = groupColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 2. Favicon Carousel Switcher with Auto-Scroll & High-Precision Active Badges
                LazyRow(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 2.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(groupTabs, key = { it.id }) { tab ->
                        val isActive = tab.id == activeTabId
                        TabFaviconToken(
                            tab = tab,
                            isActive = isActive,
                            groupColor = groupColor,
                            onClick = {
                                if (!isActive) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onTabSelected(tab)
                                }
                            },
                            onClose = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onCloseTab(tab.id)
                            }
                        )
                    }

                    // 3. "+" Add Tab in Group Button (Right Side of Carousel)
                    item(key = "tab_group_add_tab_button") {
                        val addInteractionSource = remember { MutableInteractionSource() }
                        val isAddPressed by addInteractionSource.collectIsPressedAsState()
                        val addScale by animateFloatAsState(
                            targetValue = if (isAddPressed) 0.88f else 1.0f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "addScale"
                        )

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .scale(addScale)
                                .clip(CircleShape)
                                .background(groupColor.copy(alpha = 0.12f))
                                .border(1.dp, groupColor.copy(alpha = 0.35f), CircleShape)
                                .clickable(
                                    interactionSource = addInteractionSource,
                                    indication = ripple(bounded = true, color = groupColor),
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onAddTabInGroup()
                                    }
                                )
                                .semantics {
                                    role = Role.Button
                                    contentDescription = "Add tab in group"
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = groupColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabFaviconToken(
    tab: TabSession,
    isActive: Boolean,
    groupColor: Color,
    onClick: () -> Unit,
    onClose: () -> Unit
) {
    val colors = AzokleTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.90f
            isActive -> 1.06f
            else -> 0.94f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "faviconScale"
    )

    val borderWidth by animateDpAsState(
        targetValue = if (isActive) 2.dp else 1.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy),
        label = "tokenBorderWidth"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isActive) groupColor else colors.border.copy(alpha = 0.6f),
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy),
        label = "tokenBorderColor"
    )

    val bgColor by animateColorAsState(
        targetValue = if (isActive) groupColor.copy(alpha = 0.22f) else colors.cardBg,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy),
        label = "tokenBgColor"
    )

    Box(
        modifier = Modifier
            .padding(top = 4.dp, bottom = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        // Main Favicon Token Circle
        Box(
            modifier = Modifier
                .size(38.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(bgColor)
                .border(width = borderWidth, color = borderColor, shape = CircleShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(bounded = false, radius = 22.dp, color = groupColor),
                    onClick = onClick
                )
                .semantics {
                    role = Role.Tab
                    selected = isActive
                    contentDescription = "${tab.title}, ${if (isActive) "active" else "inactive"}"
                },
            contentAlignment = Alignment.Center
        ) {
            FaviconImage(
                url = tab.url,
                faviconUrl = tab.faviconUrl,
                isTor = tab.isTor,
                size = 22.dp
            )
        }

        // Active Bottom Indicator Pill
        if (isActive) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 5.dp)
                    .size(width = 14.dp, height = 3.dp)
                    .clip(CircleShape)
                    .background(groupColor)
            )
        }

        // 'x' Close Badge at Top-Right (with comfortable 24dp touch target)
        androidx.compose.animation.AnimatedVisibility(
            visible = isActive,
            enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 6.dp, y = (-6).dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false, radius = 12.dp, color = colors.statusDestructive),
                        onClick = onClose
                    )
                    .semantics {
                        role = Role.Button
                        contentDescription = "Close tab: ${tab.title}"
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(groupColor)
                        .border(1.5.dp, colors.surface, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }
    }
}
