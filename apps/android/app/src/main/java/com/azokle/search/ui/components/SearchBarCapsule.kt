package com.azokle.search.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azokle.search.ui.theme.AzokleRadius
import com.azokle.search.ui.theme.AzokleTheme

@Composable
fun SearchBarCapsule(
    query: String,
    placeholderText: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onVoiceClick: () -> Unit,
    onCapsuleClick: () -> Unit,
    modifier: Modifier = Modifier,
    isReadOnly: Boolean = false,
    selectedEngineId: String = "azokle",
    onEngineSelected: (String) -> Unit = {},
    searchEngineModifier: Modifier = Modifier
) {
    val colors = AzokleTheme.colors
    val keyboardController = LocalSoftwareKeyboardController.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = if (isPressed) 0.985f else 1.0f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(
                elevation = if (colors.isDark) 12.dp else 6.dp,
                shape = AzokleRadius.full,
                ambientColor = colors.textPrimary.copy(alpha = if (colors.isDark) 0.08f else 0.04f),
                spotColor = colors.textPrimary.copy(alpha = if (colors.isDark) 0.12f else 0.08f)
            )
            .clip(AzokleRadius.full)
            .background(colors.cardBg.copy(alpha = 0.95f))
            .border(
                width = 1.dp,
                color = colors.borderLight,
                shape = AzokleRadius.full
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = colors.textPrimary.copy(alpha = 0.12f)),
                onClick = onCapsuleClick
            )
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .semantics {
                role = Role.Button
                contentDescription = if (query.isEmpty()) placeholderText else "Search query: $query"
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Search Engine Selector Icon (Azokle Search, DuckDuckGo, Brave, Startpage)
            SearchEngineIconDropdown(
                selectedEngineId = selectedEngineId,
                onEngineSelected = onEngineSelected,
                iconSize = 20.dp,
                showDropdownArrow = true,
                modifier = Modifier
                    .padding(start = 2.dp)
                    .then(searchEngineModifier)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Input / Placeholder Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (isReadOnly) {
                    Text(
                        text = query.ifEmpty { placeholderText },
                        color = if (query.isEmpty()) colors.textMuted else colors.textPrimary,
                        fontSize = 15.sp,
                        maxLines = 1
                    )
                } else {
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = TextStyle(
                            color = colors.textPrimary,
                            fontSize = 15.sp
                        ),
                        cursorBrush = SolidColor(colors.textPrimary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                keyboardController?.hide()
                                onSearch(query)
                            }
                        ),
                        decorationBox = { innerTextField ->
                            if (query.isEmpty()) {
                                Text(
                                    text = placeholderText,
                                    color = colors.textMuted,
                                    fontSize = 15.sp,
                                    maxLines = 1
                                )
                            }
                            innerTextField()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Clear Button (WCAG 48dp Touch Target)
            AnimatedVisibility(
                visible = query.isNotEmpty(),
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Box(
                    modifier = Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = false, radius = 20.dp, color = colors.textSecondary),
                            onClick = { onQueryChange("") }
                        )
                        .semantics {
                            role = Role.Button
                            contentDescription = "Clear search text"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Divider
            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .width(1.dp)
                    .height(18.dp)
                    .background(colors.border)
            )

            // Voice Search Button (WCAG 48dp Touch Target)
            Box(
                modifier = Modifier
                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false, radius = 20.dp, color = colors.textPrimary),
                        onClick = onVoiceClick
                    )
                    .semantics {
                        role = Role.Button
                        contentDescription = "Voice search"
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = colors.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
