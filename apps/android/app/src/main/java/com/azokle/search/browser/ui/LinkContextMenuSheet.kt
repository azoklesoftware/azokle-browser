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
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.azokle.search.browser.downloads.DownloadManager
import com.azokle.search.ui.theme.AzokleRadius
import com.azokle.search.ui.theme.AzokleTheme
import com.azokle.search.ui.theme.adaptiveContentWidth

enum class ContextMediaType {
    NONE,
    IMAGE,
    VIDEO,
    AUDIO
}

class LinkContextTarget(
    val url: String,
    val title: String = "",
    isImage: Boolean = false,
    val imageSrc: String? = null,
    val mediaType: ContextMediaType = if (isImage) ContextMediaType.IMAGE else ContextMediaType.NONE,
    val mediaSrc: String? = imageSrc
) {
    val isImage: Boolean = isImage || (mediaType == ContextMediaType.IMAGE)

    val isMedia: Boolean
        get() = mediaType != ContextMediaType.NONE || isImage || !mediaSrc.isNullOrBlank()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LinkContextTarget) return false
        return url == other.url &&
                title == other.title &&
                isImage == other.isImage &&
                imageSrc == other.imageSrc &&
                mediaType == other.mediaType &&
                mediaSrc == other.mediaSrc
    }

    override fun hashCode(): Int {
        var result = url.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + isImage.hashCode()
        result = 31 * result + (imageSrc?.hashCode() ?: 0)
        result = 31 * result + mediaType.hashCode()
        result = 31 * result + (mediaSrc?.hashCode() ?: 0)
        return result
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkContextMenuSheet(
    target: LinkContextTarget,
    isPrivate: Boolean = false,
    isTor: Boolean = false,
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

    val effectiveMediaUrl = target.mediaSrc ?: target.imageSrc ?: ""
    val activeUrl = if (target.url.isNotBlank()) target.url else effectiveMediaUrl
    val uri = remember(activeUrl) {
        try { Uri.parse(activeUrl) } catch (e: Exception) { null }
    }
    val host = uri?.host ?: activeUrl
    val cleanHost = remember(host) {
        val h = host.removePrefix("www.")
        if (h.length > 64) h.take(64) + "…" else h
    }
    val isSecure = remember(activeUrl) {
        try {
            val parsed = Uri.parse(activeUrl)
            parsed.scheme?.equals("https", ignoreCase = true) == true && parsed.userInfo.isNullOrBlank()
        } catch (e: Exception) {
            false
        }
    }

    val sanitizedTitle = remember(target.title) {
        target.title
            .replace("\n", " ")
            .replace("\r", " ")
            .replace(Regex("""[\u200E\u200F\u202A-\u202E\x00-\x1F\x7F]"""), "")
            .trim()
    }

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
                if (target.isMedia && effectiveMediaUrl.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(AzokleRadius.md)
                            .background(colors.surfaceElevated)
                            .border(1.dp, colors.border, AzokleRadius.md),
                        contentAlignment = Alignment.Center
                    ) {
                        // In Tor mode, do NOT fetch remote images over clearnet Coil to prevent IP leak
                        if (!isTor && target.mediaType == ContextMediaType.IMAGE && !effectiveMediaUrl.startsWith("data:")) {
                            AsyncImage(
                                model = effectiveMediaUrl,
                                contentDescription = "Image preview",
                                modifier = Modifier.clip(AzokleRadius.md),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            val mediaIcon = when (target.mediaType) {
                                ContextMediaType.VIDEO -> Icons.Default.Movie
                                ContextMediaType.AUDIO -> Icons.Default.MusicNote
                                else -> Icons.Default.Image
                            }
                            Icon(
                                imageVector = mediaIcon,
                                contentDescription = null,
                                tint = colors.textPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
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
                        val headerLabel = when {
                            sanitizedTitle.isNotBlank() -> sanitizedTitle
                            target.mediaType == ContextMediaType.VIDEO -> "Video Preview"
                            target.mediaType == ContextMediaType.AUDIO -> "Audio Preview"
                            target.mediaType == ContextMediaType.IMAGE -> "Image Preview"
                            else -> cleanHost
                        }
                        Text(
                            text = headerLabel,
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
                            onOpenInNewTabGroup(target.url, sanitizedTitle.ifBlank { cleanHost })
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
                            onOpenInNewTab(target.url, sanitizedTitle.ifBlank { cleanHost })
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
                            onOpenInPrivateTab(target.url, sanitizedTitle.ifBlank { cleanHost })
                            onDismiss()
                        }
                    )

                    ContextDivider(colors = colors)
                }

                // Media Specific Actions (Image, Video, Audio)
                if (target.isMedia && effectiveMediaUrl.isNotBlank()) {
                    val saveTitle = when (target.mediaType) {
                        ContextMediaType.VIDEO -> "Save Video"
                        ContextMediaType.AUDIO -> "Save Audio"
                        else -> "Save Image"
                    }
                    val openMediaTitle = when (target.mediaType) {
                        ContextMediaType.VIDEO -> "Open Video in New Tab"
                        ContextMediaType.AUDIO -> "Open Audio in New Tab"
                        else -> "Open Image in New Tab"
                    }
                    val copyMediaTitle = when (target.mediaType) {
                        ContextMediaType.VIDEO -> "Copy Video Address"
                        ContextMediaType.AUDIO -> "Copy Audio Address"
                        else -> "Copy Image Address"
                    }

                    val inferredMimeType = when (target.mediaType) {
                        ContextMediaType.VIDEO -> "video/mp4"
                        ContextMediaType.AUDIO -> "audio/mpeg"
                        ContextMediaType.IMAGE -> {
                            val lower = effectiveMediaUrl.lowercase()
                            when {
                                lower.contains(".png") -> "image/png"
                                lower.contains(".webp") -> "image/webp"
                                lower.contains(".gif") -> "image/gif"
                                lower.contains(".svg") -> "image/svg+xml"
                                lower.contains(".avif") -> "image/avif"
                                else -> "image/jpeg"
                            }
                        }
                        else -> null
                    }

                    ContextActionRow(
                        icon = Icons.Default.Download,
                        title = saveTitle,
                        colors = colors,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDismiss()
                            val filename = DownloadManager.extractFilename(effectiveMediaUrl, mimeType = inferredMimeType)
                            DownloadManager.startDownload(
                                context = context,
                                url = effectiveMediaUrl,
                                filename = filename,
                                mimeType = inferredMimeType,
                                isPrivate = isPrivate,
                                isTor = isTor
                            )
                            Toast.makeText(context, "Downloading $filename...", Toast.LENGTH_SHORT).show()
                        }
                    )

                    ContextDivider(colors = colors)

                    ContextActionRow(
                        icon = when (target.mediaType) {
                            ContextMediaType.VIDEO -> Icons.Default.Movie
                            ContextMediaType.AUDIO -> Icons.Default.MusicNote
                            else -> Icons.Default.Image
                        },
                        title = openMediaTitle,
                        colors = colors,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val tabTitle = when (target.mediaType) {
                                ContextMediaType.VIDEO -> "Video View"
                                ContextMediaType.AUDIO -> "Audio View"
                                else -> "Image View"
                            }
                            onOpenInNewTab(effectiveMediaUrl, tabTitle)
                            onDismiss()
                        }
                    )

                    ContextDivider(colors = colors)

                    ContextActionRow(
                        icon = Icons.Default.ContentCopy,
                        title = copyMediaTitle,
                        colors = colors,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Media URL", effectiveMediaUrl)
                            clipboard.setPrimaryClip(clip)
                            val label = when (target.mediaType) {
                                ContextMediaType.VIDEO -> "Video address copied"
                                ContextMediaType.AUDIO -> "Audio address copied"
                                else -> "Image address copied"
                            }
                            Toast.makeText(context, label, Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                    )

                    ContextDivider(colors = colors)
                }

                // Link Download Action (even if target is also media)
                if (target.url.isNotBlank()) {
                    ContextActionRow(
                        icon = Icons.Default.Download,
                        title = "Download Link",
                        colors = colors,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDismiss()
                            val filename = DownloadManager.extractFilename(target.url)
                            DownloadManager.startDownload(
                                context = context,
                                url = target.url,
                                filename = filename,
                                isPrivate = isPrivate,
                                isTor = isTor
                            )
                            Toast.makeText(context, "Downloading $filename...", Toast.LENGTH_SHORT).show()
                        }
                    )

                    ContextDivider(colors = colors)

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
                    title = if (target.url.isNotBlank()) "Share Link" else "Share Media",
                    colors = colors,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        try {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, activeUrl)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share").apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            })
                        } catch (e: Exception) {
                            Toast.makeText(context, "Unable to share link", Toast.LENGTH_SHORT).show()
                        }
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
