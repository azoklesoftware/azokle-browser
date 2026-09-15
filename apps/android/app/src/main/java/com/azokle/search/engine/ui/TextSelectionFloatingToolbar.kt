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
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azokle.search.common.components.ToastManager
import com.azokle.search.common.theme.AzokleTheme
import com.azokle.search.engine.TextSelectionPayload

/**
 * Ultra-Premium Floating Context Action Toolbar for Web Text Selection.
 * Features:
 * - Pixel-perfect dynamic viewport positioning with collision avoidance (status bar, navigation bar, top bars).
 * - Frosted glassmorphism capsule with accent lighting borders.
 * - Branded "Search with Azokle" prominent action.
 * - Copy, Share, Select All, Cut, and Paste with silky haptic micro-feedback.
 * - Spring physics entrance and exit transitions.
 */
@Composable
fun TextSelectionFloatingToolbar(
    payload: TextSelectionPayload?,
    onSearchRequested: (String) -> Unit,
    onShareRequested: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isVisible = payload != null && (
        payload.text.isNotBlank() ||
        payload.screenRect.width() > 0 ||
        payload.canPaste ||
        payload.canSelectAll
    )

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(spring(stiffness = 550f, dampingRatio = 0.78f)) +
                scaleIn(initialScale = 0.85f, animationSpec = spring(stiffness = 550f, dampingRatio = 0.78f)) +
                slideInVertically(initialOffsetY = { -20 }),
        exit = fadeOut(spring(stiffness = 700f)) + scaleOut(targetScale = 0.85f),
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
            val verticalGapPx = with(density) { 12.dp.toPx() }

            val actualToolbarWidthPx = if (toolbarSize.width > 0) toolbarSize.width.toFloat() else with(density) { 280.dp.toPx() }
            val actualToolbarHeightPx = if (toolbarSize.height > 0) toolbarSize.height.toFloat() else with(density) { 48.dp.toPx() }

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
                topInsetPx + with(density) { 60.dp.toPx() }
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
                        elevation = 16.dp,
                        shape = RoundedCornerShape(24.dp),
                        spotColor = colors.statusAccent.copy(alpha = 0.35f),
                        ambientColor = Color.Black.copy(alpha = 0.35f)
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .background(colors.surfaceElevated)
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                colors.statusAccent.copy(alpha = 0.35f),
                                colors.border.copy(alpha = 0.8f),
                                colors.statusAccent.copy(alpha = 0.15f)
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 5.dp)
            ) {
                Row(
                    modifier = Modifier.horizontalScroll(scrollState),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    // 1. Prominent "Search with Azokle" Action (strictly suppressed on password fields)
                    if (payload.canSearch) {
                        ToolbarActionButton(
                            icon = Icons.Default.Search,
                            label = "Search",
                            isPrimary = true,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                payload.onCopy()
                                ToastManager.showCopy("Copied to clipboard")
                                payload.onDismiss()
                            }
                        )
                    }

                    // 3. Share Action (strictly suppressed on password fields)
                    if (payload.canShare) {
                        ToolbarActionButton(
                            icon = Icons.Default.Share,
                            label = "Share",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val shareText = payload.text
                                if (shareText.isNotBlank()) {
                                    if (onShareRequested != null) {
                                        onShareRequested(shareText)
                                    } else {
                                        com.azokle.search.common.util.ShareHelper.launchSystemShare(
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
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                payload.onSelectAll()
                            }
                        )
                    }

                    // 5. Cut Action (if editable, allowed, and not a password field)
                    if (payload.canCut) {
                        ToolbarActionButton(
                            icon = Icons.Default.ContentCut,
                            label = "Cut",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                payload.onCut()
                                ToastManager.showCopy("Cut to clipboard")
                                payload.onDismiss()
                            }
                        )
                    }

                    // 6. Paste Action (if editable field)
                    if (payload.canPaste) {
                        ToolbarActionButton(
                            icon = Icons.Default.ContentPaste,
                            label = "Paste",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                payload.onPaste()
                                payload.onDismiss()
                            }
                        )
                    }

                    // Subtle Vertical Divider before Close Button
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .height(18.dp)
                            .width(1.dp)
                            .background(colors.border.copy(alpha = 0.7f))
                    )

                    // 7. Dismiss / Close Button
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, radius = 17.dp)
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                payload.onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss selection menu",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(16.dp)
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
            .clip(RoundedCornerShape(18.dp))
            .background(colors.statusAccent.copy(alpha = 0.15f))
            .border(0.75.dp, colors.statusAccent.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
    } else {
        Modifier.clip(RoundedCornerShape(18.dp))
    }

    val iconTint = if (isPrimary) colors.statusAccent else colors.textPrimary
    val textTint = if (isPrimary) colors.statusAccent else colors.textPrimary

    Row(
        modifier = Modifier
            .height(34.dp)
            .then(bgModifier)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, radius = 24.dp),
                onClick = onClick
            )
            .padding(horizontal = if (isPrimary) 10.dp else 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconTint,
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = label,
            color = textTint,
            fontSize = 12.5.sp,
            fontWeight = if (isPrimary) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}
