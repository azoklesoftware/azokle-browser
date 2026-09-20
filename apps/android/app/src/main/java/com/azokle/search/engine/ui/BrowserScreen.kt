package com.azokle.search.engine.ui

import android.Manifest
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.azokle.search.adblock.AdBlockEngine
import com.azokle.search.adblock.AdBlockStats
import com.azokle.search.adblock.AdClickManager
import com.azokle.search.adblock.SafeBrowsingGuard
import com.azokle.search.adblock.ThreatAssessment
import com.azokle.search.common.components.ToastManager
import com.azokle.search.common.theme.AzokleTheme
import com.azokle.search.common.theme.rememberWindowSize
import com.azokle.search.common.tour.TourShape
import com.azokle.search.common.tour.TourStep
import com.azokle.search.common.tour.rememberTourState
import com.azokle.search.common.tour.tourTarget
import com.azokle.search.common.util.PreferencesManager
import com.azokle.search.common.util.SEARCH_ENGINE_PROVIDERS
import com.azokle.search.common.util.UrlHelper
import com.azokle.search.common.util.findActivity
import com.azokle.search.downloads.DownloadManager
import com.azokle.search.engine.DownloadConfirmationPayload
import com.azokle.search.engine.GeckoEngineManager
import com.azokle.search.engine.TextSelectionPayload
import com.azokle.search.history.AzokleHistoryDatabase
import com.azokle.search.history.HistoryManager
import com.azokle.search.media.MediaPlaybackManager
import com.azokle.search.media.PlaybackStatus
import com.azokle.search.navigation.ExternalSchemeHandler
import com.azokle.search.navigation.HttpsUpgrader
import com.azokle.search.permissions.SitePermissionRequest
import com.azokle.search.permissions.SitePermissionState
import com.azokle.search.permissions.SitePermissionType
import com.azokle.search.permissions.SitePermissionsManager
import com.azokle.search.pwa.PwaManager
import com.azokle.search.reader.ArticleContent
import com.azokle.search.reader.ReaderExtractor
import com.azokle.search.search.HomeViewModel
import com.azokle.search.settings.WallpaperManager
import com.azokle.search.tabs.TabGroupBottomStrip
import com.azokle.search.tabs.TabManager
import com.azokle.search.tor.TorCircuitManager
import com.azokle.search.tor.TorCircuitStatusStripe
import com.azokle.search.vault.EncryptedVaultManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView
import java.io.File
import java.net.URI

@Composable
fun BrowserScreen(
    initialUrl: String,
    onClose: () -> Unit,
    viewModel: HomeViewModel? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val vaultManager = remember { EncryptedVaultManager.getInstance(context) }
    val permManager = remember { SitePermissionsManager.getInstance(context) }
    val preferencesManager = remember { PreferencesManager(context) }

    var currentUrl by remember { mutableStateOf(initialUrl) }
    var currentTitle by remember { mutableStateOf("") }
    var progress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(true) }
    var isDesktopMode by remember { mutableStateOf(false) }
    var isFullScreenMode by remember { mutableStateOf(false) }

    // Page Transition & Anti-Flash State: masks stale WebView frames during navigation
    var isPageTransitioning by remember { mutableStateOf(false) }
    var targetLoadingUrl by remember { mutableStateOf("") }

    LaunchedEffect(isPageTransitioning, targetLoadingUrl) {
        if (isPageTransitioning) {
            delay(4000)
            isPageTransitioning = false
        }
    }

    var currentSecurityInfo: GeckoSession.ProgressDelegate.SecurityInformation? by remember { mutableStateOf(null) }
    var currentError: BrowserErrorType? by remember { mutableStateOf(null) }

    var showShieldSheet by remember { mutableStateOf(false) }
    var showTabGridSheet by remember { mutableStateOf(false) }
    var showUrlEditSheet by remember { mutableStateOf(false) }
    var showVoiceModal by remember { mutableStateOf(false) }
    var showQrScanner by remember { mutableStateOf(false) }
    var showMenuSheet by remember { mutableStateOf(false) }
    var showSecurityCertSheet by remember { mutableStateOf(false) }
    var showTorCircuitSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showDownloadsScreen by remember { mutableStateOf(false) }
    var showBookmarksScreen by remember { mutableStateOf(false) }
    var showHistoryScreen by remember { mutableStateOf(false) }
    var showPwaInstallSheet by remember { mutableStateOf(false) }
    var showDiscoverScreen by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }
    var activeSharePayload by remember { mutableStateOf<SharePayload?>(null) }
    val pwaState by PwaManager.activeTabPwaState.collectAsState()

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

    val density = LocalDensity.current.density
    val barVisibilityStateMachine = remember(density) {
        BrowserBarVisibilityStateMachine(density)
    }
    val isBottomBarVisible = barVisibilityStateMachine.isVisible

    // Synchronize animation completion with the state machine
    LaunchedEffect(barVisibilityStateMachine.isVisible) {
        delay(200L)
        barVisibilityStateMachine.onAnimationFinished(barVisibilityStateMachine.isVisible)
    }

    var showBurnConfirmationSheet by remember { mutableStateOf(false) }
    var isBurningData by remember { mutableStateOf(false) }
    var activeLinkContextTarget: LinkContextTarget? by remember { mutableStateOf(null) }
    var activeTextSelection: TextSelectionPayload? by remember { mutableStateOf(null) }
    var pendingDownloadPayload by remember { mutableStateOf<DownloadConfirmationPayload?>(null) }
    var pendingThreatAssessment by remember { mutableStateOf<ThreatAssessment?>(null) }

    // Website Permissions Engine Integration
    var activePermissionRequest by remember { mutableStateOf<SitePermissionRequest?>(null) }
    var pendingActionAfterOsPerms by remember { mutableStateOf<(() -> Unit)?>(null) }

    val osPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val req = activePermissionRequest
        val isLocationReq = req?.requestedTypes?.contains(SitePermissionType.GEOLOCATION) == true
        val locationOk = if (isLocationReq) {
            (results[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
            (results[Manifest.permission.ACCESS_COARSE_LOCATION] == true) ||
            (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) ||
            (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        } else true

        val otherPerms = results.filter {
            it.key != Manifest.permission.ACCESS_FINE_LOCATION && it.key != Manifest.permission.ACCESS_COARSE_LOCATION
        }
        val otherOk = otherPerms.values.all { it }

        val allGranted = locationOk && otherOk
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
        val hasLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                          ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        val requiredPerms = request.requestedTypes.flatMap { it.requiredAndroidPermissions }.distinct()
        val missingPerms = requiredPerms.filter { perm ->
            if (perm == Manifest.permission.ACCESS_FINE_LOCATION || perm == Manifest.permission.ACCESS_COARSE_LOCATION) {
                !hasLocation
            } else {
                ContextCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED
            }
        }.distinct()

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
            request.grantClipboard(AllowOrDeny.ALLOW)
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

    fun navigateToUrl(targetUrl: String, title: String = "") {
        val upgradedUrl = HttpsUpgrader.upgrade(targetUrl)
        val cleanTargetUrl = AdClickManager.cleanTrackingParameters(upgradedUrl)

        val threatAssessment = SafeBrowsingGuard.assessUrl(cleanTargetUrl)
        if (threatAssessment.isThreat) {
            pendingThreatAssessment = threatAssessment
            return
        }

        val finalLoadUrl = if (cleanTargetUrl.startsWith("azokle://", ignoreCase = true)) {
            val page = cleanTargetUrl.substringAfter("azokle://").trimEnd('/').lowercase()
            when (page) {
                "shield", "shields" -> "resource://android/assets/webui/pages/shield/index.html"
                "settings" -> "resource://android/assets/webui/pages/settings/index.html"
                "tor" -> "resource://android/assets/webui/pages/tor/index.html"
                "history" -> "resource://android/assets/webui/pages/history/index.html"
                "bookmarks" -> "resource://android/assets/webui/pages/bookmarks/index.html"
                else -> cleanTargetUrl
            }
        } else {
            cleanTargetUrl
        }

        targetLoadingUrl = cleanTargetUrl
        isPageTransitioning = true
        currentUrl = cleanTargetUrl
        currentTitle = if (cleanTargetUrl.startsWith("azokle://", ignoreCase = true)) {
            "Azokle " + cleanTargetUrl.substringAfter("azokle://").replaceFirstChar { it.uppercase() }
        } else {
            title
        }
        currentError = null
        TabManager.updateActiveTab(cleanTargetUrl, currentTitle)
        geckoSession?.loadUri(finalLoadUrl)
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

    // AdBlocker & Active Tab Identification
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

    var forceStartTabsTour by remember { mutableStateOf(false) }

    val homeTourSteps = remember {
        listOf(
            TourStep(
                id = "home_omnibar_engine",
                title = "Change Search Engine (Top Bar)",
                description = "Tap this icon in the top bar to easily pick your search engine: Azokle, DuckDuckGo, Brave, Startpage, Mojeek, or Qwant.",
                shape = TourShape.CIRCLE,
                paddingDp = 6f
            ),
            TourStep(
                id = "home_omnibar_bar",
                title = "Search or Type Web Address",
                description = "Type what you want to search, or enter any website address here to browse immediately.",
                shape = TourShape.ROUNDED_RECT,
                paddingDp = 6f,
                cornerRadiusDp = 14f
            ),
            TourStep(
                id = "home_search_engine",
                title = "Change Search Engine (Search Box)",
                description = "You can also tap this icon right inside the search box to switch your search engine anytime.",
                shape = TourShape.CIRCLE,
                paddingDp = 6f
            ),
            TourStep(
                id = "home_search_box",
                title = "Search the Web",
                description = "Tap here to search questions or websites. You can also tap the microphone to speak your search hands-free.",
                shape = TourShape.ROUNDED_RECT,
                paddingDp = 6f,
                cornerRadiusDp = 24f
            ),
            TourStep(
                id = "home_shortcuts",
                title = "Favorite Websites",
                description = "Tap any shortcut to quickly open popular sites like YouTube, Wikipedia, and Google without typing.",
                shape = TourShape.ROUNDED_RECT,
                paddingDp = 6f,
                cornerRadiusDp = 16f
            ),
            TourStep(
                id = "home_tabs",
                title = "See Your Open Tabs",
                description = "Tap here to see all your open pages, switch between them, or organize them into groups.",
                shape = TourShape.CIRCLE,
                paddingDp = 6f
            ),
            TourStep(
                id = "home_burn",
                title = "Erase Everything",
                description = "Tap the flame icon to close all open tabs and wipe your browsing history with one touch.",
                shape = TourShape.CIRCLE,
                paddingDp = 6f
            ),
            TourStep(
                id = "home_menu",
                title = "Menu & Settings",
                description = "Find your saved bookmarks, downloaded files, browsing history, and app settings here.",
                shape = TourShape.CIRCLE,
                paddingDp = 6f
            )
        )
    }

    val homeTourState = rememberTourState(
        steps = homeTourSteps,
        onTourFinished = {
            preferencesManager.setHomeTourCompleted(true)
        }
    )

    LaunchedEffect(Unit) {
        if (!preferencesManager.hasCompletedHomeTour()) {
            delay(650)
            homeTourState.start()
        }
    }

    LaunchedEffect(Unit) {
        AdBlockEngine.instance.initialize(context)
    }

    LaunchedEffect(activeTabId) {
        if (isFullScreenMode) {
            isFullScreenMode = false
            try {
                geckoSession?.exitFullScreen()
            } catch (e: Exception) {}
        }
        barVisibilityStateMachine.forceShow()
        val tab = TabManager.activeTab
        if (tab != null) {
            isDesktopMode = tab.isDesktopMode
            if (tab.url.isNotBlank()) {
                if (tab.url != currentUrl || currentTitle != tab.title) {
                    currentUrl = tab.url
                    currentTitle = tab.title
                    currentError = null
                }
            } else {
                currentUrl = ""
                currentTitle = tab.title
                currentError = null
            }
        }
    }

    LaunchedEffect(currentUrl, isPageTransitioning) {
        if ((currentUrl.isBlank() || currentUrl == "about:home" || currentUrl == "about:blank" || currentUrl.startsWith("about:")) && !isPageTransitioning) {
            barVisibilityStateMachine.forceShow()
            AdBlockStats.resetForPage("", "", true)
        }
    }

    BackHandler(enabled = true) {
        if (homeTourState.isVisible) {
            homeTourState.skip()
        } else if (isFullScreenMode) {
            isFullScreenMode = false
            try {
                geckoSession?.exitFullScreen()
            } catch (e: Exception) {}
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
        } else if (activeTextSelection != null) {
            activeTextSelection?.onDismiss?.invoke()
            activeTextSelection = null
        } else if (showDownloadsScreen) {
            showDownloadsScreen = false
        } else if (showBookmarksScreen) {
            showBookmarksScreen = false
        } else if (showHistoryScreen) {
            showHistoryScreen = false
        } else if (showMenuSheet) {
            showMenuSheet = false
        } else if (showShieldSheet) {
            showShieldSheet = false
        } else if (showUrlEditSheet) {
            showUrlEditSheet = false
        } else if (showQrScanner) {
            showQrScanner = false
        } else if (showVoiceModal) {
            showVoiceModal = false
        } else if (showTabGridSheet) {
            showTabGridSheet = false
        } else if (homeUiState?.isSearchEngineSheetOpen == true) {
            viewModel?.setSearchEngineSheetOpen(false)
        } else if (homeUiState?.isLanguageSheetOpen == true) {
            viewModel?.setLanguageSheetOpen(false)
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
                isPageTransitioning = false
                targetLoadingUrl = ""
                try {
                    geckoView?.visibility = View.INVISIBLE
                    geckoView?.alpha = 0f
                } catch (e: Exception) {}
                activeTabId?.let { id -> TabManager.clearTabLoaded(id) }
                TabManager.updateActiveTab("", "New Tab")
                geckoSession?.stop()
            }
        } else if (canGoBack) {
            geckoSession?.goBack()
        } else if (currentUrl.isNotBlank() && currentUrl != "about:home" && currentUrl != "about:blank") {
            currentUrl = ""
            currentTitle = "New Tab"
            isPageTransitioning = false
            targetLoadingUrl = ""
            try {
                geckoView?.visibility = View.INVISIBLE
                geckoView?.alpha = 0f
            } catch (e: Exception) {}
            activeTabId?.let { id -> TabManager.clearTabLoaded(id) }
            AdBlockStats.resetForPage("", "", true)
            TabManager.updateActiveTab("", "New Tab")
            geckoSession?.stop()
        } else {
            onClose()
        }
    }

    val colors = AzokleTheme.colors
    val windowSize = rememberWindowSize()
    val configuration = LocalConfiguration.current
    var lastOrientation by remember { mutableIntStateOf(configuration.orientation) }
    val mediaState by MediaPlaybackManager.mediaState.collectAsState()

    val isMediaPlaying = mediaState.status == PlaybackStatus.PLAYING || mediaState.status == PlaybackStatus.BUFFERING
    val isHomeOrEmpty = currentUrl.isBlank() || currentUrl == "about:home" || currentUrl == "about:blank" || currentUrl == "about:tor"

    LaunchedEffect(configuration.orientation, isMediaPlaying) {
        val currentOrientation = configuration.orientation
        if (lastOrientation != currentOrientation) {
            if (currentOrientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
                if (isMediaPlaying && !isHomeOrEmpty && !isFullScreenMode) {
                    isFullScreenMode = true
                }
            } else if (currentOrientation == android.content.res.Configuration.ORIENTATION_PORTRAIT) {
                if (isFullScreenMode) {
                    isFullScreenMode = false
                    try {
                        geckoSession?.exitFullScreen()
                    } catch (e: Exception) {}
                }
            }
            lastOrientation = currentOrientation
        }
    }

    DisposableEffect(isFullScreenMode, colors.isDark) {
        val activity = context.findActivity()
        val window = activity?.window
        if (activity != null && window != null) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            if (isFullScreenMode) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                val savedOrientation = activity.requestedOrientation
                val savedCutoutMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    window.attributes.layoutInDisplayCutoutMode
                } else 0

                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }

                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

                onDispose {
                    try {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } catch (e: Exception) {}

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        window.attributes.layoutInDisplayCutoutMode = savedCutoutMode
                    }
                    activity.requestedOrientation = if (savedOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED &&
                        savedOrientation != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                        savedOrientation
                    } else {
                        ActivityInfo.SCREEN_ORIENTATION_USER
                    }
                    insetsController.show(WindowInsetsCompat.Type.systemBars())
                    insetsController.isAppearanceLightStatusBars = !colors.isDark
                    insetsController.isAppearanceLightNavigationBars = !colors.isDark
                }
            } else {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
                insetsController.isAppearanceLightStatusBars = !colors.isDark
                insetsController.isAppearanceLightNavigationBars = !colors.isDark
                onDispose {}
            }
        } else {
            onDispose {}
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isFullScreenMode || isInPipMode) Color.Black else colors.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            val shouldShowOmnibar = isBottomBarVisible || isHomeOrEmpty || isFindInPageVisible

            // 0. Permanent Status Bar Protection Scrim (protects system clock, battery, camera cutout & icons)
            if (!isFullScreenMode && !isInPipMode) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsTopHeight(WindowInsets.statusBars)
                        .background(
                            if (isHomeOrEmpty) Color.Transparent
                            else colors.cardBg
                        )
                )
            }

            // 1. Header Omnibar or Find In Page Toolbar
            if (!isFullScreenMode && !isInPipMode && !showDiscoverScreen) {
                AnimatedVisibility(
                    visible = shouldShowOmnibar,
                    enter = slideInVertically(
                        initialOffsetY = { -it },
                        animationSpec = tween(180, easing = FastOutSlowInEasing)
                    ) + expandVertically(
                        expandFrom = Alignment.Top,
                        animationSpec = tween(180, easing = FastOutSlowInEasing)
                    ),
                    exit = slideOutVertically(
                        targetOffsetY = { -it },
                        animationSpec = tween(180, easing = FastOutSlowInEasing)
                    ) + shrinkVertically(
                        shrinkTowards = Alignment.Top,
                        animationSpec = tween(180, easing = FastOutSlowInEasing)
                    )
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (isFindInPageVisible) {
                            FindInPageBar(
                                query = findInPageQuery,
                                currentMatchIndex = findCurrentMatch,
                                totalMatches = findTotalMatches,
                                onQueryChange = { q ->
                                    findInPageQuery = q
                                    executeFind(q)
                                },
                                onNextMatch = { executeFind(findInPageQuery, backwards = false) },
                                onPreviousMatch = { executeFind(findInPageQuery, backwards = true) },
                                onClose = {
                                    try {
                                        geckoSession?.finder?.clear()
                                    } catch (e: Exception) {}
                                    isFindInPageVisible = false
                                    findInPageQuery = ""
                                    findCurrentMatch = 0
                                    findTotalMatches = 0
                                },
                                includeStatusBarPadding = false
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
                                    if (currentUrl.isBlank() || currentUrl == "about:home" || currentUrl == "about:blank" || currentUrl == "about:tor") {
                                        val next = WallpaperManager.rotateWallpaper()
                                        activeTabId?.let { id -> TabManager.updateTabWallpaper(id, next) }
                                        ToastManager.show("Wallpaper refreshed")
                                    } else if (isLoading) {
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
                                            socksPort = TorCircuitManager.socksPort
                                        )
                                        activeArticleReader = article
                                    }
                                },
                                onMenuClick = { showMenuSheet = true },
                                onQrClick = { showQrScanner = true },
                                onVoiceClick = { showVoiceModal = true },
                                onUrlBarClick = { showUrlEditSheet = true },
                                showOverflowMenu = windowSize.showOmnibarOverflowMenu,
                                openTabsCount = openTabs.size,
                                onHomeClick = {
                                    barVisibilityStateMachine.forceShow()
                                    showDiscoverScreen = false
                                    showTabGridSheet = false
                                    isPageTransitioning = false
                                    targetLoadingUrl = ""
                                    AdBlockStats.resetForPage("", "", true)
                                    try {
                                        geckoView?.visibility = View.INVISIBLE
                                        geckoView?.alpha = 0f
                                    } catch (e: Exception) {}
                                    if (currentUrl.isNotBlank() && currentUrl != "about:home" && currentUrl != "about:blank") {
                                        currentUrl = ""
                                        currentTitle = "New Tab"
                                        currentError = null
                                        activeTabId?.let { id -> TabManager.clearTabLoaded(id) }
                                        TabManager.updateActiveTab("", "New Tab")
                                        try {
                                            geckoSession?.stop()
                                            geckoSession?.loadUri("about:blank")
                                        } catch (e: Exception) {}
                                    } else {
                                        val next = WallpaperManager.rotateWallpaper()
                                        activeTabId?.let { id -> TabManager.updateTabWallpaper(id, next) }
                                    }
                                },
                                onDiscoverClick = {
                                    barVisibilityStateMachine.forceShow()
                                    showDiscoverScreen = true
                                },
                                onTabsClick = {
                                    barVisibilityStateMachine.forceShow()
                                    showTabGridSheet = true
                                },
                                onBurnDataClick = {
                                    showBurnConfirmationSheet = true
                                },
                                onSettingsClick = {
                                    barVisibilityStateMachine.forceShow()
                                    showSettingsSheet = true
                                },
                                isDesktopMode = isDesktopMode,
                                isBookmarked = isBookmarked,
                                onToggleBookmark = {
                                    val added = vaultManager.toggleBookmark(currentTitle, currentUrl)
                                    ToastManager.showSuccess(if (added) "Saved to Bookmarks" else "Removed from Bookmarks")
                                },
                                onShare = {
                                    val activeUrl = currentUrl.takeIf { it.isNotBlank() } ?: currentActiveTab?.url ?: ""
                                    if (activeUrl.isNotBlank()) {
                                        activeSharePayload = SharePayload(url = activeUrl, title = currentTitle)
                                        showShareSheet = true
                                    }
                                },
                                onToggleDesktopMode = {
                                    val activeUrl = currentUrl.takeIf { it.isNotBlank() } ?: currentActiveTab?.url ?: ""
                                    if (activeUrl.isNotBlank() && activeUrl != "about:blank" && activeUrl != "about:home") {
                                        targetLoadingUrl = activeUrl
                                        isPageTransitioning = true
                                    }
                                    val newMode = TabManager.toggleDesktopMode(activeTabId)
                                    isDesktopMode = newMode
                                    currentError = null
                                    geckoSession?.reload(GeckoSession.LOAD_FLAGS_BYPASS_CACHE)
                                    ToastManager.show(if (newMode) "Desktop site enabled" else "Mobile site enabled")
                                },
                                onSaveAsPdf = {
                                    val session = geckoSession
                                    if (session != null && session.isOpen) {
                                        GeckoEngineManager.printPageToPdf(
                                            context = context,
                                            session = session,
                                            title = currentTitle,
                                            url = currentUrl
                                        ) { file: File? ->
                                            if (file != null) {
                                                ToastManager.show("PDF saved: ${file.name}")
                                            } else {
                                                ToastManager.show("Failed to generate PDF")
                                            }
                                        }
                                    } else {
                                        ToastManager.show("Page not loaded yet")
                                    }
                                },
                                onOpenFindInPage = {
                                    isFindInPageVisible = true
                                    if (findInPageQuery.isNotBlank()) {
                                        executeFind(findInPageQuery)
                                    }
                                },
                                onOpenNewTab = {
                                    showDiscoverScreen = false
                                    val newId = TabManager.openTab("", "New Tab")
                                    TabManager.selectTab(newId)
                                    currentUrl = ""
                                    currentTitle = "New Tab"
                                    WallpaperManager.rotateWallpaper()
                                },
                                onOpenNewPrivateTab = {
                                    showDiscoverScreen = false
                                    val newId = TabManager.openPrivateTab("", "Private Tab")
                                    TabManager.selectTab(newId)
                                    currentUrl = ""
                                    currentTitle = "Private Tab"
                                    WallpaperManager.rotateWallpaper()
                                },
                                onOpenTorWindow = {
                                    showDiscoverScreen = false
                                    currentError = null
                                    val newTorTabId = TabManager.openTorTab(context, "", "Tor Window")
                                    val torTab = TabManager.tabs.value.find { it.id == newTorTabId }
                                    if (torTab != null) {
                                        currentUrl = torTab.url
                                        currentTitle = torTab.title
                                    }
                                    WallpaperManager.rotateWallpaper()
                                    ToastManager.showTor("Connecting to Isolated Tor Onion Network...")
                                },
                                onOpenBookmarks = {
                                    showBookmarksScreen = true
                                },
                                onOpenDownloads = {
                                    showDownloadsScreen = true
                                },
                                onOpenHistory = {
                                    showHistoryScreen = true
                                },
                                pwaEligibility = pwaState?.eligibility,
                                onInstallPwa = {
                                    showPwaInstallSheet = true
                                },
                                includeStatusBarPadding = false,
                                selectedSearchEngine = viewModel?.uiState?.collectAsState()?.value?.selectedSearchEngine ?: "azokle",
                                onSelectSearchEngine = { engineId ->
                                    viewModel?.setSearchEngine(engineId)
                                },
                                searchEngineModifier = Modifier.tourTarget(homeTourSteps[0], homeTourState),
                                omnibarCapsuleModifier = Modifier.tourTarget(homeTourSteps[1], homeTourState)
                            )
                        }

                        // 🧅 Tor Circuit Status Stripe (below Omnibar when in a Tor tab/window)
                        if (isCurrentTabTor && !showDiscoverScreen) {
                            TorCircuitStatusStripe(
                                onNewCircuit = {
                                    TorCircuitManager.requestNewCircuit()
                                    ToastManager.showTor("Building new 3-hop Tor circuit...")
                                },
                                onOpenDetails = {
                                    showTorCircuitSheet = true
                                }
                            )
                        }
                    }
                }
            }

            // 2. Main Viewport Engine Surface
            BrowserEngineSurface(
                showDiscoverScreen = showDiscoverScreen,
                currentUrl = currentUrl,
                currentTitle = currentTitle,
                activeTabId = activeTabId,
                openTabs = openTabs,
                isCurrentTabPrivate = isCurrentTabPrivate,
                isCurrentTabTor = isCurrentTabTor,
                isDesktopMode = isDesktopMode,
                isFullScreenMode = isFullScreenMode,
                colors = colors,
                isPageTransitioning = isPageTransitioning,
                targetLoadingUrl = targetLoadingUrl,
                currentError = currentError,
                selectedSearchEngine = viewModel?.uiState?.collectAsState()?.value?.selectedSearchEngine ?: "azokle",
                homeTourSteps = homeTourSteps,
                homeTourState = homeTourState,
                onNavigateToUrl = { url, title -> navigateToUrl(url, title) },
                onOpenSearchSheet = { showUrlEditSheet = true },
                onVoiceClick = { showVoiceModal = true },
                onSelectSearchEngine = { engineId -> viewModel?.setSearchEngine(engineId) },
                onCloseDiscoverScreen = { showDiscoverScreen = false },
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
                    if (p >= 20) {
                        isPageTransitioning = false
                    }
                },
                onLoadingChanged = { loading ->
                    isLoading = loading
                    if (!loading) {
                        isPageTransitioning = false
                    }
                },
                onSecurityChanged = { secInfo ->
                    currentSecurityInfo = secInfo
                },
                onCanGoBackChanged = { canGoBack = it },
                onCanGoForwardChanged = { canGoForward = it },
                onScrollChanged = { scrollX, scrollY ->
                    barVisibilityStateMachine.onScroll(scrollX, scrollY)
                },
                onLinkLongPressed = { target ->
                    activeTextSelection = null
                    activeLinkContextTarget = target
                },
                onTextSelectionChanged = { selection ->
                    activeTextSelection = selection
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
                    targetLoadingUrl = targetUrl
                    isPageTransitioning = true
                    currentUrl = targetUrl
                    currentTitle = "New Tab"
                    currentError = null

                    val upgradedUrl = HttpsUpgrader.upgrade(targetUrl)
                    val threat = SafeBrowsingGuard.assessUrl(upgradedUrl)
                    if (threat.isThreat) {
                        pendingThreatAssessment = threat
                    }
                    ToastManager.show("Tab added to group")
                },
                onLoadErrorChanged = { error ->
                    currentError = error
                    if (error != null) {
                        isPageTransitioning = false
                        val host = try { URI(currentUrl).host } catch (e: Exception) { null }
                        if (host != null && currentUrl.startsWith("https://", ignoreCase = true)) {
                            HttpsUpgrader.recordUpgradeFailure(host)
                        }
                    }
                },
                onFullScreenChanged = { fs ->
                    isFullScreenMode = fs
                    if (fs) {
                        activeLinkContextTarget = null
                        activeTextSelection?.onDismiss?.invoke()
                        activeTextSelection = null
                        isFindInPageVisible = false
                    }
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
                },
                onRetryError = {
                    currentError = null
                    geckoSession?.reload()
                },
                onHomeFromError = {
                    currentError = null
                    currentUrl = ""
                    currentTitle = "New Tab"
                    isPageTransitioning = false
                    targetLoadingUrl = ""
                    try {
                        geckoView?.visibility = View.INVISIBLE
                        geckoView?.alpha = 0f
                    } catch (e: Exception) {}
                    activeTabId?.let { id -> TabManager.clearTabLoaded(id) }
                    TabManager.updateActiveTab("", "New Tab")
                    geckoSession?.stop()
                },
                onBypassError = currentError?.takeIf { it.canBypass }?.let {
                    {
                        currentError = null
                        geckoSession?.reload()
                    }
                },
                modifier = Modifier.weight(1f)
            )

            // 3. Tab Group Bottom Strip + Docked Bottom Action Toolbar
            if (!isFullScreenMode && !isInPipMode) {
                val shouldShowBottomControls = !windowSize.shouldHideBottomBar && (isBottomBarVisible || showDiscoverScreen || (currentUrl.isBlank() || currentUrl == "about:home"))

                AnimatedVisibility(
                    visible = shouldShowBottomControls,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(180, easing = FastOutSlowInEasing)
                    ) + expandVertically(
                        expandFrom = Alignment.Bottom,
                        animationSpec = tween(180, easing = FastOutSlowInEasing)
                    ),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(180, easing = FastOutSlowInEasing)
                    ) + shrinkVertically(
                        shrinkTowards = Alignment.Bottom,
                        animationSpec = tween(180, easing = FastOutSlowInEasing)
                    )
                ) {
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
                                    barVisibilityStateMachine.forceShow()
                                    TabManager.selectTab(selectedTab.id)
                                    currentUrl = selectedTab.url
                                    currentTitle = selectedTab.title
                                },
                                onAddTabInGroup = {
                                    barVisibilityStateMachine.forceShow()
                                    activeGroup?.let { g ->
                                        val newId = TabManager.openTabInGroup("", "New Tab", g.id)
                                        TabManager.selectTab(newId)
                                        currentUrl = ""
                                        currentTitle = "New Tab"
                                    }
                                },
                                onCloseTab = { tabIdToClose ->
                                    val wasActive = (tabIdToClose == activeTabId)
                                    TabManager.closeTab(tabIdToClose)
                                    if (wasActive) {
                                        val next = TabManager.activeTab
                                        if (next != null) {
                                            barVisibilityStateMachine.forceShow()
                                            currentUrl = next.url
                                            currentTitle = next.title
                                        }
                                    }
                                },
                                onOpenGroupOverview = {
                                    barVisibilityStateMachine.forceShow()
                                    showGroupDetailSheet = true
                                }
                            )
                        }

                        BrowserBottomBar(
                            openTabsCount = openTabs.size,
                            isHomeSelected = !showDiscoverScreen && !showTabGridSheet && (currentUrl.isBlank() || currentUrl == "about:home"),
                            isDiscoverSelected = showDiscoverScreen,
                            isTabsSelected = showTabGridSheet,
                            onHomeClick = {
                                barVisibilityStateMachine.forceShow()
                                showDiscoverScreen = false
                                showTabGridSheet = false
                                isPageTransitioning = false
                                targetLoadingUrl = ""
                                AdBlockStats.resetForPage("", "", true)
                                try {
                                    geckoView?.visibility = View.INVISIBLE
                                    geckoView?.alpha = 0f
                                } catch (e: Exception) {}
                                if (currentUrl.isNotBlank() && currentUrl != "about:home" && currentUrl != "about:blank") {
                                    currentUrl = ""
                                    currentTitle = "New Tab"
                                    currentError = null
                                    activeTabId?.let { id -> TabManager.clearTabLoaded(id) }
                                    TabManager.updateActiveTab("", "New Tab")
                                    try {
                                        geckoSession?.stop()
                                        geckoSession?.loadUri("about:blank")
                                    } catch (e: Exception) {}
                                } else {
                                    val next = WallpaperManager.rotateWallpaper()
                                    activeTabId?.let { id -> TabManager.updateTabWallpaper(id, next) }
                                }
                            },
                            onDiscoverClick = {
                                barVisibilityStateMachine.forceShow()
                                showDiscoverScreen = true
                            },
                            onTabsClick = {
                                barVisibilityStateMachine.forceShow()
                                showTabGridSheet = true
                            },
                            onBurnDataClick = {
                                showBurnConfirmationSheet = true
                            },
                            onMenuClick = {
                                barVisibilityStateMachine.forceShow()
                                showMenuSheet = true
                            },
                            showMenuDropup = showMenuSheet,
                            onDismissMenuDropup = { showMenuSheet = false },
                            currentUrl = currentUrl,
                            currentTitle = currentTitle,
                            isDesktopMode = isDesktopMode,
                            isBookmarked = isBookmarked,
                            blockedTrackersCount = shieldStats.adsBlockedCount,
                            isBrowserContext = true,
                            onReload = {
                                showMenuSheet = false
                                currentError = null
                                if (currentUrl.isBlank() || currentUrl == "about:home" || currentUrl == "about:blank" || currentUrl == "about:tor") {
                                    val next = WallpaperManager.rotateWallpaper()
                                    activeTabId?.let { id -> TabManager.updateTabWallpaper(id, next) }
                                    ToastManager.show("Wallpaper refreshed")
                                } else {
                                    geckoSession?.reload()
                                }
                            },
                            onToggleBookmark = {
                                val added = vaultManager.toggleBookmark(currentTitle, currentUrl)
                                ToastManager.showSuccess(if (added) "Saved to Bookmarks" else "Removed from Bookmarks")
                            },
                            onShare = {
                                showMenuSheet = false
                                val activeUrl = currentUrl.takeIf { it.isNotBlank() } ?: currentActiveTab?.url ?: ""
                                if (activeUrl.isNotBlank()) {
                                    activeSharePayload = SharePayload(url = activeUrl, title = currentTitle)
                                    showShareSheet = true
                                }
                            },
                            onToggleDesktopMode = {
                                showMenuSheet = false
                                val activeUrl = currentUrl.takeIf { it.isNotBlank() } ?: currentActiveTab?.url ?: ""
                                if (activeUrl.isNotBlank() && activeUrl != "about:blank" && activeUrl != "about:home") {
                                    targetLoadingUrl = activeUrl
                                    isPageTransitioning = true
                                }
                                val newMode = TabManager.toggleDesktopMode(activeTabId)
                                isDesktopMode = newMode
                                currentError = null
                                geckoSession?.reload(GeckoSession.LOAD_FLAGS_BYPASS_CACHE)
                                ToastManager.show(if (newMode) "Desktop site enabled" else "Mobile site enabled")
                            },
                            onSaveAsPdf = {
                                showMenuSheet = false
                                val session = geckoSession
                                if (session != null && session.isOpen) {
                                    GeckoEngineManager.printPageToPdf(
                                        context = context,
                                        session = session,
                                        title = currentTitle,
                                        url = currentUrl
                                    ) { file: File? ->
                                        if (file != null) {
                                            ToastManager.show("PDF saved: ${file.name}")
                                        } else {
                                            ToastManager.show("Failed to generate PDF")
                                        }
                                    }
                                } else {
                                    ToastManager.show("Page not loaded yet")
                                }
                            },
                            onOpenReaderMode = {
                                showMenuSheet = false
                                scope.launch {
                                    ToastManager.show("Extracting reader view...")
                                    val article = ReaderExtractor.extractArticleFromUrl(
                                        url = currentUrl,
                                        fallbackTitle = currentTitle,
                                        isTor = isCurrentTabTor,
                                        socksPort = TorCircuitManager.socksPort
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
                            onOpenNewTab = {
                                showMenuSheet = false
                                showDiscoverScreen = false
                                val newId = TabManager.openTab("", "New Tab")
                                TabManager.selectTab(newId)
                                currentUrl = ""
                                currentTitle = "New Tab"
                                WallpaperManager.rotateWallpaper()
                            },
                            onOpenNewPrivateTab = {
                                showMenuSheet = false
                                showDiscoverScreen = false
                                val newId = TabManager.openPrivateTab("", "Private Tab")
                                TabManager.selectTab(newId)
                                currentUrl = ""
                                currentTitle = "Private Tab"
                                WallpaperManager.rotateWallpaper()
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
                                WallpaperManager.rotateWallpaper()
                                ToastManager.showTor("Connecting to Isolated Tor Onion Network...")
                            },
                            onOpenShield = {
                                showMenuSheet = false
                                showShieldSheet = true
                            },
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
                            tabsModifier = Modifier.tourTarget(homeTourSteps[5], homeTourState),
                            burnModifier = Modifier.tourTarget(homeTourSteps[6], homeTourState),
                            menuModifier = Modifier.tourTarget(homeTourSteps[7], homeTourState)
                        )
                    }
                }
            }
        }

        // 4. Modal Overlays, Sheets, Dialogs & HUD Host
        BrowserModalOverlaysHost(
            activeArticleReader = activeArticleReader,
            onDismissArticleReader = { activeArticleReader = null },
            showShieldSheet = showShieldSheet,
            onDismissShieldSheet = { showShieldSheet = false },
            onShieldToggled = { geckoSession?.reload() },
            showSecurityCertSheet = showSecurityCertSheet,
            currentUrl = currentUrl,
            currentTitle = currentTitle,
            currentSecurityInfo = currentSecurityInfo,
            onDismissSecurityCertSheet = { showSecurityCertSheet = false },
            onReloadSecurityCert = { geckoSession?.reload() },
            showTorCircuitSheet = showTorCircuitSheet,
            onDismissTorCircuitSheet = { showTorCircuitSheet = false },
            showUrlEditSheet = showUrlEditSheet,
            onNavigateFromUrlEdit = { input ->
                showUrlEditSheet = false
                val trimmed = input.trim()
                if (ExternalSchemeHandler.isExternalScheme(trimmed)) {
                    ExternalSchemeHandler.handleExternalUri(
                        context = context,
                        uriStr = trimmed,
                        session = geckoSession,
                        isPrivate = isCurrentTabPrivate,
                        isTor = isCurrentTabTor
                    )
                    return@BrowserModalOverlaysHost
                }
                val resolved = if (trimmed.startsWith("http://", ignoreCase = true) ||
                    trimmed.startsWith("https://", ignoreCase = true) ||
                    trimmed.startsWith("about:", ignoreCase = true) ||
                    trimmed.startsWith("azokle://", ignoreCase = true)) {
                    trimmed
                } else {
                    UrlHelper.resolveSearchOrUrl(context, trimmed)
                }
                if (!isCurrentTabPrivate && !isCurrentTabTor && !resolved.startsWith("about:", ignoreCase = true) && !resolved.startsWith("azokle://", ignoreCase = true)) {
                    HistoryManager.recordTypedVisit(
                        url = resolved,
                        title = "",
                        isPrivate = false
                    )
                }
                navigateToUrl(resolved, "")
            },
            onDismissUrlEditSheet = { showUrlEditSheet = false },
            selectedSearchEngine = homeUiState?.selectedSearchEngine ?: "azokle",
            onSelectSearchEngine = { engineId -> viewModel?.setSearchEngine(engineId) },
            showMenuSheet = showMenuSheet,
            isDesktopMode = isDesktopMode,
            isBookmarked = isBookmarked,
            blockedTrackersCount = shieldStats.adsBlockedCount,
            onReloadMenu = {
                showMenuSheet = false
                currentError = null
                if (currentUrl.isBlank() || currentUrl == "about:home" || currentUrl == "about:blank" || currentUrl == "about:tor") {
                    val next = WallpaperManager.rotateWallpaper()
                    activeTabId?.let { id -> TabManager.updateTabWallpaper(id, next) }
                    ToastManager.show("Wallpaper refreshed")
                } else {
                    geckoSession?.reload()
                }
            },
            onToggleBookmarkMenu = {
                val added = vaultManager.toggleBookmark(currentTitle, currentUrl)
                ToastManager.showSuccess(if (added) "Saved to Bookmarks" else "Removed from Bookmarks")
            },
            onShareMenu = {
                showMenuSheet = false
                val activeUrl = currentUrl.takeIf { it.isNotBlank() } ?: currentActiveTab?.url ?: ""
                if (activeUrl.isNotBlank()) {
                    activeSharePayload = SharePayload(url = activeUrl, title = currentTitle)
                    showShareSheet = true
                }
            },
            onToggleDesktopModeMenu = {
                showMenuSheet = false
                val activeUrl = currentUrl.takeIf { it.isNotBlank() } ?: currentActiveTab?.url ?: ""
                if (activeUrl.isNotBlank() && activeUrl != "about:blank" && activeUrl != "about:home") {
                    targetLoadingUrl = activeUrl
                    isPageTransitioning = true
                }
                val newMode = TabManager.toggleDesktopMode(activeTabId)
                isDesktopMode = newMode
                currentError = null
                geckoSession?.reload(GeckoSession.LOAD_FLAGS_BYPASS_CACHE)
                ToastManager.show(if (newMode) "Desktop site enabled" else "Mobile site enabled")
            },
            onSaveAsPdfMenu = {
                showMenuSheet = false
                val session = geckoSession
                if (session != null && session.isOpen) {
                    GeckoEngineManager.printPageToPdf(
                        context = context,
                        session = session,
                        title = currentTitle,
                        url = currentUrl
                    ) { file: File? ->
                        if (file != null) {
                            ToastManager.show("PDF saved: ${file.name}")
                        } else {
                            ToastManager.show("Failed to generate PDF")
                        }
                    }
                } else {
                    ToastManager.show("Page not loaded yet")
                }
            },
            onOpenReaderModeMenu = {
                showMenuSheet = false
                scope.launch {
                    ToastManager.show("Extracting reader view...")
                    val article = ReaderExtractor.extractArticleFromUrl(
                        url = currentUrl,
                        fallbackTitle = currentTitle,
                        isTor = isCurrentTabTor,
                        socksPort = TorCircuitManager.socksPort
                    )
                    activeArticleReader = article
                }
            },
            onOpenFindInPageMenu = {
                showMenuSheet = false
                isFindInPageVisible = true
                if (findInPageQuery.isNotBlank()) {
                    executeFind(findInPageQuery)
                }
            },
            onOpenBookmarksMenu = {
                showMenuSheet = false
                showBookmarksScreen = true
            },
            onOpenDownloadsMenu = {
                showMenuSheet = false
                showDownloadsScreen = true
            },
            onOpenHistoryMenu = {
                showMenuSheet = false
                showHistoryScreen = true
            },
            onOpenShieldMenu = {
                showMenuSheet = false
                showShieldSheet = true
            },
            onOpenNewTabMenu = {
                showMenuSheet = false
                showDiscoverScreen = false
                val newId = TabManager.openTab("", "New Tab")
                TabManager.selectTab(newId)
                currentUrl = ""
                currentTitle = "New Tab"
                WallpaperManager.rotateWallpaper()
            },
            onOpenNewPrivateTabMenu = {
                showMenuSheet = false
                showDiscoverScreen = false
                val newId = TabManager.openPrivateTab("", "Private Tab")
                TabManager.selectTab(newId)
                currentUrl = ""
                currentTitle = "Private Tab"
                WallpaperManager.rotateWallpaper()
            },
            onOpenTorWindowMenu = {
                showMenuSheet = false
                showDiscoverScreen = false
                currentError = null
                val newTorTabId = TabManager.openTorTab(context, "", "Tor Window")
                val torTab = TabManager.tabs.value.find { it.id == newTorTabId }
                if (torTab != null) {
                    currentUrl = torTab.url
                    currentTitle = torTab.title
                }
                WallpaperManager.rotateWallpaper()
                ToastManager.showTor("Connecting to Isolated Tor Onion Network...")
            },
            onOpenSettingsMenu = {
                showMenuSheet = false
                showSettingsSheet = true
            },
            pwaState = pwaState,
            showPwaInstallSheet = showPwaInstallSheet,
            onDismissPwaInstallSheet = { showPwaInstallSheet = false },
            onInstallPwaMenu = {
                showMenuSheet = false
                showPwaInstallSheet = true
            },
            onBurnDataMenu = {
                showMenuSheet = false
                showBurnConfirmationSheet = true
            },
            onDismissMenuSheet = { showMenuSheet = false },
            showBookmarksScreen = showBookmarksScreen,
            onDismissBookmarksScreen = { showBookmarksScreen = false },
            onNavigateFromBookmarks = { targetUrl ->
                showBookmarksScreen = false
                showDiscoverScreen = false
                navigateToUrl(targetUrl)
            },
            onOpenTorFromBookmarks = { targetUrl ->
                showBookmarksScreen = false
                showDiscoverScreen = false
                val newTorTabId = TabManager.openTorTab(context, targetUrl, "Tor")
                val torTab = TabManager.tabs.value.find { it.id == newTorTabId }
                if (torTab != null) {
                    currentUrl = torTab.url
                    currentTitle = torTab.title
                }
            },
            showDownloadsScreen = showDownloadsScreen,
            onDismissDownloadsScreen = { showDownloadsScreen = false },
            showHistoryScreen = showHistoryScreen,
            onDismissHistoryScreen = { showHistoryScreen = false },
            onHistoryQueryClick = { query ->
                showHistoryScreen = false
                showDiscoverScreen = false
                val searchEngineConfig = SEARCH_ENGINE_PROVIDERS.find { it.id == viewModel?.uiState?.value?.selectedSearchEngine }
                    ?: SEARCH_ENGINE_PROVIDERS[0]
                val targetUrl = UrlHelper.resolveSearchOrUrl(
                    input = query,
                    searchEngineQueryUrl = searchEngineConfig.queryUrl
                )
                navigateToUrl(targetUrl, query)
            },
            onHistoryUrlClick = { url ->
                showHistoryScreen = false
                showDiscoverScreen = false
                navigateToUrl(url)
            },
            viewModel = viewModel,
            showTabGridSheet = showTabGridSheet,
            forceStartTabsTour = forceStartTabsTour,
            onTabSelected = { selectedTab ->
                showTabGridSheet = false
                showDiscoverScreen = false
                currentError = null
                TabManager.selectTab(selectedTab.id)
                currentUrl = selectedTab.url
                currentTitle = selectedTab.title
            },
            onNewTabFromSwitcher = {
                showTabGridSheet = false
                showDiscoverScreen = false
                currentError = null
                val newTabId = TabManager.openTab("", "New Tab")
                TabManager.selectTab(newTabId)
                currentUrl = ""
                currentTitle = "New Tab"
                WallpaperManager.rotateWallpaper()
            },
            onCloseTabsSwitcher = {
                showTabGridSheet = false
                if (TabManager.tabs.value.isEmpty()) {
                    onClose()
                }
            },
            onTabsTourConsumed = { forceStartTabsTour = false },
            showSettingsSheet = showSettingsSheet,
            onDismissSettingsSheet = { showSettingsSheet = false },
            onReplayHomeTour = {
                showSettingsSheet = false
                if (currentUrl.isNotBlank() && currentUrl != "about:home" && currentUrl != "about:blank") {
                    currentUrl = ""
                    currentTitle = "Search"
                }
                scope.launch {
                    delay(350)
                    homeTourState.start()
                }
            },
            onReplayTabsTour = {
                showSettingsSheet = false
                forceStartTabsTour = true
                showTabGridSheet = true
            },
            onOpenUrlFromSettings = { url ->
                val newId = TabManager.openTab(url, "Azokle")
                TabManager.selectTab(newId)
                geckoSession?.loadUri(url)
            },
            isSearchEngineSheetOpen = homeUiState?.isSearchEngineSheetOpen == true,
            onDismissSearchEngineSheet = { viewModel?.setSearchEngineSheetOpen(false) },
            isLanguageSheetOpen = homeUiState?.isLanguageSheetOpen == true,
            selectedLanguageCode = homeUiState?.selectedLanguage ?: "system",
            onSelectLanguage = { langCode -> viewModel?.setAppLanguage(langCode) },
            onDismissLanguageSheet = { viewModel?.setLanguageSheetOpen(false) },
            isPrivacyModalOpen = homeUiState?.isPrivacyModalOpen == true,
            onDismissPrivacyModal = { viewModel?.setPrivacyModalOpen(false) },
            isTermsModalOpen = homeUiState?.isTermsModalOpen == true,
            onDismissTermsModal = { viewModel?.setTermsModalOpen(false) },
            isAboutModalOpen = homeUiState?.isAboutModalOpen == true,
            onDismissAboutModal = { viewModel?.setAboutModalOpen(false) },
            activeLinkContextTarget = activeLinkContextTarget,
            isCurrentTabPrivate = isCurrentTabPrivate,
            isCurrentTabTor = isCurrentTabTor,
            onOpenInNewTabGroup = { url, title ->
                val parentId = activeTabId ?: ""
                val parentTab = openTabs.find { it.id == parentId }
                val isPriv = parentTab?.isPrivate ?: isCurrentTabPrivate
                val isTor = parentTab?.isTor ?: isCurrentTabTor
                val newId = if (parentId.isNotBlank()) {
                    TabManager.openTabFromParent(url, title, parentId, isPrivate = isPriv, isTor = isTor)
                } else {
                    TabManager.openTab(url, title, isPrivate = isPriv, isTor = isTor)
                }
                TabManager.selectTab(newId)
                currentUrl = url
                currentTitle = title
                currentError = null
                ToastManager.show("Opened in tab group")
            },
            onOpenInNewTab = { url, title ->
                val parentTab = openTabs.find { it.id == activeTabId }
                val isPriv = parentTab?.isPrivate ?: isCurrentTabPrivate
                val isTor = parentTab?.isTor ?: isCurrentTabTor
                val newId = TabManager.openTab(url, title, isPrivate = isPriv, isTor = isTor)
                TabManager.selectTab(newId)
                currentUrl = url
                currentTitle = title
                currentError = null
                ToastManager.show("Opened in new tab")
            },
            onOpenInBackgroundTab = { url, title ->
                val parentTab = openTabs.find { it.id == activeTabId }
                val isPriv = parentTab?.isPrivate ?: isCurrentTabPrivate
                val isTor = parentTab?.isTor ?: isCurrentTabTor
                TabManager.openTab(url, title, isPrivate = isPriv, isTor = isTor)
                ToastManager.show("Tab opened in background")
            },
            onOpenInPrivateTab = { url, title ->
                val newId = TabManager.openTab(url, title, isPrivate = true)
                TabManager.selectTab(newId)
                currentUrl = url
                currentTitle = title
                currentError = null
                ToastManager.show("Opened in private tab")
            },
            onDismissLinkContext = { activeLinkContextTarget = null },
            activeTextSelection = activeTextSelection,
            onSearchRequested = { query ->
                val searchUrl = UrlHelper.resolveSearchOrUrl(
                    context = context,
                    input = query
                )
                val newId = TabManager.openTab(
                    url = searchUrl,
                    title = "Search",
                    isPrivate = isCurrentTabPrivate,
                    isTor = isCurrentTabTor
                )
                TabManager.selectTab(newId)
                currentUrl = searchUrl
                currentTitle = "Search"
                currentError = null
                geckoSession?.loadUri(searchUrl)
                ToastManager.show("Searching web")
            },
            onShareRequested = { selectedText ->
                activeSharePayload = SharePayload(
                    url = currentUrl,
                    title = currentTitle,
                    selectedText = selectedText
                )
                showShareSheet = true
            },
            showShareSheet = showShareSheet,
            activeSharePayload = activeSharePayload,
            onDismissShareSheet = {
                showShareSheet = false
                activeSharePayload = null
            },
            onPrintToPdfShare = {
                val session = geckoSession
                if (session != null && session.isOpen) {
                    GeckoEngineManager.printPageToPdf(
                        context = context,
                        session = session,
                        title = currentTitle,
                        url = currentUrl
                    ) { pdfFile ->
                        if (pdfFile != null) {
                            ToastManager.showSuccess("Page saved to Downloads")
                        } else {
                            ToastManager.show("Failed to export PDF")
                        }
                    }
                }
            },
            showGroupDetailSheet = showGroupDetailSheet,
            activeGroup = activeGroup,
            activeGroupTabs = activeGroupTabs,
            activeTabId = activeTabId,
            onAddTabInGroup = {
                showGroupDetailSheet = false
                activeGroup?.let { g ->
                    val newId = TabManager.openTabInGroup("", "New Tab", g.id)
                    TabManager.selectTab(newId)
                    currentUrl = ""
                    currentTitle = "New Tab"
                }
            },
            onCloseTabInGroup = { tabIdToClose ->
                val wasActive = (tabIdToClose == activeTabId)
                TabManager.closeTab(tabIdToClose)
                if (wasActive) {
                    val next = TabManager.activeTab
                    if (next != null) {
                        currentUrl = next.url
                        currentTitle = next.title
                    }
                }
            },
            onDismissGroupDetailSheet = { showGroupDetailSheet = false },
            showBurnConfirmationSheet = showBurnConfirmationSheet,
            onConfirmBurnData = {
                showBurnConfirmationSheet = false
                isBurningData = true
            },
            onDismissBurnConfirmation = { showBurnConfirmationSheet = false },
            isBurningData = isBurningData,
            onCompleteBurnData = {
                TabManager.closeAllTabs()
                GeckoEngineManager.clearAllData(context)
                HistoryManager.clearAll()
                DownloadManager.clearAllHistory(context)
                DownloadManager.shredPrivateDownloads(context)
                SitePermissionsManager.getInstance(context).clearAllPermissions()
                AzokleHistoryDatabase.getInstance(context).clearAllSecurely()
                PreferencesManager(context).clearRecentSearches()
                AdBlockStats.resetForPage("", "", true)

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

                ToastManager.showSuccess("All browsing data cleared")
            },
            activePermissionRequest = activePermissionRequest,
            onAllowAlwaysPermission = { req -> handleAllowPermission(req, true) },
            onAllowOncePermission = { req -> handleAllowPermission(req, false) },
            onDenyPermission = { req, rem -> handleDenyPermission(req, rem) },
            pendingThreatAssessment = pendingThreatAssessment,
            onReturnToSafetyThreat = {
                pendingThreatAssessment = null
                navigateToUrl("about:home", "Home")
            },
            onProceedUnsafeThreat = { threat ->
                SafeBrowsingGuard.bypassThreatForSession(threat.host)
                val target = threat.url
                pendingThreatAssessment = null
                navigateToUrl(target)
            },
            pendingDownloadPayload = pendingDownloadPayload,
            onConfirmDownload = { payload ->
                pendingDownloadPayload = null
                DownloadManager.startDownload(
                    context = context,
                    url = payload.url,
                    filename = payload.filename,
                    mimeType = payload.mimeType,
                    contentLength = payload.contentLength,
                    headers = payload.headers,
                    isPrivate = payload.isPrivate,
                    isTor = payload.isTor
                )
                ToastManager.show("Downloading ${payload.filename}...")
            },
            onDismissDownload = { pendingDownloadPayload = null },
            showVoiceModal = showVoiceModal,
            onVoiceSearchResult = { recognizedQuery ->
                showVoiceModal = false
                val searchEngineConfig = SEARCH_ENGINE_PROVIDERS.find { it.id == viewModel?.uiState?.value?.selectedSearchEngine }
                    ?: SEARCH_ENGINE_PROVIDERS[0]
                val targetUrl = UrlHelper.resolveSearchOrUrl(
                    input = recognizedQuery,
                    searchEngineQueryUrl = searchEngineConfig.queryUrl
                )
                navigateToUrl(targetUrl, recognizedQuery)
            },
            onDismissVoiceModal = { showVoiceModal = false },
            showQrScanner = showQrScanner,
            onQrScanResult = { rawData ->
                showQrScanner = false
                val trimmed = rawData.trim()
                if (trimmed.isNotBlank()) {
                    val searchEngineConfig = SEARCH_ENGINE_PROVIDERS.find { it.id == viewModel?.uiState?.value?.selectedSearchEngine }
                        ?: SEARCH_ENGINE_PROVIDERS[0]
                    val targetUrl = UrlHelper.resolveSearchOrUrl(
                        input = trimmed,
                        searchEngineQueryUrl = searchEngineConfig.queryUrl
                    )
                    navigateToUrl(targetUrl, trimmed)
                }
            },
            onDismissQrScanner = { showQrScanner = false },
            showDiscoverScreen = showDiscoverScreen,
            homeTourState = homeTourState,
            geckoView = geckoView
        )
    }
}
