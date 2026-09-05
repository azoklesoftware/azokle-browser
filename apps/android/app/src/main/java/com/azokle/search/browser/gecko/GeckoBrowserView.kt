package com.azokle.search.browser.gecko

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
import com.azokle.search.browser.permissions.SitePermissionRequest
import com.azokle.search.browser.permissions.SitePermissionState
import com.azokle.search.browser.permissions.SitePermissionType
import com.azokle.search.browser.permissions.SitePermissionsManager
import com.azokle.search.browser.media.GeckoMediaSessionBridge
import com.azokle.search.browser.media.MediaPlaybackManager
import com.azokle.search.browser.tabs.TabManager
import com.azokle.search.browser.ui.BrowserCrashRecoverySurface
import com.azokle.search.browser.ui.BrowserErrorType
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
    val headers: Map<String, String>? = null
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
    onLinkLongPressed: (com.azokle.search.browser.ui.LinkContextTarget) -> Unit = {},
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
            val nativeBgColor = if (isDarkMode) android.graphics.Color.parseColor("#121212") else android.graphics.Color.parseColor("#FFFFFF")
            val swipeRefresh = GeckoSwipeRefreshLayout(ctx).apply {
                currentUrl = url
                isFullScreen = isFullScreenMode
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
                // Sovereign Anti-Flash: start invisible so no stale frame buffer is ever painted
                visibility = android.view.View.INVISIBLE
                alpha = 0f
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                // Initialize native media subsystem & bridge GeckoView MediaSession
                MediaPlaybackManager.initialize(ctx)
                try {
                    session.settings.suspendMediaWhenInactive = false
                } catch (e: Exception) {}
                val tabSessionId = session.hashCode().toString()
                session.mediaSessionDelegate = GeckoMediaSessionBridge(
                    tabId = tabSessionId,
                    isPrivate = isPrivate,
                    isTor = isTor
                )

                // 1. Navigation Delegate & Error Interceptor
                session.navigationDelegate = object : GeckoSession.NavigationDelegate {
                    override fun onLocationChange(
                        session: GeckoSession,
                        url: String?,
                        perms: MutableList<GeckoSession.PermissionDelegate.ContentPermission>,
                        hasUserGesture: Boolean
                    ) {
                        url?.let {
                            if (it.isNotBlank() && !it.equals("about:blank", ignoreCase = true)) {
                                lastLoadedUrl = it
                                if (tabId != null) {
                                    TabManager.markTabLoaded(tabId, it)
                                }
                                onUrlChanged(it)
                                TabManager.updateActiveTab(it, "")
                                onLoadErrorChanged(null)
                            }
                        }
                    }

                    override fun onLoadRequest(
                        session: GeckoSession,
                        request: GeckoSession.NavigationDelegate.LoadRequest
                    ): org.mozilla.geckoview.GeckoResult<org.mozilla.geckoview.AllowOrDeny>? {
                        val uriStr = request.uri
                        val scheme = try { android.net.Uri.parse(uriStr).scheme?.lowercase() ?: "" } catch (e: Exception) { "" }

                        // 1. Block dangerous internal and privileged schemes (allow javascript for internal scripts)
                        if (scheme in setOf("file", "content", "chrome", "resource")) {
                            return org.mozilla.geckoview.GeckoResult.fromValue(org.mozilla.geckoview.AllowOrDeny.DENY)
                        }

                        // 2. Standard web browsing, data/blob, and javascript schemes
                        if (scheme == "http" || scheme == "https" || scheme == "about" || scheme == "blob" || scheme == "data" || scheme == "javascript") {
                            return org.mozilla.geckoview.GeckoResult.fromValue(org.mozilla.geckoview.AllowOrDeny.ALLOW)
                        }

                        // 3. Whitelisted external application schemes (tel, mailto, sms, geo, market, intent)
                        val safeExternalSchemes = setOf("tel", "mailto", "sms", "smsto", "mms", "mmsto", "geo", "market", "intent")
                        if (scheme in safeExternalSchemes) {
                            try {
                                if (scheme == "intent") {
                                    val intent = android.content.Intent.parseUri(uriStr, android.content.Intent.URI_INTENT_SCHEME).apply {
                                        addCategory(android.content.Intent.CATEGORY_BROWSABLE)
                                        component = null
                                        selector = null
                                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: android.content.ActivityNotFoundException) {
                                        if (!fallbackUrl.isNullOrBlank() && (fallbackUrl.startsWith("http://", ignoreCase = true) || fallbackUrl.startsWith("https://", ignoreCase = true))) {
                                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                                session.loadUri(fallbackUrl)
                                            }
                                        }
                                    }
                                } else {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(uriStr)).apply {
                                        addCategory(android.content.Intent.CATEGORY_BROWSABLE)
                                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                }
                            } catch (e: Exception) {
                                // Ignore unhandled intent schemes safely
                            }
                            return org.mozilla.geckoview.GeckoResult.fromValue(org.mozilla.geckoview.AllowOrDeny.DENY)
                        }

                        return org.mozilla.geckoview.GeckoResult.fromValue(org.mozilla.geckoview.AllowOrDeny.DENY)
                    }

                    override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
                        onCanGoBackChanged(canGoBack)
                    }

                    override fun onCanGoForward(session: GeckoSession, canGoForward: Boolean) {
                        onCanGoForwardChanged(canGoForward)
                    }

                    override fun onNewSession(session: GeckoSession, uri: String): GeckoResult<GeckoSession>? {
                        if (uri.isNotBlank()) {
                            val scheme = com.azokle.search.util.UrlHelper.extractScheme(uri)
                            val dangerousSchemes = setOf("file", "content", "chrome", "resource", "javascript", "data")
                            if (scheme == null || scheme !in dangerousSchemes) {
                                onNewWindowRequested(uri)
                            }
                        }
                        return null
                    }

                    override fun onLoadError(
                        session: GeckoSession,
                        uri: String?,
                        error: org.mozilla.geckoview.WebRequestError
                    ): GeckoResult<String>? {
                        val failedUri = uri ?: ""
                        val errorType = when (error.code) {
                            org.mozilla.geckoview.WebRequestError.ERROR_UNKNOWN_HOST -> BrowserErrorType.dnsNotFound(failedUri)
                            org.mozilla.geckoview.WebRequestError.ERROR_CONNECTION_REFUSED -> BrowserErrorType.connectionRefused(failedUri)
                            org.mozilla.geckoview.WebRequestError.ERROR_NET_TIMEOUT -> BrowserErrorType.timeout(failedUri)
                            org.mozilla.geckoview.WebRequestError.ERROR_OFFLINE -> BrowserErrorType.noInternet(failedUri)
                            org.mozilla.geckoview.WebRequestError.ERROR_REDIRECT_LOOP -> BrowserErrorType.redirectLoop(failedUri)
                            org.mozilla.geckoview.WebRequestError.ERROR_PORT_BLOCKED -> BrowserErrorType.portBlocked(failedUri)
                            org.mozilla.geckoview.WebRequestError.ERROR_SECURITY_SSL,
                            org.mozilla.geckoview.WebRequestError.ERROR_SECURITY_BAD_CERT -> BrowserErrorType.sslWarning(failedUri)
                            else -> {
                                if (error.category == org.mozilla.geckoview.WebRequestError.ERROR_CATEGORY_SECURITY) {
                                    BrowserErrorType.sslWarning(failedUri)
                                } else if (error.code == org.mozilla.geckoview.WebRequestError.ERROR_UNKNOWN_HOST) {
                                    BrowserErrorType.dnsNotFound(failedUri)
                                } else {
                                    BrowserErrorType.generic(failedUri, "ERR_CODE_${error.code}")
                                }
                            }
                        }
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            swipeRefresh.isRefreshing = false
                        }
                        onLoadErrorChanged(errorType)
                        return null
                    }
                }

                // 2. Content Blocking & Shield Delegate
                session.contentBlockingDelegate = object : org.mozilla.geckoview.ContentBlocking.Delegate {
                    override fun onContentBlocked(
                        session: GeckoSession,
                        event: org.mozilla.geckoview.ContentBlocking.BlockEvent
                    ) {
                        try {
                            val uriStr = event.uri
                            if (!uriStr.isNullOrBlank()) {
                                val host = try { android.net.Uri.parse(uriStr).host ?: uriStr } catch (e: Exception) { uriStr }
                                com.azokle.search.browser.adblock.AdBlockStats.recordBlock(host)
                            }
                        } catch (e: Exception) {
                            // Fallback
                        }
                    }
                }

                // 3. Progress & Security Delegate
                session.progressDelegate = object : GeckoSession.ProgressDelegate {
                    override fun onPageStart(session: GeckoSession, url: String) {
                        swipeRefresh.isPageLoading = true
                        swipeRefresh.currentUrl = url
                        swipeRefresh.currentScrollY = 0
                        swipeRefresh.isContentAtTop = true
                        swipeRefresh.resetGestureState()
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            isCrashed = false
                            isOomKill = false
                            this@apply.visibility = android.view.View.INVISIBLE
                            this@apply.alpha = 0f
                        }
                        onLoadingChanged(true)
                        onProgressChanged(10)
                        onLoadErrorChanged(null)
                        val host = try { android.net.Uri.parse(url).host ?: "" } catch (e: Exception) { "" }
                        val isShield = com.azokle.search.browser.adblock.AdBlockEngine.instance.isShieldEnabled(host)
                        com.azokle.search.browser.adblock.AdBlockStats.resetForPage(
                            url = url,
                            host = host,
                            isEnabled = isShield
                        )
                        if (!isPrivate && !isTor) {
                            com.azokle.search.browser.pwa.PwaManager.onPageStarted(tabId ?: "active", url)
                        }
                    }

                    override fun onPageStop(session: GeckoSession, success: Boolean) {
                        swipeRefresh.isPageLoading = false
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            if (this@apply.visibility != android.view.View.VISIBLE) {
                                this@apply.visibility = android.view.View.VISIBLE
                                this@apply.alpha = 1f
                            }
                            swipeRefresh.isRefreshing = false
                        }
                        onLoadingChanged(false)
                        onProgressChanged(100)

                        if (success && !isPrivate && !isTor) {
                            val activeUrl = url.takeIf { it.isNotBlank() && (it.startsWith("http://", ignoreCase = true) || it.startsWith("https://", ignoreCase = true)) }
                            if (activeUrl != null) {
                                com.azokle.search.browser.history.HistoryManager.recordVisit(
                                    url = activeUrl,
                                    isPrivate = false
                                )
                            }
                        }

                        // Capture Lightweight Screenshot Thumbnail for Active Tabs Grid (Strictly Non-Private/Non-Tor Only)
                        if (!isPrivate && !isTor) {
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                try {
                                    val view = this@apply
                                    val activity = view.context as? android.app.Activity
                                    if (activity != null && (activity.isFinishing || activity.isDestroyed)) return@postDelayed
                                    if (view.width > 0 && view.height > 0) {
                                        val targetW = 320
                                        val targetH = (320f * view.height / view.width).toInt().coerceIn(180, 480)
                                        val bitmap = android.graphics.Bitmap.createBitmap(
                                            targetW,
                                            targetH,
                                            android.graphics.Bitmap.Config.ARGB_8888
                                        )

                                        var surfaceView: android.view.SurfaceView? = null
                                        var textureView: android.view.TextureView? = null
                                        fun findViews(parent: android.view.ViewGroup) {
                                            for (i in 0 until parent.childCount) {
                                                val child = parent.getChildAt(i)
                                                if (child is android.view.SurfaceView) {
                                                    surfaceView = child
                                                } else if (child is android.view.TextureView) {
                                                    textureView = child
                                                } else if (child is android.view.ViewGroup) {
                                                    findViews(child)
                                                }
                                            }
                                        }
                                        val vg = view as? android.view.ViewGroup
                                        if (vg != null) findViews(vg)

                                        var copyDispatched = false
                                        if (textureView != null) {
                                            try {
                                                val texBmp = textureView?.getBitmap(targetW, targetH)
                                                if (texBmp != null) {
                                                    TabManager.updateActiveTabThumbnail(texBmp)
                                                    copyDispatched = true
                                                }
                                            } catch (e: Exception) {}
                                        }
                                        if (!copyDispatched && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && surfaceView != null && surfaceView?.holder?.surface?.isValid == true) {
                                            try {
                                                android.view.PixelCopy.request(
                                                    surfaceView!!,
                                                    bitmap,
                                                    { copyResult ->
                                                        if (copyResult == android.view.PixelCopy.SUCCESS) {
                                                            TabManager.updateActiveTabThumbnail(bitmap)
                                                        } else {
                                                            bitmap.recycle()
                                                        }
                                                    },
                                                    android.os.Handler(android.os.Looper.getMainLooper())
                                                )
                                                copyDispatched = true
                                            } catch (e: Exception) {
                                                copyDispatched = false
                                            }
                                        }
                                        if (!copyDispatched) {
                                            val canvas = android.graphics.Canvas(bitmap)
                                            canvas.scale(targetW.toFloat() / view.width, targetH.toFloat() / view.height)
                                            view.draw(canvas)
                                            TabManager.updateActiveTabThumbnail(bitmap)
                                        }
                                    }
                                } catch (e: Exception) {
                                    // Ignore
                                }
                            }, 450)
                        }
                    }

                    override fun onProgressChange(session: GeckoSession, progress: Int) {
                        if (progress >= 25) {
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                if (this@apply.visibility != android.view.View.VISIBLE) {
                                    this@apply.visibility = android.view.View.VISIBLE
                                    this@apply.animate().alpha(1f).setDuration(120).start()
                                }
                            }
                        }
                        if (progress >= 100) {
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                swipeRefresh.isRefreshing = false
                            }
                        }
                        onProgressChanged(progress)
                    }

                    override fun onSecurityChange(
                        session: GeckoSession,
                        securityInfo: SecurityInformation
                    ) {
                        onSecurityChanged(securityInfo)
                    }
                }

                // 4. Content Delegate (Title & Link Context Menu)
                session.contentDelegate = object : GeckoSession.ContentDelegate {
                    override fun onTitleChange(session: GeckoSession, title: String?) {
                        title?.let {
                            onTitleChanged(it)
                            TabManager.updateActiveTab("", it)
                            if (!isPrivate && !isTor && url.isNotBlank()) {
                                com.azokle.search.browser.history.HistoryManager.updateTitle(
                                    url = url,
                                    title = it
                                )
                                com.azokle.search.browser.pwa.PwaManager.onTitleChanged(tabId ?: "active", it)
                            }
                        }
                    }

                    override fun onFullScreen(session: GeckoSession, fullScreen: Boolean) {
                        swipeRefresh.isFullScreen = fullScreen
                        MediaPlaybackManager.updateFullscreenState(
                            tabId = session.hashCode().toString(),
                            isFullscreen = fullScreen,
                            videoAspectRatio = null
                        )
                        onFullScreenChanged(fullScreen)
                    }

                    override fun onContextMenu(
                        session: GeckoSession,
                        screenX: Int,
                        screenY: Int,
                        element: GeckoSession.ContentDelegate.ContextElement
                    ) {
                        val baseUri = element.baseUri
                        val rawLink = element.linkUri
                        val srcUri = element.srcUri
                        val title = element.title?.takeIf { it.isNotBlank() }
                            ?: element.linkText?.takeIf { it.isNotBlank() }
                            ?: element.altText?.takeIf { it.isNotBlank() }
                            ?: ""

                        val effectiveBaseUri = if (!baseUri.isNullOrBlank()) baseUri else url
                        val resolvedLink = if (!rawLink.isNullOrBlank() && !rawLink.startsWith("http://") && !rawLink.startsWith("https://") && !effectiveBaseUri.isNullOrBlank()) {
                            try {
                                java.net.URI(effectiveBaseUri).resolve(rawLink).toString()
                            } catch (e: Exception) {
                                rawLink
                            }
                        } else {
                            rawLink
                        }

                        if (!resolvedLink.isNullOrBlank() || !srcUri.isNullOrBlank()) {
                            val target = com.azokle.search.browser.ui.LinkContextTarget(
                                url = resolvedLink ?: "",
                                title = title,
                                isImage = !srcUri.isNullOrBlank(),
                                imageSrc = srcUri
                            )
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                try {
                                    this@apply.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                                } catch (e: Exception) {}
                                onLinkLongPressed(target)
                            }
                        }
                    }

                    override fun onExternalResponse(
                        session: GeckoSession,
                        response: org.mozilla.geckoview.WebResponse
                    ) {
                        val downloadUrl = response.uri
                        val contentType = response.headers["Content-Type"] ?: response.headers["content-type"]
                        val contentDisposition = response.headers["Content-Disposition"] ?: response.headers["content-disposition"]
                        val filename = com.azokle.search.browser.downloads.DownloadManager.extractFilename(
                            urlStr = downloadUrl,
                            contentDisposition = contentDisposition,
                            mimeType = contentType
                        )
                        val mimeType = contentType ?: "application/octet-stream"
                        val contentLength = (response.headers["Content-Length"] ?: response.headers["content-length"])?.toLongOrNull() ?: 0L

                        val payload = DownloadConfirmationPayload(
                            url = downloadUrl,
                            filename = filename,
                            mimeType = mimeType,
                            contentLength = contentLength,
                            headers = response.headers
                        )

                        if (onDownloadConfirmationRequested != null) {
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                onDownloadConfirmationRequested(payload)
                            }
                        } else {
                            com.azokle.search.browser.downloads.DownloadManager.startDownload(
                                context = context,
                                url = downloadUrl,
                                filename = filename,
                                mimeType = mimeType,
                                contentLength = contentLength
                            )
                            android.widget.Toast.makeText(context, "📥 Downloading $filename...", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }

                    private var crashCount = 0
                    private var lastCrashTime = 0L

                    override fun onCrash(session: GeckoSession) {
                        android.util.Log.w("GeckoBrowserView", "Gecko content process crashed.")
                        val now = System.currentTimeMillis()
                        if (now - lastCrashTime < 10000L) {
                            crashCount++
                        } else {
                            crashCount = 1
                        }
                        lastCrashTime = now

                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            swipeRefresh.isRefreshing = false
                            if (crashCount > 1) {
                                isCrashed = true
                                isOomKill = false
                            } else {
                                try {
                                    if (session.isOpen) {
                                        session.reload(GeckoSession.LOAD_FLAGS_NONE)
                                    } else {
                                        isCrashed = true
                                        isOomKill = false
                                    }
                                } catch (e: Exception) {
                                    isCrashed = true
                                    isOomKill = false
                                }
                            }
                        }
                    }

                    override fun onKill(session: GeckoSession) {
                        android.util.Log.w("GeckoBrowserView", "Gecko content process killed by OS due to memory reclamation.")
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            swipeRefresh.isRefreshing = false
                            isCrashed = true
                            isOomKill = true
                        }
                    }

                    override fun onSlowScript(
                        session: GeckoSession,
                        scriptFileName: String
                    ): GeckoResult<SlowScriptResponse>? {
                        android.util.Log.w("GeckoBrowserView", "Slow/hanging script detected: $scriptFileName, interrupting execution to prevent ANR")
                        return GeckoResult.fromValue(SlowScriptResponse.STOP)
                    }

                    override fun onCloseRequest(session: GeckoSession) {
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            if (tabId != null) {
                                TabManager.closeTab(tabId)
                            }
                        }
                    }

                    override fun onWebAppManifest(session: GeckoSession, manifest: org.json.JSONObject) {
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            try {
                                val activeTabId = tabId ?: "active"
                                val name = manifest.optString("name", "").ifBlank { manifest.optString("short_name", "") }
                                val startUrl = manifest.optString("start_url", "").takeIf { it.isNotBlank() }
                                val themeColorStr = manifest.optString("theme_color", "").takeIf { it.isNotBlank() }
                                val themeColor = themeColorStr?.let {
                                    try { android.graphics.Color.parseColor(it) } catch (e: Exception) { null }
                                }
                                com.azokle.search.browser.pwa.PwaManager.onManifestDiscovered(
                                    tabId = activeTabId,
                                    manifestUrl = startUrl,
                                    manifestJson = manifest.toString(),
                                    documentUrl = url,
                                    pageTitle = name,
                                    fallbackFaviconUrl = null,
                                    fallbackThemeColor = themeColor
                                )
                            } catch (e: Exception) {
                                // Safe fallback
                            }
                        }
                    }
                }

                // 5. Scroll Delegate (Hides bottom bar when scrolling down and enables Pull-to-Refresh strictly at top)
                session.scrollDelegate = object : GeckoSession.ScrollDelegate {
                    override fun onScrollChanged(session: GeckoSession, scrollX: Int, scrollY: Int) {
                        onScrollChanged(scrollX, scrollY)
                        swipeRefresh.currentScrollY = scrollY
                        swipeRefresh.isContentAtTop = scrollY <= 15
                    }
                }

                // 5.1 History Delegate (Brave/Firefox-grade visit tracking, redirect filtering, and back/forward stack sync)
                session.historyDelegate = object : GeckoSession.HistoryDelegate {
                    override fun onVisited(
                        session: GeckoSession,
                        url: String,
                        lastVisitedURL: String?,
                        flags: Int
                    ): org.mozilla.geckoview.GeckoResult<Boolean>? {
                        val isRedirect = (flags and org.mozilla.geckoview.GeckoSession.HistoryDelegate.VISIT_REDIRECT_PERMANENT) != 0 ||
                                (flags and org.mozilla.geckoview.GeckoSession.HistoryDelegate.VISIT_REDIRECT_TEMPORARY) != 0
                        if (!isRedirect && url.isNotBlank() && !url.startsWith("about:", ignoreCase = true)) {
                            com.azokle.search.browser.history.HistoryManager.recordVisit(
                                url = url,
                                title = "",
                                isPrivate = isPrivate || isTor
                            )
                        }
                        return org.mozilla.geckoview.GeckoResult.fromValue(true)
                    }

                    override fun onHistoryStateChange(
                        session: GeckoSession,
                        historyList: org.mozilla.geckoview.GeckoSession.HistoryDelegate.HistoryList
                    ) {
                        onCanGoBackChanged(historyList.currentIndex > 0)
                        onCanGoForwardChanged(historyList.currentIndex < historyList.size - 1)
                        if (tabId != null && historyList is org.mozilla.geckoview.GeckoSession.SessionState) {
                            com.azokle.search.browser.tabs.TabManager.updateTabSessionState(tabId, historyList)
                        }
                    }
                }

                // 6. Prompt Delegate (JavaScript alerts, confirms, prompts, HTTP Auth, Popups)
                session.promptDelegate = GeckoPromptDelegateImpl(context) { targetUri ->
                    val scheme = com.azokle.search.util.UrlHelper.extractScheme(targetUri)
                    val dangerousSchemes = setOf("file", "content", "chrome", "resource", "javascript", "data")
                    if (scheme == null || scheme !in dangerousSchemes) {
                        onNewWindowRequested(targetUri)
                    }
                }

                // 6.1 Selection Action Delegate (Native Floating ActionMode: Copy, Cut, Paste, Select All, Share, Web Search)
                var currentContext = context
                while (currentContext is android.content.ContextWrapper && currentContext !is android.app.Activity) {
                    currentContext = currentContext.baseContext
                }
                if (currentContext is android.app.Activity) {
                    session.selectionActionDelegate = AzokleSelectionDelegate(currentContext, onTextSelectionChanged)
                }

                // 7. Permission Delegate (Two-Tier Brave-Grade Origin Security & Hardware Access Engine)
                session.permissionDelegate = object : GeckoSession.PermissionDelegate {
                    override fun onContentPermissionRequest(
                        session: GeckoSession,
                        permRequest: GeckoSession.PermissionDelegate.ContentPermission
                    ): GeckoResult<Int>? {
                        val uriStr = permRequest.uri
                        val scheme = try { android.net.Uri.parse(uriStr).scheme?.lowercase() ?: "" } catch (e: Exception) { "" }
                        val host = try { android.net.Uri.parse(uriStr).host ?: uriStr } catch (e: Exception) { uriStr }

                        // Autoplay policy handling (allow inaudible & audible video playback unless explicitly blocked)
                        if (permRequest.permission == GeckoSession.PermissionDelegate.PERMISSION_AUTOPLAY_INAUDIBLE) {
                            return GeckoResult.fromValue(GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW)
                        }
                        if (permRequest.permission == GeckoSession.PermissionDelegate.PERMISSION_AUTOPLAY_AUDIBLE) {
                            val permManager = SitePermissionsManager.getInstance(context)
                            val origin = permManager.canonicalizeOrigin(uriStr)
                            val siteState = permManager.getPermission(origin, SitePermissionType.AUTOPLAY, isPrivate = isPrivate || isTor)
                            return if (siteState == SitePermissionState.BLOCK) {
                                GeckoResult.fromValue(GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY)
                            } else {
                                GeckoResult.fromValue(GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW)
                            }
                        }

                        // Deny all sensors & permissions on insecure HTTP origins (except localhost/testing)
                        if (scheme != "https" && !host.equals("localhost", true) && !host.equals("127.0.0.1", true)) {
                            return GeckoResult.fromValue(GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY)
                        }

                        val permManager = SitePermissionsManager.getInstance(context)
                        val origin = permManager.canonicalizeOrigin(uriStr)
                        val isTabPrivate = isPrivate || isTor

                        val mappedType = when (permRequest.permission) {
                            GeckoSession.PermissionDelegate.PERMISSION_GEOLOCATION -> SitePermissionType.GEOLOCATION
                            GeckoSession.PermissionDelegate.PERMISSION_DESKTOP_NOTIFICATION -> SitePermissionType.NOTIFICATIONS
                            GeckoSession.PermissionDelegate.PERMISSION_MEDIA_KEY_SYSTEM_ACCESS -> SitePermissionType.MEDIA_KEY_SYSTEM
                            GeckoSession.PermissionDelegate.PERMISSION_PERSISTENT_STORAGE -> SitePermissionType.PERSISTENT_STORAGE
                            GeckoSession.PermissionDelegate.PERMISSION_AUTOPLAY_AUDIBLE -> SitePermissionType.AUTOPLAY
                            else -> null
                        }

                        if (mappedType == SitePermissionType.MEDIA_KEY_SYSTEM) {
                            return GeckoResult.fromValue(GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW)
                        }

                        // Hard-deny persistent storage in Private / Tor mode
                        if (isTabPrivate && mappedType == SitePermissionType.PERSISTENT_STORAGE) {
                            return GeckoResult.fromValue(GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY)
                        }

                        // Sovereign Gatekeeper: Hard-deny unknown hardware / sensor APIs by default
                        if (mappedType == null) {
                            return GeckoResult.fromValue(GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY)
                        }

                        val siteState = permManager.getPermission(origin, mappedType, isPrivate = isTabPrivate)
                        if (siteState == SitePermissionState.BLOCK) {
                            return GeckoResult.fromValue(GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY)
                        }

                        val reqAndroidPerms = mappedType.requiredAndroidPermissions
                        val hasAllAndroidPerms = reqAndroidPerms.all {
                            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                        }

                        if (siteState == SitePermissionState.ALLOW && hasAllAndroidPerms) {
                            return GeckoResult.fromValue(GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW)
                        }

                        // Needs user prompt
                        val geckoResult = GeckoResult<Int>()
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            val requestPayload = SitePermissionRequest(
                                host = permManager.getDisplayHost(origin),
                                origin = origin,
                                uri = uriStr,
                                isPrivate = isTabPrivate,
                                requestedTypes = listOf(mappedType),
                                contentCallback = { grantResult ->
                                    geckoResult.complete(grantResult)
                                }
                            )
                            onPermissionPromptRequested(requestPayload)
                        }
                        return geckoResult
                    }

                    override fun onMediaPermissionRequest(
                        session: GeckoSession,
                        uri: String,
                        video: Array<out GeckoSession.PermissionDelegate.MediaSource>?,
                        audio: Array<out GeckoSession.PermissionDelegate.MediaSource>?,
                        callback: GeckoSession.PermissionDelegate.MediaCallback
                    ) {
                        val scheme = try { android.net.Uri.parse(uri).scheme?.lowercase() ?: "" } catch (e: Exception) { "" }
                        val host = try { android.net.Uri.parse(uri).host ?: uri } catch (e: Exception) { uri }

                        // Insecure origins are strictly denied media hardware access
                        if (scheme != "https" && !host.equals("localhost", true) && !host.equals("127.0.0.1", true)) {
                            callback.reject()
                            return
                        }

                        val permManager = SitePermissionsManager.getInstance(context)
                        val origin = permManager.canonicalizeOrigin(uri)
                        val isTabPrivate = isPrivate || isTor

                        val requestedTypes = mutableListOf<SitePermissionType>()
                        if (video != null && video.isNotEmpty()) requestedTypes.add(SitePermissionType.CAMERA)
                        if (audio != null && audio.isNotEmpty()) requestedTypes.add(SitePermissionType.MICROPHONE)

                        if (requestedTypes.isEmpty()) {
                            callback.reject()
                            return
                        }

                        // If any requested type is explicitly blocked for this site, reject immediately
                        val isAnyBlocked = requestedTypes.any { permManager.getPermission(origin, it, isPrivate = isTabPrivate) == SitePermissionState.BLOCK }
                        if (isAnyBlocked) {
                            callback.reject()
                            return
                        }

                        // Check if already allowed and OS permissions are granted
                        val isAllAllowed = requestedTypes.all { permManager.getPermission(origin, it, isPrivate = isTabPrivate) == SitePermissionState.ALLOW }
                        val hasAllAndroidPerms = requestedTypes.flatMap { it.requiredAndroidPermissions }.all {
                            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                        }

                        if (isAllAllowed && hasAllAndroidPerms) {
                            val selectedVideo = video?.firstOrNull()
                            val selectedAudio = audio?.firstOrNull()
                            callback.grant(selectedVideo, selectedAudio)
                            return
                        }

                        // Dispatch prompt to Compose UI
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            val requestPayload = SitePermissionRequest(
                                host = permManager.getDisplayHost(origin),
                                origin = origin,
                                uri = uri,
                                isPrivate = isTabPrivate,
                                requestedTypes = requestedTypes,
                                videoSources = video,
                                audioSources = audio,
                                mediaCallback = callback
                            )
                            onPermissionPromptRequested(requestPayload)
                        }
                    }
                }

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
            swipeRefresh.currentUrl = url
            swipeRefresh.isFullScreen = isFullScreenMode
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

