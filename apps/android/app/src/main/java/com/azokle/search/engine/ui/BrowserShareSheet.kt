package com.azokle.search.engine.ui

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import com.azokle.search.common.theme.AzokleRadius
import com.azokle.search.common.theme.AzokleTheme
import com.azokle.search.common.theme.adaptiveContentWidth
import com.azokle.search.common.util.ShareHelper
import com.azokle.search.common.util.ShareTargetApp

/**
 * Data model for share operations.
 */
data class SharePayload(
    val url: String,
    val title: String = "",
    val selectedText: String? = null
)

/**
 * Ultra-Minimalist In-App Share Sheet:
 *  - Clean URL/title preview card
 *  - Direct 1-tap Installed App Targets (WhatsApp, Telegram, Gmail, etc.)
 *  - Single-line clean action items (Copy Link, System Share, Save to PDF, Open in External App)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserShareSheet(
    payload: SharePayload,
    onDismiss: () -> Unit,
    onPrintToPdf: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val colors = AzokleTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val uri = try { Uri.parse(payload.url) } catch (e: Exception) { null }
    val host = uri?.host ?: payload.url
    val cleanHost = host.removePrefix("www.")
    val isSecure = payload.url.startsWith("https://", ignoreCase = true)

    val effectiveShareText = if (!payload.selectedText.isNullOrBlank()) {
        if (payload.url.isNotBlank()) "${payload.selectedText}\n\n${payload.url}" else payload.selectedText
    } else {
        payload.url
    }

    val installedShareApps = remember(effectiveShareText) {
        ShareHelper.getInstalledShareApps(
            context = context,
            text = effectiveShareText,
            title = payload.title,
            limit = 6
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.cardBg,
        contentColor = colors.textPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 10.dp, bottom = 6.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(colors.borderSubtle)
            )
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .adaptiveContentWidth(560.dp)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (payload.selectedText != null) "Share Text" else "Share",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    modifier = Modifier.semantics { heading() }
                )

                Box(
                    modifier = Modifier
                        .sizeIn(minWidth = 36.dp, minHeight = 36.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = false, radius = 18.dp, color = colors.textSecondary),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onDismiss()
                            }
                        )
                        .semantics {
                            role = Role.Button
                            contentDescription = "Close share sheet"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceElevated)
                            .border(0.8.dp, colors.borderSubtle, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = colors.textPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 1. Preview Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AzokleRadius.lg)
                    .background(colors.surfaceElevated)
                    .border(0.8.dp, colors.borderSubtle, AzokleRadius.lg)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Favicon
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(AzokleRadius.sm)
                        .background(colors.cardBg)
                        .border(0.8.dp, colors.borderSubtle, AzokleRadius.sm),
                    contentAlignment = Alignment.Center
                ) {
                    if (cleanHost.isNotBlank()) {
                        AsyncImage(
                            model = "https://www.google.com/s2/favicons?domain=$cleanHost&sz=128",
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = colors.textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (!payload.selectedText.isNullOrBlank()) {
                            "\"${payload.selectedText.take(60)}${if (payload.selectedText.length > 60) "..." else ""}\""
                        } else {
                            payload.title.ifBlank { cleanHost.ifBlank { "Shared Link" } }
                        },
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(1.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isSecure) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = colors.statusSuccess,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = cleanHost.ifBlank { payload.url.take(36) },
                            fontSize = 11.5.sp,
                            color = colors.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 2. Direct App Targets
            if (installedShareApps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    installedShareApps.forEach { app ->
                        ShareAppTargetItem(
                            app = app,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onDismiss()
                                ShareHelper.launchDirectAppShare(context, app)
                            }
                        )
                    }

                    // System "More" Target
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(54.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = false, radius = 24.dp, color = colors.textPrimary),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onDismiss()
                                    ShareHelper.launchSystemShare(context, effectiveShareText, payload.title)
                                }
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceElevated)
                                .border(0.8.dp, colors.borderSubtle, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreHoriz,
                                contentDescription = "More Apps",
                                tint = colors.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "More",
                            fontSize = 10.5.sp,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Single-Line Action List
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AzokleRadius.lg)
                    .background(colors.surfaceElevated)
                    .border(0.8.dp, colors.borderSubtle, AzokleRadius.lg)
            ) {
                // Copy Link / Text
                ShareActionItemRow(
                    icon = Icons.Default.ContentCopy,
                    label = if (payload.selectedText != null) "Copy Text" else "Copy Link",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        ShareHelper.copyToClipboard(
                            context = context,
                            text = effectiveShareText,
                            successMessage = if (payload.selectedText != null) "Text copied" else "Link copied"
                        )
                        onDismiss()
                    }
                )

                HorizontalDivider(color = colors.borderSubtle, thickness = 0.5.dp, modifier = Modifier.padding(start = 48.dp))

                // Android System Share
                ShareActionItemRow(
                    icon = Icons.Default.Share,
                    label = "System Share",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDismiss()
                        ShareHelper.launchSystemShare(context, effectiveShareText, payload.title)
                    }
                )

                // Save as PDF / Print
                if (payload.url.isNotBlank() && onPrintToPdf != null) {
                    HorizontalDivider(color = colors.borderSubtle, thickness = 0.5.dp, modifier = Modifier.padding(start = 48.dp))

                    ShareActionItemRow(
                        icon = Icons.Default.PictureAsPdf,
                        label = "Save as PDF",
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onDismiss()
                            onPrintToPdf()
                        }
                    )
                }

                // Open in External App
                if (payload.url.isNotBlank()) {
                    HorizontalDivider(color = colors.borderSubtle, thickness = 0.5.dp, modifier = Modifier.padding(start = 48.dp))

                    ShareActionItemRow(
                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                        label = "Open in External App",
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onDismiss()
                            ShareHelper.openInExternalBrowser(context, payload.url)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ShareAppTargetItem(
    app: ShareTargetApp,
    onClick: () -> Unit
) {
    val colors = AzokleTheme.colors
    val iconBitmap = remember(app.icon) {
        try {
            app.icon.toBitmap(width = 84, height = 84).asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(54.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 24.dp, color = colors.textPrimary),
                onClick = onClick
            )
            .semantics {
                role = Role.Button
                contentDescription = "Share with ${app.label}"
            }
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(colors.surfaceElevated)
                .border(0.8.dp, colors.borderSubtle, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap,
                    contentDescription = null,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    tint = colors.textPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = app.label,
            fontSize = 10.5.sp,
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ShareActionItemRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val colors = AzokleTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 48.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = colors.textPrimary.copy(alpha = 0.08f)),
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(AzokleRadius.sm)
                .background(colors.cardBg)
                .border(0.8.dp, colors.borderSubtle, AzokleRadius.sm),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.textPrimary,
                modifier = Modifier.size(15.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = label,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textPrimary
        )
    }
}
