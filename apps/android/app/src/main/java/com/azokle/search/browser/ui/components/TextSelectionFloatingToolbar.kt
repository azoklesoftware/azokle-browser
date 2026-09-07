package com.azokle.search.browser.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azokle.search.browser.gecko.TextSelectionPayload
import com.azokle.search.ui.components.ToastManager
import com.azokle.search.ui.theme.AzokleTheme
import kotlin.math.max
import kotlin.math.min

/**
 * Sovereign Floating Context Menu for Website Text Selection.
 * Anchors dynamically above or below the active selection rectangle with smooth animations,
 * tactile haptic feedback, and instant Copy, Share, Web Search, and Select All actions.
 */
@Composable
fun TextSelectionFloatingToolbar(
    payload: TextSelectionPayload?,
    onSearchRequested: (String) -> Unit,
    onShareRequested: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isVisible = payload != null && (payload.text.isNotBlank() || payload.screenRect.width() > 0 || payload.canPaste || payload.canSelectAll)

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(spring(stiffness = 500f)) + scaleIn(initialScale = 0.82f, animationSpec = spring(stiffness = 500f)),
        exit = fadeOut(spring(stiffness = 700f)) + scaleOut(targetScale = 0.85f),
        modifier = modifier
    ) {
        if (payload == null) return@AnimatedVisibility

        val context = LocalContext.current
        val haptic = LocalHapticFeedback.current
        val density = LocalDensity.current
        val colors = AzokleTheme.colors

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val screenWidthDp = maxWidth
            val screenHeightDp = maxHeight

            // Density translation from physical pixels to Compose DP
            val rect = payload.screenRect
            val densityVal = density.density
            val hasValidCoordinates = rect.left > 0 || rect.top > 0 || rect.right > 0 || rect.bottom > 0

            val rectTopDp = if (hasValidCoordinates) (rect.top / densityVal).dp else 140.dp
            val rectBottomDp = if (hasValidCoordinates) (rect.bottom / densityVal).dp else 160.dp
            val rectCenterXDp = if (hasValidCoordinates) ((rect.left + (rect.width() / 2f)) / densityVal).dp else screenWidthDp / 2

            // Dynamic capsule width estimation bounded by screen
            val maxAvailableWidth = (screenWidthDp - 32.dp).coerceAtLeast(180.dp)
            val toolbarEstimatedWidthDp = (if (payload.canCut || payload.canPaste) 320.dp else 260.dp).coerceAtMost(maxAvailableWidth)
            val toolbarHeightDp = 46.dp

            // Vertical positioning: Prefer above the selection; if too close to top bar, place below
            val targetYDp = if (rectTopDp > 110.dp) {
                rectTopDp - toolbarHeightDp - 10.dp
            } else {
                rectBottomDp + 12.dp
            }
            val clampedYDp = targetYDp.coerceIn(60.dp, screenHeightDp - toolbarHeightDp - 70.dp)

            // Horizontal positioning: Center on selection, clamped within screen margins
            val targetXDp = rectCenterXDp - (toolbarEstimatedWidthDp / 2)
            val clampedXDp = targetXDp.coerceIn(16.dp, (screenWidthDp - toolbarEstimatedWidthDp - 16.dp).coerceAtLeast(16.dp))

            val scrollState = rememberScrollState()

            Box(
                modifier = Modifier
                    .offset(x = clampedXDp, y = clampedYDp)
                    .widthIn(max = maxAvailableWidth)
                    .shadow(elevation = 12.dp, shape = RoundedCornerShape(24.dp), spotColor = colors.statusAccent.copy(alpha = 0.25f))
                    .clip(RoundedCornerShape(24.dp))
                    .background(colors.surfaceElevated)
                    .border(width = 1.dp, color = colors.border, shape = RoundedCornerShape(24.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.horizontalScroll(scrollState),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // 1. Copy Action
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

                    // 2. Web Search Action (strictly suppressed on password fields)
                    if (payload.canSearch) {
                        ToolbarActionButton(
                            icon = Icons.Default.Search,
                            label = "Search",
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
                                        com.azokle.search.util.ShareHelper.launchSystemShare(
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
                            label = "All",
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

                    // Divider before close
                    Box(
                        modifier = Modifier
                            .height(18.dp)
                            .width(1.dp)
                            .background(colors.border)
                    )

                    // 7. Dismiss / Close Button
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, radius = 16.dp)
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
                            modifier = Modifier.size(15.dp)
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
    onClick: () -> Unit
) {
    val colors = AzokleTheme.colors

    Row(
        modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, radius = 24.dp),
                onClick = onClick
            )
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = colors.textPrimary,
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = label,
            color = colors.textPrimary,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
