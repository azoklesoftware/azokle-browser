package com.azokle.search.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import android.content.pm.PackageManager
import com.azokle.search.permissions.SitePermissionRequest
import com.azokle.search.permissions.SitePermissionState
import com.azokle.search.permissions.SitePermissionType
import com.azokle.search.permissions.SitePermissionsManager
import com.azokle.search.media.GeckoMediaSessionBridge
import com.azokle.search.media.MediaPlaybackManager
import com.azokle.search.tabs.TabManager
import com.azokle.search.engine.ui.BrowserCrashRecoverySurface
import com.azokle.search.engine.ui.BrowserErrorType
import com.azokle.search.engine.delegates.AzokleContentBlockingDelegate
import com.azokle.search.engine.delegates.AzokleContentDelegate
import com.azokle.search.engine.delegates.AzokleHistoryDelegate
import com.azokle.search.engine.delegates.AzokleNavigationDelegate
import com.azokle.search.engine.delegates.AzoklePermissionDelegate
import com.azokle.search.engine.delegates.AzokleProgressDelegate
import com.azokle.search.engine.delegates.AzokleScrollDelegate
import com.azokle.search.common.util.UrlHelper
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSession.ProgressDelegate.SecurityInformation
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.SlowScriptResponse

data class DownloadConfirmationPayload(
    val url: String,
    val filename: String,
    val mimeType: String,
    val contentLength: Long,
    val headers: Map<String, String>? = null,
    val isPrivate: Boolean = false,
    val isTor: Boolean = false
)

@Composable
fun GeckoBrowserView(
    url: String,
    tabId: String? = null,
    isDesktopMode: Boolean,
    isDarkMode: Boolean = false,
    isPrivate: Boolean = false,
    isTor: Boolean = false,
    isFullScreenMode: Boolean = false,
    onUrlChanged: (String) -> Unit,
    onTitleChanged: (String) -> Unit,
    onProgressChanged: (Int) -> Unit,
    onLoadingChanged: (Boolean) -> Unit,
    onSecurityChanged: (SecurityInformation?) -> Unit,
    onCanGoBackChanged: (Boolean) -> Unit,
    onCanGoForwardChanged: (Boolean) -> Unit,
    onScrollChanged: (Int, Int) -> Unit = { _, _ -> },
    onLinkLongPressed: (com.azokle.search.engine.ui.LinkContextTarget) -> Unit = {},
    onNewWindowRequested: (String) -> Unit = {},
    onLoadErrorChanged: (BrowserErrorType?) -> Unit = {},
    onFullScreenChanged: (Boolean) -> Unit = {},
    onPermissionPromptRequested: (SitePermissionRequest) -> Unit = {},
    onDownloadConfirmationRequested: ((DownloadConfirmationPayload) -> Unit)? = null,
    onTextSelectionChanged: ((TextSelectionPayload?) -> Unit)? = null,
    onSessionReady: (GeckoSession, GeckoView) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val runtime = remember { GeckoEngineManager.getRuntime(context) }
    val session = remember(tabId, isPrivate, isTor) {
        if (tabId != null) {
            TabManager.getOrCreateSession(tabId, context, isPrivate, isTor)
        } else {
            GeckoEngineManager.createSession(context, isPrivate, isTor)
        }
    }
    var lastLoadedUrl by remember { androidx.compose.runtime.mutableStateOf("") }
    var isCrashed by remember { androidx.compose.runtime.mutableStateOf(false) }
    var isOomKill by remember { androidx.compose.runtime.mutableStateOf(false) }

    var isFirstDesktopEffect by remember { androidx.compose.runtime.mutableStateOf(true) }
    LaunchedEffect(isDesktopMode) {
        if (isFirstDesktopEffect) {
            isFirstDesktopEffect = false
            if (isDesktopMode) {
                session.settings.userAgentMode = GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
                session.settings.viewportMode = GeckoSessionSettings.VIEWPORT_MODE_DESKTOP
                session.settings.userAgentOverride = GeckoEngineManager.MODERN_DESKTOP_UA
            }
            return@LaunchedEffect
        }
        if (isDesktopMode) {
            session.settings.userAgentMode = GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
            session.settings.viewportMode = GeckoSessionSettings.VIEWPORT_MODE_DESKTOP
            session.settings.userAgentOverride = GeckoEngineManager.MODERN_DESKTOP_UA
        } else {
            session.settings.userAgentMode = GeckoSessionSettings.USER_AGENT_MODE_MOBILE
            session.settings.viewportMode = GeckoSessionSettings.VIEWPORT_MODE_MOBILE
            session.settings.userAgentOverride = null
        }
        if (session.isOpen) {
            session.reload()
        }
    }

    var swipeRefreshRef by remember { androidx.compose.runtime.mutableStateOf<GeckoSwipeRefreshLayout?>(null) }

    LaunchedEffect(tabId, url) {
        swipeRefreshRef?.currentUrl = url
        swipeRefreshRef?.resetGestureState()
        if (url.isNotBlank() && !url.equals("about:blank", ignoreCase = true) && !url.equals("about:home", ignoreCase = true)) {
            val isAlreadyLoaded = (tabId != null && TabManager.isTabLoaded(tabId, url))
            if (!isAlreadyLoaded) {
                if (tabId != null) {
                    TabManager.markTabLoaded(tabId, url)
                }
                lastLoadedUrl = url
                if (!session.isOpen) {
                    session.open(runtime)
                }
                session.loadUri(url)
            }
        }
    }

    LaunchedEffect(isFullScreenMode) {
        swipeRefreshRef?.isFullScreen = isFullScreenMode
    }

    androidx.compose.foundation.layout.Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
        factory = { ctx ->
            val nativeBgColor = if (isFullScreenMode) {
                android.graphics.Color.BLACK
            } else if (isDarkMode) {
                android.graphics.Color.parseColor("#121212")
            } else {
                android.graphics.Color.parseColor("#FFFFFF")
            }
            val swipeRefresh = GeckoSwipeRefreshLayout(ctx).apply {
                currentUrl = url
                isFullScreen = isFullScreenMode
                currentScrollY = 0
                isContentAtTop = true
                setBackgroundColor(nativeBgColor)
                setColorSchemeColors(
                    android.graphics.Color.parseColor("#38BDF8"),
                    android.graphics.Color.parseColor("#818CF8"),
                    android.graphics.Color.parseColor("#C084FC")
                )
                setProgressBackgroundColorSchemeColor(
                    if (isDarkMode) android.graphics.Color.parseColor("#27272A")
                    else android.graphics.Color.parseColor("#FFFFFF")
                )
                setOnRefreshListener {
                    if (!canTriggerRefresh()) {
                        isRefreshing = false
                        return@setOnRefreshListener
                    }
                    onLoadErrorChanged(null)
                    if (session.isOpen) {
                        session.reload(GeckoSession.LOAD_FLAGS_NONE)
                        // Safety timeout to prevent spinner from hanging indefinitely on stalled connections
                        postDelayed({
                            if (isRefreshing) {
                                isRefreshing = false
                            }
                        }, 10000L)
                    } else {
                        isRefreshing = false
                    }
                }
            }
            swipeRefreshRef = swipeRefresh

            val geckoView = NestedGeckoView(ctx).apply {
                setBackgroundColor(nativeBgColor)
                visibility = android.view.View.VISIBLE
                alpha = 1f
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                // Initialize native media subsystem & bridge GeckoView MediaSession
                MediaPlaybackManager.initialize(ctx)
                try {
                    session.settings.suspendMediaWhenInactive = false
                } catch (e: Exception) {}
                val tabSessionId = tabId ?: session.hashCode().toString()
                session.mediaSessionDelegate = GeckoMediaSessionBridge(
                    tabId = tabSessionId,
                    isPrivate = isPrivate,
                    isTor = isTor,
                    currentUrlProvider = { lastLoadedUrl }
                )

                // 1. Navigation Delegate & Error Interceptor
                session.navigationDelegate = AzokleNavigationDelegate(
                    context = context,
                    tabId = tabId,
                    isPrivate = isPrivate,
                    isTor = isTor,
                    onUrlChanged = { newUrl ->
                        lastLoadedUrl = newUrl
                        onUrlChanged(newUrl)
                    },
                    onLoadErrorChanged = onLoadErrorChanged,
                    onCanGoBackChanged = onCanGoBackChanged,
                    onCanGoForwardChanged = onCanGoForwardChanged,
                    onNewWindowRequested = onNewWindowRequested,
                    onRefreshStateChanged = { isRefreshing ->
                        swipeRefresh.isRefreshing = isRefreshing
                    }
                )

                // 2. Content Blocking & Shield Delegate
                session.contentBlockingDelegate = AzokleContentBlockingDelegate()

                // 3. Progress & Security Delegate
                session.progressDelegate = AzokleProgressDelegate(
                    viewProvider = { this@apply },
                    swipeRefreshProvider = { swipeRefresh },
                    tabId = tabId,
                    isPrivate = isPrivate,
                    isTor = isTor,
                    onLoadingChanged = onLoadingChanged,
                    onProgressChanged = onProgressChanged,
                    onSecurityChanged = onSecurityChanged,
                    onLoadErrorChanged = onLoadErrorChanged,
                    onCrashStateReset = {
                        isCrashed = false
                        isOomKill = false
                    }
                )

                // 4. Content Delegate (Title, Fullscreen, Context Menu, Downloads, Crash Recovery, PWA)
                session.contentDelegate = AzokleContentDelegate(
                    context = context,
                    viewProvider = { this@apply },
                    swipeRefreshProvider = { swipeRefresh },
                    tabId = tabId,
                    isPrivate = isPrivate,
                    isTor = isTor,
                    currentUrlProvider = { url },
                    onTitleChanged = onTitleChanged,
                    onFullScreenChanged = onFullScreenChanged,
                    onLinkLongPressed = onLinkLongPressed,
                    onDownloadConfirmationRequested = onDownloadConfirmationRequested,
                    onCrashStateChanged = { crashed, oom ->
                        isCrashed = crashed
                        isOomKill = oom
                    }
                )

                // 5. Scroll Delegate (Hides bottom bar when scrolling down and enables Pull-to-Refresh strictly at top)
                session.scrollDelegate = AzokleScrollDelegate(
                    swipeRefreshProvider = { swipeRefresh },
                    onScrollChanged = onScrollChanged
                )

                // 6. History Delegate (Visit tracking, redirect filtering, and back/forward stack sync)
                session.historyDelegate = AzokleHistoryDelegate(
                    tabId = tabId,
                    isPrivate = isPrivate,
                    isTor = isTor,
                    onCanGoBackChanged = onCanGoBackChanged,
                    onCanGoForwardChanged = onCanGoForwardChanged
                )

                // 7. Prompt Delegate (JavaScript alerts, confirms, prompts, HTTP Auth, Popups)
                session.promptDelegate = GeckoPromptDelegateImpl(
                    context = context,
                    hasRecentUserInteraction = { this@apply.hasRecentUserInteraction() },
                    onPopupRequested = { targetUri ->
                        val scheme = UrlHelper.extractScheme(targetUri)
                        val dangerousSchemes = setOf("file", "content", "chrome", "resource", "javascript", "data")
                        if (scheme == null || scheme !in dangerousSchemes) {
                            onNewWindowRequested(targetUri)
                        }
                    }
                )

                // 8. Selection Action Delegate (Floating Context Menu: Copy, Cut, Paste, Select All, Share, Web Search)
                session.selectionActionDelegate = AzokleSelectionDelegate(onTextSelectionChanged)

                // 9. Permission Delegate (Two-Tier Brave-Grade Origin Security & Hardware Access Engine)
                session.permissionDelegate = AzoklePermissionDelegate(
                    context = context,
                    isPrivate = isPrivate,
                    isTor = isTor,
                    onPermissionPromptRequested = onPermissionPromptRequested
                )

                // Attach session to GeckoView & open runtime
                setSession(session)
                if (!session.isOpen) {
                    session.open(runtime)
                }

                isFocusable = true
                isFocusableInTouchMode = true
                try {
                    session.setFocused(true)
                } catch (e: Exception) {}

                swipeRefresh.targetNestedGeckoView = this

                onSessionReady(session, this)
            }

            swipeRefresh.addView(geckoView)
            swipeRefresh
        },
        update = { swipeRefresh ->
            val currentBgColor = if (isFullScreenMode) {
                android.graphics.Color.BLACK
            } else if (isDarkMode) {
                android.graphics.Color.parseColor("#121212")
            } else {
                android.graphics.Color.parseColor("#FFFFFF")
            }
            swipeRefresh.setBackgroundColor(currentBgColor)
            swipeRefresh.targetNestedGeckoView?.setBackgroundColor(currentBgColor)
            swipeRefresh.currentUrl = url
            swipeRefresh.isFullScreen = isFullScreenMode
            (session.selectionActionDelegate as? AzokleSelectionDelegate)?.onSelectionAction = onTextSelectionChanged
        },
        onRelease = { swipeRefresh ->
            try {
                swipeRefresh.targetNestedGeckoView = null
                for (i in 0 until swipeRefresh.childCount) {
                    val child = swipeRefresh.getChildAt(i)
                    if (child is org.mozilla.geckoview.GeckoView) {
                        child.visibility = android.view.View.INVISIBLE
                        child.alpha = 0f
                        child.releaseSession()
                    }
                }
                if (tabId == null) {
                    try {
                        session.close()
                    } catch (e: Exception) {}
                }
            } catch (e: Exception) {
                // Ignore transient release errors safely
            }
        },
        modifier = Modifier.fillMaxSize()
    )

    if (isCrashed) {
        BrowserCrashRecoverySurface(
            isDarkMode = isDarkMode,
            isOomKill = isOomKill,
            onRestoreTab = {
                isCrashed = false
                isOomKill = false
                try {
                    if (session.isOpen) {
                        session.reload(GeckoSession.LOAD_FLAGS_NONE)
                    } else {
                        session.open(runtime)
                        session.loadUri(url)
                    }
                } catch (e: Exception) {
                    // Safe reload fallback
                }
            },
            onCloseTab = {
                if (tabId != null) {
                    TabManager.closeTab(tabId)
                }
            }
        )
    }
    }
}

