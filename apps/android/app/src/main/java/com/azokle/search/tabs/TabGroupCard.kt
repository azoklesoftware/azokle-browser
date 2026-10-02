package com.azokle.search.tabs

import android.net.Uri
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.azokle.search.common.components.FaviconImage
import com.azokle.search.common.theme.AudiowideFontFamily
import com.azokle.search.common.theme.AzokleTheme
import com.azokle.search.settings.WallpaperManager

/**
 * Tab Group Card for the Active Tabs Grid.
 * Standardized to 230dp height and RoundedCornerShape(18.dp) to align with ActiveTabCard.
 * Features:
 *  - 38dp elevated header matching ActiveTabCard structure.
 *  - 2x2 live preview matrix rendering cached screenshots, wallpapers, or domain favicons.
 *  - Tactile elevation and haptic feedback.
 *  - Monospace active indicator badge when any child tab is active.
 */
@Composable
fun TabGroupCard(
    group: TabGroup,
    tabs: List<TabSession>,
    isActive: Boolean,
    onClick: () -> Unit,
    onCloseGroup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AzokleTheme.colors
    val haptic = LocalHapticFeedback.current
    val groupColor = Color(group.color)
    val shape = RoundedCornerShape(18.dp)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.965f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "groupCardPressScale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(230.dp)
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .shadow(
                elevation = if (isActive) 5.dp else 1.dp,
                shape = shape,
                spotColor = if (isActive) groupColor.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.2f)
            )
            .clip(shape)
            .background(colors.cardBg)
            .border(
                width = if (isActive) 1.5.dp else 0.8.dp,
                color = if (isActive) groupColor else colors.border.copy(alpha = 0.55f),
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = groupColor.copy(alpha = 0.25f)),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            )
            .semantics {
                role = Role.Button
                contentDescription = "Tab Group: ${group.title}, ${tabs.size} tabs${if (isActive) ", Active" else ""}"
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Group Header Bar (Matching ActiveTabCard 38dp standard)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .background(colors.surfaceElevated.copy(alpha = 0.85f))
                    .padding(start = 10.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Group Halo Dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(groupColor)
                        .border(1.5.dp, groupColor.copy(alpha = 0.35f), CircleShape)
                )

                Spacer(modifier = Modifier.width(7.dp))

                // Group Title
                Text(
                    text = group.title,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Tab Count Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(groupColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${tabs.size}",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = groupColor
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Close Group Button (Matching ActiveTabCard 26dp with haptic feedback)
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(colors.surface)
                        .border(0.5.dp, colors.border.copy(alpha = 0.4f), CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = false, radius = 13.dp, color = colors.statusDestructive),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onCloseGroup()
                            }
                        )
                        .semantics {
                            role = Role.Button
                            contentDescription = "Close Group: ${group.title}"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            // Hairline divider below header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(colors.border.copy(alpha = 0.4f))
            )

            // 2. 2x2 Preview Matrix (Expands smoothly in the remaining 192dp area)
            val displayPreviewTabs = tabs.take(4)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(colors.surfaceElevated.copy(alpha = 0.4f))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MiniTabTile(
                        tab = displayPreviewTabs.getOrNull(0),
                        groupColor = groupColor,
                        modifier = Modifier.weight(1f)
                    )
                    MiniTabTile(
                        tab = displayPreviewTabs.getOrNull(1),
                        groupColor = groupColor,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MiniTabTile(
                        tab = displayPreviewTabs.getOrNull(2),
                        groupColor = groupColor,
                        modifier = Modifier.weight(1f)
                    )
                    MiniTabTile(
                        tab = displayPreviewTabs.getOrNull(3),
                        groupColor = groupColor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 3. Active Group Indicator Badge
        if (isActive) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(groupColor)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                    Text(
                        text = "ACTIVE",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Mini Tab Tile inside the Tab Group 2x2 matrix:
 *  - Renders live cached screenshots if available.
 *  - Renders custom home wallpaper with "azokle" wordmark for home tabs.
 *  - Renders domain Favicon + host text for standard tabs.
 *  - Renders clean translucent placeholder when empty.
 */
@Composable
private fun MiniTabTile(
    tab: TabSession?,
    groupColor: Color,
    modifier: Modifier = Modifier
) {
    val colors = AzokleTheme.colors
    val tileShape = RoundedCornerShape(10.dp)

    if (tab == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .clip(tileShape)
                .background(colors.cardBg.copy(alpha = 0.25f))
                .border(0.5.dp, colors.border.copy(alpha = 0.25f), tileShape)
        )
        return
    }

    val cachedThumbnail = TabThumbnailCache.get(tab.id)
    if (cachedThumbnail != null) {
        val imageBitmap = remember(cachedThumbnail) { cachedThumbnail.asImageBitmap() }
        Box(
            modifier = modifier
                .fillMaxSize()
                .clip(tileShape)
                .border(0.6.dp, colors.border.copy(alpha = 0.45f), tileShape)
        ) {
            Image(
                bitmap = imageBitmap,
                contentDescription = tab.title,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0x35000000))
                        )
                    )
            )
        }
        return
    }

    val isHome = tab.url.isBlank() || tab.url == "about:home" || tab.url == "about:blank"
    if (isHome) {
        val wallpaperRes = tab.wallpaperResId ?: WallpaperManager.getCurrentWallpaper()
        Box(
            modifier = modifier
                .fillMaxSize()
                .clip(tileShape)
                .border(0.6.dp, colors.border.copy(alpha = 0.45f), tileShape),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(wallpaperRes)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x35000000))
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "azokle",
                    fontFamily = AudiowideFontFamily,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
        return
    }

    val host = remember(tab.url) {
        try {
            Uri.parse(tab.url).host?.removePrefix("www.") ?: tab.url
        } catch (e: Exception) {
            tab.url
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(tileShape)
            .background(colors.cardBg)
            .border(0.6.dp, colors.border.copy(alpha = 0.45f), tileShape)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            FaviconImage(
                url = tab.url,
                faviconUrl = tab.faviconUrl,
                isTor = tab.isTor,
                size = 18.dp
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = tab.title.ifBlank { host },
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
