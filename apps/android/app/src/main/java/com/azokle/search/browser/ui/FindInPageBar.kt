package com.azokle.search.browser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azokle.search.ui.theme.AzokleRadius
import com.azokle.search.ui.theme.AzokleTheme

@Composable
fun FindInPageBar(
    query: String,
    currentMatchIndex: Int,
    totalMatches: Int,
    onQueryChange: (String) -> Unit,
    onNextMatch: () -> Unit,
    onPreviousMatch: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    includeStatusBarPadding: Boolean = true
) {
    val colors = AzokleTheme.colors
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(if (includeStatusBarPadding) Modifier.statusBarsPadding() else Modifier)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        val barShape = AzokleRadius.full
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(barShape)
                .background(colors.modalBg)
                .border(1.dp, colors.border, barShape)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Search Text Field
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                textStyle = TextStyle(
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(colors.actionPrimaryBg),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { onNextMatch() }),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(
                            text = "Find in page...",
                            color = colors.textMuted,
                            fontSize = 14.sp
                        )
                    }
                    innerTextField()
                }
            )

            // Match Count Indicator
            if (query.isNotEmpty()) {
                val matchText = if (totalMatches > 0) {
                    "$currentMatchIndex of $totalMatches"
                } else {
                    "0 of 0"
                }
                Text(
                    text = matchText,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    color = if (totalMatches > 0) colors.statusAccent else colors.statusDestructive,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )

                // Clear Query Button (48dp touch delegate)
                Box(
                    modifier = Modifier
                        .sizeIn(minWidth = 36.dp, minHeight = 44.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = false, radius = 16.dp, color = colors.textSecondary),
                            onClick = { onQueryChange("") }
                        )
                        .semantics {
                            role = Role.Button
                            contentDescription = "Clear find query"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = colors.textMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(2.dp))

            // Previous Button (Up Arrow) (48dp touch delegate)
            Box(
                modifier = Modifier
                    .sizeIn(minWidth = 38.dp, minHeight = 44.dp)
                    .clip(CircleShape)
                    .clickable(
                        enabled = totalMatches > 0,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false, radius = 18.dp, color = colors.textPrimary),
                        onClick = onPreviousMatch
                    )
                    .semantics {
                        role = Role.Button
                        contentDescription = "Previous match"
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = null,
                    tint = if (totalMatches > 0) colors.textPrimary else colors.textMuted.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Next Button (Down Arrow) (48dp touch delegate)
            Box(
                modifier = Modifier
                    .sizeIn(minWidth = 38.dp, minHeight = 44.dp)
                    .clip(CircleShape)
                    .clickable(
                        enabled = totalMatches > 0,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false, radius = 18.dp, color = colors.textPrimary),
                        onClick = onNextMatch
                    )
                    .semantics {
                        role = Role.Button
                        contentDescription = "Next match"
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = if (totalMatches > 0) colors.textPrimary else colors.textMuted.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Close Find In Page (48dp touch delegate)
            Box(
                modifier = Modifier
                    .sizeIn(minWidth = 38.dp, minHeight = 44.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false, radius = 18.dp, color = colors.textPrimary),
                        onClick = onClose
                    )
                    .semantics {
                        role = Role.Button
                        contentDescription = "Close find in page"
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = colors.textPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
