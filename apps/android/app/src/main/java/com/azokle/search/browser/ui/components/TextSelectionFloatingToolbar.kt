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
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = payload != null && (payload.text.isNotBlank() || payload.screenRect.width() > 0),
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
            val hasValidRect = rect.width() > 0 && rect.height() > 0

            val rectLeftDp = if (hasValidRect) (rect.left / densityVal).dp else screenWidthDp / 2
            val rectTopDp = if (hasValidRect) (rect.top / densityVal).dp else 140.dp
            val rectBottomDp = if (hasValidRect) (rect.bottom / densityVal).dp else 160.dp
            val rectCenterXDp = if (hasValidRect) (rect.centerX() / densityVal).dp else screenWidthDp / 2

            // Estimated dimensions of the floating capsule
            val toolbarEstimatedWidthDp = if (payload.canCut || payload.canPaste) 310.dp else 260.dp
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

            Box(
                modifier = Modifier
                    .offset(x = clampedXDp, y = clampedYDp)
                    .shadow(elevation = 12.dp, shape = RoundedCornerShape(24.dp), spotColor = colors.statusAccent.copy(alpha = 0.25f))
                    .clip(RoundedCornerShape(24.dp))
                    .background(colors.surfaceElevated)
                    .border(width = 1.dp, color = colors.border, shape = RoundedCornerShape(24.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Row(
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
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                cm?.setPrimaryClip(ClipData.newPlainText("text", payload.text))
                                payload.onCopy()
                                ToastManager.showCopy("Copied to clipboard")
                                payload.onDismiss()
                            }
                        )
                    }

                    // 2. Web Search Action
                    ToolbarActionButton(
                        icon = Icons.Default.Search,
                        label = "Search",
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (payload.text.isNotBlank()) {
                                onSearchRequested(payload.text)
                            }
                            payload.onDismiss()
                        }
                    )

                    // 3. Share Action
                    ToolbarActionButton(
                        icon = Icons.Default.Share,
                        label = "Share",
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (payload.text.isNotBlank()) {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, payload.text)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share text"))
                            }
                            payload.onDismiss()
                        }
                    )

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

                    // 5. Cut Action (if editable field)
                    if (payload.canCut) {
                        ToolbarActionButton(
                            icon = Icons.Default.ContentCut,
                            label = "Cut",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                cm?.setPrimaryClip(ClipData.newPlainText("text", payload.text))
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
