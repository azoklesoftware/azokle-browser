package com.azokle.search.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azokle.search.ui.theme.AzokleRadius
import com.azokle.search.ui.theme.AzokleTheme
import com.azokle.search.ui.theme.adaptiveContentWidth
import com.azokle.search.ui.theme.rememberWindowSize

@Composable
fun AppBottomNavBar(
    currentTab: AppNavTab,
    onTabSelected: (AppNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AzokleTheme.colors
    val haptic = LocalHapticFeedback.current
    val barShape = RoundedCornerShape(32.dp)

    val windowSize = rememberWindowSize()
    val isCompactWidth = windowSize.widthDp < 360

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .adaptiveContentWidth(500.dp)
                .shadow(
                    elevation = if (colors.isDark) 20.dp else 10.dp,
                    shape = barShape,
                    ambientColor = if (colors.isDark) Color(0x33FFFFFF) else Color(0x1A000000),
                    spotColor = if (colors.isDark) Color(0x44FFFFFF) else Color(0x22000000)
                )
                .clip(barShape)
                .background(
                    if (colors.isDark) {
                        Brush.verticalGradient(
                            listOf(Color(0xF51C1C1E), Color(0xF8121214))
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(Color(0xF8FFFFFF), Color(0xF2F1F5F9))
                        )
                    }
                )
                .border(
                    width = 1.dp,
                    brush = if (colors.isDark) {
                        Brush.verticalGradient(
                            listOf(Color(0x33FFFFFF), Color(0x10FFFFFF))
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(Color(0x60E2E8F0), Color(0x20CBD5E1))
                        )
                    },
                    shape = barShape
                )
                .padding(horizontal = if (isCompactWidth) 4.dp else 6.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppNavTab.entries.forEach { tab ->
                val isSelected = currentTab == tab
                val label = stringResource(tab.titleRes)
                val interactionSource = remember { MutableInteractionSource() }

                val itemBgColor by animateColorAsState(
                    targetValue = if (isSelected) colors.actionPrimaryBg else Color.Transparent,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessHigh
                    ),
                    label = "tabBg"
                )

                val iconTint by animateColorAsState(
                    targetValue = if (isSelected) colors.actionPrimaryText else colors.textSecondary,
                    animationSpec = spring(stiffness = Spring.StiffnessHigh),
                    label = "tabIcon"
                )

                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.0f else 0.95f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessHigh
                    ),
                    label = "tabScale"
                )

                Box(
                    modifier = Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .scale(scale)
                        .clip(AzokleRadius.full)
                        .background(itemBgColor)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = ripple(
                                bounded = true,
                                color = if (isSelected) colors.actionPrimaryText.copy(alpha = 0.2f) else colors.textPrimary.copy(alpha = 0.12f)
                            ),
                            onClick = {
                                if (!isSelected) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onTabSelected(tab)
                                }
                            }
                        )
                        .padding(
                            horizontal = if (isSelected) 14.dp else 10.dp,
                            vertical = 8.dp
                        )
                        .semantics {
                            role = Role.Tab
                            selected = isSelected
                            contentDescription = if (isSelected) "$label, Selected" else label
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(22.dp)
                        )

                        if (isSelected) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = label,
                                color = colors.actionPrimaryText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
