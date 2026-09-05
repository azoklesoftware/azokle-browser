package com.azokle.search.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import com.azokle.search.data.PreferencesManager
import com.azokle.search.ui.tour.DriverTourOverlay
import com.azokle.search.ui.tour.TourShape
import com.azokle.search.ui.tour.TourStep
import com.azokle.search.ui.tour.rememberTourState
import com.azokle.search.ui.tour.tourTarget
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
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.ui.res.painterResource
import com.azokle.search.R
import com.azokle.search.browser.ui.components.FaviconImage
import com.azokle.search.ui.theme.AudiowideFontFamily
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.azokle.search.browser.tabs.TabGroup
import com.azokle.search.browser.tabs.TabManager
import com.azokle.search.browser.tabs.TabSession
import com.azokle.search.browser.ui.components.TabGroupCard
import com.azokle.search.browser.ui.components.TabGroupDetailSheet
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
    modifier: Modifier = Modifier,
    forceStartTour: Boolean = false,
    onTourConsumed: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
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
                description = "Tap this plus button anytime to open a new tab, a private tab, a Tor window, or create a tab group.",
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
    var showNewTabMenu by remember { mutableStateOf(false) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }

    val standardCount = remember(tabs) { tabs.count { !it.isPrivate && !it.isTor } }
    val privateCount = remember(tabs) { tabs.count { it.isPrivate && !it.isTor } }
    val torCount = remember(tabs) { tabs.count { it.isTor } }

    val categoryGroups = remember(groups, selectedCategory, searchQuery, tabs) {
        groups.filter { group ->
            when (selectedCategory) {
                TabCategory.STANDARD -> !group.isPrivate && !group.isTor
                TabCategory.PRIVATE -> group.isPrivate && !group.isTor
                TabCategory.TOR -> group.isTor
            }
        }.filter { group ->
            if (searchQuery.isBlank()) true
            else {
                val groupTabList = tabs.filter { it.groupId == group.id }
                group.title.contains(searchQuery, ignoreCase = true) ||
                groupTabList.any { it.title.contains(searchQuery, ignoreCase = true) || it.url.contains(searchQuery, ignoreCase = true) }
            }
        }
    }

    val groupedTabIds = remember(categoryGroups) {
        categoryGroups.flatMap { it.tabIds }.toSet()
    }

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

    val allActiveGroupIds = remember(groups) { groups.map { it.id }.toSet() }
    val standaloneTabs = remember(displayTabs, groupedTabIds, allActiveGroupIds) {
        displayTabs.filter { !groupedTabIds.contains(it.id) && (it.groupId == null || !allActiveGroupIds.contains(it.groupId)) }
    }

    BackHandler(enabled = tabsTourState.isVisible || expandedGroup != null || onClose != null) {
        if (tabsTourState.isVisible) {
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

                // Right Action Controls (Clear All, + New Tab, Done)
                Row(
                    modifier = Modifier.tourTarget(tabsTourSteps[2], tabsTourState),
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
                val emptyTourModifier = if (filteredTabs.isEmpty()) Modifier.tourTarget(tabsTourSteps[1], tabsTourState) else Modifier
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .then(emptyTourModifier),
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
                        if (selectedCategory == TabCategory.TOR) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_tor),
                                contentDescription = null,
                                modifier = Modifier.size(36.dp)
                            )
                        } else {
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

                        // Actions Row in Empty State (New Tab + New Tab Group)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Primary CTA Button: Open New Tab
                            Box(
                                modifier = Modifier
                                    .clip(AzokleRadius.full)
                                    .background(colors.actionPrimaryBg)
                                    .clickable {
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
                                    .padding(horizontal = 18.dp, vertical = 11.dp)
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
                                            modifier = Modifier.size(16.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = colors.actionPrimaryText,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = when (selectedCategory) {
                                            TabCategory.STANDARD -> "New Tab"
                                            TabCategory.PRIVATE -> "Private Tab"
                                            TabCategory.TOR -> "Tor Window"
                                        },
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.actionPrimaryText
                                    )
                                }
                            }

                            // Secondary CTA Button: New Tab Group
                            Box(
                                modifier = Modifier
                                    .clip(AzokleRadius.full)
                                    .background(colors.surfaceElevated)
                                    .border(1.dp, colors.border, AzokleRadius.full)
                                    .clickable { showCreateGroupDialog = true }
                                    .padding(horizontal = 16.dp, vertical = 11.dp)
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
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "New Group",
                                        fontSize = 13.sp,
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // 1. Render Tab Group Cards (Brave-Grade Multi-Tab Stack)
                    items(categoryGroups, key = { "group_${it.id}" }) { group ->
                        val groupTabs = tabs.filter { it.groupId == group.id }
                        val isActive = groupTabs.any { it.id == activeTabId }
                        TabGroupCard(
                            group = group,
                            tabs = groupTabs,
                            isActive = isActive,
                            onClick = { expandedGroup = group },
                            onCloseGroup = { TabManager.closeGroup(group.id) }
                        )
                    }

                    // 2. Render Standalone Tabs
                    items(standaloneTabs, key = { it.id }) { tab ->
                        val isFirstCard = tab.id == standaloneTabs.firstOrNull()?.id
                        val cardTourModifier = if (isFirstCard) Modifier.tourTarget(tabsTourSteps[1], tabsTourState) else Modifier
                        val isActive = tab.id == activeTabId

                        SwipeableTabCard(
                            tab = tab,
                            isActive = isActive,
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
                            modifier = cardTourModifier
                        )
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

        // 5. Tab Group Detail Modal Sheet (Expanded Group View with Live State)
        val liveExpandedGroup = remember(expandedGroup, groups) {
            if (expandedGroup == null) null
            else groups.find { it.id == expandedGroup?.id }
        }

        liveExpandedGroup?.let { group ->
            val groupTabs = tabs.filter { it.groupId == group.id }
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
                        fontSize = 17.sp
                    )
                },
                text = {
                    Text(
                        text = "All ${filteredTabs.size} open tabs in this view will be closed. Are you sure you want to proceed?",
                        fontSize = 13.5.sp,
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
                            com.azokle.search.ui.components.ToastManager.show("Tabs closed in this mode")
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

        // 5. Bottom Area Centered Floating Action Button (+)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 18.dp)
        ) {
            Box(
                modifier = Modifier
                    .tourTarget(tabsTourSteps[3], tabsTourState)
                    .size(56.dp)
                    .shadow(
                        elevation = 14.dp,
                        shape = CircleShape,
                        ambientColor = Color.Black.copy(alpha = 0.4f),
                        spotColor = colors.actionPrimaryBg.copy(alpha = 0.5f)
                    )
                    .clip(CircleShape)
                    .background(colors.actionPrimaryBg)
                    .border(1.2.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, color = colors.actionPrimaryText.copy(alpha = 0.35f)),
                        onClick = { showNewTabMenu = true }
                    )
                    .semantics {
                        role = Role.Button
                        contentDescription = "New Tab Options"
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = colors.actionPrimaryText,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // 6. New Tab Options Bottom Sheet (Standard, Private, Tor, Tab Group)
        if (showNewTabMenu) {
            NewTabOptionsSheet(
                colors = colors,
                onDismiss = { showNewTabMenu = false },
                onNewStandardTab = {
                    showNewTabMenu = false
                    selectedCategory = TabCategory.STANDARD
                    onNewTabClick()
                },
                onNewPrivateTab = {
                    showNewTabMenu = false
                    selectedCategory = TabCategory.PRIVATE
                    val newId = TabManager.openTab("", "Private Tab", isPrivate = true)
                    val newTab = TabManager.tabs.value.find { it.id == newId }
                    if (newTab != null) onTabSelected(newTab)
                },
                onNewTorWindow = {
                    showNewTabMenu = false
                    selectedCategory = TabCategory.TOR
                    val newId = TabManager.openTorTab(context, "about:tor", "Tor Window")
                    val newTab = TabManager.tabs.value.find { it.id == newId }
                    if (newTab != null) onTabSelected(newTab)
                },
                onNewTabGroup = {
                    showNewTabMenu = false
                    showCreateGroupDialog = true
                }
            )
        }

        // 7. Create Tab Group Modal Dialog
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
                    com.azokle.search.ui.components.ToastManager.show("Tab group \"$title\" created 📑")
                },
                onDismiss = { showCreateGroupDialog = false }
            )
        }

        // 🚀 Driver.js-Grade Interactive Spotlight Tour Overlay for Tabs Screen
        DriverTourOverlay(tourState = tabsTourState)
    }
}

/**
 * Sovereign Swipeable Tab Card:
 * - Left-to-right swipe-to-delete with animated reveal, haptic feedback, and undo capability.
 * - Refined squircle geometry (20.dp), clean typography, crisp favicons, and live thumbnails.
 * - Tactile tap response and accessible touch targets.
 */
@Composable
private fun SwipeableTabCard(
    tab: TabSession,
    isActive: Boolean,
    onTabSelected: (TabSession) -> Unit,
    onCloseTab: (TabSession) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AzokleTheme.colors
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(20.dp)
    val isHome = tab.url.isBlank() || tab.url == "about:home" || tab.url == "about:blank"
    val host = remember(tab.url) {
        try {
            Uri.parse(tab.url).host?.removePrefix("www.")?.ifBlank { tab.url } ?: tab.url
        } catch (e: Exception) {
            tab.url
        }
    }

    val offsetX = remember { Animatable(0f) }
    var cardWidthPx by remember { mutableStateOf(1) }
    val threshold = cardWidthPx * 0.38f
    val swipeProgress = (offsetX.value / cardWidthPx.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val isPastThreshold = offsetX.value >= threshold

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(236.dp)
            .clip(shape)
            .onSizeChanged { cardWidthPx = it.width }
    ) {
        // 1. Background Reveal Layer (Destructive Swipe Surface)
        val revealBg by animateColorAsState(
            targetValue = if (isPastThreshold) colors.statusDestructive
            else colors.statusDestructive.copy(alpha = (0.22f + swipeProgress * 0.65f).coerceIn(0.2f, 0.9f)),
            label = "revealBg"
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .background(revealBg)
                .padding(start = 18.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = "Close tab",
                    tint = Color.White,
                    modifier = Modifier.size(
                        (20.dp + (8.dp * swipeProgress)).coerceIn(20.dp, 28.dp)
                    )
                )
                if (swipeProgress > 0.28f) {
                    Text(
                        text = if (isPastThreshold) "Release to close" else "Close",
                        color = Color.White,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }

        // 2. Foreground Tab Card Surface (Draggable from Left to Right & Clickable)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(tab.id) {
                    coroutineScope {
                        while (isActive) {
                            awaitPointerEventScope {
                                val down = awaitFirstDown(requireUnconsumed = true)
                                val downPointerId = down.id
                                var totalX = 0f
                                var isDrag = false
                                var hapticFired = false

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == downPointerId } ?: break
                                    if (!change.pressed) {
                                        if (!isDrag) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onTabSelected(tab)
                                        } else {
                                            val currentOffset = offsetX.value
                                            if (currentOffset >= threshold) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                launch {
                                                    offsetX.animateTo(cardWidthPx.toFloat(), tween(160))
                                                    onCloseTab(tab)
                                                }
                                            } else {
                                                launch {
                                                    offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium))
                                                }
                                            }
                                        }
                                        break
                                    }

                                    val dragX = change.position.x - change.previousPosition.x
                                    val dragY = change.position.y - change.previousPosition.y

                                    if (!isDrag) {
                                        totalX += dragX
                                        if (Math.abs(totalX) > viewConfiguration.touchSlop) {
                                            if (Math.abs(dragY) > Math.abs(dragX) * 1.4f && totalX <= 0f) {
                                                break
                                            }
                                            isDrag = true
                                        }
                                    }

                                    if (isDrag) {
                                        val newX = (offsetX.value + dragX).coerceAtLeast(0f)
                                        launch { offsetX.snapTo(newX) }
                                        if (newX >= threshold && !hapticFired) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            hapticFired = true
                                        } else if (newX < threshold && hapticFired) {
                                            hapticFired = false
                                        }
                                        change.consume()
                                    }
                                }
                            }
                        }
                    }
                }
                .shadow(
                    elevation = if (isActive) 8.dp else 2.dp,
                    shape = shape,
                    ambientColor = if (isActive) colors.actionPrimaryBg.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.06f),
                    spotColor = if (isActive) colors.actionPrimaryBg.copy(alpha = 0.32f) else Color.Black.copy(alpha = 0.10f)
                )
                .clip(shape)
                .background(colors.cardBg)
                .border(
                    width = if (isActive) 2.dp else 1.dp,
                    color = if (isActive) colors.actionPrimaryBg else colors.border.copy(alpha = 0.55f),
                    shape = shape
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
                        .height(40.dp)
                        .background(colors.surfaceElevated.copy(alpha = 0.85f))
                        .padding(start = 10.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
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
                                modifier = Modifier.size(13.dp)
                            )
                        } else if (tab.isTor) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_tor),
                                contentDescription = "Tor Tab",
                                modifier = Modifier.size(13.dp)
                            )
                        } else if (tab.isPrivate) {
                            Icon(
                                imageVector = Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = Color(0xFFA855F7),
                                modifier = Modifier.size(13.dp)
                            )
                        } else {
                            FaviconImage(
                                url = tab.url,
                                faviconUrl = tab.faviconUrl,
                                isTor = tab.isTor,
                                size = 14.dp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = if (isHome) "New Tab" else tab.title.ifBlank { host },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    // Close Tab Button (Accessible Touch Delegate)
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(colors.surface)
                            .border(0.5.dp, colors.border.copy(alpha = 0.4f), CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = false, radius = 14.dp, color = colors.statusDestructive),
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
                    if (tab.thumbnail != null) {
                        Image(
                            bitmap = tab.thumbnail.asImageBitmap(),
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
                            Image(
                                painter = painterResource(id = tab.wallpaperResId ?: com.azokle.search.browser.wallpaper.WallpaperManager.getCurrentWallpaper()),
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
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "azokle",
                                    fontFamily = AudiowideFontFamily,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(7.dp))
                                Box(
                                    modifier = Modifier
                                        .width(110.dp)
                                        .height(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x88000000))
                                        .border(0.5.dp, Color(0x55FFFFFF), CircleShape)
                                        .padding(horizontal = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.8f),
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Text(
                                            text = "Search",
                                            fontSize = 8.5.sp,
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Mic,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.8f),
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(colors.surfaceElevated, colors.cardBg)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(colors.surface)
                                        .border(1.dp, colors.border.copy(alpha = 0.5f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    FaviconImage(
                                        url = tab.url,
                                        faviconUrl = tab.faviconUrl,
                                        isTor = tab.isTor,
                                        size = 22.dp
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(colors.surface)
                                        .border(0.5.dp, colors.border.copy(alpha = 0.4f), CircleShape)
                                        .padding(horizontal = 10.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = host,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Status Badges
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (tab.isPrivate) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFA855F7))
                                    .padding(horizontal = 6.dp, vertical = 2.5.dp)
                            ) {
                                Text(
                                    text = "PRIVATE",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.White
                                )
                            }
                        }

                        if (tab.isTor) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF8B5CF6))
                                    .padding(horizontal = 5.dp, vertical = 2.5.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_tor),
                                        contentDescription = null,
                                        modifier = Modifier.size(9.dp)
                                    )
                                    Text(
                                        text = "TOR",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        if (isActive) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(colors.actionPrimaryBg)
                                    .padding(horizontal = 6.dp, vertical = 2.5.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
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
            }
        }
    }
}

@Composable
private fun CreateTabGroupDialog(
    colors: com.azokle.search.ui.theme.AzokleThemeColors,
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
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "New Tab Group",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Group tabs together to organize your research, reading, or projects.",
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    placeholder = { Text("Group name (e.g. Work, Research)", fontSize = 14.sp) },
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

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "THEME COLOR",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textSecondary,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    colorOptions.forEach { col ->
                        val isSelected = selectedColor == col
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(col))
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) colors.textPrimary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = col },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
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
                Text("Create & Open Tab", color = colors.actionPrimaryBg, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textSecondary)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewTabOptionsSheet(
    colors: com.azokle.search.ui.theme.AzokleThemeColors,
    onDismiss: () -> Unit,
    onNewStandardTab: () -> Unit,
    onNewPrivateTab: () -> Unit,
    onNewTorWindow: () -> Unit,
    onNewTabGroup: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.modalBg,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(colors.border)
            )
        },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 4.dp)
                .padding(bottom = 16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "New Tab",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Choose browsing mode or workspace",
                        fontSize = 12.sp,
                        color = colors.textMuted
                    )
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(colors.cardBg)
                        .border(1.dp, colors.border, CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = colors.textPrimary),
                            onClick = onDismiss
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            // Options List
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // 1. New Standard Tab
                NewTabOptionItem(
                    title = "New Tab",
                    subtitle = "Fast sovereign search & browsing",
                    iconColor = colors.actionPrimaryBg,
                    iconBgColor = colors.actionPrimaryBg.copy(alpha = 0.15f),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = colors.actionPrimaryBg,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    colors = colors,
                    onClick = onNewStandardTab
                )

                // 2. New Private Tab
                NewTabOptionItem(
                    title = "New Private Tab",
                    subtitle = "Zero history, cookies, or telemetry",
                    iconColor = Color(0xFFA855F7),
                    iconBgColor = Color(0xFFA855F7).copy(alpha = 0.15f),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = Color(0xFFA855F7),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    colors = colors,
                    onClick = onNewPrivateTab
                )

                // 3. New Tor Window
                NewTabOptionItem(
                    title = "New Tor Window",
                    subtitle = "3-hop onion encrypted anonymity",
                    iconColor = Color(0xFF8B5CF6),
                    iconBgColor = Color(0xFF8B5CF6).copy(alpha = 0.15f),
                    icon = {
                        Image(
                            painter = painterResource(id = R.drawable.ic_tor),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    colors = colors,
                    onClick = onNewTorWindow
                )

                // 4. New Tab Group
                NewTabOptionItem(
                    title = "New Tab Group",
                    subtitle = "Organize tabs in a workspace folder",
                    iconColor = Color(0xFF10B981),
                    iconBgColor = Color(0xFF10B981).copy(alpha = 0.15f),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.CreateNewFolder,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    colors = colors,
                    onClick = onNewTabGroup
                )
            }
        }
    }
}

@Composable
private fun NewTabOptionItem(
    title: String,
    subtitle: String,
    iconColor: Color,
    iconBgColor: Color,
    icon: @Composable () -> Unit,
    colors: com.azokle.search.ui.theme.AzokleThemeColors,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.cardBg)
            .border(1.dp, colors.border, RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = iconColor.copy(alpha = 0.2f)),
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(iconBgColor)
                .border(0.8.dp, iconColor.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.5.sp,
                color = colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = colors.textMuted.copy(alpha = 0.5f),
            modifier = Modifier.size(18.dp)
        )
    }
}
