package com.azokle.search.browser.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.azokle.search.browser.tabs.TabGroup
import com.azokle.search.browser.tabs.TabSession
import com.azokle.search.ui.theme.AzokleRadius
import com.azokle.search.ui.theme.AzokleTheme

/**
 * Brave-Inspired Tab Group Card for the Active Tabs Grid:
 *  - Displays a 2x2 multi-tab matrix of previews/favicons.
 *  - Colored header with group title and tab counter.
 *  - Direct actions to expand or close group.
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
    val groupColor = Color(group.color)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(AzokleRadius.xl)
            .background(colors.cardBg)
            .border(
                width = if (isActive) 2.dp else 1.dp,
                color = if (isActive) groupColor else colors.border,
                shape = AzokleRadius.xl
            )
            .clickable(onClick = onClick)
            .padding(10.dp)
            .semantics {
                role = Role.Button
                contentDescription = "Tab Group: ${group.title}, ${tabs.size} tabs"
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Group Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(groupColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = group.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(groupColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${tabs.size}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = groupColor
                        )
                    }
                }

                IconButton(
                    onClick = onCloseGroup,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Group",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // 2. 2x2 Mini Preview Matrix (up to 4 preview tiles)
            val displayPreviewTabs = tabs.take(4)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(AzokleRadius.lg)
                    .background(colors.surface.copy(alpha = 0.6f))
                    .padding(6.dp),
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
                        modifier = Modifier.weight(1f)
                    )
                    MiniTabTile(
                        tab = displayPreviewTabs.getOrNull(1),
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
                        modifier = Modifier.weight(1f)
                    )
                    MiniTabTile(
                        tab = displayPreviewTabs.getOrNull(3),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniTabTile(
    tab: TabSession?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = AzokleTheme.colors

    if (tab == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .background(colors.cardBg.copy(alpha = 0.3f))
        )
        return
    }

    val host = remember(tab.url) {
        try {
            Uri.parse(tab.url).host?.removePrefix("www.") ?: tab.url
        } catch (e: Exception) {
            tab.url
        }
    }
    val faviconUrl = tab.faviconUrl ?: "https://www.google.com/s2/favicons?domain=$host&sz=64"

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.cardBg)
            .border(1.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
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
                size = 16.dp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = tab.title,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
