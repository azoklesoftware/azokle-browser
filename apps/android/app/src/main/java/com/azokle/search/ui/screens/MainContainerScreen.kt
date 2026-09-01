package com.azokle.search.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import com.azokle.search.browser.tabs.TabManager
import com.azokle.search.browser.ui.BrowserBottomBar
import com.azokle.search.browser.ui.BrowserMenuSheet
import com.azokle.search.browser.ui.BrowserScreen
import com.azokle.search.navigation.AppNavTab
import com.azokle.search.ui.components.FireIncinerationOverlay
import com.azokle.search.ui.theme.AzokleTheme
import com.azokle.search.viewmodel.HomeViewModel

@Composable
fun MainContainerScreen(
    viewModel: HomeViewModel,
    onVoiceSearchRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(AppNavTab.SEARCH) }
    var isAppBottomBarVisible by remember { mutableStateOf(true) }
    var isBurningData by remember { mutableStateOf(false) }
    var showMenuSheet by remember { mutableStateOf(false) }
    var showDownloadsScreen by remember { mutableStateOf(false) }
    var showBookmarksScreen by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()
    val openTabs by TabManager.tabs.collectAsState()
    val discoverViewModel: com.azokle.search.viewmodel.DiscoverViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

    androidx.activity.compose.BackHandler(enabled = showBookmarksScreen) {
        showBookmarksScreen = false
    }

    androidx.activity.compose.BackHandler(enabled = !showBookmarksScreen && showDownloadsScreen) {
        showDownloadsScreen = false
    }

    androidx.activity.compose.BackHandler(enabled = !showBookmarksScreen && !showDownloadsScreen && currentTab != AppNavTab.SEARCH) {
        currentTab = AppNavTab.SEARCH
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta < -12) {
                    isAppBottomBarVisible = false
                } else if (delta > 12) {
                    isAppBottomBarVisible = true
                }
                return Offset.Zero
            }
        }
    }

    // 1. Top-level In-Built Privacy Browser
    if (uiState.inAppBrowserUrl != null) {
        BrowserScreen(
            initialUrl = uiState.inAppBrowserUrl!!,
            onClose = { viewModel.closeInAppBrowser() },
            viewModel = viewModel
        )
        return
    }

    val isFullScreenOverlay = uiState.isSearchExpanded

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AzokleTheme.colors.background)
            .nestedScroll(nestedScrollConnection)
    ) {
        // Silky-Smooth Animated Tab Content Transitions
        AnimatedContent(
            targetState = currentTab,
            transitionSpec = {
                val enter = fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                        scaleIn(initialScale = 0.98f, animationSpec = tween(220, easing = FastOutSlowInEasing))
                val exit = fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
                        scaleOut(targetScale = 1.01f, animationSpec = tween(180, easing = FastOutSlowInEasing))
                enter togetherWith exit
            },
            label = "tabTransition",
            modifier = Modifier.fillMaxSize()
        ) { targetTab ->
            when (targetTab) {
                AppNavTab.SEARCH -> {
                    HomeScreen(
                        viewModel = viewModel,
                        onVoiceSearchRequest = onVoiceSearchRequest
                    )
                }
                AppNavTab.DISCOVER -> {
                    DiscoverScreen(
                        viewModel = discoverViewModel,
                        onTopicClick = { topic ->
                            currentTab = AppNavTab.SEARCH
                            viewModel.onQueryChange(topic)
                            viewModel.executeSearch(topic)
                        },
                        onNewsClick = { url ->
                            viewModel.openNewsArticle(url)
                        }
                    )
                }
                AppNavTab.TABS -> {
                    ActiveTabsScreen(
                        onTabSelected = { tab ->
                            TabManager.selectTab(tab.id)
                            viewModel.openInAppBrowser(tab.url, isTor = tab.isTor)
                        },
                        onNewTabClick = {
                            val newId = TabManager.openTab("https://search.azokle.com", "Search")
                            TabManager.selectTab(newId)
                            viewModel.openInAppBrowser("https://search.azokle.com")
                        }
                    )
                }
                AppNavTab.HISTORY -> {
                    HistoryScreen(
                        viewModel = viewModel,
                        onHistoryQueryClick = { query ->
                            currentTab = AppNavTab.SEARCH
                            viewModel.onQueryChange(query)
                            viewModel.executeSearch(query)
                        },
                        onOpenUrl = { url ->
                            viewModel.openInAppBrowser(url)
                        }
                    )
                }
            }
        }

        // Shared Unified BrowserBottomBar for Home & Discover
        AnimatedVisibility(
            visible = !isFullScreenOverlay && isAppBottomBarVisible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            BrowserBottomBar(
                openTabsCount = openTabs.size.coerceAtLeast(1),
                isHomeSelected = currentTab == AppNavTab.SEARCH,
                isDiscoverSelected = currentTab == AppNavTab.DISCOVER,
                isTabsSelected = currentTab == AppNavTab.TABS,
                onHomeClick = { currentTab = AppNavTab.SEARCH },
                onDiscoverClick = { currentTab = AppNavTab.DISCOVER },
                onTabsClick = { currentTab = AppNavTab.TABS },
                onBurnDataClick = { isBurningData = true },
                onMenuClick = { showMenuSheet = true }
            )
        }

        // Shared Browser Menu Sheet for Home & Discover
        if (showMenuSheet) {
            BrowserMenuSheet(
                url = "https://search.azokle.com",
                title = "Azokle Search",
                isDesktopMode = false,
                isBookmarked = false,
                blockedTrackersCount = 0,
                isBrowserContext = false,
                onOpenNewTab = {
                    val newId = TabManager.openTab("https://search.azokle.com", "Search")
                    TabManager.selectTab(newId)
                    viewModel.openInAppBrowser("https://search.azokle.com")
                },
                onOpenNewPrivateTab = {
                    val newId = TabManager.openPrivateTab("https://search.azokle.com", "Private Search")
                    TabManager.selectTab(newId)
                    viewModel.openInAppBrowser("https://search.azokle.com")
                },
                onOpenTorWindow = {
                    val newTorId = TabManager.openTorTab(context, "https://check.torproject.org", "Tor")
                    TabManager.selectTab(newTorId)
                    viewModel.openInAppBrowser("https://check.torproject.org", isTor = true)
                },
                onOpenBookmarks = {
                    showBookmarksScreen = true
                },
                onOpenDownloads = {
                    showDownloadsScreen = true
                },
                onOpenHistory = { currentTab = AppNavTab.HISTORY },
                onOpenShield = { viewModel.setPrivacyModalOpen(true) },
                onOpenSettings = { viewModel.setSettingsOpen(true) },
                onOpenAbout = { viewModel.setAboutModalOpen(true) },
                onBurnData = { isBurningData = true },
                onDismiss = { showMenuSheet = false }
            )
        }

        // Dedicated Bookmarks & Quick Links Screen Overlay
        if (showBookmarksScreen) {
            com.azokle.search.browser.bookmarks.BookmarksScreen(
                onDismiss = { showBookmarksScreen = false },
                onNavigate = { targetUrl ->
                    showBookmarksScreen = false
                    viewModel.openInAppBrowser(targetUrl)
                },
                onOpenTor = { targetUrl ->
                    showBookmarksScreen = false
                    viewModel.openInAppBrowser(targetUrl, isTor = true)
                }
            )
        }

        // In-App Downloads Screen Overlay
        if (showDownloadsScreen) {
            com.azokle.search.browser.downloads.DownloadsScreen(
                onDismiss = { showDownloadsScreen = false }
            )
        }

        // Cinema-Grade Fire Incineration Overlay
        if (isBurningData) {
            FireIncinerationOverlay(
                onComplete = {
                    TabManager.closeAllTabs()
                    com.azokle.search.browser.gecko.GeckoEngineManager.clearAllData(context)
                    com.azokle.search.browser.history.HistoryManager.clearAll()
                    com.azokle.search.data.PreferencesManager(context).clearRecentSearches()
                    isBurningData = false
                    currentTab = AppNavTab.SEARCH
                }
            )
        }
    }
}
