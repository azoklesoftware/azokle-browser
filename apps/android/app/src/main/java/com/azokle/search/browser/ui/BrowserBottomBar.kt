package com.azokle.search.browser.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azokle.search.ui.theme.AzokleRadius
import com.azokle.search.ui.theme.AzokleTheme
import com.azokle.search.ui.theme.adaptiveContentWidth

@Composable
fun BrowserBottomBar(
    openTabsCount: Int,
    onHomeClick: () -> Unit,
    onSearchClick: () -> Unit,
    onTabsClick: () -> Unit,
    onBurnDataClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    val colors = AzokleTheme.colors
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.cardBg)
            .border(1.dp, colors.border)
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .adaptiveContentWidth(580.dp)
                .height(58.dp)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Home Button (48dp Touch Target)
        Box(
            modifier = Modifier
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false, radius = 22.dp, color = colors.textPrimary),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onHomeClick()
                    }
                )
                .semantics {
                    role = Role.Button
                    contentDescription = "Home"
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = null,
                tint = colors.textPrimary,
                modifier = Modifier.size(23.dp)
            )
        }

        // 2. Search Button (48dp Touch Target)
        Box(
            modifier = Modifier
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false, radius = 22.dp, color = colors.textPrimary),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSearchClick()
                    }
                )
                .semantics {
                    role = Role.Button
                    contentDescription = "Search"
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = colors.textPrimary,
                modifier = Modifier.size(23.dp)
            )
        }

        // 3. Tab Switcher Badge Button (48dp Touch Target with Scale/Fade Transition)
        Box(
            modifier = Modifier
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false, radius = 22.dp, color = colors.textPrimary),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onTabsClick()
                    }
                )
                .semantics {
                    role = Role.Button
                    contentDescription = "Active Tabs, $openTabsCount open"
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = openTabsCount,
                    transitionSpec = {
                        val enter = fadeIn(animationSpec = tween(180)) + scaleIn(initialScale = 0.8f, animationSpec = tween(180))
                        val exit = fadeOut(animationSpec = tween(140)) + scaleOut(targetScale = 1.2f, animationSpec = tween(140))
                        enter togetherWith exit
                    },
                    label = "tabCountAnim"
                ) { count ->
                    Text(
                        text = "$count",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = colors.textPrimary
                    )
                }
            }
        }

        // 4. DuckDuckGo-Style Flame / Incinerator Button (48dp Touch Target)
        Box(
            modifier = Modifier
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false, radius = 22.dp, color = colors.statusDestructive),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onBurnDataClick()
                    }
                )
                .semantics {
                    role = Role.Button
                    contentDescription = "Burn Data and close tabs"
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = null,
                tint = colors.statusDestructive,
                modifier = Modifier.size(24.dp)
            )
        }

        // 5. 3-Dots Action Menu Trigger (48dp Touch Target)
        Box(
            modifier = Modifier
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false, radius = 22.dp, color = colors.textPrimary),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onMenuClick()
                    }
                )
                .semantics {
                    role = Role.Button
                    contentDescription = "Browser Menu"
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = null,
                tint = colors.textPrimary,
                modifier = Modifier.size(23.dp)
            )
        }
    }
}
}
