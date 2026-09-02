package com.azokle.search.browser.ui.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.azokle.search.browser.tabs.TabGroup
import com.azokle.search.browser.tabs.TabManager
import com.azokle.search.browser.tabs.TabSession
import com.azokle.search.ui.theme.AzokleRadius
import com.azokle.search.ui.theme.AzokleTheme
import com.azokle.search.ui.theme.adaptiveContentWidth

/**
 * Tab Group Bottom Favicon Switcher Strip:
 *  - Appears right above the bottom navigation bar when the active tab belongs to a group of 2+ tabs.
 *  - Renders interactive domain favicon circles for instant tab switching.
 *  - Displays top-right 'x' close badge on the active favicon circle to close that specific tab.
 *  - Ends with a circular '+' button to open a new home tab in the group.
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

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
        ) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        val groupColor = remember(group) {
            if (group != null) Color(group.color) else colors.statusAccent
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface.copy(alpha = 0.96f))
                .border(1.dp, colors.border),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .adaptiveContentWidth(580.dp)
                    .height(48.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. Group Overview Trigger Button (Chevron Up)
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(groupColor.copy(alpha = 0.15f))
                        .border(1.dp, groupColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = groupColor),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onOpenGroupOverview()
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Group Overview: ${group?.title}",
                        tint = groupColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 2. Favicon Icons Horizontal Switcher List with Close Badge + End Add Button
                LazyRow(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
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

                    // "+" Add Tab in Group Button (Right Side of Strip)
                    item {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(colors.cardBg)
                                .border(1.dp, colors.border, CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = false, radius = 18.dp, color = colors.textPrimary),
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

@Composable
private fun TabFaviconToken(
    tab: TabSession,
    isActive: Boolean,
    groupColor: Color,
    onClick: () -> Unit,
    onClose: () -> Unit
) {
    val colors = AzokleTheme.colors

    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.06f else 0.94f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "faviconScale"
    )

    Box(
        modifier = Modifier
            .padding(top = 4.dp, end = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        // Main Favicon Circular Button
        Box(
            modifier = Modifier
                .size(36.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(if (isActive) groupColor.copy(alpha = 0.22f) else colors.cardBg)
                .border(
                    width = if (isActive) 2.dp else 1.dp,
                    color = if (isActive) groupColor else colors.border,
                    shape = CircleShape
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false, radius = 20.dp, color = groupColor),
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

        // 'x' Close Badge at Top-Right of the Favicon Circle (matches user screenshot)
        if (isActive) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 5.dp, y = (-5).dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(groupColor)
                    .border(1.5.dp, colors.surface, CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false, radius = 10.dp, color = colors.statusDestructive),
                        onClick = onClose
                    )
                    .semantics {
                        role = Role.Button
                        contentDescription = "Close tab: ${tab.title}"
                    },
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

