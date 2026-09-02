package com.azokle.search.browser.ui

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import com.azokle.search.browser.gecko.DownloadConfirmationPayload
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import com.azokle.search.ui.theme.AudiowideFontFamily
import com.azokle.search.ui.theme.AzokleWhite
import com.azokle.search.ui.theme.adaptiveContentWidth
import com.azokle.search.ui.components.SearchBarCapsule
import com.azokle.search.ui.components.VoiceSearchModal
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.azokle.search.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azokle.search.browser.adblock.AdBlockEngine
import com.azokle.search.browser.adblock.AdBlockStats
import com.azokle.search.browser.gecko.GeckoBrowserView
import com.azokle.search.browser.reader.ArticleContent
import com.azokle.search.browser.reader.ReaderExtractor
import com.azokle.search.browser.reader.ReaderModeScreen
import com.azokle.search.browser.tabs.TabManager
import com.azokle.search.browser.tabs.TabSession
import com.azokle.search.data.vault.EncryptedVaultManager
import com.azokle.search.ui.components.ChooseSearchEngineSheet
import com.azokle.search.ui.components.ChooseLanguageSheet
import com.azokle.search.ui.components.ProUpgradeDialog
import com.azokle.search.ui.components.InfoDialog
import com.azokle.search.ui.screens.ActiveTabsScreen
import com.azokle.search.ui.theme.AzokleDarkBg
import com.azokle.search.util.UrlHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import com.azokle.search.browser.permissions.SitePermissionRequest
import com.azokle.search.browser.permissions.SitePermissionState
import com.azokle.search.browser.permissions.SitePermissionType
import com.azokle.search.browser.permissions.SitePermissionsManager
import com.azokle.search.browser.ui.SitePermissionPromptSheet
import com.azokle.search.browser.ui.SiteSettingsSheet
import com.azokle.search.browser.ui.components.TabGroupBottomStrip
import com.azokle.search.browser.ui.components.TabGroupDetailSheet
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView

@Composable
fun BrowserScreen(
    initialUrl: String,
    onClose: () -> Unit,
    viewModel: com.azokle.search.viewmodel.HomeViewModel? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val vaultManager = remember { EncryptedVaultManager.getInstance(context) }

    var currentUrl by remember { mutableStateOf(initialUrl) }
    var currentTitle by remember { mutableStateOf("") }
    var progress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(true) }
    var isDesktopMode by remember { mutableStateOf(false) }
    var isFullScreenMode by remember { mutableStateOf(false) }

    var currentSecurityInfo: org.mozilla.geckoview.GeckoSession.ProgressDelegate.SecurityInformation? by remember { mutableStateOf(null) }
    var currentError: BrowserErrorType? by remember { mutableStateOf(null) }

    var showShieldSheet by remember { mutableStateOf(false) }
    var showTabGridSheet by remember { mutableStateOf(false) }
    var showUrlEditSheet by remember { mutableStateOf(false) }
    var showVoiceModal by remember { mutableStateOf(false) }
    var showMenuSheet by remember { mutableStateOf(false) }
    var showSecurityCertSheet by remember { mutableStateOf(false) }
    var showTorCircuitSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showDownloadsScreen by remember { mutableStateOf(false) }
    var showBookmarksScreen by remember { mutableStateOf(false) }
    var showHistoryScreen by remember { mutableStateOf(false) }
    var showPwaInstallSheet by remember { mutableStateOf(false) }
    var showDiscoverScreen by remember { mutableStateOf(false) }
    val pwaState by com.azokle.search.browser.pwa.PwaManager.activeTabPwaState.collectAsState()

    var geckoSession: GeckoSession? by remember { mutableStateOf(null) }
    var geckoView: GeckoView? by remember { mutableStateOf(null) }

    var isFindInPageVisible by remember { mutableStateOf(false) }
    var findInPageQuery by remember { mutableStateOf("") }
    var findCurrentMatch by remember { mutableIntStateOf(0) }
    var findTotalMatches by remember { mutableIntStateOf(0) }

    fun executeFind(query: String, backwards: Boolean = false) {
        val session = geckoSession ?: return
        if (query.isBlank()) {
            try {
                session.finder.clear()
            } catch (e: Exception) {}
            findCurrentMatch = 0
            findTotalMatches = 0
            return
        }
        val flags = if (backwards) GeckoSession.FINDER_FIND_BACKWARDS else 0
        try {
            session.finder.displayFlags = GeckoSession.FINDER_DISPLAY_HIGHLIGHT_ALL
            session.finder.find(query, flags).accept { result: GeckoSession.FinderResult? ->
                if (result != null && result.found) {
                    findCurrentMatch = result.current
                    findTotalMatches = result.total
                } else {
                    findCurrentMatch = 0
                    findTotalMatches = 0
                }
            }
        } catch (e: Exception) {
            findCurrentMatch = 0
            findTotalMatches = 0
        }
    }

    var activeArticleReader: ArticleContent? by remember { mutableStateOf(null) }

    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }

    var isBottomBarVisible by remember { mutableStateOf(true) }
    var lastScrollY by remember { mutableIntStateOf(0) }
    var showBurnConfirmationSheet by remember { mutableStateOf(false) }
    var isBurningData by remember { mutableStateOf(false) }
    var activeLinkContextTarget: LinkContextTarget? by remember { mutableStateOf(null) }
    var pendingDownloadPayload by remember { mutableStateOf<DownloadConfirmationPayload?>(null) }

    // Website Permissions Engine Integration
    val permManager = remember { SitePermissionsManager.getInstance(context) }
    var activePermissionRequest by remember { mutableStateOf<SitePermissionRequest?>(null) }
    var showSiteSettingsSheet by remember { mutableStateOf(false) }
    var pendingActionAfterOsPerms by remember { mutableStateOf<(() -> Unit)?>(null) }

    val osPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        val req = activePermissionRequest
        if (req != null) {
            if (allGranted) {
                pendingActionAfterOsPerms?.invoke()
            } else {
                req.reject(GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY)
                activePermissionRequest = null
                Toast.makeText(context, "Permission denied in device settings", Toast.LENGTH_SHORT).show()
            }
        }
        pendingActionAfterOsPerms = null
    }

    fun handleAllowPermission(request: SitePermissionRequest, rememberChoice: Boolean) {
        val requiredPerms = request.requestedTypes.flatMap { it.requiredAndroidPermissions }.distinct()
        val missingPerms = requiredPerms.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }

        val grantAction = {
            if (rememberChoice) {
                request.requestedTypes.forEach { type ->
                    permManager.setPermission(request.origin, type, SitePermissionState.ALLOW, isPrivate = request.isPrivate)
                }
            }
            val video = request.videoSources?.firstOrNull()
            val audio = request.audioSources?.firstOrNull()
            request.grantMedia(video, audio)
            request.grantContent(GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW)
            activePermissionRequest = null
        }

        if (missingPerms.isNotEmpty()) {
            pendingActionAfterOsPerms = grantAction
            osPermissionLauncher.launch(missingPerms.toTypedArray())
        } else {
            grantAction()
        }
    }

    fun handleDenyPermission(request: SitePermissionRequest, rememberChoice: Boolean) {
        if (rememberChoice) {
            request.requestedTypes.forEach { type ->
                permManager.setPermission(request.origin, type, SitePermissionState.BLOCK, isPrivate = request.isPrivate)
            }
        }
        request.reject(GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY)
        activePermissionRequest = null
    }

    val shieldStats by AdBlockStats.currentStats.collectAsState()
    val openTabs by TabManager.tabs.collectAsState()
    val groups by TabManager.groups.collectAsState()
    val bookmarks by vaultManager.bookmarksFlow.collectAsState()
    val isBookmarked = remember(currentUrl, bookmarks) {
        vaultManager.isBookmarked(currentUrl)
    }

    val homeUiState = viewModel?.uiState?.collectAsState()?.value
    val isInPipMode = homeUiState?.isInPipMode == true

    // Initialize AdBlocker & Cosmetic assets
    val activeTabId by TabManager.activeTabId.collectAsState()
    val currentActiveTab = TabManager.activeTab
    val isCurrentTabTor = currentActiveTab?.isTor == true
    val isCurrentTabPrivate = currentActiveTab?.isPrivate == true || isCurrentTabTor

    val activeGroup = remember(openTabs, groups, activeTabId) {
        val cur = openTabs.find { it.id == activeTabId } ?: openTabs.firstOrNull()
        val gid = cur?.groupId
        if (gid != null) groups.find { it.id == gid } else null
    }
    val activeGroupTabs = remember(openTabs, activeGroup) {
        if (activeGroup != null) openTabs.filter { it.groupId == activeGroup.id } else emptyList()
    }
    var showGroupDetailSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        AdBlockEngine.instance.initialize(context)
    }

    LaunchedEffect(activeTabId) {
        val tab = TabManager.activeTab
        if (tab != null) {
            isDesktopMode = tab.isDesktopMode
            if (tab.url.isNotBlank() && (tab.url != currentUrl || currentTitle != tab.title)) {
                currentUrl = tab.url
                currentTitle = tab.title
                currentError = null
            }
        }
    }

    LaunchedEffect(currentUrl) {
        if (currentUrl.isBlank() || currentUrl == "about:home" || currentUrl == "about:blank" || currentUrl.startsWith("about:")) {
            com.azokle.search.browser.adblock.AdBlockStats.resetForPage("", "", true)
        }
    }

    BackHandler(enabled = true) {
        if (isFullScreenMode) {
            isFullScreenMode = false
            geckoSession?.exitFullScreen()
        } else if (isBurningData) {
            // Ignore back during burning
        } else if (showBurnConfirmationSheet) {
            showBurnConfirmationSheet = false
        } else if (activePermissionRequest != null) {
            activePermissionRequest?.let { handleDenyPermission(it, false) }
        } else if (showGroupDetailSheet) {
            showGroupDetailSheet = false
        } else if (activeLinkContextTarget != null) {
            activeLinkContextTarget = null
        } else if (showDownloadsScreen) {
            showDownloadsScreen = false
        } else if (showBookmarksScreen) {
            showBookmarksScreen = false
        } else if (showHistoryScreen) {
            showHistoryScreen = false
        } else if (showSiteSettingsSheet) {
            showSiteSettingsSheet = false
        } else if (showMenuSheet) {
            showMenuSheet = false
        } else if (showShieldSheet) {
            showShieldSheet = false
        } else if (showUrlEditSheet) {
            showUrlEditSheet = false
        } else if (showVoiceModal) {
            showVoiceModal = false
        } else if (showTabGridSheet) {
            showTabGridSheet = false
        } else if (homeUiState?.isSearchEngineSheetOpen == true) {
            viewModel?.setSearchEngineSheetOpen(false)
        } else if (homeUiState?.isLanguageSheetOpen == true) {
            viewModel?.setLanguageSheetOpen(false)
        } else if (homeUiState?.isProModalOpen == true) {
            viewModel?.setProModalOpen(false)
        } else if (homeUiState?.isPrivacyModalOpen == true) {
            viewModel?.setPrivacyModalOpen(false)
        } else if (homeUiState?.isTermsModalOpen == true) {
            viewModel?.setTermsModalOpen(false)
        } else if (homeUiState?.isAboutModalOpen == true) {
            viewModel?.setAboutModalOpen(false)
        } else if (showSettingsSheet) {
            showSettingsSheet = false
        } else if (showSecurityCertSheet) {
            showSecurityCertSheet = false
        } else if (showTorCircuitSheet) {
            showTorCircuitSheet = false
        } else if (isFindInPageVisible) {
            isFindInPageVisible = false
            executeFind("")
        } else if (activeArticleReader != null) {
            activeArticleReader = null
        } else if (showDiscoverScreen) {
            showDiscoverScreen = false
        } else if (currentError != null) {
            currentError = null
            if (canGoBack) {
                geckoSession?.goBack()
            } else {
                currentUrl = ""
                currentTitle = "New Tab"
                TabManager.updateActiveTab("", "New Tab")
            }
        } else if (canGoBack) {
            geckoSession?.goBack()
        } else if (currentUrl.isNotBlank() && currentUrl != "about:home" && currentUrl != "about:blank") {
            currentUrl = ""
            currentTitle = "New Tab"
            com.azokle.search.browser.adblock.AdBlockStats.resetForPage("", "", true)
            TabManager.updateActiveTab("", "New Tab")
        } else {
            onClose()
        }
    }

    DisposableEffect(isFullScreenMode) {
        val activity = context as? android.app.Activity
        val window = activity?.window
        if (window != null) {
            val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            if (isFullScreenMode) {
                insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                insetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            val activity = context as? android.app.Activity
            val window = activity?.window
            if (window != null) {
                val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    val colors = com.azokle.search.ui.theme.AzokleTheme.colors

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isInPipMode) Color.Black else colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // 1. Header Omnibar or Find In Page Toolbar
            if (!isFullScreenMode && !isInPipMode && !showDiscoverScreen) {
                if (isFindInPageVisible) {
                    FindInPageBar(
                        query = findInPageQuery,
                        currentMatchIndex = findCurrentMatch,
                        totalMatches = findTotalMatches,
                        onQueryChange = { q ->
                            findInPageQuery = q
                            executeFind(q)
                        },
                        onNextMatch = {
                            executeFind(findInPageQuery, backwards = false)
                        },
                        onPreviousMatch = {
                            executeFind(findInPageQuery, backwards = true)
                        },
                        onClose = {
                            try {
                                geckoSession?.finder?.clear()
                            } catch (e: Exception) {}
                            isFindInPageVisible = false
                            findInPageQuery = ""
                            findCurrentMatch = 0
                            findTotalMatches = 0
                        }
                    )
                } else {
                    BrowserOmnibar(
                        url = currentUrl,
                        title = currentTitle,
                        blockedCount = if (currentUrl.isBlank() || currentUrl == "about:home" || currentUrl == "about:blank" || currentUrl.startsWith("about:")) 0 else shieldStats.adsBlockedCount,
                        isShieldEnabled = shieldStats.isShieldEnabled,
                        isLoading = isLoading,
                        progress = progress,
                        onSecurityLockClick = {
                            if (isCurrentTabTor) {
                                showTorCircuitSheet = true
                            } else {
                                showSecurityCertSheet = true
                            }
                        },
                        onRefreshClick = {
                            currentError = null
                            if (isLoading) {
                                geckoSession?.stop()
                            } else {
                                geckoSession?.reload()
                            }
                        },
                        onShieldClick = { showShieldSheet = true },
                        onReaderModeClick = {
                            scope.launch {
                                Toast.makeText(context, "Extracting reader view...", Toast.LENGTH_SHORT).show()
                                val article = ReaderExtractor.extractArticleFromUrl(
                                    url = currentUrl,
                                    fallbackTitle = currentTitle,
                                    isTor = isCurrentTabTor,
                                    socksPort = com.azokle.search.security.TorCircuitManager.socksPort
                                )
                                activeArticleReader = article
                            }
                        },
                        onMenuClick = { showMenuSheet = true },
                        onVoiceClick = { showVoiceModal = true },
                        onUrlBarClick = { showUrlEditSheet = true }
                    )
                }

                // 🧅 Tor Circuit Status Stripe (below Omnibar when in a Tor tab/window)
                if (isCurrentTabTor && !showDiscoverScreen) {
                    com.azokle.search.browser.ui.components.TorCircuitStatusStripe(
                        onNewCircuit = {
                            com.azokle.search.security.TorCircuitManager.requestNewCircuit()
                            com.azokle.search.ui.components.ToastManager.showTor("Building new 3-hop Tor circuit 🧅...")
                        },
                        onOpenDetails = {
                            showTorCircuitSheet = true
                        }
                    )
                }
            }

            // 2. 100% Exclusive Mozilla Gecko Quantum Engine with Multi-Tab Session Isolation
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                androidx.compose.runtime.DisposableEffect(isCurrentTabPrivate) {
                    val activity = context as? android.app.Activity
                    if (isCurrentTabPrivate) {
                        activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                    } else {
                        activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                    }
                    onDispose {
                        activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }

                androidx.compose.runtime.LaunchedEffect(isCurrentTabTor) {
                    if (isCurrentTabTor) {
                        com.azokle.search.security.TorCircuitManager.startTorService(context)
                    }
                    com.azokle.search.security.TorCircuitManager.setTorMode(isCurrentTabTor)
                }

                androidx.compose.runtime.DisposableEffect(Unit) {
                    onDispose {
                        com.azokle.search.security.TorCircuitManager.setTorMode(false)
                    }
                }



                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    if (showDiscoverScreen) {
                        val discoverViewModel: com.azokle.search.viewmodel.DiscoverViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                        com.azokle.search.ui.screens.DiscoverScreen(
                            viewModel = discoverViewModel,
                            onTopicClick = { topic ->
                                showDiscoverScreen = false
                                val searchEngineConfig = com.azokle.search.data.SEARCH_ENGINE_PROVIDERS.find { it.id == viewModel?.uiState?.value?.selectedSearchEngine }
                                    ?: com.azokle.search.data.SEARCH_ENGINE_PROVIDERS[0]
                                val targetUrl = com.azokle.search.util.UrlHelper.resolveSearchOrUrl(
                                    input = topic,
                                    searchEngineQueryUrl = searchEngineConfig.queryUrl
                                )
                                currentUrl = targetUrl
                                currentTitle = topic
                                currentError = null
                                TabManager.updateActiveTab(targetUrl, topic)
                                geckoSession?.loadUri(targetUrl)
                            },
                            onNewsClick = { url ->
                                showDiscoverScreen = false
                                currentUrl = url
                                currentTitle = "News"
                                currentError = null
                                TabManager.updateActiveTab(url, "News")
                                geckoSession?.loadUri(url)
                            }
                        )
                    } else if (currentUrl.isBlank() || currentUrl == "about:home" || currentUrl == "about:blank" || currentUrl == "about:tor") {
                        // Render Photographic Wallpaper Browser Home Page (Unified for Normal, Private, and Tor tabs)
                        BrowserHomeScreenContent(
                            onNavigate = { targetUrl ->
                                currentUrl = targetUrl
                                currentTitle = if (isCurrentTabTor) "Tor Browsing" else "Search"
                                currentError = null
                                TabManager.updateActiveTab(targetUrl, currentTitle)
                                geckoSession?.loadUri(targetUrl)
                            },
                            onOpenSearchSheet = {
                                showUrlEditSheet = true
                            },
                            onVoiceClick = {
                                showVoiceModal = true
                            }
                        )
                    } else {
                        androidx.compose.runtime.key(activeTabId) {
                            GeckoBrowserView(
                                url = currentUrl,
                                tabId = activeTabId,
                                isDesktopMode = isDesktopMode,
                                isDarkMode = colors.isDark,
                                isPrivate = isCurrentTabPrivate,
                                isTor = isCurrentTabTor,
                                onUrlChanged = { newUrl ->
                                    currentUrl = newUrl
                                    currentError = null
                                    TabManager.updateActiveTab(newUrl, currentTitle)
                                },
                                onTitleChanged = { t ->
                                    currentTitle = t
                                    TabManager.updateActiveTab(currentUrl, t)
                                },
                                onProgressChanged = { p ->
                                    progress = p / 100f
                                    isLoading = p < 100
                                },
                                onLoadingChanged = { loading ->
                                    isLoading = loading
                                },
                                onSecurityChanged = { secInfo ->
                                    currentSecurityInfo = secInfo
                                },
                                onCanGoBackChanged = { canGoBack = it },
                                onCanGoForwardChanged = { canGoForward = it },
                                onScrollChanged = { _, scrollY ->
                                    val delta = scrollY - lastScrollY
                                    if (scrollY <= 40) {
                                        isBottomBarVisible = true
                                    } else if (delta > 35) {
                                        // User scrolled DOWN with intent -> hide bottom navigation bar
                                        isBottomBarVisible = false
                                    } else if (delta < -25) {
                                        // User scrolled UP with intent -> show bottom navigation bar
                                        isBottomBarVisible = true
                                    }
                                    lastScrollY = scrollY
                                },
                                onLinkLongPressed = { target ->
                                    activeLinkContextTarget = target
                                },
                                onNewWindowRequested = { targetUrl ->
                                     val parentId = activeTabId ?: ""
                                     val newId = if (parentId.isNotBlank()) {
                                         TabManager.openTabFromParent(
                                             url = targetUrl,
                                             title = "New Tab",
                                             parentTabId = parentId,
                                             isPrivate = isCurrentTabPrivate,
                                             isTor = isCurrentTabTor
                                         )
                                     } else {
                                         TabManager.openTab(targetUrl, "New Tab", isPrivate = isCurrentTabPrivate, isTor = isCurrentTabTor)
                                     }
                                     TabManager.selectTab(newId)
                                     currentUrl = targetUrl
                                     currentTitle = "New Tab"
                                     currentError = null
                                     geckoSession?.loadUri(targetUrl)
                                     com.azokle.search.ui.components.ToastManager.show("Tab added to group 📑")
                                 },
                                 onLoadErrorChanged = { error ->
                                     currentError = error
                                 },
                                 onFullScreenChanged = { fs ->
                                     isFullScreenMode = fs
                                 },
                                 onPermissionPromptRequested = { req ->
                                     activePermissionRequest = req
                                 },
                                 onDownloadConfirmationRequested = { payload ->
                                     pendingDownloadPayload = payload
                                 },
                                 onSessionReady = { session, view ->
                                     geckoSession = session
                                     geckoView = view
                                     val tabId = activeTabId ?: ""
                                     if (tabId.isNotBlank()) {
                                         TabManager.registerSession(tabId, session)
                                     }
                                 }
                             )
                         }

                         // Brave/Firefox-grade Luxury Error Screen Surface
                         currentError?.let { err ->
                             BrowserErrorScreen(
                                 error = err,
                                 failedUrl = currentUrl,
                                 onRetry = {
                                     currentError = null
                                     geckoSession?.reload()
                                 },
                                 onHome = {
                                     currentError = null
                                     currentUrl = ""
                                     currentTitle = "New Tab"
                                     TabManager.updateActiveTab("", "New Tab")
                                 },
                                 onBypass = if (err.canBypass) {
                                     {
                                         currentError = null
                                         geckoSession?.reload()
                                     }
                                 } else null
                             )
                         }
                     }
                 }
             }

             // 3. Tab Group Bottom Strip + Docked Bottom Action Toolbar
             if (!isFullScreenMode && !isInPipMode) {
                 Column(
                     modifier = Modifier.fillMaxWidth(),
                     horizontalAlignment = Alignment.CenterHorizontally
                 ) {
                      if (!showDiscoverScreen) {
                          TabGroupBottomStrip(
                              group = activeGroup,
                              groupTabs = activeGroupTabs,
                              activeTabId = activeTabId,
                              onTabSelected = { selectedTab ->
                                  TabManager.selectTab(selectedTab.id)
                                  currentUrl = selectedTab.url
                                  currentTitle = selectedTab.title
                                  if (selectedTab.url.isNotBlank() && selectedTab.url != "about:home" && selectedTab.url != "about:blank") {
                                      geckoSession?.loadUri(selectedTab.url)
                                  }
                              },
                              onAddTabInGroup = {
                                  activeGroup?.let { g ->
                                      val newId = TabManager.openTabInGroup("", "New Tab", g.id)
                                      TabManager.selectTab(newId)
                                      currentUrl = ""
                                      currentTitle = "New Tab"
                                  }
                              },
                              onCloseTab = { tabIdToClose ->
                                  TabManager.closeTab(tabIdToClose)
                                  val next = TabManager.activeTab
                                  if (next != null) {
                                      currentUrl = next.url
                                      currentTitle = next.title
                                      if (next.url.isNotBlank() && next.url != "about:home" && next.url != "about:blank") {
                                          geckoSession?.loadUri(next.url)
                                      }
                                  }
                              },
                              onOpenGroupOverview = {
                                  showGroupDetailSheet = true
                              }
                          )
                      }

                     androidx.compose.animation.AnimatedVisibility(
                         visible = isBottomBarVisible || showDiscoverScreen || currentUrl.isBlank() || currentUrl == "about:home",
                         enter = androidx.compose.animation.expandVertically(
                             animationSpec = androidx.compose.animation.core.tween(180)
                         ) + androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(180)),
                         exit = androidx.compose.animation.shrinkVertically(
                             animationSpec = androidx.compose.animation.core.tween(180)
                         ) + androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(180))
                     ) {
                         BrowserBottomBar(
                             openTabsCount = openTabs.size,
                             isHomeSelected = !showDiscoverScreen && !showTabGridSheet && (currentUrl.isBlank() || currentUrl == "about:home"),
                             isDiscoverSelected = showDiscoverScreen,
                             isTabsSelected = showTabGridSheet,
                             onHomeClick = {
                                 showDiscoverScreen = false
                                 showTabGridSheet = false
                                 com.azokle.search.browser.adblock.AdBlockStats.resetForPage("", "", true)
                                 if (currentUrl.isNotBlank() && currentUrl != "about:home" && currentUrl != "about:blank") {
                                     currentUrl = ""
                                     currentTitle = "New Tab"
                                     currentError = null
                                     TabManager.updateActiveTab("", "New Tab")
                                 }
                             },
                             onDiscoverClick = {
                                 showDiscoverScreen = true
                             },
                             onTabsClick = { showTabGridSheet = true },
                             onBurnDataClick = {
                                 showBurnConfirmationSheet = true
                             },
                             onMenuClick = { showMenuSheet = true }
                         )
                     }
                 }
             }
         }

        // Reader Mode Full Screen View
        activeArticleReader?.let { article ->
            ReaderModeScreen(
                article = article,
                onClose = { activeArticleReader = null }
            )
        }

        // Shield HUD Sheet
        if (showShieldSheet) {
            PrivacyShieldSheet(
                onDismiss = { showShieldSheet = false },
                onShieldToggled = {
                    geckoSession?.reload()
                }
            )
        }

        // Luxury SSL / TLS Certificate Inspector Sheet with Real X.509 Security Information
        if (showSecurityCertSheet) {
            SiteSecurityCertificateSheet(
                url = currentUrl,
                securityInfo = currentSecurityInfo,
                onDismiss = { showSecurityCertSheet = false },
                onReload = { geckoSession?.reload() }
            )
        }

        // Live 3-Hop Tor Circuit Inspector & Node Visualizer
        if (showTorCircuitSheet) {
            TorCircuitVisualizerSheet(
                url = currentUrl,
                onDismiss = { showTorCircuitSheet = false }
            )
        }

        // Interactive Omnibar URL / Search Editor Modal
        if (showUrlEditSheet) {
            BrowserUrlEditSheet(
                initialText = currentUrl,
                onNavigate = { input ->
                    showUrlEditSheet = false
                    val resolved = if (input.startsWith("http://") || input.startsWith("https://") || input.startsWith("about:")) {
                        input
                    } else {
                        UrlHelper.resolveSearchOrUrl(context, input)
                    }
                    currentUrl = resolved
                    currentError = null
                    TabManager.updateActiveTab(resolved, "")
                    geckoSession?.loadUri(resolved)
                },
                onDismiss = { showUrlEditSheet = false }
            )
        }

        // Brave/Firefox-Style Action Menu Sheet
        if (showMenuSheet) {
            BrowserMenuSheet(
                url = currentUrl,
                title = currentTitle,
                isDesktopMode = isDesktopMode,
                isBookmarked = isBookmarked,
                blockedTrackersCount = shieldStats.adsBlockedCount,
                onReload = {
                    showMenuSheet = false
                    currentError = null
                    geckoSession?.reload()
                },
                onToggleBookmark = {
                    val added = vaultManager.toggleBookmark(currentTitle, currentUrl)
                    com.azokle.search.ui.components.ToastManager.showSuccess(
                        if (added) "Saved to Bookmarks 🔖" else "Removed from Bookmarks"
                    )
                },
                onShare = {
                    try {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, currentUrl)
                            putExtra(Intent.EXTRA_SUBJECT, currentTitle)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Webpage"))
                    } catch (e: Exception) {
                        // Safe ignore
                    }
                },
                onToggleDesktopMode = {
                    showMenuSheet = false
                    val newMode = TabManager.toggleDesktopMode(activeTabId)
                    isDesktopMode = newMode
                    currentError = null
                    geckoSession?.reload()
                    com.azokle.search.ui.components.ToastManager.show(
                        if (newMode) "Desktop site enabled 💻" else "Mobile site enabled 📱"
                    )
                },
                onSaveAsPdf = {
                    showMenuSheet = false
                    val session = geckoSession
                    if (session != null && session.isOpen) {
                        com.azokle.search.browser.gecko.GeckoEngineManager.printPageToPdf(
                            context = context,
                            session = session,
                            title = currentTitle,
                            url = currentUrl
                        ) { file: java.io.File? ->
                            if (file != null) {
                                com.azokle.search.ui.components.ToastManager.show("📄 PDF saved: ${file.name}")
                            } else {
                                com.azokle.search.ui.components.ToastManager.show("Failed to generate PDF")
                            }
                        }
                    } else {
                        com.azokle.search.ui.components.ToastManager.show("Page not loaded yet")
                    }
                },
                onOpenReaderMode = {
                    showMenuSheet = false
                    scope.launch {
                        com.azokle.search.ui.components.ToastManager.show("Extracting reader view...")
                        val article = ReaderExtractor.extractArticleFromUrl(
                            url = currentUrl,
                            fallbackTitle = currentTitle,
                            isTor = isCurrentTabTor,
                            socksPort = com.azokle.search.security.TorCircuitManager.socksPort
                        )
                        activeArticleReader = article
                    }
                },
                onOpenFindInPage = {
                    showMenuSheet = false
                    isFindInPageVisible = true
                    if (findInPageQuery.isNotBlank()) {
                        executeFind(findInPageQuery)
                    }
                },
                isBrowserContext = true,
                onOpenBookmarks = {
                    showMenuSheet = false
                    showBookmarksScreen = true
                },
                onOpenDownloads = {
                    showMenuSheet = false
                    showDownloadsScreen = true
                },
                onOpenHistory = {
                    showMenuSheet = false
                    showHistoryScreen = true
                },
                onOpenShield = {
                    showMenuSheet = false
                    showShieldSheet = true
                },
                onOpenNewTab = {
                    showMenuSheet = false
                    showDiscoverScreen = false
                    val newId = TabManager.openTab("", "New Tab")
                    TabManager.selectTab(newId)
                    currentUrl = ""
                    currentTitle = "New Tab"
                },
                onOpenNewPrivateTab = {
                    showMenuSheet = false
                    showDiscoverScreen = false
                    val newId = TabManager.openPrivateTab("", "Private Tab")
                    TabManager.selectTab(newId)
                    currentUrl = ""
                    currentTitle = "Private Tab"
                },
                onOpenTorWindow = {
                    showMenuSheet = false
                    showDiscoverScreen = false
                    currentError = null
                    val newTorTabId = TabManager.openTorTab(context, "", "Tor Window")
                    val torTab = TabManager.tabs.value.find { it.id == newTorTabId }
                    if (torTab != null) {
                        currentUrl = torTab.url
                        currentTitle = torTab.title
                    }
                    com.azokle.search.ui.components.ToastManager.showTor("Connecting to Isolated Tor Onion Network...")
                },
                onOpenSettings = {
                    showMenuSheet = false
                    showSettingsSheet = true
                },
                pwaEligibility = pwaState?.eligibility,
                onInstallPwa = {
                    showMenuSheet = false
                    showPwaInstallSheet = true
                },
                onBurnData = {
                    showMenuSheet = false
                    showBurnConfirmationSheet = true
                },
                onDismiss = { showMenuSheet = false }
            )
        }

        // PWA Install / Add to Home Screen Bottom Sheet
        if (showPwaInstallSheet && pwaState?.eligibility != null) {
            com.azokle.search.browser.pwa.ui.PwaInstallBottomSheet(
                eligibility = pwaState!!.eligibility!!,
                onDismiss = { showPwaInstallSheet = false }
            )
        }

        // Dedicated Bookmarks & Quick Links Screen Overlay
        if (showBookmarksScreen) {
            com.azokle.search.browser.bookmarks.BookmarksScreen(
                onDismiss = { showBookmarksScreen = false },
                onNavigate = { targetUrl ->
                    showBookmarksScreen = false
                    showDiscoverScreen = false
                    currentUrl = targetUrl
                    geckoSession?.loadUri(targetUrl)
                },
                onOpenTor = { targetUrl ->
                    showBookmarksScreen = false
                    showDiscoverScreen = false
                    val newTorTabId = TabManager.openTorTab(context, targetUrl, "Tor")
                    val torTab = TabManager.tabs.value.find { it.id == newTorTabId }
                    if (torTab != null) {
                        currentUrl = torTab.url
                        currentTitle = torTab.title
                    }
                }
            )
        }

        // Dedicated Downloads Screen Overlay
        if (showDownloadsScreen) {
            com.azokle.search.browser.downloads.DownloadsScreen(
                onDismiss = { showDownloadsScreen = false }
            )
        }

        // Dedicated History Screen Overlay
        if (showHistoryScreen) {
            com.azokle.search.ui.screens.HistoryScreen(
                viewModel = viewModel ?: androidx.lifecycle.viewmodel.compose.viewModel(),
                onHistoryQueryClick = { query ->
                    showHistoryScreen = false
                    showDiscoverScreen = false
                    currentError = null
                    val searchEngineConfig = com.azokle.search.data.SEARCH_ENGINE_PROVIDERS.find { it.id == viewModel?.uiState?.value?.selectedSearchEngine }
                        ?: com.azokle.search.data.SEARCH_ENGINE_PROVIDERS[0]
                    val targetUrl = com.azokle.search.util.UrlHelper.resolveSearchOrUrl(
                        input = query,
                        searchEngineQueryUrl = searchEngineConfig.queryUrl
                    )
                    currentUrl = targetUrl
                    TabManager.updateActiveTab(targetUrl, query)
                    geckoSession?.loadUri(targetUrl)
                },
                onOpenUrl = { url ->
                    showHistoryScreen = false
                    showDiscoverScreen = false
                    currentError = null
                    currentUrl = url
                    TabManager.updateActiveTab(url, "")
                    geckoSession?.loadUri(url)
                },
                onClose = { showHistoryScreen = false }
            )
        }

        // Unified Active Tabs Screen Switcher (100% Reliable Gecko Tab Switching)
        if (showTabGridSheet) {
            ActiveTabsScreen(
                onTabSelected = { selectedTab ->
                    showTabGridSheet = false
                    showDiscoverScreen = false
                    currentError = null
                    TabManager.selectTab(selectedTab.id)
                    currentUrl = selectedTab.url
                    currentTitle = selectedTab.title
                    geckoSession?.loadUri(selectedTab.url)
                },
                onNewTabClick = {
                    showTabGridSheet = false
                    showDiscoverScreen = false
                    currentError = null
                    val newTabId = TabManager.openTab("", "New Tab")
                    TabManager.selectTab(newTabId)
                    currentUrl = ""
                    currentTitle = "New Tab"
                },
                onClose = {
                    showTabGridSheet = false
                    if (TabManager.tabs.value.isEmpty()) {
                        onClose()
                    }
                }
            )
        }

        // Settings Sheet directly accessible from Browser Header
        if (showSettingsSheet) {
            val uiState = viewModel?.uiState?.collectAsState()?.value
            com.azokle.search.ui.components.SettingsSheet(
                selectedThemeMode = uiState?.selectedThemeMode ?: "system",
                onThemeModeChange = { viewModel?.setThemeMode(it) },
                selectedDohProvider = uiState?.selectedDohProvider ?: "cloudflare",
                onDohProviderChange = { viewModel?.setDohProvider(it) },
                selectedSearchEngine = uiState?.selectedSearchEngine ?: "azokle",
                onOpenSearchEngine = { viewModel?.setSearchEngineSheetOpen(true) },
                selectedLanguageCode = uiState?.selectedLanguage ?: "system",
                onOpenLanguage = { viewModel?.setLanguageSheetOpen(true) },
                onDismiss = { showSettingsSheet = false },
                onOpenPro = { viewModel?.setProModalOpen(true) },
                onOpenPrivacyPolicy = { viewModel?.setPrivacyModalOpen(true) },
                onOpenTerms = { viewModel?.setTermsModalOpen(true) },
                onOpenAbout = { viewModel?.setAboutModalOpen(true) },
                onOpenUrl = { url ->
                    val newId = TabManager.openTab(url, "Azokle")
                    TabManager.selectTab(newId)
                    geckoSession?.loadUri(url)
                }
            )
        }

        // Choose Search Engine Sheet
        if (homeUiState?.isSearchEngineSheetOpen == true) {
            ChooseSearchEngineSheet(
                selectedEngineId = homeUiState.selectedSearchEngine,
                onSelectEngine = { engineId ->
                    viewModel?.setSearchEngine(engineId)
                    viewModel?.setSearchEngineSheetOpen(false)
                },
                onDismiss = { viewModel?.setSearchEngineSheetOpen(false) }
            )
        }

        // Choose Language Sheet
        if (homeUiState?.isLanguageSheetOpen == true) {
            ChooseLanguageSheet(
                selectedLanguageCode = homeUiState.selectedLanguage,
                onSelectLanguage = { langCode ->
                    viewModel?.setAppLanguage(langCode)
                    viewModel?.setLanguageSheetOpen(false)
                },
                onDismiss = { viewModel?.setLanguageSheetOpen(false) }
            )
        }

        // Pro Upgrade Dialog
        if (homeUiState?.isProModalOpen == true) {
            ProUpgradeDialog(
                onDismiss = { viewModel?.setProModalOpen(false) }
            )
        }

        // Privacy Policy Dialog
        if (homeUiState?.isPrivacyModalOpen == true) {
            InfoDialog(
                title = androidx.compose.ui.res.stringResource(com.azokle.search.R.string.privacy_policy),
                content = androidx.compose.ui.res.stringResource(com.azokle.search.R.string.privacy_policy_content),
                onDismiss = { viewModel?.setPrivacyModalOpen(false) }
            )
        }

        // Terms of Service Dialog
        if (homeUiState?.isTermsModalOpen == true) {
            InfoDialog(
                title = androidx.compose.ui.res.stringResource(com.azokle.search.R.string.terms_service),
                content = androidx.compose.ui.res.stringResource(com.azokle.search.R.string.terms_service_content),
                onDismiss = { viewModel?.setTermsModalOpen(false) }
            )
        }

        // About Dialog
        if (homeUiState?.isAboutModalOpen == true) {
            InfoDialog(
                title = androidx.compose.ui.res.stringResource(com.azokle.search.R.string.about_app),
                content = androidx.compose.ui.res.stringResource(com.azokle.search.R.string.about_app_content),
                onDismiss = { viewModel?.setAboutModalOpen(false) }
            )
        }

        // Link Context Menu Sheet (Long-Press hyperlink)
        activeLinkContextTarget?.let { target ->
            LinkContextMenuSheet(
                target = target,
                onOpenInNewTabGroup = { url, title ->
                    // Adds to current group or creates a new group containing parent + child
                    val parentId = activeTabId ?: ""
                    val newId = if (parentId.isNotBlank()) {
                        TabManager.openTabFromParent(url, title, parentId, isPrivate = false)
                    } else {
                        TabManager.openTab(url, title, isPrivate = false)
                    }
                    TabManager.selectTab(newId)
                    currentUrl = url
                    currentTitle = title
                    currentError = null
                    geckoSession?.loadUri(url)
                    com.azokle.search.ui.components.ToastManager.show("Opened in tab group 📑")
                },
                onOpenInNewTab = { url, title ->
                    // ALWAYS opens as a separate, standalone tab with NO group
                    val newId = TabManager.openTab(url, title, isPrivate = false)
                    TabManager.selectTab(newId)
                    currentUrl = url
                    currentTitle = title
                    currentError = null
                    geckoSession?.loadUri(url)
                    com.azokle.search.ui.components.ToastManager.show("Opened in new tab 🗂️")
                },
                onOpenInPrivateTab = { url, title ->
                    // Standalone private tab with NO group
                    val newId = TabManager.openTab(url, title, isPrivate = true)
                    TabManager.selectTab(newId)
                    currentUrl = url
                    currentTitle = title
                    currentError = null
                    geckoSession?.loadUri(url)
                    com.azokle.search.ui.components.ToastManager.show("Opened in private tab 🕵️")
                },
                onDismiss = { activeLinkContextTarget = null }
            )
        }

        // Tab Group Detail Modal Sheet (Overview, reorder, rename, palette picker)
        if (showGroupDetailSheet && activeGroup != null) {
            TabGroupDetailSheet(
                group = activeGroup,
                tabs = activeGroupTabs,
                activeTabId = activeTabId,
                onTabSelected = { selectedTab ->
                    showGroupDetailSheet = false
                    TabManager.selectTab(selectedTab.id)
                    currentUrl = selectedTab.url
                    currentTitle = selectedTab.title
                    geckoSession?.loadUri(selectedTab.url)
                },
                onAddTabInGroup = {
                    showGroupDetailSheet = false
                    val newId = TabManager.openTabInGroup("", "New Tab", activeGroup.id)
                    TabManager.selectTab(newId)
                    currentUrl = ""
                    currentTitle = "New Tab"
                },
                onCloseTab = { tabIdToClose ->
                    TabManager.closeTab(tabIdToClose)
                    val next = TabManager.activeTab
                    if (next != null) {
                        currentUrl = next.url
                        currentTitle = next.title
                        if (next.url.isNotBlank() && next.url != "about:home" && next.url != "about:blank") {
                            geckoSession?.loadUri(next.url)
                        }
                    }
                },
                onDismiss = { showGroupDetailSheet = false }
            )
        }

        // 🔥 Burn Data Confirmation Sheet
        if (showBurnConfirmationSheet) {
            com.azokle.search.ui.components.BurnConfirmationSheet(
                onConfirm = {
                    showBurnConfirmationSheet = false
                    isBurningData = true
                },
                onDismiss = { showBurnConfirmationSheet = false }
            )
        }

        // 🔥 Full Screen Cinema-Grade Fire Incineration Overlay
        if (isBurningData) {
            com.azokle.search.ui.components.FireIncinerationOverlay(
                onComplete = {
                    TabManager.closeAllTabs()
                    com.azokle.search.browser.gecko.GeckoEngineManager.clearAllData(context)
                    com.azokle.search.browser.history.HistoryManager.clearAll()
                    com.azokle.search.data.PreferencesManager(context).clearRecentSearches()
                    com.azokle.search.browser.adblock.AdBlockStats.resetForPage("", "", true)

                    // Open fresh new tab on the Wallpaper Home Screen
                    val newTabId = TabManager.openTab("", "New Tab")
                    TabManager.selectTab(newTabId)

                    currentUrl = ""
                    currentTitle = "New Tab"
                    currentError = null
                    showDiscoverScreen = false
                    showTabGridSheet = false
                    showGroupDetailSheet = false
                    showMenuSheet = false
                    showBurnConfirmationSheet = false
                    isBurningData = false

                    com.azokle.search.ui.components.ToastManager.showSuccess("All browsing data burned 🔥")
                }
            )
        }

        // 🛡️ Brave-Grade Site Permission Prompt Modal
        activePermissionRequest?.let { request ->
            SitePermissionPromptSheet(
                request = request,
                onAllowAlways = { handleAllowPermission(request, rememberChoice = true) },
                onAllowOnce = { handleAllowPermission(request, rememberChoice = false) },
                onDeny = { rememberChoice -> handleDenyPermission(request, rememberChoice = rememberChoice) },
                onDismiss = { handleDenyPermission(request, rememberChoice = false) }
            )
        }

        // 📥 Sovereign Download Authorization Modal (Fix SEC-002)
        pendingDownloadPayload?.let { payload ->
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { pendingDownloadPayload = null },
                title = {
                    androidx.compose.material3.Text(
                        text = "Download File?",
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        fontSize = 17.sp
                    )
                },
                text = {
                    androidx.compose.foundation.layout.Column {
                        androidx.compose.material3.Text(
                            text = "Do you want to download '${payload.filename}'?",
                            fontSize = 14.sp
                        )
                        if (payload.contentLength > 0) {
                            val sizeMb = String.format(java.util.Locale.US, "%.2f MB", payload.contentLength / (1024.0 * 1024.0))
                            Spacer(modifier = Modifier.height(4.dp))
                            androidx.compose.material3.Text(
                                text = "Size: $sizeMb",
                                fontSize = 12.sp,
                                color = colors.textMuted
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        androidx.compose.material3.Text(
                            text = "Type: ${payload.mimeType}",
                            fontSize = 12.sp,
                            color = colors.textMuted
                        )
                    }
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            val activePayload = payload
                            pendingDownloadPayload = null
                            com.azokle.search.browser.downloads.DownloadManager.startDownload(
                                context = context,
                                url = activePayload.url,
                                filename = activePayload.filename,
                                mimeType = activePayload.mimeType,
                                contentLength = activePayload.contentLength,
                                headers = activePayload.headers
                            )
                            android.widget.Toast.makeText(context, "📥 Downloading ${activePayload.filename}...", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        androidx.compose.material3.Text(text = "Download", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { pendingDownloadPayload = null }) {
                        androidx.compose.material3.Text(text = "Cancel")
                    }
                }
            )
        }

        // Voice Search Modal in Browser Screen
        if (showVoiceModal) {
            VoiceSearchModal(
                onResult = { recognizedQuery ->
                    showVoiceModal = false
                    val searchEngineConfig = com.azokle.search.data.SEARCH_ENGINE_PROVIDERS.find { it.id == viewModel?.uiState?.value?.selectedSearchEngine }
                        ?: com.azokle.search.data.SEARCH_ENGINE_PROVIDERS[0]
                    val targetUrl = com.azokle.search.util.UrlHelper.resolveSearchOrUrl(
                        input = recognizedQuery,
                        searchEngineQueryUrl = searchEngineConfig.queryUrl
                    )
                    currentUrl = targetUrl
                    currentTitle = recognizedQuery
                    currentError = null
                    TabManager.updateActiveTab(targetUrl, recognizedQuery)
                    geckoSession?.loadUri(targetUrl)
                },
                onDismiss = { showVoiceModal = false }
            )
        }

        // 🌟 Premium Floating Toast HUD
        com.azokle.search.ui.components.AzokleToastHost()
    }
}

@Composable
private fun BrowserHomeScreenContent(
    onNavigate: (String) -> Unit,
    onOpenSearchSheet: () -> Unit,
    onVoiceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = com.azokle.search.ui.theme.AzokleTheme.colors

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // 1. High-Resolution Photographic Wallpaper Backdrop
        Image(
            painter = painterResource(id = R.drawable.bg_home),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Top Status Bar Scrim
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.60f),
                            Color.Black.copy(alpha = 0.20f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Bottom Navigation Scrim
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.35f),
                            Color.Black.copy(alpha = 0.70f)
                        )
                    )
                )
        )

        // Center Content Stack: Logo, Search Capsule, Quick Shortcuts
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .adaptiveContentWidth(640.dp)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Audiowide Brand Logo in Lowercase (Exact match to Home Page)
            Text(
                text = "azokle",
                color = AzokleWhite,
                fontFamily = AudiowideFontFamily,
                fontSize = 52.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = (-1.0).sp,
                textAlign = TextAlign.Center
            )

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(24.dp))

            // Exact Home Page Search Bar Capsule
            SearchBarCapsule(
                query = "",
                placeholderText = stringResource(R.string.search_placeholder),
                onQueryChange = {},
                onSearch = { onNavigate(it) },
                onVoiceClick = onVoiceClick,
                onCapsuleClick = onOpenSearchSheet,
                isReadOnly = true
            )

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(24.dp))

            // Quick Speed Dial Shortcuts (YouTube, Google, Wikipedia, GitHub, Reddit, X)
            val shortcuts = listOf(
                BrowserShortcut("YouTube", "https://www.youtube.com", "▶", Color(0xFFFF0000)),
                BrowserShortcut("Google", "https://www.google.com", "G", Color(0xFF4285F4)),
                BrowserShortcut("Wikipedia", "https://www.wikipedia.org", "W", Color(0xFFFFFFFF)),
                BrowserShortcut("GitHub", "https://github.com", "🐙", Color(0xFF24292F)),
                BrowserShortcut("Reddit", "https://www.reddit.com", "👾", Color(0xFFFF4500)),
                BrowserShortcut("X", "https://x.com", "𝕏", Color(0xFF000000))
            )

            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                shortcuts.forEach { item ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onNavigate(item.url) }
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(colors.cardBg.copy(alpha = 0.88f))
                                .border(1.dp, colors.borderLight, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.iconText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (item.iconText == "W" || item.iconText == "G") item.tintColor else colors.textPrimary
                            )
                        }
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = item.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }
    }
}

private data class BrowserShortcut(
    val label: String,
    val url: String,
    val iconText: String,
    val tintColor: Color
)
