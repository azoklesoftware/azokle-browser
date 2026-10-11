/*
 * Copyright (c) 2026 The Azokle Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.azokle.search.common.components.ToastManager
import com.azokle.search.common.theme.AzokleTheme
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

    /**
     * Executes the equals operation.
     */
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

    /**
     * Checks if h code exists.
     */
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
 * Ultra-Minimalist Link & Media Context Menu Popup Dialog.
 * Compact, fast, minimalist layout replacing bottom sheets with a modern centered floating dialog.
 */
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
    LinkContextMenuPopup(
        target = target,
        isPrivate = isPrivate,
        isTor = isTor,
        onOpenInNewTabGroup = onOpenInNewTabGroup,
        onOpenInNewTab = onOpenInNewTab,
        onOpenInBackgroundTab = onOpenInBackgroundTab,
        onOpenInPrivateTab = onOpenInPrivateTab,
        onDismiss = onDismiss,
        modifier = modifier
    )
}

/**
 * Centered Floating Link Context Dialog with ultra-minimal text and fast touch targets.
 */
@Composable
fun LinkContextMenuPopup(
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

    val effectiveMediaUrl = target.mediaSrc ?: target.imageSrc ?: ""
    val activeUrl = if (target.url.isNotBlank()) target.url else effectiveMediaUrl
    val uri = remember(activeUrl) {
        try { Uri.parse(activeUrl) } catch (e: Exception) { null }
    }
    val host = uri?.host ?: activeUrl
    val cleanHost = remember(host) {
        val h = host.removePrefix("www.")
        if (h.length > 36) h.take(36) + "…" else h
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

    val displayTitle = when {
        sanitizedTitle.isNotBlank() -> sanitizedTitle
        cleanHost.isNotBlank() -> cleanHost
        target.mediaType == ContextMediaType.IMAGE -> "Image"
        target.mediaType == ContextMediaType.VIDEO -> "Video"
        target.mediaType == ContextMediaType.AUDIO -> "Audio"
        else -> "Link"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = true,
            dismissOnBackPress = true
        )
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 340.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.cardBg)
                    .border(1.dp, colors.borderSubtle, RoundedCornerShape(20.dp))
                    .padding(vertical = 12.dp, horizontal = 12.dp)
            ) {
                // Header: Compact URL / Host Title + Security Badge + Close Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceElevated)
                                .border(0.75.dp, colors.borderSubtle, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isOnion || isTor) Icons.Default.Shield else if (isSecure) Icons.Default.Lock else Icons.Default.Language,
                                contentDescription = null,
                                tint = if (isOnion || isTor) colors.statusTor else if (isSecure) colors.statusSuccess else colors.textMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = displayTitle,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (activeUrl.isNotBlank() && activeUrl != displayTitle) {
                                Text(
                                    text = cleanHost,
                                    fontSize = 11.sp,
                                    color = colors.textMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Compact Close Icon
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = false, radius = 14.dp, color = colors.textSecondary),
                                onClick = onDismiss
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = colors.textMuted,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                HorizontalDivider(
                    color = colors.borderSubtle,
                    thickness = 0.6.dp,
                    modifier = Modifier.padding(vertical = 6.dp)
                )

                // Scrollable actions
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    if (target.url.isNotBlank()) {
                        // 1. Open in New Tab
                        MinimalActionRow(
                            icon = Icons.AutoMirrored.Filled.OpenInNew,
                            text = "New Tab",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onOpenInNewTab(target.url, sanitizedTitle.ifBlank { cleanHost })
                                onDismiss()
                            }
                        )

                        // 2. Open in Background Tab
                        if (onOpenInBackgroundTab != null) {
                            MinimalActionRow(
                                icon = Icons.Default.AddBox,
                                text = "Background Tab",
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onOpenInBackgroundTab(target.url, sanitizedTitle.ifBlank { cleanHost })
                                    onDismiss()
                                }
                            )
                        }

                        // 3. Open in Tab Group
                        MinimalActionRow(
                            icon = Icons.Default.FolderOpen,
                            text = "New Group",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onOpenInNewTabGroup(target.url, sanitizedTitle.ifBlank { cleanHost })
                                onDismiss()
                            }
                        )

                        // 4. Open in Private / Tor Tab
                        MinimalActionRow(
                            icon = if (isTor) Icons.Default.Shield else Icons.Default.Security,
                            text = if (isTor) "Tor Tab" else "Private Tab",
                            accent = isTor,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onOpenInPrivateTab(target.url, sanitizedTitle.ifBlank { cleanHost })
                                onDismiss()
                            }
                        )

                        HorizontalDivider(
                            color = colors.borderSubtle,
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
                        )

                        // 5. Copy Clean Link (Strips trackers)
                        MinimalActionRow(
                            icon = Icons.Default.Shield,
                            text = "Copy Clean Link",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val unwrapped = UrlHelper.unwrapLinkShim(target.url)
                                val cleanUrl = UrlHelper.stripTrackingParameters(unwrapped)
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Clean URL", cleanUrl)
                                clipboard.setPrimaryClip(clip)
                                ToastManager.showCopy("Clean link copied")
                                onDismiss()
                            }
                        )

                        // 6. Copy Link Address
                        MinimalActionRow(
                            icon = Icons.Default.ContentCopy,
                            text = "Copy Link",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("URL", target.url)
                                clipboard.setPrimaryClip(clip)
                                ToastManager.showCopy("Link copied")
                                onDismiss()
                            }
                        )

                        // 7. Share Link
                        MinimalActionRow(
                            icon = Icons.Default.Share,
                            text = "Share",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                                    ToastManager.show("Unable to share")
                                }
                                onDismiss()
                            }
                        )

                        // 8. Download Link Target File
                        MinimalActionRow(
                            icon = Icons.Default.Download,
                            text = "Download Link",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                    }

                    // Media actions if applicable
                    if (target.isMedia && effectiveMediaUrl.isNotBlank()) {
                        if (target.url.isNotBlank()) {
                            HorizontalDivider(
                                color = colors.borderSubtle,
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
                            )
                        }

                        val mediaSaveText = when (target.mediaType) {
                            ContextMediaType.VIDEO -> "Save Video"
                            ContextMediaType.AUDIO -> "Save Audio"
                            else -> "Save Image"
                        }
                        val mediaOpenText = when (target.mediaType) {
                            ContextMediaType.VIDEO -> "Open Video"
                            ContextMediaType.AUDIO -> "Open Audio"
                            else -> "Open Image"
                        }
                        val mediaCopyText = when (target.mediaType) {
                            ContextMediaType.VIDEO -> "Copy Video Link"
                            ContextMediaType.AUDIO -> "Copy Audio Link"
                            else -> "Copy Image Link"
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

                        MinimalActionRow(
                            icon = Icons.Default.Download,
                            text = mediaSaveText,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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

                        MinimalActionRow(
                            icon = when (target.mediaType) {
                                ContextMediaType.VIDEO -> Icons.Default.Movie
                                ContextMediaType.AUDIO -> Icons.Default.MusicNote
                                else -> Icons.Default.Image
                            },
                            text = mediaOpenText,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val tabTitle = when (target.mediaType) {
                                    ContextMediaType.VIDEO -> "Video"
                                    ContextMediaType.AUDIO -> "Audio"
                                    else -> "Image"
                                }
                                onOpenInNewTab(effectiveMediaUrl, tabTitle)
                                onDismiss()
                            }
                        )

                        MinimalActionRow(
                            icon = Icons.Default.ContentCopy,
                            text = mediaCopyText,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Media URL", effectiveMediaUrl)
                                clipboard.setPrimaryClip(clip)
                                ToastManager.showCopy("Copied to clipboard")
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Composable that renders the minimal action row UI component.
 */
@Composable
private fun MinimalActionRow(
    icon: ImageVector,
    text: String,
    accent: Boolean = false,
    onClick: () -> Unit
) {
    val colors = AzokleTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = colors.textPrimary.copy(alpha = 0.08f)),
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 9.dp)
            .semantics {
                role = Role.Button
                contentDescription = text
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (accent) colors.statusTor else colors.textSecondary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
