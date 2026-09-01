package com.azokle.search.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azokle.search.data.news.DiscoverLayoutMode
import com.azokle.search.ui.theme.AzokleRadius
import com.azokle.search.ui.theme.AzokleTheme
import com.azokle.search.ui.theme.adaptiveContentWidth

data class NewsRegionEdition(
    val code: String,
    val name: String,
    val hl: String,
    val gl: String,
    val ceid: String
)

val NEWS_REGIONS = listOf(
    NewsRegionEdition("US", "United States (English)", "en-US", "US", "US:en"),
    NewsRegionEdition("GLOBAL", "International Edition", "en", "US", "US:en"),
    NewsRegionEdition("IN", "India (English)", "en-IN", "IN", "IN:en"),
    NewsRegionEdition("GB", "United Kingdom (English)", "en-GB", "GB", "GB:en"),
    NewsRegionEdition("CA", "Canada (English)", "en-CA", "CA", "CA:en"),
    NewsRegionEdition("AU", "Australia (English)", "en-AU", "AU", "AU:en"),
    NewsRegionEdition("DE", "Germany (Deutsch)", "de", "DE", "DE:de"),
    NewsRegionEdition("FR", "France (Français)", "fr", "FR", "FR:fr")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverSettingsSheet(
    currentLayoutMode: DiscoverLayoutMode,
    currentRegionCode: String,
    mutedPublishers: Set<String>,
    onSelectLayoutMode: (DiscoverLayoutMode) -> Unit,
    onSelectRegion: (NewsRegionEdition) -> Unit,
    onUnmutePublisher: (String) -> Unit,
    onClearCache: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = AzokleTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedSection by remember { mutableStateOf("general") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.modalBg,
        contentColor = colors.textPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 10.dp, bottom = 6.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(colors.border)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .adaptiveContentWidth(560.dp)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = colors.statusAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DISCOVER SETTINGS",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = colors.textPrimary,
                        letterSpacing = 0.8.sp
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 28.dp)
            ) {
                // Section 1: Feed Layout Mode
                item {
                    Text(
                        text = "FEED LAYOUT MODE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = colors.textSecondary,
                        letterSpacing = 0.6.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LayoutModeOption(
                            title = "YouTube 16:9",
                            icon = Icons.Default.ViewStream,
                            isSelected = currentLayoutMode == DiscoverLayoutMode.DYNAMIC_YOUTUBE,
                            onClick = { onSelectLayoutMode(DiscoverLayoutMode.DYNAMIC_YOUTUBE) },
                            modifier = Modifier.weight(1f),
                            colors = colors
                        )
                        LayoutModeOption(
                            title = "Editorial Split",
                            icon = Icons.Default.ViewAgenda,
                            isSelected = currentLayoutMode == DiscoverLayoutMode.COMPACT_EDITORIAL,
                            onClick = { onSelectLayoutMode(DiscoverLayoutMode.COMPACT_EDITORIAL) },
                            modifier = Modifier.weight(1f),
                            colors = colors
                        )
                    }
                }

                // Section 2: News Region & Edition
                item {
                    HorizontalDivider(color = colors.border, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "NEWS REGION & EDITION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = colors.textSecondary,
                        letterSpacing = 0.6.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(NEWS_REGIONS) { region ->
                    val isSelected = currentRegionCode.equals(region.code, ignoreCase = true)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(AzokleRadius.md)
                            .background(if (isSelected) colors.cardBg else Color.Transparent)
                            .border(
                                1.dp,
                                if (isSelected) colors.statusAccent else colors.border,
                                AzokleRadius.md
                            )
                            .clickable { onSelectRegion(region) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = null,
                                tint = if (isSelected) colors.statusAccent else colors.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = region.name,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = colors.textPrimary
                            )
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = colors.statusAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Section 3: Muted Publishers
                if (mutedPublishers.isNotEmpty()) {
                    item {
                        HorizontalDivider(color = colors.border, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "MUTED SOURCES (${mutedPublishers.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = colors.textSecondary,
                            letterSpacing = 0.6.sp
                        )
                    }

                    items(mutedPublishers.toList()) { publisher ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(AzokleRadius.md)
                                .background(colors.cardBg)
                                .border(1.dp, colors.border, AzokleRadius.md)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = publisher,
                                fontSize = 13.sp,
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Medium
                            )

                            IconButton(
                                onClick = { onUnmutePublisher(publisher) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Unmute",
                                    tint = colors.statusDestructive,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Section 4: Clear Offline Cache
                item {
                    HorizontalDivider(color = colors.border, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(AzokleRadius.lg)
                            .background(colors.cardBg)
                            .border(1.dp, colors.border, AzokleRadius.lg)
                            .clickable {
                                onClearCache()
                                onDismiss()
                            }
                            .padding(vertical = 12.dp, horizontal = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Clear Offline News Feed Cache",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LayoutModeOption(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: com.azokle.search.ui.theme.AzokleThemeColors
) {
    Box(
        modifier = modifier
            .clip(AzokleRadius.lg)
            .background(if (isSelected) colors.cardBg else colors.surface)
            .border(
                1.dp,
                if (isSelected) colors.statusAccent else colors.border,
                AzokleRadius.lg
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) colors.statusAccent else colors.textSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) colors.statusAccent else colors.textPrimary
            )
        }
    }
}
