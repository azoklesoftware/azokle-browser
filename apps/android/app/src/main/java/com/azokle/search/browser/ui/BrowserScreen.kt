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
    var showMenuSheet by remember { mutableStateOf(false) }
    var showSecurityCertSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showDownloadsScreen by remember { mutableStateOf(false) }
    var showHistoryScreen by remember { mutableStateOf(false) }
    var showPwaInstallSheet by remember { mutableStateOf(false) }
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
    var isBurningData by remember { mutableStateOf(false) }
    var activeLinkContextTarget: LinkContextTarget? by remember { mutableStateOf(null) }
    var newTabNotification: TabSession? by remember { mutableStateOf(null) }
    var tabSnackbarJob: Job? by remember { mutableStateOf(null) }
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

    LaunchedEffect(Unit) {
        AdBlockEngine.instance.initialize(context)
    }

    BackHandler(enabled = true) {
        if (isFullScreenMode) {
            isFullScreenMode = false
            geckoSession?.exitFullScreen()
        } else if (isBurningData) {
            // Ignore back during burning
        } else if (activePermissionRequest != null) {
            activePermissionRequest?.let { handleDenyPermission(it, false) }
        } else if (activeLinkContextTarget != null) {
            activeLinkContextTarget = null
        } else if (showDownloadsScreen) {
            showDownloadsScreen = false
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
        } else if (showTabGridSheet) {
            showTabGridSheet = false
        } else if (showSettingsSheet) {
            showSettingsSheet = false
        } else if (showSecurityCertSheet) {
            showSecurityCertSheet = false
        } else if (isFindInPageVisible) {
            isFindInPageVisible = false
            executeFind("")
        } else if (activeArticleReader != null) {
            activeArticleReader = null
        } else if (currentError != null) {
            currentError = null
            if (canGoBack) geckoSession?.goBack() else onClose()
        } else if (canGoBack) {
            geckoSession?.goBack()
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
            if (!isFullScreenMode && !isInPipMode) {
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
                        blockedCount = shieldStats.adsBlockedCount,
                        isShieldEnabled = shieldStats.isShieldEnabled,
                        isLoading = isLoading,
                        progress = progress,
                        onSecurityLockClick = { showSecurityCertSheet = true },
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
                        onSettingsClick = { showSettingsSheet = true },
                        onMenuClick = { showMenuSheet = true },
                        onUrlBarClick = { showUrlEditSheet = true }
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

                if (!isFullScreenMode && !isInPipMode && isCurrentTabTor) {
                    val torState by com.azokle.search.security.TorCircuitManager.connectionState.collectAsState()
                    val isDarkTheme = colors.isDark
                    val bannerBg = if (isDarkTheme) Color(0xFF0D0D0D) else Color(0xFFFAFAFA)
                    val bannerBorder = if (isDarkTheme) Color(0xFF242424) else Color(0xFFE4E4E7)
                    val btnBg = if (isDarkTheme) Color(0xFF161616) else Color(0xFFF4F4F5)
                    val textColor = if (isDarkTheme) Color(0xFFFFFFFF) else Color(0xFF000000)
                    val mutedColor = if (isDarkTheme) Color(0xFFA1A1AA) else Color(0xFF52525B)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(bannerBg)
                            .border(0.5.dp, bannerBorder)
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = textColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(7.dp))
                            when (val state = torState) {
                                is com.azokle.search.security.TorConnectionState.Connected -> {
                                    Text(
                                        text = "Circuit: ${state.exitCountry} (${state.exitIp})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = FontFamily.Monospace,
                                        color = textColor
                                    )
                                }
                                is com.azokle.search.security.TorConnectionState.Bootstrapping -> {
                                    Text(
                                        text = "Connecting Tor (${state.progress}%)...",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        fontFamily = FontFamily.Monospace,
                                        color = mutedColor
                                    )
                                }
                                is com.azokle.search.security.TorConnectionState.Starting -> {
                                    Text(
                                        text = "Starting Tor daemon...",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        fontFamily = FontFamily.Monospace,
                                        color = mutedColor
                                    )
                                }
                                else -> {
                                    Text(
                                        text = "Tor Circuit Ready",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = FontFamily.Monospace,
                                        color = textColor
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(btnBg)
                                .border(0.5.dp, bannerBorder, RoundedCornerShape(6.dp))
                                .clickable {
                                     com.azokle.search.security.TorCircuitManager.requestNewCircuit()
                                     geckoSession?.reload()
                                     com.azokle.search.ui.components.ToastManager.showTor("New Tor Circuit Established")
                                 }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = textColor,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "New Circuit",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                            }
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    if (isCurrentTabTor && (currentUrl == "about:tor" || currentUrl.isBlank())) {
                        TorConnectScreen(
                            onNavigate = { targetUrl ->
                                currentUrl = targetUrl
                                currentTitle = "Tor Browsing"
                                currentError = null
                                TabManager.updateActiveTab(targetUrl, "Tor Browsing")
                                geckoSession?.loadUri(targetUrl)
                            },
                            onClose = onClose
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
                                    val newId = TabManager.openTab(targetUrl, "New Tab")
                                    val newTab = TabManager.tabs.value.find { it.id == newId }
                                    newTabNotification = newTab
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
                                onHome = onClose,
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

            // 3. Docked Bottom Action Toolbar (Zero overlap with webpage footer)
            if (!isFullScreenMode && !isInPipMode) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = isBottomBarVisible,
                    enter = androidx.compose.animation.expandVertically(
                        animationSpec = androidx.compose.animation.core.tween(180)
                    ) + androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(180)),
                    exit = androidx.compose.animation.shrinkVertically(
                        animationSpec = androidx.compose.animation.core.tween(180)
                    ) + androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(180))
                ) {
                    BrowserBottomBar(
                        openTabsCount = openTabs.size,
                        onHomeClick = onClose,
                        onSearchClick = { showUrlEditSheet = true },
                        onTabsClick = { showTabGridSheet = true },
                        onBurnDataClick = {
                            isBurningData = true
                        },
                        onMenuClick = { showMenuSheet = true }
                    )
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
                    isDesktopMode = !isDesktopMode
                    currentError = null
                    geckoSession?.reload()
                    com.azokle.search.ui.components.ToastManager.show(
                        if (isDesktopMode) "Desktop site enabled 💻" else "Mobile site enabled 📱"
                    )
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
                onOpenTorWindow = {
                    showMenuSheet = false
                    currentError = null
                    val newTorTabId = TabManager.openTorTab(context, "about:tor", "Tor Window")
                    val torTab = TabManager.tabs.value.find { it.id == newTorTabId }
                    if (torTab != null) {
                        currentUrl = torTab.url
                        currentTitle = torTab.title
                    }
                    com.azokle.search.ui.components.ToastManager.showTor("Connecting to Isolated Tor Onion Network...")
                },
                onEnterPictureInPicture = if (com.azokle.search.browser.media.PictureInPictureHelper.supportsPictureInPicture(context)) {
                    {
                        val activity = context as? android.app.Activity
                        if (activity != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            val state = com.azokle.search.browser.media.MediaPlaybackManager.mediaState.value
                            val isPlaying = state.status == com.azokle.search.browser.media.PlaybackStatus.PLAYING
                            val params = com.azokle.search.browser.media.PictureInPictureHelper.buildPipParams(
                                activity = activity,
                                isPlaying = isPlaying,
                                isFullscreen = true,
                                aspectRatio = state.metadata.videoAspectRatio
                            )
                            try {
                                activity.enterPictureInPictureMode(params)
                            } catch (e: Exception) {
                                // Safe ignore
                            }
                        }
                    }
                } else null,
                pwaEligibility = pwaState?.eligibility,
                onInstallPwa = {
                    showMenuSheet = false
                    showPwaInstallSheet = true
                },
                onBurnData = {
                    showMenuSheet = false
                    isBurningData = true
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
                    currentError = null
                    TabManager.selectTab(selectedTab.id)
                    currentUrl = selectedTab.url
                    currentTitle = selectedTab.title
                    geckoSession?.loadUri(selectedTab.url)
                },
                onNewTabClick = {
                    showTabGridSheet = false
                    currentError = null
                    val newTabId = TabManager.openTab("https://search.azokle.com", "Search")
                    TabManager.selectTab(newTabId)
                    currentUrl = "https://search.azokle.com"
                    currentTitle = "Search"
                    geckoSession?.loadUri("https://search.azokle.com")
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

        // Link Context Menu Sheet (Long-Press hyperlink)
        activeLinkContextTarget?.let { target ->
            LinkContextMenuSheet(
                target = target,
                onOpenInNewTab = { url, title ->
                    val newId = TabManager.openTab(url, title, isPrivate = false)
                    val newTab = TabManager.tabs.value.find { it.id == newId }
                    newTabNotification = newTab
                    tabSnackbarJob?.cancel()
                    tabSnackbarJob = scope.launch {
                        delay(4500)
                        newTabNotification = null
                    }
                },
                onOpenInPrivateTab = { url, title ->
                    val newId = TabManager.openTab(url, title, isPrivate = true)
                    val newTab = TabManager.tabs.value.find { it.id == newId }
                    newTabNotification = newTab
                    tabSnackbarJob?.cancel()
                    tabSnackbarJob = scope.launch {
                        delay(4500)
                        newTabNotification = null
                    }
                },
                onDismiss = { activeLinkContextTarget = null }
            )
        }

        // Floating "Opened in New Tab • SWITCH" Action Pill (Brave/Arc pattern)
        AnimatedVisibility(
            visible = newTabNotification != null,
            enter = slideInVertically(initialOffsetY = { it * 2 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it * 2 }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (isBottomBarVisible) 76.dp else 24.dp)
        ) {
            newTabNotification?.let { tab ->
                Box(
                    modifier = Modifier
                        .shadow(16.dp, RoundedCornerShape(24.dp), spotColor = Color.White.copy(alpha = 0.15f))
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF121218))
                        .border(
                            1.dp,
                            Color(0xFF27272A),
                            RoundedCornerShape(24.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF38BDF8))
                        )
                        Spacer(modifier = Modifier.width(9.dp))
                        Text(
                            text = if (tab.isPrivate) stringResource(R.string.opened_in_private_tab) else stringResource(R.string.opened_in_new_tab),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .clickable {
                                    TabManager.selectTab(tab.id)
                                    currentUrl = tab.url
                                    currentTitle = tab.title
                                    geckoSession?.loadUri(tab.url)
                                    newTabNotification = null
                                }
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.btn_switch),
                                color = Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 🔥 Full Screen Cinema-Grade Fire Incineration Overlay
        if (isBurningData) {
            com.azokle.search.ui.components.FireIncinerationOverlay(
                onComplete = {
                    TabManager.closeAllTabs()
                    com.azokle.search.browser.gecko.GeckoEngineManager.clearAllData(context)
                    com.azokle.search.browser.history.HistoryManager.clearAll()
                    com.azokle.search.data.PreferencesManager(context).clearRecentSearches()
                    isBurningData = false
                    onClose()
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

        // 🌟 Premium Floating Toast HUD
        com.azokle.search.ui.components.AzokleToastHost()
    }
}
