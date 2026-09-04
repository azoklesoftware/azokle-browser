package com.azokle.search.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azokle.search.data.SEARCH_ENGINE_PROVIDERS
import com.azokle.search.ui.theme.AzokleRadius
import com.azokle.search.ui.theme.AzokleTheme

/**
 * Modern, interactive search engine icon and selector dropdown.
 * Displays the active search engine (Azokle, DuckDuckGo, Brave, Startpage)
 * and allows rapid one-tap switching via an anchored dropdown menu.
 */
@Composable
fun SearchEngineIconDropdown(
    selectedEngineId: String,
    onEngineSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 20.dp,
    showDropdownArrow: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    val colors = AzokleTheme.colors
    val haptic = LocalHapticFeedback.current

    val currentEngine = remember(selectedEngineId) {
        SEARCH_ENGINE_PROVIDERS.find { it.id.equals(selectedEngineId, ignoreCase = true) }
            ?: SEARCH_ENGINE_PROVIDERS[0]
    }

    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "engineArrowRotation"
    )

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, radius = 24.dp, color = colors.textPrimary.copy(alpha = 0.15f)),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        expanded = !expanded
                    }
                )
                .padding(horizontal = 4.dp, vertical = 4.dp)
                .semantics {
                    role = Role.Button
                    contentDescription = "Current search engine: ${currentEngine.name}. Click to change."
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Engine Brand Icon
            if (currentEngine.iconRes != 0) {
                Image(
                    painter = painterResource(id = currentEngine.iconRes),
                    contentDescription = currentEngine.name,
                    modifier = Modifier
                        .size(iconSize)
                        .clip(CircleShape),
                    contentScale = ContentScale.Fit
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = currentEngine.name,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(iconSize)
                )
            }

            if (showDropdownArrow) {
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = colors.textMuted,
                    modifier = Modifier
                        .size(16.dp)
                        .rotate(arrowRotation)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(220.dp)
                .background(colors.surfaceElevated)
                .border(1.dp, colors.border, AzokleRadius.md)
        ) {
            SEARCH_ENGINE_PROVIDERS.forEach { engine ->
                val isSelected = engine.id.equals(selectedEngineId, ignoreCase = true)
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (engine.iconRes != 0) {
                                Image(
                                    painter = painterResource(id = engine.iconRes),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Text(
                                text = engine.name,
                                color = if (isSelected) colors.statusAccent else colors.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = colors.statusAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        expanded = false
                        if (!isSelected) {
                            onEngineSelected(engine.id)
                            ToastManager.show("Search engine: ${engine.name}")
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }
    }
}
