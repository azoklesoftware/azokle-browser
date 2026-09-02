package com.azokle.search.browser.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.azokle.search.browser.downloads.DownloadManager
import com.azokle.search.ui.theme.AzokleRadius
import com.azokle.search.ui.theme.AzokleTheme
import com.azokle.search.ui.theme.adaptiveContentWidth

data class LinkContextTarget(
    val url: String,
    val title: String = "",
    val isImage: Boolean = false,
    val imageSrc: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkContextMenuSheet(
    target: LinkContextTarget,
    onOpenInNewTabGroup: (String, String) -> Unit,
    onOpenInNewTab: (String, String) -> Unit,
    onOpenInPrivateTab: (String, String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val colors = AzokleTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val activeUrl = if (target.url.isNotBlank()) target.url else (target.imageSrc ?: "")
    val uri = try { Uri.parse(activeUrl) } catch (e: Exception) { null }
    val host = uri?.host ?: activeUrl
    val cleanHost = host.removePrefix("www.")
    val isSecure = activeUrl.startsWith("https://", ignoreCase = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.background,
        contentColor = colors.textPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 10.dp, bottom = 4.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(colors.border)
            )
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .adaptiveContentWidth(560.dp)
                .padding(horizontal = 18.dp)
                .padding(top = 2.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Clean Header Preview Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AzokleRadius.lg)
                    .background(colors.cardBg)
                    .border(1.dp, colors.border, AzokleRadius.lg)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (target.isImage && !target.imageSrc.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(AzokleRadius.md)
                            .background(colors.surfaceElevated)
                            .border(1.dp, colors.border, AzokleRadius.md),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = target.imageSrc,
                            contentDescription = "Image preview",
                            modifier = Modifier.clip(AzokleRadius.md),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceElevated)
                            .border(1.dp, colors.border, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = colors.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (target.isImage && target.title.isBlank()) "Image Preview" else if (target.title.isNotBlank()) target.title else cleanHost,
                            color = colors.textPrimary,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isSecure) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Secure",
                                tint = colors.statusSuccess,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = activeUrl,
                        color = colors.textMuted,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Action Group Card (Clean, Calm, Apple-Style Grouped List)
            val shape = AzokleRadius.lg
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(colors.cardBg)
                    .border(1.dp, colors.border, shape)
            ) {
                // Link Navigation Actions
                if (target.url.isNotBlank()) {
                    // 1. Open in New Tab in Group (Adds to group or creates group with parent)
                    ContextActionRow(
                        icon = Icons.Default.FolderOpen,
                        title = "Open in New Tab in Group",
                        colors = colors,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onOpenInNewTabGroup(target.url, target.title.ifBlank { cleanHost })
                            onDismiss()
                        }
                    )

                    ContextDivider(colors = colors)

                    // 2. Open in New Tab (Standalone Separate Tab, NO Group)
                    ContextActionRow(
                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                        title = "Open in New Tab",
                        colors = colors,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onOpenInNewTab(target.url, target.title.ifBlank { cleanHost })
                            onDismiss()
                        }
                    )

                    ContextDivider(colors = colors)

                    // 3. Open in Private Tab
                    ContextActionRow(
                        icon = Icons.Default.Security,
                        title = "Open in Private Tab",
                        colors = colors,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onOpenInPrivateTab(target.url, target.title.ifBlank { cleanHost })
                            onDismiss()
                        }
                    )

                    ContextDivider(colors = colors)
                }

                // Image Specific Actions
                if (target.isImage && !target.imageSrc.isNullOrBlank()) {
                    ContextActionRow(
                        icon = Icons.Default.Download,
                        title = "Save Image",
                        colors = colors,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDismiss()
                            val imgSrc = target.imageSrc
                            val filename = DownloadManager.extractFilename(imgSrc, mimeType = "image/jpeg")
                            DownloadManager.startDownload(context, imgSrc, filename, mimeType = "image/jpeg")
                            Toast.makeText(context, "Downloading $filename...", Toast.LENGTH_SHORT).show()
                        }
                    )

                    ContextDivider(colors = colors)

                    ContextActionRow(
                        icon = Icons.Default.Image,
                        title = "Open Image in New Tab",
                        colors = colors,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onOpenInNewTab(target.imageSrc, "Image View")
                            onDismiss()
                        }
                    )

                    ContextDivider(colors = colors)
                } else if (target.url.isNotBlank()) {
                    ContextActionRow(
                        icon = Icons.Default.Download,
                        title = "Download Link",
                        colors = colors,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDismiss()
                            val filename = DownloadManager.extractFilename(target.url)
                            DownloadManager.startDownload(context, target.url, filename)
                            Toast.makeText(context, "Downloading $filename...", Toast.LENGTH_SHORT).show()
                        }
                    )

                    ContextDivider(colors = colors)
                }

                // Copy Action
                if (target.isImage && !target.imageSrc.isNullOrBlank()) {
                    ContextActionRow(
                        icon = Icons.Default.ContentCopy,
                        title = "Copy Image Address",
                        colors = colors,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Image URL", target.imageSrc)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Image address copied", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                    )

                    ContextDivider(colors = colors)
                }

                if (target.url.isNotBlank()) {
                    ContextActionRow(
                        icon = Icons.Default.ContentCopy,
                        title = "Copy Link Address",
                        colors = colors,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("URL", target.url)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Link address copied", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                    )

                    ContextDivider(colors = colors)
                }

                // Share Action
                ContextActionRow(
                    icon = Icons.Default.Share,
                    title = "Share Link",
                    colors = colors,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, activeUrl)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share").apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun ContextActionRow(
    icon: ImageVector,
    title: String,
    colors: com.azokle.search.ui.theme.AzokleThemeColors,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 52.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = colors.textPrimary.copy(alpha = 0.08f)),
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .semantics {
                role = Role.Button
                contentDescription = title
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(colors.surfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.textPrimary,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ContextDivider(colors: com.azokle.search.ui.theme.AzokleThemeColors) {
    HorizontalDivider(
        color = colors.border.copy(alpha = 0.5f),
        thickness = 0.5.dp,
        modifier = Modifier.padding(start = 54.dp)
    )
}
