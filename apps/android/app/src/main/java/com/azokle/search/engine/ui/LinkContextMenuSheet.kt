package com.azokle.search.engine.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
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
import com.azokle.search.common.components.ToastManager
import com.azokle.search.common.theme.AzokleRadius
import com.azokle.search.common.theme.AzokleTheme
import com.azokle.search.common.theme.adaptiveContentWidth
import com.azokle.search.common.util.UrlHelper
import com.azokle.search.downloads.DownloadManager

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

/**
 * Sovereign Apple / Brave-Grade Link & Media Context Menu Sheet.
 * Features:
 * - Grouped rounded card hierarchy (Navigation, Media, Privacy & Sharing).
 * - "Copy Clean Link" (strips telemetry & tracker tokens like utm_*, fbclid, gclid).
 * - "Open in Background Tab" support.
 * - Cryptographic Onion / Tor isolation badge.
 * - Instant clipboard copy & deep file download routing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkContextMenuSheet(
    target: LinkContextTarget,
    isPrivate: Boolean = false,
    isTor: Boolean = false,
    onOpenInNewTabGroup: (String, String) -> Unit,
    onOpenInNewTab: (String, String) -> Unit,
    onOpenInBackgroundTab: ((String, String) -> Unit)? = null,
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

    val isOnion = remember(host) { host.endsWith(".onion", ignoreCase = true) }
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
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 10.dp, bottom = 6.dp)
                    .size(width = 38.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(colors.border)
            )
        },
        modifier = modifier
    ) {
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .adaptiveContentWidth(560.dp)
                .verticalScroll(scrollState)
                .padding(horizontal = 18.dp)
                .padding(top = 2.dp, bottom = 16.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Sovereign Header Preview Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 2.dp, shape = AzokleRadius.xl, spotColor = colors.statusAccent.copy(alpha = 0.08f))
                    .clip(AzokleRadius.xl)
                    .background(colors.cardBg)
                    .border(1.dp, colors.border, AzokleRadius.xl)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (target.isMedia && effectiveMediaUrl.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(AzokleRadius.lg)
                            .background(colors.surfaceElevated)
                            .border(1.dp, colors.border, AzokleRadius.lg),
                        contentAlignment = Alignment.Center
                    ) {
                        // In Tor mode, avoid direct clearnet Coil fetch to prevent IP leak
                        if (!isTor && target.mediaType == ContextMediaType.IMAGE && !effectiveMediaUrl.startsWith("data:")) {
                            AsyncImage(
                                model = effectiveMediaUrl,
                                contentDescription = "Image preview",
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(AzokleRadius.lg),
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
                                tint = colors.statusAccent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceElevated)
                            .border(1.dp, colors.border, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isOnion) Icons.Default.Shield else Icons.Default.Language,
                            contentDescription = null,
                            tint = if (isOnion) colors.statusAccent else colors.textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(13.dp))

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

                        Spacer(modifier = Modifier.width(6.dp))

                        // Security Badge
                        if (isTor || isOnion) {
                            BadgePill(
                                label = "TOR",
                                color = colors.statusAccent,
                                icon = Icons.Default.Shield
                            )
                        } else if (isSecure) {
                            BadgePill(
                                label = "HTTPS",
                                color = colors.statusSuccess,
                                icon = Icons.Default.Lock
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

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

            // 2. Navigation Actions Card (Open in New Tab, Background Tab, Group, Private)
            if (target.url.isNotBlank()) {
                val groupShape = AzokleRadius.xl
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(groupShape)
                        .background(colors.cardBg)
                        .border(1.dp, colors.border, groupShape)
                ) {
                    // 1. Open in New Tab
                    ContextActionRow(
                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                        title = "Open in New Tab",
                        subtitle = "Switch to tab immediately",
                        colors = colors,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onOpenInNewTab(target.url, sanitizedTitle.ifBlank { cleanHost })
                            onDismiss()
                        }
                    )

                    ContextDivider(colors = colors)

                    // 2. Open in Background Tab
                    if (onOpenInBackgroundTab != null) {
                        ContextActionRow(
                            icon = Icons.Default.AddBox,
                            title = "Open in Background Tab",
                            subtitle = "Stay on this page and load in background",
                            colors = colors,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onOpenInBackgroundTab(target.url, sanitizedTitle.ifBlank { cleanHost })
                                onDismiss()
                            }
                        )

                        ContextDivider(colors = colors)
                    }

                    // 3. Open in Tab Group
                    ContextActionRow(
                        icon = Icons.Default.FolderOpen,
                        title = "Open in Tab Group",
                        subtitle = "Group with current related tab",
                        colors = colors,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onOpenInNewTabGroup(target.url, sanitizedTitle.ifBlank { cleanHost })
                            onDismiss()
                        }
                    )

                    ContextDivider(colors = colors)

                    // 4. Open in Private / Tor Tab
                    ContextActionRow(
                        icon = if (isTor) Icons.Default.Shield else Icons.Default.Security,
                        title = if (isTor) "Open in Tor Tab" else "Open in Private Tab",
                        subtitle = if (isTor) "Route via Onion Circuit" else "Leaves no trace in history",
                        colors = colors,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onOpenInPrivateTab(target.url, sanitizedTitle.ifBlank { cleanHost })
                            onDismiss()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // 3. Media Actions Card (Save, Open Media, Copy Media URL)
            if (target.isMedia && effectiveMediaUrl.isNotBlank()) {
                val mediaShape = AzokleRadius.xl
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

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(mediaShape)
                        .background(colors.cardBg)
                        .border(1.dp, colors.border, mediaShape)
                ) {
                    // Save Media
                    ContextActionRow(
                        icon = Icons.Default.Download,
                        title = saveTitle,
                        subtitle = "Download directly to storage",
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
                            ToastManager.show("Downloading $filename...")
                        }
                    )

                    ContextDivider(colors = colors)

                    // Open Media in Tab
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

                    // Copy Media Address
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
                            ToastManager.showCopy(label)
                            onDismiss()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // 4. Privacy, Clipboard & Share Card
            val privacyShape = AzokleRadius.xl
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(privacyShape)
                    .background(colors.cardBg)
                    .border(1.dp, colors.border, privacyShape)
            ) {
                // 1. Copy Clean Link (Azokle Privacy-First: Strips UTM / Ad tracking tokens)
                if (target.url.isNotBlank()) {
                    ContextActionRow(
                        icon = Icons.Default.Shield,
                        title = "Copy Clean Link",
                        subtitle = "Strips ad trackers & tracking parameters",
                        colors = colors,
                        accentIcon = true,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val unwrapped = UrlHelper.unwrapLinkShim(target.url)
                            val cleanUrl = UrlHelper.stripTrackingParameters(unwrapped)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Clean URL", cleanUrl)
                            clipboard.setPrimaryClip(clip)
                            ToastManager.showCopy("Clean link copied (trackers stripped)")
                            onDismiss()
                        }
                    )

                    ContextDivider(colors = colors)

                    // 2. Copy Original Link Address
                    ContextActionRow(
                        icon = Icons.Default.ContentCopy,
                        title = "Copy Link Address",
                        colors = colors,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("URL", target.url)
                            clipboard.setPrimaryClip(clip)
                            ToastManager.showCopy("Link address copied")
                            onDismiss()
                        }
                    )

                    ContextDivider(colors = colors)

                    // 3. Download Link Target File
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
                            ToastManager.show("Downloading $filename...")
                        }
                    )

                    ContextDivider(colors = colors)
                }

                // Share Link / Media
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
                            ToastManager.show("Unable to share link")
                        }
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun BadgePill(
    label: String,
    color: Color,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.14f))
            .border(0.5.dp, color.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(10.dp)
        )
        Text(
            text = label,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun ContextActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    colors: com.azokle.search.common.theme.AzokleThemeColors,
    accentIcon: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 54.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = colors.textPrimary.copy(alpha = 0.08f)),
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .semantics {
                role = Role.Button
                contentDescription = title
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (accentIcon) colors.statusAccent.copy(alpha = 0.12f) else colors.surfaceElevated)
                .border(
                    width = 0.75.dp,
                    color = if (accentIcon) colors.statusAccent.copy(alpha = 0.3f) else colors.border.copy(alpha = 0.6f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (accentIcon) colors.statusAccent else colors.textPrimary,
                modifier = Modifier.size(17.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.5.sp,
                    color = colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ContextDivider(colors: com.azokle.search.common.theme.AzokleThemeColors) {
    HorizontalDivider(
        color = colors.border.copy(alpha = 0.45f),
        thickness = 0.5.dp,
        modifier = Modifier.padding(start = 64.dp)
    )
}
