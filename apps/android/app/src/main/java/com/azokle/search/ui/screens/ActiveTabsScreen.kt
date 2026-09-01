package com.azokle.search.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azokle.search.browser.tabs.TabManager
import com.azokle.search.browser.tabs.TabSession
import com.azokle.search.ui.theme.AzokleRadius
import com.azokle.search.ui.theme.AzokleTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class TabCategory {
    STANDARD,
    PRIVATE,
    TOR
}

@Composable
fun ActiveTabsScreen(
    onTabSelected: (TabSession) -> Unit,
    onNewTabClick: () -> Unit,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val colors = AzokleTheme.colors
    val scope = rememberCoroutineScope()

    val tabs by TabManager.tabs.collectAsState()
    val activeTabId by TabManager.activeTabId.collectAsState()

    val activeTab = remember(tabs, activeTabId) { tabs.find { it.id == activeTabId } }

    var selectedCategory by remember {
        val initialCat = when {
            activeTab?.isTor == true -> TabCategory.TOR
            activeTab?.isPrivate == true -> TabCategory.PRIVATE
            else -> TabCategory.STANDARD
        }
        mutableStateOf(initialCat)
    }

    var searchQuery by remember { mutableStateOf("") }
    var isGroupedByDomain by remember { mutableStateOf(false) }
    var lastClosedTab by remember { mutableStateOf<TabSession?>(null) }
    var undoSnackbarJob by remember { mutableStateOf<Job?>(null) }

    val standardCount = remember(tabs) { tabs.count { !it.isPrivate && !it.isTor } }
    val privateCount = remember(tabs) { tabs.count { it.isPrivate && !it.isTor } }
    val torCount = remember(tabs) { tabs.count { it.isTor } }

    val filteredTabs = remember(tabs, selectedCategory, searchQuery) {
        tabs.filter { tab ->
            when (selectedCategory) {
                TabCategory.STANDARD -> !tab.isPrivate && !tab.isTor
                TabCategory.PRIVATE -> tab.isPrivate && !tab.isTor
                TabCategory.TOR -> tab.isTor
            }
        }.filter {
            if (searchQuery.isBlank()) true
            else it.title.contains(searchQuery, ignoreCase = true) || it.url.contains(searchQuery, ignoreCase = true)
        }
    }

    val displayTabs = remember(filteredTabs, isGroupedByDomain) {
        if (isGroupedByDomain) {
            filteredTabs.sortedBy { tab ->
                try { android.net.Uri.parse(tab.url).host?.removePrefix("www.") ?: tab.url } catch (e: Exception) { tab.url }
            }
        } else {
            filteredTabs
        }
    }

    if (onClose != null) {
        BackHandler {
            onClose()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(bottom = if (onClose == null) 84.dp else 16.dp)
        ) {
            // 1. Top Bar (3-Way Segmented Switcher + Action Controls)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Segmented Pill Switcher (Standard vs Private vs Tor)
                Row(
                    modifier = Modifier
                        .clip(AzokleRadius.full)
                        .background(colors.cardBg)
                        .border(1.dp, colors.border, AzokleRadius.full)
                        .padding(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Standard Tabs Pill
                    Box(
                        modifier = Modifier
                            .sizeIn(minHeight = 36.dp)
                            .clip(AzokleRadius.full)
                            .background(if (selectedCategory == TabCategory.STANDARD) colors.actionPrimaryBg else Color.Transparent)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = colors.actionPrimaryText.copy(alpha = 0.2f))
                            ) { selectedCategory = TabCategory.STANDARD }
                            .padding(horizontal = 11.dp, vertical = 6.dp)
                            .semantics {
                                role = Role.Tab
                                contentDescription = "Standard Tabs, $standardCount open"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = null,
                                tint = if (selectedCategory == TabCategory.STANDARD) colors.actionPrimaryText else colors.textMuted,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Tabs ($standardCount)",
                                fontSize = 11.5.sp,
                                fontWeight = if (selectedCategory == TabCategory.STANDARD) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedCategory == TabCategory.STANDARD) colors.actionPrimaryText else colors.textMuted
                            )
                        }
                    }

                    // Private Tabs Pill
                    Box(
                        modifier = Modifier
                            .sizeIn(minHeight = 36.dp)
                            .clip(AzokleRadius.full)
                            .background(if (selectedCategory == TabCategory.PRIVATE) colors.actionPrimaryBg else Color.Transparent)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = colors.actionPrimaryText.copy(alpha = 0.2f))
                            ) { selectedCategory = TabCategory.PRIVATE }
                            .padding(horizontal = 11.dp, vertical = 6.dp)
                            .semantics {
                                role = Role.Tab
                                contentDescription = "Private Tabs, $privateCount open"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = if (selectedCategory == TabCategory.PRIVATE) colors.actionPrimaryText else colors.textMuted,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Private ($privateCount)",
                                fontSize = 11.5.sp,
                                fontWeight = if (selectedCategory == TabCategory.PRIVATE) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedCategory == TabCategory.PRIVATE) colors.actionPrimaryText else colors.textMuted
                            )
                        }
                    }

                    // Tor Tabs Pill
                    Box(
                        modifier = Modifier
                            .sizeIn(minHeight = 36.dp)
                            .clip(AzokleRadius.full)
                            .background(if (selectedCategory == TabCategory.TOR) colors.actionPrimaryBg else Color.Transparent)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = colors.actionPrimaryText.copy(alpha = 0.2f))
                            ) { selectedCategory = TabCategory.TOR }
                            .padding(horizontal = 11.dp, vertical = 6.dp)
                            .semantics {
                                role = Role.Tab
                                contentDescription = "Tor Window Tabs, $torCount open"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = if (selectedCategory == TabCategory.TOR) colors.actionPrimaryText else colors.textMuted,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Tor ($torCount)",
                                fontSize = 11.5.sp,
                                fontWeight = if (selectedCategory == TabCategory.TOR) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedCategory == TabCategory.TOR) colors.actionPrimaryText else colors.textMuted
                            )
                        }
                    }
                }

                // Right Action Controls (Clear All, + New Tab, Done)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Domain Grouping Toggle (48dp Touch Target)
                    if (filteredTabs.size > 1) {
                        Box(
                            modifier = Modifier
                                .sizeIn(minWidth = 40.dp, minHeight = 40.dp)
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = false, radius = 18.dp, color = colors.actionPrimaryBg),
                                    onClick = { isGroupedByDomain = !isGroupedByDomain }
                                )
                                .semantics {
                                    role = Role.Button
                                    contentDescription = if (isGroupedByDomain) "Ungroup tabs" else "Group tabs by domain"
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (isGroupedByDomain) colors.actionPrimaryBg else colors.cardBg)
                                    .border(1.dp, if (isGroupedByDomain) colors.actionPrimaryBg else colors.border, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = null,
                                    tint = if (isGroupedByDomain) colors.actionPrimaryText else colors.textMuted,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }

                    // Close All in Mode (48dp Touch Target)
                    if (filteredTabs.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .sizeIn(minWidth = 40.dp, minHeight = 40.dp)
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = false, radius = 18.dp, color = colors.statusDestructive),
                                    onClick = {
                                        when (selectedCategory) {
                                            TabCategory.STANDARD -> TabManager.closeCategoryTabs(isPrivateOnly = false, isTorOnly = false)
                                            TabCategory.PRIVATE -> TabManager.closeCategoryTabs(isPrivateOnly = true, isTorOnly = false)
                                            TabCategory.TOR -> TabManager.closeCategoryTabs(isPrivateOnly = false, isTorOnly = true)
                                        }
                                    }
                                )
                                .semantics {
                                    role = Role.Button
                                    contentDescription = "Close all tabs in this mode"
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(colors.cardBg)
                                    .border(1.dp, colors.border, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = null,
                                    tint = colors.statusDestructive,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // New Tab Button (+ Solid Primary) (48dp Touch Target)
                    Box(
                        modifier = Modifier
                            .sizeIn(minWidth = 40.dp, minHeight = 40.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = false, radius = 18.dp, color = colors.actionPrimaryBg),
                                onClick = {
                                    when (selectedCategory) {
                                        TabCategory.STANDARD -> onNewTabClick()
                                        TabCategory.PRIVATE -> {
                                            val newId = TabManager.openTab("https://search.azokle.com", "Private Search", isPrivate = true)
                                            val newTab = TabManager.tabs.value.find { it.id == newId }
                                            if (newTab != null) onTabSelected(newTab)
                                        }
                                        TabCategory.TOR -> {
                                            val newId = TabManager.openTorTab(context, "about:tor", "Tor Window")
                                            val newTab = TabManager.tabs.value.find { it.id == newId }
                                            if (newTab != null) onTabSelected(newTab)
                                        }
                                    }
                                }
                            )
                            .semantics {
                                role = Role.Button
                                contentDescription = when (selectedCategory) {
                                    TabCategory.STANDARD -> "Open new tab"
                                    TabCategory.PRIVATE -> "Open new private tab"
                                    TabCategory.TOR -> "Open new Tor window"
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(colors.actionPrimaryBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = colors.actionPrimaryText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Dismiss / Done Button (48dp Touch Target)
                    if (onClose != null) {
                        Box(
                            modifier = Modifier
                                .sizeIn(minWidth = 40.dp, minHeight = 40.dp)
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = false, radius = 18.dp, color = colors.textPrimary),
                                    onClick = onClose
                                )
                                .semantics {
                                    role = Role.Button
                                    contentDescription = "Done"
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(colors.cardBg)
                                    .border(1.dp, colors.border, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = colors.textPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Instant Tab Filter Bar (When >= 3 Tabs)
            if (tabs.size > 2) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .height(38.dp)
                        .clip(AzokleRadius.full)
                        .background(colors.cardBg)
                        .border(1.dp, colors.border, AzokleRadius.full)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = colors.textMuted,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        textStyle = TextStyle(
                            color = colors.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(colors.textPrimary),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search open tabs...",
                                    color = colors.textMuted,
                                    fontSize = 13.sp
                                )
                            }
                            innerTextField()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .sizeIn(minWidth = 32.dp, minHeight = 32.dp)
                                .clip(CircleShape)
                                .clickable { searchQuery = "" },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = colors.textMuted,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 3. Adaptive Responsive Tabs Grid (Adapts to tablet, foldable, landscape)
            if (filteredTabs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(colors.surface)
                                .border(1.dp, colors.border, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (selectedCategory) {
                                    TabCategory.STANDARD -> Icons.Default.Layers
                                    TabCategory.PRIVATE -> Icons.Default.VisibilityOff
                                    TabCategory.TOR -> Icons.Default.Security
                                },
                                contentDescription = null,
                                tint = colors.textPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = when (selectedCategory) {
                                TabCategory.STANDARD -> "No Open Tabs"
                                TabCategory.PRIVATE -> "No Private Tabs Open"
                                TabCategory.TOR -> "No Tor Windows Open"
                            },
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = when (selectedCategory) {
                                TabCategory.STANDARD -> "Open a new tab to explore the sovereign web."
                                TabCategory.PRIVATE -> "Private tabs do not save history, cookies, or telemetry."
                                TabCategory.TOR -> "Tor routes your traffic through 3 encrypted relays with zero telemetry or trace logs."
                            },
                            fontSize = 13.sp,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Primary CTA Button
                        Box(
                            modifier = Modifier
                                .clip(AzokleRadius.full)
                                .background(colors.actionPrimaryBg)
                                .clickable {
                                    when (selectedCategory) {
                                        TabCategory.STANDARD -> onNewTabClick()
                                        TabCategory.PRIVATE -> {
                                            val newId = TabManager.openTab("https://search.azokle.com", "Private Search", isPrivate = true)
                                            val newTab = TabManager.tabs.value.find { it.id == newId }
                                            if (newTab != null) onTabSelected(newTab)
                                        }
                                        TabCategory.TOR -> {
                                            val newId = TabManager.openTorTab(context, "about:tor", "Tor Window")
                                            val newTab = TabManager.tabs.value.find { it.id == newId }
                                            if (newTab != null) onTabSelected(newTab)
                                        }
                                    }
                                }
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                                .semantics {
                                    role = Role.Button
                                    contentDescription = when (selectedCategory) {
                                        TabCategory.STANDARD -> "Open New Tab"
                                        TabCategory.PRIVATE -> "Open Private Tab"
                                        TabCategory.TOR -> "Open Tor Window"
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (selectedCategory == TabCategory.TOR) Icons.Default.Security else Icons.Default.Add,
                                    contentDescription = null,
                                    tint = colors.actionPrimaryText,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = when (selectedCategory) {
                                        TabCategory.STANDARD -> "Open New Tab"
                                        TabCategory.PRIVATE -> "Open Private Tab"
                                        TabCategory.TOR -> "Open Tor Window"
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.actionPrimaryText
                                )
                            }
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 155.dp),
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(displayTabs, key = { it.id }) { tab ->
                        val isActive = tab.id == activeTabId
                        val shape = AzokleRadius.lg

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                                .shadow(
                                    elevation = if (isActive) 12.dp else 3.dp,
                                    shape = shape,
                                    ambientColor = if (isActive) colors.statusAccent.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.08f),
                                    spotColor = if (isActive) colors.statusAccent.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.12f)
                                )
                                .clip(shape)
                                .background(colors.cardBg)
                                .border(
                                    width = if (isActive) 1.5.dp else 1.dp,
                                    color = if (isActive) colors.statusAccent else colors.border,
                                    shape = shape
                                )
                                .clickable { onTabSelected(tab) }
                                .semantics {
                                    role = Role.Button
                                    contentDescription = "Tab: ${tab.title.ifBlank { tab.url }}${if (isActive) ", Active" else ""}"
                                }
                        ) {
                            // 1. Tab Card Header Bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(colors.surface)
                                    .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (tab.isTor) Icons.Default.Security else if (tab.isPrivate) Icons.Default.VisibilityOff else if (tab.url.startsWith("https://")) Icons.Default.Lock else Icons.Default.Language,
                                    contentDescription = null,
                                    tint = if (tab.url.startsWith("https://")) colors.statusSuccess else colors.textMuted,
                                    modifier = Modifier.size(13.dp)
                                )

                                Spacer(modifier = Modifier.width(6.dp))

                                Text(
                                    text = tab.title.ifBlank { tab.url },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                // Close Tab Button (44dp Accessible Touch Delegate)
                                Box(
                                    modifier = Modifier
                                        .sizeIn(minWidth = 40.dp, minHeight = 40.dp)
                                        .clip(CircleShape)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = ripple(bounded = false, radius = 18.dp, color = colors.statusDestructive),
                                            onClick = {
                                                val closed = TabManager.closeTab(tab.id)
                                                if (closed != null) {
                                                    lastClosedTab = closed
                                                    undoSnackbarJob?.cancel()
                                                    undoSnackbarJob = scope.launch {
                                                        delay(4500)
                                                        lastClosedTab = null
                                                    }
                                                }
                                            }
                                        )
                                        .semantics {
                                            role = Role.Button
                                            contentDescription = "Close tab: ${tab.title}"
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(colors.surfaceElevated),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = null,
                                            tint = colors.textPrimary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }

                            // 2. Webpage Live Screenshot or Blueprint Wireframe Preview
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .background(colors.surfaceElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                if (tab.thumbnail != null) {
                                    Image(
                                        bitmap = tab.thumbnail.asImageBitmap(),
                                        contentDescription = tab.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    // Subtle solid contrast overlay (zero gradient)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(if (colors.isDark) Color(0x33000000) else Color(0x14000000))
                                    )
                                } else {
                                    // Web Blueprint Fallback
                                    val host = remember(tab.url) {
                                        try { Uri.parse(tab.url).host?.removePrefix("www.") ?: tab.url } catch (e: Exception) { tab.url }
                                    }

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        // Mini domain capsule
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(AzokleRadius.sm)
                                                .background(colors.cardBg)
                                                .border(0.5.dp, colors.border, AzokleRadius.sm)
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = if (tab.isTor) Icons.Default.Security else Icons.Default.Lock,
                                                    contentDescription = null,
                                                    tint = colors.textMuted,
                                                    modifier = Modifier.size(9.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = host,
                                                    fontSize = 10.sp,
                                                    color = colors.textSecondary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Initial monogram badge
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(colors.surfaceElevated)
                                                .border(1.dp, colors.border, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (tab.isTor) "🧅" else (host.firstOrNull()?.uppercase() ?: "A"),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.textPrimary
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Skeleton bars
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(0.7f)
                                                .height(4.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(colors.border)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(0.5f)
                                                .height(4.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(colors.surface)
                                        )
                                    }
                                }

                                // 3. Status Badges (Active / Tor / Private)
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (isActive) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(colors.actionPrimaryBg)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "ACTIVE",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                color = colors.actionPrimaryText
                                            )
                                        }
                                    }

                                    if (tab.isTor) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(colors.surface)
                                                .border(0.5.dp, colors.border, RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "TOR",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                color = colors.textPrimary
                                            )
                                        }
                                    } else if (tab.isPrivate) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(colors.surface)
                                                .border(0.5.dp, colors.border, RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "PRIVATE",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                color = colors.textSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Undo Closed Tab Floating Snackbar
        AnimatedVisibility(
            visible = lastClosedTab != null,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = if (onClose == null) 88.dp else 24.dp, start = 16.dp, end = 16.dp)
        ) {
            val closed = lastClosedTab
            if (closed != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(16.dp, AzokleRadius.xl, ambientColor = colors.textPrimary.copy(alpha = 0.1f), spotColor = Color.Black.copy(alpha = 0.2f))
                        .clip(AzokleRadius.xl)
                        .background(colors.cardBg)
                        .border(1.dp, colors.border, AzokleRadius.xl)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(colors.statusAccent)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Closed \"${closed.title.ifBlank { closed.url }}\"",
                                color = colors.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Box(
                            modifier = Modifier
                                .clip(AzokleRadius.md)
                                .background(colors.actionPrimaryBg)
                                .clickable {
                                    TabManager.undoCloseTab()
                                    lastClosedTab = null
                                    undoSnackbarJob?.cancel()
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .semantics {
                                    role = Role.Button
                                    contentDescription = "Undo closed tab"
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Undo,
                                    contentDescription = null,
                                    tint = colors.actionPrimaryText,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "UNDO",
                                    color = colors.actionPrimaryText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
