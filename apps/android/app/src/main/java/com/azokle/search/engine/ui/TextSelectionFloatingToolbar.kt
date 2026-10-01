package com.azokle.search.engine.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azokle.search.common.components.ToastManager
import com.azokle.search.common.theme.AzokleRadius
import com.azokle.search.common.theme.AzokleTheme
import com.azokle.search.common.util.ShareHelper
import com.azokle.search.engine.TextSelectionPayload

/**
 * Ultra-Minimalist Floating Context Action Toolbar for Web Text Selection.
 * Features:
 * - Dynamic viewport positioning with collision avoidance.
 * - Clean minimalist floating pill container with subtle borders.
 * - Concise, single-line actions with haptic micro-feedback.
 * - Spring entrance/exit transitions.
 */
@Composable
fun TextSelectionFloatingToolbar(
    payload: TextSelectionPayload?,
    onSearchRequested: (String) -> Unit,
    onShareRequested: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isVisible = payload != null && (
        (payload.text.trim().isNotEmpty() && (payload.canCopy || payload.canSearch || payload.canShare || payload.canCut)) ||
        (payload.isEditable && (payload.canPaste || payload.canSelectAll || payload.canCut))
    )

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(spring(stiffness = 550f, dampingRatio = 0.78f)) +
                scaleIn(initialScale = 0.88f, animationSpec = spring(stiffness = 550f, dampingRatio = 0.78f)) +
                slideInVertically(initialOffsetY = { -16 }),
        exit = fadeOut(spring(stiffness = 700f)) + scaleOut(targetScale = 0.88f),
        modifier = modifier
    ) {
        if (payload == null) return@AnimatedVisibility

        val context = LocalContext.current
        val haptic = LocalHapticFeedback.current
        val density = LocalDensity.current
        val colors = AzokleTheme.colors

        var toolbarSize by remember { mutableStateOf(IntSize.Zero) }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val screenWidthPx = with(density) { maxWidth.toPx() }
            val screenHeightPx = with(density) { maxHeight.toPx() }

            val rect = payload.screenRect
            val hasValidRect = rect.width() > 0 || rect.height() > 0 || rect.top > 0 || rect.left > 0

            val horizontalMarginPx = with(density) { 14.dp.toPx() }
            val topInsetPx = with(density) { 68.dp.toPx() }
            val bottomInsetPx = with(density) { 76.dp.toPx() }
            val verticalGapPx = with(density) { 10.dp.toPx() }

            val actualToolbarWidthPx = if (toolbarSize.width > 0) toolbarSize.width.toFloat() else with(density) { 260.dp.toPx() }
            val actualToolbarHeightPx = if (toolbarSize.height > 0) toolbarSize.height.toFloat() else with(density) { 44.dp.toPx() }

            // 1. Precise Horizontal Centering & Screen Clamping
            val rectCenterX = if (hasValidRect) rect.centerX() else screenWidthPx / 2f
            val targetXPx = rectCenterX - (actualToolbarWidthPx / 2f)
            val maxXPx = (screenWidthPx - actualToolbarWidthPx - horizontalMarginPx).coerceAtLeast(horizontalMarginPx)
            val clampedXPx = targetXPx.coerceIn(horizontalMarginPx, maxXPx).toInt()

            // 2. Adaptive Vertical Placement: Above selection if space permits, otherwise below
            val targetYPx = if (hasValidRect) {
                if (rect.top > (topInsetPx + actualToolbarHeightPx + verticalGapPx)) {
                    rect.top - actualToolbarHeightPx - verticalGapPx
                } else {
                    rect.bottom + verticalGapPx
                }
            } else {
                topInsetPx + with(density) { 50.dp.toPx() }
            }

            val maxYPx = (screenHeightPx - actualToolbarHeightPx - bottomInsetPx).coerceAtLeast(topInsetPx)
            val clampedYPx = targetYPx.coerceIn(topInsetPx, maxYPx).toInt()

            val scrollState = rememberScrollState()
            val maxAvailableWidthDp = (maxWidth - 28.dp).coerceAtLeast(200.dp)

            Box(
                modifier = Modifier
                    .offset { IntOffset(clampedXPx, clampedYPx) }
                    .onSizeChanged { toolbarSize = it }
                    .widthIn(max = maxAvailableWidthDp)
                    .shadow(
                        elevation = 8.dp,
                        shape = CircleShape,
                        spotColor = Color.Black.copy(alpha = 0.25f),
                        ambientColor = Color.Black.copy(alpha = 0.15f)
                    )
                    .clip(CircleShape)
                    .background(colors.surfaceElevated)
                    .border(
                        width = 0.8.dp,
                        color = colors.borderSubtle,
                        shape = CircleShape
                    )
                    .padding(horizontal = 5.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.horizontalScroll(scrollState),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // 1. Search Action
                    if (payload.canSearch) {
                        ToolbarActionButton(
                            icon = Icons.Default.Search,
                            label = "Search",
                            isPrimary = true,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val query = payload.text.trim()
                                if (query.isNotBlank()) {
                                    onSearchRequested(query)
                                }
                                payload.onDismiss()
                            }
                        )
                    }

                    // 2. Copy Action
                    if (payload.canCopy) {
                        ToolbarActionButton(
                            icon = Icons.Default.ContentCopy,
                            label = "Copy",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                payload.onCopy()
                                ToastManager.showCopy("Copied")
                                payload.onDismiss()
                            }
                        )
                    }

                    // 3. Share Action
                    if (payload.canShare) {
                        ToolbarActionButton(
                            icon = Icons.Default.Share,
                            label = "Share",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val shareText = payload.text
                                if (shareText.isNotBlank()) {
                                    if (onShareRequested != null) {
                                        onShareRequested(shareText)
                                    } else {
                                        ShareHelper.launchSystemShare(
                                            context = context,
                                            text = shareText,
                                            chooserTitle = "Share text"
                                        )
                                    }
                                }
                                payload.onDismiss()
                            }
                        )
                    }

                    // 4. Select All Action
                    if (payload.canSelectAll) {
                        ToolbarActionButton(
                            icon = Icons.Default.SelectAll,
                            label = "Select All",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                payload.onSelectAll()
                            }
                        )
                    }

                    // 5. Cut Action
                    if (payload.canCut) {
                        ToolbarActionButton(
                            icon = Icons.Default.ContentCut,
                            label = "Cut",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                payload.onCut()
                                ToastManager.showCopy("Cut")
                                payload.onDismiss()
                            }
                        )
                    }

                    // 6. Paste Action
                    if (payload.canPaste) {
                        ToolbarActionButton(
                            icon = Icons.Default.ContentPaste,
                            label = "Paste",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                payload.onPaste()
                                payload.onDismiss()
                            }
                        )
                    }

                    // Vertical Divider
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .height(16.dp)
                            .width(0.5.dp)
                            .background(colors.borderSubtle)
                    )

                    // 7. Dismiss Close Button
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = false, radius = 14.dp, color = colors.textSecondary),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    payload.onDismiss()
                                }
                            )
                            .semantics {
                                role = Role.Button
                                contentDescription = "Dismiss toolbar"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = colors.textMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolbarActionButton(
    icon: ImageVector,
    label: String,
    isPrimary: Boolean = false,
    onClick: () -> Unit
) {
    val colors = AzokleTheme.colors

    val bgModifier = if (isPrimary) {
        Modifier
            .clip(CircleShape)
            .background(colors.actionPrimaryBg.copy(alpha = 0.12f))
            .border(0.8.dp, colors.actionPrimaryBg.copy(alpha = 0.3f), CircleShape)
    } else {
        Modifier.clip(CircleShape)
    }

    val iconTint = if (isPrimary) colors.actionPrimaryText else colors.textPrimary
    val textTint = if (isPrimary) colors.actionPrimaryText else colors.textPrimary

    Row(
        modifier = Modifier
            .height(32.dp)
            .then(bgModifier)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, radius = 20.dp, color = colors.textPrimary.copy(alpha = 0.08f)),
                onClick = onClick
            )
            .padding(horizontal = if (isPrimary) 9.dp else 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = label,
            color = textTint,
            fontSize = 12.sp,
            fontWeight = if (isPrimary) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}
