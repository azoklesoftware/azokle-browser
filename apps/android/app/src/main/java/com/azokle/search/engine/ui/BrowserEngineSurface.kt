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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.azokle.search.common.theme.AzokleThemeColors
import com.azokle.search.common.tour.TourState
import com.azokle.search.common.tour.TourStep
import com.azokle.search.common.tour.tourTarget
import com.azokle.search.common.util.findActivity
import com.azokle.search.discover.DiscoverScreen
import com.azokle.search.discover.DiscoverViewModel
import com.azokle.search.engine.DownloadConfirmationPayload
import com.azokle.search.engine.GeckoBrowserView
import com.azokle.search.engine.TextSelectionPayload
import com.azokle.search.permissions.SitePermissionRequest
import com.azokle.search.tabs.TabManager
import com.azokle.search.tabs.TabSession
import com.azokle.search.tor.TorCircuitManager
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView

/**
 * Composable that renders the browser engine surface UI component.
 */
@Composable
fun BrowserEngineSurface(
    showDiscoverScreen: Boolean,
    currentUrl: String,
    currentTitle: String,
    activeTabId: String?,
    openTabs: List<TabSession>,
    isCurrentTabPrivate: Boolean,
    isCurrentTabTor: Boolean,
    isDesktopMode: Boolean,
    isFullScreenMode: Boolean,
    colors: AzokleThemeColors,
    isPageTransitioning: Boolean,
    targetLoadingUrl: String,
    currentError: BrowserErrorType?,
    selectedSearchEngine: String,
    homeTourSteps: List<TourStep>,
    homeTourState: TourState,
    onNavigateToUrl: (String, String) -> Unit,
    onOpenSearchSheet: () -> Unit,
    onVoiceClick: () -> Unit,
    onSelectSearchEngine: (String) -> Unit,
    onCloseDiscoverScreen: () -> Unit,
    onUrlChanged: (String) -> Unit,
    onTitleChanged: (String) -> Unit,
    onProgressChanged: (Int) -> Unit,
    onLoadingChanged: (Boolean) -> Unit,
    onSecurityChanged: (GeckoSession.ProgressDelegate.SecurityInformation?) -> Unit,
    onCanGoBackChanged: (Boolean) -> Unit,
    onCanGoForwardChanged: (Boolean) -> Unit,
    onScrollChanged: (Int, Int) -> Unit,
    onLinkLongPressed: (LinkContextTarget) -> Unit,
    onTextSelectionChanged: (TextSelectionPayload?) -> Unit,
    onNewWindowRequested: (String) -> Unit,
    onLoadErrorChanged: (BrowserErrorType?) -> Unit,
    onFullScreenChanged: (Boolean) -> Unit,
    onPermissionPromptRequested: (SitePermissionRequest) -> Unit,
    onDownloadConfirmationRequested: (DownloadConfirmationPayload) -> Unit,
    onSessionReady: (GeckoSession, GeckoView) -> Unit,
    onRetryError: () -> Unit,
    onHomeFromError: () -> Unit,
    onBypassError: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    DisposableEffect(isCurrentTabPrivate) {
        val activity = context.findActivity()
        if (isCurrentTabPrivate) {
            activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    LaunchedEffect(isCurrentTabTor) {
        if (isCurrentTabTor) {
            TorCircuitManager.startTorService(context)
        }
        TorCircuitManager.setTorMode(isCurrentTabTor)
    }

    DisposableEffect(Unit) {
        onDispose {
            TorCircuitManager.setTorMode(false)
        }
    }

    val currentTab = openTabs.find { it.id == activeTabId }
    val effectiveUrl = if (currentUrl.isNotBlank() && currentUrl != "about:home" && currentUrl != "about:tor") {
        currentUrl
    } else {
        currentTab?.url ?: ""
    }
    val isHomePage = !showDiscoverScreen &&
            (effectiveUrl.isBlank() || effectiveUrl == "about:home" || effectiveUrl == "about:tor") &&
            currentTab?.parentTabId == null

    Box(modifier = modifier.fillMaxWidth()) {
        if (showDiscoverScreen) {
            val discoverViewModel: DiscoverViewModel = viewModel()
            DiscoverScreen(
                viewModel = discoverViewModel,
                onOpenUrl = { url, title ->
                    onCloseDiscoverScreen()
                    onNavigateToUrl(url, title)
                }
            )
        } else if (isHomePage) {
            // Render Photographic Wallpaper Browser Home Page (Unified for Normal, Private, and Tor tabs)
            BrowserHomeScreenContent(
                activeTab = currentTab,
                onNavigate = { targetUrl ->
                    val title = if (isCurrentTabTor) "Tor Browsing" else "Search"
                    onNavigateToUrl(targetUrl, title)
                },
                onOpenSearchSheet = onOpenSearchSheet,
                onVoiceClick = onVoiceClick,
                selectedSearchEngine = selectedSearchEngine,
                onSelectSearchEngine = onSelectSearchEngine,
                searchEngineModifier = Modifier.tourTarget(homeTourSteps[2], homeTourState),
                searchBarModifier = Modifier.tourTarget(homeTourSteps[3], homeTourState),
                shortcutsModifier = Modifier.tourTarget(homeTourSteps[4], homeTourState)
            )
        } else {
            key(activeTabId) {
                GeckoBrowserView(
                    url = effectiveUrl,
                    tabId = activeTabId,
                    isDesktopMode = isDesktopMode,
                    isDarkMode = colors.isDark,
                    isPrivate = isCurrentTabPrivate,
                    isTor = isCurrentTabTor,
                    isFullScreenMode = isFullScreenMode,
                    onUrlChanged = onUrlChanged,
                    onTitleChanged = onTitleChanged,
                    onProgressChanged = onProgressChanged,
                    onLoadingChanged = onLoadingChanged,
                    onSecurityChanged = onSecurityChanged,
                    onCanGoBackChanged = onCanGoBackChanged,
                    onCanGoForwardChanged = onCanGoForwardChanged,
                    onScrollChanged = onScrollChanged,
                    onLinkLongPressed = onLinkLongPressed,
                    onTextSelectionChanged = onTextSelectionChanged,
                    onNewWindowRequested = onNewWindowRequested,
                    onLoadErrorChanged = onLoadErrorChanged,
                    onFullScreenChanged = onFullScreenChanged,
                    onPermissionPromptRequested = onPermissionPromptRequested,
                    onDownloadConfirmationRequested = onDownloadConfirmationRequested,
                    onSessionReady = { session, view ->
                        onSessionReady(session, view)
                        val tabId = activeTabId ?: ""
                        if (tabId.isNotBlank()) {
                            TabManager.registerSession(tabId, session)
                        }
                    }
                )
            }

            // Sovereign Anti-Flash Layer: masks stale WebViews with a clean theme-synchronized loading state
            AnimatedVisibility(
                visible = isPageTransitioning && currentError == null,
                enter = EnterTransition.None,
                exit = fadeOut(animationSpec = tween(180)),
                modifier = Modifier.fillMaxSize()
            ) {
                PageLoadingTransitionSurface(
                    targetUrl = targetLoadingUrl.ifBlank { currentUrl },
                    colors = colors
                )
            }

            // Brave/Firefox-grade Luxury Error Screen Surface
            currentError?.let { err ->
                BrowserErrorScreen(
                    error = err,
                    failedUrl = currentUrl,
                    onRetry = onRetryError,
                    onHome = onHomeFromError,
                    onBypass = onBypassError
                )
            }
        }
    }
}
