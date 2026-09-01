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
import com.azokle.search.browser.ui.BrowserScreen
import com.azokle.search.navigation.AppBottomNavBar
import com.azokle.search.navigation.AppNavTab
import com.azokle.search.ui.theme.AzokleTheme
import com.azokle.search.viewmodel.HomeViewModel

@Composable
fun MainContainerScreen(
    viewModel: HomeViewModel,
    onVoiceSearchRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTab by remember { mutableStateOf(AppNavTab.SEARCH) }
    var isAppBottomBarVisible by remember { mutableStateOf(true) }
    val uiState by viewModel.uiState.collectAsState()

    androidx.activity.compose.BackHandler(enabled = currentTab != AppNavTab.SEARCH) {
        currentTab = AppNavTab.SEARCH
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta < -12) {
                    // Scrolled down -> hide bottom bar
                    isAppBottomBarVisible = false
                } else if (delta > 12) {
                    // Scrolled up -> show bottom bar
                    isAppBottomBarVisible = true
                }
                return Offset.Zero
            }
        }
    }

    // 1. Top-level In-Built Privacy Browser (Instantly renders header Omnibar & footer Toolbar together)
    if (uiState.inAppBrowserUrl != null) {
        BrowserScreen(
            initialUrl = uiState.inAppBrowserUrl!!,
            onClose = { viewModel.closeInAppBrowser() },
            viewModel = viewModel
        )
        return
    }

    // Determine if full-screen overlay is active (e.g. expanded search)
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
                        viewModel = viewModel,
                        onTopicClick = { topic ->
                            currentTab = AppNavTab.SEARCH
                            viewModel.onQueryChange(topic)
                            viewModel.executeSearch(topic)
                        },
                        onNewsClick = { url ->
                            viewModel.openInAppBrowser(url)
                        }
                    )
                }
                AppNavTab.TABS -> {
                    ActiveTabsScreen(
                        onTabSelected = { tab ->
                            com.azokle.search.browser.tabs.TabManager.selectTab(tab.id)
                            viewModel.openInAppBrowser(tab.url, isTor = tab.isTor)
                        },
                        onNewTabClick = {
                            val newId = com.azokle.search.browser.tabs.TabManager.openTab("https://search.azokle.com", "Search")
                            com.azokle.search.browser.tabs.TabManager.selectTab(newId)
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

        // Floating Glassmorphic Bottom Navigation Bar (Auto-hides on scroll down)
        AnimatedVisibility(
            visible = !isFullScreenOverlay && isAppBottomBarVisible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            AppBottomNavBar(
                currentTab = currentTab,
                onTabSelected = { currentTab = it }
            )
        }
    }
}
