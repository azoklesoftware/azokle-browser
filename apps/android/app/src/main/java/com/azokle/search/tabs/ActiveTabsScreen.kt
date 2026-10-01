package com.azokle.search.tabs

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
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.azokle.search.R
import com.azokle.search.common.components.FaviconImage
import com.azokle.search.common.components.ToastManager
import com.azokle.search.common.theme.AzokleRadius
import com.azokle.search.common.theme.AzokleTheme
import com.azokle.search.common.theme.AzokleThemeColors
import com.azokle.search.common.tour.DriverTourOverlay
import com.azokle.search.common.tour.TourShape
import com.azokle.search.common.tour.TourState
import com.azokle.search.common.tour.TourStep
import com.azokle.search.common.tour.rememberTourState
import com.azokle.search.common.tour.tourTarget
import com.azokle.search.common.util.PreferencesManager
import com.azokle.search.settings.WallpaperManager
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
    modifier: Modifier = Modifier,
    forceStartTour: Boolean = false,
    onTourConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val colors = AzokleTheme.colors
    val scope = rememberCoroutineScope()
    val preferencesManager = remember { PreferencesManager(context) }

    val tabsTourSteps = remember {
        listOf(
            TourStep(
                id = "tabs_modes",
                title = "Switch Browsing Modes",
                description = "Switch between Regular tabs, Private tabs (no history saved), and Tor (super private onion network).",
                shape = TourShape.ROUNDED_RECT,
                paddingDp = 6f,
                cornerRadiusDp = 20f
            ),
            TourStep(
                id = "tabs_card",
                title = "Your Open Pages",
                description = "Tap any card to view that page. Tap the 'X' button on any card to close it.",
                shape = TourShape.ROUNDED_RECT,
                paddingDp = 6f,
                cornerRadiusDp = 18f
            ),
            TourStep(
                id = "tabs_actions",
                title = "Quick Clean Up",
                description = "Tap the broom icon to close all tabs at once, or group pages from the same website together.",
                shape = TourShape.ROUNDED_RECT,
                paddingDp = 6f,
                cornerRadiusDp = 18f
            ),
            TourStep(
                id = "tabs_new_hub",
                title = "Open New Tabs (+)",
                description = "Tap this plus button to quickly open a tab in your current mode, or tap the three dots for more options.",
                shape = TourShape.CIRCLE,
                paddingDp = 6f
            )
        )
    }

    val tabsTourState = rememberTourState(
        steps = tabsTourSteps,
        onTourFinished = {
            preferencesManager.setTabsTourCompleted(true)
            onTourConsumed()
        }
    )

    LaunchedEffect(forceStartTour) {
        if (forceStartTour) {
            delay(400)
            tabsTourState.start()
        } else if (!preferencesManager.hasCompletedTabsTour()) {
            delay(500)
            tabsTourState.start()
        }
    }

    val tabs by TabManager.tabs.collectAsState()
    val groups by TabManager.groups.collectAsState()
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
    var expandedGroup by remember { mutableStateOf<TabGroup?>(null) }
    var showCloseAllDialog by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }

    // Streamlined single-pass counting
    val (standardCount, privateCount, torCount) = remember(tabs) {
        var std = 0
        var priv = 0
        var tor = 0
        tabs.forEach {
            when {
                it.isTor -> tor++
                it.isPrivate -> priv++
                else -> std++
            }
        }
        Triple(std, priv, tor)
    }

    // Consolidated category groups derivation
    val categoryGroups = remember(groups, selectedCategory, searchQuery, tabs) {
        val query = searchQuery.trim()
        groups.filter { group ->
            when (selectedCategory) {
                TabCategory.STANDARD -> !group.isPrivate && !group.isTor
                TabCategory.PRIVATE -> group.isPrivate && !group.isTor
                TabCategory.TOR -> group.isTor
            }
        }.filter { group ->
            if (query.isBlank()) true
            else {
                val groupTabList = tabs.filter { it.groupId == group.id }
                group.title.contains(query, ignoreCase = true) ||
                        groupTabList.any { it.title.contains(query, ignoreCase = true) || it.url.contains(query, ignoreCase = true) }
            }
        }.reversed()
    }

    val groupedTabIds = remember(categoryGroups) {
        categoryGroups.flatMap { it.tabIds }.toSet()
    }

    // Consolidated filtered tabs derivation
    val filteredTabs = remember(tabs, selectedCategory, searchQuery, isGroupedByDomain) {
        val query = searchQuery.trim()
        val baseList = tabs.filter { tab ->
            when (selectedCategory) {
                TabCategory.STANDARD -> !tab.isPrivate && !tab.isTor
                TabCategory.PRIVATE -> tab.isPrivate && !tab.isTor
                TabCategory.TOR -> tab.isTor
            }
        }.filter {
            if (query.isBlank()) true
            else it.title.contains(query, ignoreCase = true) || it.url.contains(query, ignoreCase = true)
        }.reversed()

        if (isGroupedByDomain) {
            baseList.sortedBy { tab ->
                try {
                    Uri.parse(tab.url).host?.removePrefix("www.") ?: tab.url
                } catch (e: Exception) {
                    tab.url
                }
            }
        } else {
            baseList
        }
    }

    val allActiveGroupIds = remember(groups) { groups.map { it.id }.toSet() }
    val standaloneTabs = remember(filteredTabs, groupedTabIds, allActiveGroupIds) {
        filteredTabs.filter { !groupedTabIds.contains(it.id) && (it.groupId == null || !allActiveGroupIds.contains(it.groupId)) }
    }

    BackHandler(enabled = showOverflowMenu || tabsTourState.isVisible || expandedGroup != null || onClose != null) {
        if (showOverflowMenu) {
            showOverflowMenu = false
        } else if (tabsTourState.isVisible) {
            tabsTourState.skip()
        } else if (expandedGroup != null) {
            expandedGroup = null
        } else if (onClose != null) {
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
                        .tourTarget(tabsTourSteps[0], tabsTourState)
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
                            Image(
                                painter = painterResource(id = R.drawable.ic_tor),
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
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

                // Right Action Controls (Domain Grouping, Clear All, Done)
                Row(
                    modifier = Modifier.tourTarget(tabsTourSteps[2], tabsTourState),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Domain Grouping Toggle
                    if (filteredTabs.size > 1) {
                        Box(
                            modifier = Modifier
                                .sizeIn(minWidth = 36.dp, minHeight = 36.dp)
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
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(if (isGroupedByDomain) colors.actionPrimaryBg else colors.cardBg)
                                    .border(0.8.dp, if (isGroupedByDomain) colors.actionPrimaryBg else colors.border, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = null,
                                    tint = if (isGroupedByDomain) colors.actionPrimaryText else colors.textMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    // Close All in Mode
                    if (filteredTabs.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .sizeIn(minWidth = 36.dp, minHeight = 36.dp)
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = false, radius = 18.dp, color = colors.statusDestructive),
                                    onClick = { showCloseAllDialog = true }
                                )
                                .semantics {
                                    role = Role.Button
                                    contentDescription = "Close all tabs in this mode"
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(colors.cardBg)
                                    .border(0.8.dp, colors.border, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = null,
                                    tint = colors.statusDestructive,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }

                    // Dismiss / Done Button
                    if (onClose != null) {
                        Box(
                            modifier = Modifier
                                .sizeIn(minWidth = 36.dp, minHeight = 36.dp)
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
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(colors.cardBg)
                                    .border(0.8.dp, colors.border, CircleShape),
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
                }
            }

            // 2. Instant Tab Filter Bar (When >= 3 Tabs)
            if (tabs.size > 2) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                        .height(36.dp)
                        .clip(AzokleRadius.full)
                        .background(colors.cardBg)
                        .border(0.8.dp, colors.border, AzokleRadius.full)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = colors.textMuted,
                        modifier = Modifier.size(14.dp)
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
                                    fontSize = 12.5.sp
                                )
                            }
                            innerTextField()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .sizeIn(minWidth = 28.dp, minHeight = 28.dp)
                                .clip(CircleShape)
                                .clickable { searchQuery = "" },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = colors.textMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 3. Adaptive Responsive Tabs Grid
            ActiveTabsGrid(
                filteredTabs = filteredTabs,
                categoryGroups = categoryGroups,
                standaloneTabs = standaloneTabs,
                tabs = tabs,
                activeTabId = activeTabId,
                selectedCategory = selectedCategory,
                onTabSelected = onTabSelected,
                onCloseTab = { tabToClose ->
                    val closed = TabManager.closeTab(tabToClose.id)
                    if (closed != null) {
                        lastClosedTab = closed
                        undoSnackbarJob?.cancel()
                        undoSnackbarJob = scope.launch {
                            delay(4500)
                            lastClosedTab = null
                        }
                    }
                },
                onExpandGroup = { expandedGroup = it },
                onNewTabClick = onNewTabClick,
                onNewPrivateTabClick = {
                    val newId = TabManager.openTab("", "Private Tab", isPrivate = true)
                    val newTab = TabManager.tabs.value.find { it.id == newId }
                    if (newTab != null) onTabSelected(newTab)
                },
                onNewTorTabClick = {
                    val newId = TabManager.openTorTab(context, "about:tor", "Tor Window")
                    val newTab = TabManager.tabs.value.find { it.id == newId }
                    if (newTab != null) onTabSelected(newTab)
                },
                onCreateGroupClick = { showCreateGroupDialog = true },
                tourState = tabsTourState,
                tourSteps = tabsTourSteps,
                modifier = Modifier.weight(1f)
            )
        }

        // 4. Undo Closed Tab Floating Snackbar
        AnimatedVisibility(
            visible = lastClosedTab != null,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 86.dp, start = 16.dp, end = 16.dp)
        ) {
            val closed = lastClosedTab
            if (closed != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(16.dp, AzokleRadius.xl, ambientColor = colors.textPrimary.copy(alpha = 0.1f), spotColor = Color.Black.copy(alpha = 0.2f))
                        .clip(AzokleRadius.xl)
                        .background(colors.cardBg)
                        .border(0.8.dp, colors.border, AzokleRadius.xl)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
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
                                fontSize = 12.5.sp,
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
                                .padding(horizontal = 12.dp, vertical = 5.dp)
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

        // 5. Tab Group Detail Modal Sheet
        val liveExpandedGroup = remember(expandedGroup, groups) {
            if (expandedGroup == null) null
            else groups.find { it.id == expandedGroup?.id }
        }

        liveExpandedGroup?.let { group ->
            val groupTabs = tabs.filter { it.groupId == group.id }.reversed()
            TabGroupDetailSheet(
                group = group,
                tabs = groupTabs,
                activeTabId = activeTabId,
                onTabSelected = { selectedTab ->
                    expandedGroup = null
                    onTabSelected(selectedTab)
                },
                onAddTabInGroup = {
                    expandedGroup = null
                    val newId = TabManager.openTabInGroup("", "New Tab", group.id)
                    val newTab = TabManager.tabs.value.find { it.id == newId }
                    if (newTab != null) onTabSelected(newTab)
                },
                onCloseTab = { tabIdToClose ->
                    TabManager.closeTab(tabIdToClose)
                    if (groupTabs.size <= 1) {
                        expandedGroup = null
                    }
                },
                onDismiss = { expandedGroup = null }
            )
        }

        // 6. Close All Tabs Confirmation Dialog
        if (showCloseAllDialog) {
            AlertDialog(
                onDismissRequest = { showCloseAllDialog = false },
                containerColor = colors.modalBg,
                titleContentColor = colors.textPrimary,
                textContentColor = colors.textSecondary,
                shape = AzokleRadius.xl,
                title = {
                    Text(
                        text = when (selectedCategory) {
                            TabCategory.STANDARD -> "Close all tabs?"
                            TabCategory.PRIVATE -> "Close all private tabs?"
                            TabCategory.TOR -> "Close all Tor windows?"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.5.sp
                    )
                },
                text = {
                    Text(
                        text = "All ${filteredTabs.size} open tabs in this view will be closed.",
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showCloseAllDialog = false
                            when (selectedCategory) {
                                TabCategory.STANDARD -> TabManager.closeCategoryTabs(isPrivateOnly = false, isTorOnly = false)
                                TabCategory.PRIVATE -> TabManager.closeCategoryTabs(isPrivateOnly = true, isTorOnly = false)
                                TabCategory.TOR -> TabManager.closeCategoryTabs(isPrivateOnly = false, isTorOnly = true)
                            }
                            ToastManager.show("Tabs closed in this mode")
                        }
                    ) {
                        Text("Close All", color = colors.statusDestructive, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCloseAllDialog = false }) {
                        Text("Cancel", color = colors.textSecondary)
                    }
                }
            )
        }

        // 7. Bottom Action Area (Centered FAB (+) and Right-Anchored Overflow 3-dots Menu)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 18.dp)
        ) {
            // Centered + Action Button
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .tourTarget(tabsTourSteps[3], tabsTourState)
                    .size(52.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = CircleShape,
                        ambientColor = Color.Black.copy(alpha = 0.35f),
                        spotColor = colors.actionPrimaryBg.copy(alpha = 0.45f)
                    )
                    .clip(CircleShape)
                    .background(colors.actionPrimaryBg)
                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, color = colors.actionPrimaryText.copy(alpha = 0.35f)),
                        onClick = {
                            when (selectedCategory) {
                                TabCategory.STANDARD -> onNewTabClick()
                                TabCategory.PRIVATE -> {
                                    val newId = TabManager.openTab("", "Private Tab", isPrivate = true)
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
                            TabCategory.STANDARD -> "New Tab"
                            TabCategory.PRIVATE -> "New Private Tab"
                            TabCategory.TOR -> "New Tor Window"
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = colors.actionPrimaryText,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Right-Anchored 3-Dots Button with Overflow Menu
            Box(
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(
                            elevation = 6.dp,
                            shape = CircleShape,
                            ambientColor = Color.Black.copy(alpha = 0.3f),
                            spotColor = Color.Black.copy(alpha = 0.2f)
                        )
                        .clip(CircleShape)
                        .background(colors.cardBg)
                        .border(0.8.dp, colors.border, CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = colors.textPrimary.copy(alpha = 0.2f)),
                            onClick = { showOverflowMenu = true }
                        )
                        .semantics {
                            role = Role.Button
                            contentDescription = "More Tab Options"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = null,
                        tint = colors.textPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                DropdownMenu(
                    expanded = showOverflowMenu,
                    onDismissRequest = { showOverflowMenu = false },
                    modifier = Modifier
                        .background(colors.modalBg)
                        .border(0.8.dp, colors.border, RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    // 1. New Tab
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "New Tab",
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.5.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = colors.actionPrimaryBg,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        onClick = {
                            showOverflowMenu = false
                            selectedCategory = TabCategory.STANDARD
                            onNewTabClick()
                        }
                    )

                    // 2. New Group Tab
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "New Group Tab",
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.5.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.CreateNewFolder,
                                contentDescription = null,
                                tint = colors.textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        onClick = {
                            showOverflowMenu = false
                            showCreateGroupDialog = true
                        }
                    )

                    // 3. New Private Tab
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "New Private Tab",
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.5.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = colors.textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        onClick = {
                            showOverflowMenu = false
                            selectedCategory = TabCategory.PRIVATE
                            val newId = TabManager.openTab("", "Private Tab", isPrivate = true)
                            val newTab = TabManager.tabs.value.find { it.id == newId }
                            if (newTab != null) onTabSelected(newTab)
                        }
                    )

                    // 4. New Tor Window
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "New Tor Window",
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.5.sp
                            )
                        },
                        leadingIcon = {
                            Image(
                                painter = painterResource(id = R.drawable.ic_tor),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        onClick = {
                            showOverflowMenu = false
                            selectedCategory = TabCategory.TOR
                            val newId = TabManager.openTorTab(context, "about:tor", "Tor Window")
                            val newTab = TabManager.tabs.value.find { it.id == newId }
                            if (newTab != null) onTabSelected(newTab)
                        }
                    )
                }
            }
        }

        // 8. Create Tab Group Modal Dialog
        if (showCreateGroupDialog) {
            CreateTabGroupDialog(
                colors = colors,
                onConfirm = { title, color ->
                    showCreateGroupDialog = false
                    val isPriv = selectedCategory == TabCategory.PRIVATE
                    val isTor = selectedCategory == TabCategory.TOR
                    val newGroupId = TabManager.createGroup(
                        title = title,
                        color = color,
                        isPrivate = isPriv,
                        isTor = isTor
                    )
                    val defaultUrl = if (isTor) "about:tor" else ""
                    val newTabId = TabManager.openTabInGroup(
                        url = defaultUrl,
                        title = if (isTor) "Tor Window" else "New Tab",
                        groupId = newGroupId
                    )
                    val newTab = TabManager.tabs.value.find { it.id == newTabId }
                    if (newTab != null) onTabSelected(newTab)
                    ToastManager.show("Tab group \"$title\" created")
                },
                onDismiss = { showCreateGroupDialog = false }
            )
        }

        // Tour Overlay
        DriverTourOverlay(tourState = tabsTourState)
    }
}

/**
 * Isolated Active Tabs Grid:
 * Standalone composable so changing overlays avoids recomposing the grid and cards.
 */
@Composable
private fun ActiveTabsGrid(
    filteredTabs: List<TabSession>,
    categoryGroups: List<TabGroup>,
    standaloneTabs: List<TabSession>,
    tabs: List<TabSession>,
    activeTabId: String?,
    selectedCategory: TabCategory,
    onTabSelected: (TabSession) -> Unit,
    onCloseTab: (TabSession) -> Unit,
    onExpandGroup: (TabGroup) -> Unit,
    onNewTabClick: () -> Unit,
    onNewPrivateTabClick: () -> Unit,
    onNewTorTabClick: () -> Unit,
    onCreateGroupClick: () -> Unit,
    tourState: TourState,
    tourSteps: List<TourStep>,
    modifier: Modifier = Modifier
) {
    val colors = AzokleTheme.colors
    if (filteredTabs.isEmpty()) {
        val emptyTourModifier = Modifier.tourTarget(tourSteps[1], tourState)
        Box(
            modifier = modifier
                .fillMaxWidth()
                .then(emptyTourModifier),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceElevated)
                        .border(0.8.dp, colors.border, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedCategory == TabCategory.TOR) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_tor),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                    } else {
                        Icon(
                            imageVector = when (selectedCategory) {
                                TabCategory.STANDARD -> Icons.Default.Layers
                                TabCategory.PRIVATE -> Icons.Default.VisibilityOff
                                TabCategory.TOR -> Icons.Default.Layers
                            },
                            contentDescription = null,
                            tint = colors.textPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = when (selectedCategory) {
                        TabCategory.STANDARD -> "No Open Tabs"
                        TabCategory.PRIVATE -> "No Private Tabs"
                        TabCategory.TOR -> "No Tor Windows"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = when (selectedCategory) {
                        TabCategory.STANDARD -> "Open a new tab to explore the sovereign web."
                        TabCategory.PRIVATE -> "Private tabs do not save history, cookies, or telemetry."
                        TabCategory.TOR -> "Tor routes traffic through 3 encrypted relays with zero traces."
                    },
                    fontSize = 12.5.sp,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Actions Row in Empty State
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(AzokleRadius.full)
                            .background(colors.actionPrimaryBg)
                            .clickable {
                                when (selectedCategory) {
                                    TabCategory.STANDARD -> onNewTabClick()
                                    TabCategory.PRIVATE -> onNewPrivateTabClick()
                                    TabCategory.TOR -> onNewTorTabClick()
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 9.dp)
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
                            if (selectedCategory == TabCategory.TOR) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_tor),
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = colors.actionPrimaryText,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (selectedCategory) {
                                    TabCategory.STANDARD -> "New Tab"
                                    TabCategory.PRIVATE -> "Private Tab"
                                    TabCategory.TOR -> "Tor Window"
                                },
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.actionPrimaryText
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(AzokleRadius.full)
                            .background(colors.surfaceElevated)
                            .border(0.8.dp, colors.border, AzokleRadius.full)
                            .clickable(onClick = onCreateGroupClick)
                            .padding(horizontal = 14.dp, vertical = 9.dp)
                            .semantics {
                                role = Role.Button
                                contentDescription = "Create New Tab Group"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CreateNewFolder,
                                contentDescription = null,
                                tint = colors.statusTor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "New Group",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                        }
                    }
                }
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 155.dp),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = modifier.fillMaxWidth()
        ) {
            // 1. Tab Group Cards
            items(
                items = categoryGroups,
                key = { "group_${it.id}" },
                contentType = { "tab_group" }
            ) { group ->
                val groupTabs = remember(tabs, group.id) { tabs.filter { it.groupId == group.id }.reversed() }
                val isActive = groupTabs.any { it.id == activeTabId }
                TabGroupCard(
                    group = group,
                    tabs = groupTabs,
                    isActive = isActive,
                    onClick = { onExpandGroup(group) },
                    onCloseGroup = { TabManager.closeGroup(group.id) }
                )
            }

            // 2. Standalone Tabs
            items(
                items = standaloneTabs,
                key = { it.id },
                contentType = { "tab_card" }
            ) { tab ->
                val isFirstCard = tab.id == standaloneTabs.firstOrNull()?.id
                val cardTourModifier = if (isFirstCard) Modifier.tourTarget(tourSteps[1], tourState) else Modifier
                val isActive = tab.id == activeTabId

                ActiveTabCard(
                    tab = tab,
                    isActive = isActive,
                    onTabSelected = onTabSelected,
                    onCloseTab = onCloseTab,
                    modifier = cardTourModifier
                )
            }
        }
    }
}

/**
 * Ultra-Smooth Sovereign Tab Card:
 * - Instant tactile ripple feedback on tap.
 * - Hardware-accelerated cached thumbnail previews.
 * - Accessible close button with haptic feedback.
 */
@Composable
private fun ActiveTabCard(
    tab: TabSession,
    isActive: Boolean,
    onTabSelected: (TabSession) -> Unit,
    onCloseTab: (TabSession) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AzokleTheme.colors
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(18.dp)
    val isHome = tab.url.isBlank() || tab.url == "about:home" || tab.url == "about:blank"
    val host = remember(tab.url) {
        try {
            Uri.parse(tab.url).host?.removePrefix("www.")?.ifBlank { tab.url } ?: tab.url
        } catch (e: Exception) {
            tab.url
        }
    }

    val activeAccent = if (!tab.groupId.isNullOrBlank()) {
        tab.groupColor?.let { Color(it) } ?: colors.statusAccent
    } else {
        colors.statusAccent
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(230.dp)
            .shadow(
                elevation = if (isActive) 5.dp else 1.dp,
                shape = shape,
                spotColor = if (isActive) activeAccent.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.2f)
            )
            .clip(shape)
            .background(colors.cardBg)
            .border(
                width = if (isActive) 1.5.dp else 0.8.dp,
                color = if (isActive) activeAccent else colors.border.copy(alpha = 0.55f),
                shape = shape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = activeAccent.copy(alpha = 0.25f)),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onTabSelected(tab)
                }
            )
            .semantics {
                role = Role.Button
                contentDescription = "Tab: ${tab.title.ifBlank { if (isHome) "New Tab" else host }}${if (isActive) ", Active" else ""}"
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .background(colors.surfaceElevated.copy(alpha = 0.85f))
                    .padding(start = 10.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(AzokleRadius.sm)
                        .background(colors.surface)
                        .border(0.5.dp, colors.border.copy(alpha = 0.5f), AzokleRadius.sm),
                    contentAlignment = Alignment.Center
                ) {
                    if (isHome) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            tint = colors.actionPrimaryBg,
                            modifier = Modifier.size(12.dp)
                        )
                    } else if (tab.isTor) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_tor),
                            contentDescription = "Tor Tab",
                            modifier = Modifier.size(12.dp)
                        )
                    } else if (tab.isPrivate) {
                        Icon(
                            imageVector = Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = Color(0xFFA855F7),
                            modifier = Modifier.size(12.dp)
                        )
                    } else {
                        FaviconImage(
                            url = tab.url,
                            faviconUrl = tab.faviconUrl,
                            isTor = tab.isTor,
                            size = 13.dp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (!tab.groupId.isNullOrBlank()) {
                    val groupColor = tab.groupColor?.let { Color(it) } ?: colors.statusAccent
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(groupColor)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                }

                Text(
                    text = if (isHome) "New Tab" else tab.title.ifBlank { host },
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(4.dp))

                // Close Tab Button
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
                                onCloseTab(tab)
                            }
                        )
                        .semantics {
                            role = Role.Button
                            contentDescription = "Close tab: ${tab.title}"
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

            // Thumbnail / Content Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(colors.surfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                val cachedThumbnail = TabThumbnailCache.get(tab.id)
                if (cachedThumbnail != null) {
                    val imageBitmap = remember(cachedThumbnail) { cachedThumbnail.asImageBitmap() }
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
                                    listOf(
                                        Color.Transparent,
                                        Color(0x08000000),
                                        Color(0x40000000)
                                    )
                                )
                            )
                    )
                } else if (isHome) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(tab.wallpaperResId ?: WallpaperManager.getCurrentWallpaper())
                                .crossfade(false)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x33000000))
                        )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.align(Alignment.Center)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Home",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp)
                    ) {
                        FaviconImage(
                            url = tab.url,
                            faviconUrl = tab.faviconUrl,
                            isTor = tab.isTor,
                            size = 28.dp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = host,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Active Tab Indicator Badge
        if (isActive) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(activeAccent)
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

@Composable
private fun CreateTabGroupDialog(
    colors: AzokleThemeColors,
    onConfirm: (title: String, color: Long) -> Unit,
    onDismiss: () -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    val colorOptions = listOf(
        0xFF38BDF8, // Sky Blue
        0xFF22C55E, // Emerald
        0xFFA855F7, // Purple
        0xFFF59E0B, // Amber
        0xFFF43F5E, // Rose
        0xFF6366F1  // Indigo
    )
    var selectedColor by remember { mutableStateOf(colorOptions[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.modalBg,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textSecondary,
        shape = AzokleRadius.xl,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CreateNewFolder,
                    contentDescription = null,
                    tint = Color(selectedColor),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "New Tab Group",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Group tabs together to organize your research or projects.",
                    fontSize = 12.5.sp,
                    color = colors.textSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    placeholder = { Text("Group name", fontSize = 13.5.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(selectedColor),
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedContainerColor = colors.surfaceElevated,
                        unfocusedContainerColor = colors.surfaceElevated
                    ),
                    shape = AzokleRadius.md,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    colorOptions.forEach { col ->
                        val isSelected = selectedColor == col
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(col))
                                .border(
                                    width = if (isSelected) 2.5.dp else 0.dp,
                                    color = if (isSelected) colors.textPrimary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = col },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val finalTitle = groupName.trim().ifBlank { "New Group" }
                    onConfirm(finalTitle, selectedColor)
                }
            ) {
                Text("Create", color = colors.actionPrimaryBg, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textSecondary)
            }
        }
    )
}
