package com.azokle.search.browser.pwa

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import com.azokle.search.AzokleApplication
import com.azokle.search.MainActivity
import com.azokle.search.browser.gecko.GeckoEngineManager
import com.azokle.search.browser.permissions.SitePermissionRequest
import com.azokle.search.browser.permissions.SitePermissionState
import com.azokle.search.browser.permissions.SitePermissionType
import com.azokle.search.browser.permissions.SitePermissionsManager
import com.azokle.search.browser.ui.SitePermissionPromptSheet
import com.azokle.search.data.PreferencesManager
import com.azokle.search.ui.theme.AzokleSearchTheme
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView

class PwaActivity : FragmentActivity() {

    private var geckoSession: GeckoSession? = null
    private var pwaId: String = ""
    private var startUrl: String = ""
    private var pwaScope: String = ""
    private var pwaName: String = "Web App"
    private var displayMode: DisplayMode = DisplayMode.STANDALONE
    private var themeColor: Int? = null
    private var backgroundColor: Int? = null
    private var cachedIconBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Parse incoming intent extras
        pwaId = intent.getStringExtra(PwaShortcutManager.EXTRA_PWA_ID) ?: ""
        startUrl = intent.getStringExtra(PwaShortcutManager.EXTRA_PWA_START_URL)
            ?: intent.dataString
            ?: "https://azokle.com"
        pwaScope = intent.getStringExtra(PwaShortcutManager.EXTRA_PWA_SCOPE) ?: startUrl
        pwaName = intent.getStringExtra(PwaShortcutManager.EXTRA_PWA_NAME) ?: "Web App"
        displayMode = DisplayMode.fromString(intent.getStringExtra(PwaShortcutManager.EXTRA_PWA_DISPLAY_MODE))

        if (intent.hasExtra(PwaShortcutManager.EXTRA_PWA_THEME_COLOR)) {
            themeColor = intent.getIntExtra(PwaShortcutManager.EXTRA_PWA_THEME_COLOR, AndroidColor.BLACK)
        }
        if (intent.hasExtra(PwaShortcutManager.EXTRA_PWA_BG_COLOR)) {
            backgroundColor = intent.getIntExtra(PwaShortcutManager.EXTRA_PWA_BG_COLOR, AndroidColor.BLACK)
        }

        // Try loading registered icon from PwaStorageManager
        val storedPwa = PwaStorageManager.getInstance(this).getPwaById(pwaId)
            ?: PwaStorageManager.getInstance(this).findPwaForUrl(startUrl)
        if (storedPwa != null) {
            cachedIconBitmap = storedPwa.getIconBitmap()
            if (themeColor == null) themeColor = storedPwa.themeColor
            if (backgroundColor == null) backgroundColor = storedPwa.backgroundColor
        }

        // Configure system bars & window
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (themeColor != null) {
            @Suppress("DEPRECATION")
            window.statusBarColor = themeColor!!
            @Suppress("DEPRECATION")
            window.navigationBarColor = themeColor!!
        }

        val prefs = PreferencesManager(this)
        val appTheme = prefs.getThemeMode()
        AzokleApplication.applyThemeMode(appTheme)

        setContent {
            AzokleSearchTheme(themeMode = appTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = backgroundColor?.let { Color(it) } ?: MaterialTheme.colorScheme.background
                ) {
                    PwaContainerScreen(
                        pwaName = pwaName,
                        startUrl = startUrl,
                        scope = pwaScope,
                        displayMode = displayMode,
                        themeColor = themeColor,
                        backgroundColor = backgroundColor,
                        iconBitmap = cachedIconBitmap,
                        onClose = { finish() },
                        onOpenInBrowser = { url ->
                            val browserIntent = Intent(this@PwaActivity, MainActivity::class.java).apply {
                                action = Intent.ACTION_VIEW
                                data = Uri.parse(url)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            startActivity(browserIntent)
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            geckoSession?.close()
            geckoSession = null
        } catch (e: Exception) {
            // Safe ignore
        }
    }
}

@Composable
fun PwaContainerScreen(
    pwaName: String,
    startUrl: String,
    scope: String,
    displayMode: DisplayMode,
    themeColor: Int?,
    backgroundColor: Int?,
    iconBitmap: Bitmap?,
    onClose: () -> Unit,
    onOpenInBrowser: (String) -> Unit
) {
    val context = LocalContext.current
    val runtime = remember { GeckoEngineManager.getRuntime(context) }
    val session = remember { GeckoEngineManager.createSession(context, isPrivate = false) }

    DisposableEffect(session) {
        onDispose {
            try {
                session.close()
            } catch (e: Exception) {}
        }
    }

    var currentUrl by remember { mutableStateOf(startUrl) }
    var currentTitle by remember { mutableStateOf(pwaName) }
    var progress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(true) }
    var isSplashVisible by remember { mutableStateOf(true) }
    var canGoBack by remember { mutableStateOf(false) }
    var isOutOfScope by remember { mutableStateOf(false) }

    var activePermissionRequest by remember { mutableStateOf<SitePermissionRequest?>(null) }
    val permissionsManager = remember { SitePermissionsManager.getInstance(context) }

    fun handleAllowPermission(req: SitePermissionRequest, rememberChoice: Boolean) {
        if (rememberChoice) {
            req.requestedTypes.forEach { type ->
                permissionsManager.setPermission(req.origin, type, SitePermissionState.ALLOW, isPrivate = req.isPrivate)
            }
        }
        val video = req.videoSources?.firstOrNull()
        val audio = req.audioSources?.firstOrNull()
        req.grantMedia(video, audio)
        req.grantContent(GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW)
        activePermissionRequest = null
    }

    fun handleDenyPermission(req: SitePermissionRequest, rememberChoice: Boolean) {
        if (rememberChoice) {
            req.requestedTypes.forEach { type ->
                permissionsManager.setPermission(req.origin, type, SitePermissionState.BLOCK, isPrivate = req.isPrivate)
            }
        }
        req.reject(GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY)
        activePermissionRequest = null
    }

    BackHandler {
        if (session.isOpen && canGoBack) {
            session.goBack()
        } else {
            onClose()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor?.let { Color(it) } ?: Color(0xFF09090B))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Out of Scope Warning Bar (Scope Guard)
            AnimatedVisibility(
                visible = isOutOfScope,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E1B4B))
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                                .clickable {
                                    if (session.isOpen) session.loadUri(startUrl)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Return to App",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "External Link: ${Uri.parse(currentUrl).host ?: currentUrl}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Tap back to return to $pwaName",
                                fontSize = 10.sp,
                                color = Color(0xFFA5B4FC)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable { onOpenInBrowser(currentUrl) }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = "Open in Browser",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Minimal Progress Indicator during page navigation
            if (isLoading && !isSplashVisible) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp),
                    color = themeColor?.let { Color(it) } ?: Color(0xFF38BDF8),
                    trackColor = Color.Transparent
                )
            }

            // GeckoView Quantum Engine Renderer
            AndroidView(
                factory = { ctx ->
                    GeckoView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        session.navigationDelegate = object : GeckoSession.NavigationDelegate {
                            override fun onLocationChange(
                                session: GeckoSession,
                                url: String?,
                                perms: MutableList<GeckoSession.PermissionDelegate.ContentPermission>,
                                hasUserGesture: Boolean
                            ) {
                                url?.let {
                                    if (it.isNotBlank() && !it.equals("about:blank", ignoreCase = true)) {
                                        currentUrl = it
                                        isOutOfScope = !PwaInstallabilityValidator.isUrlInScope(it, scope)
                                    }
                                }
                            }

                            override fun onCanGoBack(session: GeckoSession, canGoBackVal: Boolean) {
                                canGoBack = canGoBackVal
                            }

                            override fun onLoadRequest(
                                session: GeckoSession,
                                request: GeckoSession.NavigationDelegate.LoadRequest
                            ): GeckoResult<org.mozilla.geckoview.AllowOrDeny>? {
                                val uriStr = request.uri
                                val scheme = try { Uri.parse(uriStr).scheme?.lowercase() ?: "" } catch (e: Exception) { "" }
                                if (scheme in setOf("file", "content", "chrome", "resource")) {
                                    return GeckoResult.fromValue(org.mozilla.geckoview.AllowOrDeny.DENY)
                                }
                                if (scheme == "http" || scheme == "https" || scheme == "about" || scheme == "blob" || scheme == "data" || scheme == "javascript") {
                                    return GeckoResult.fromValue(org.mozilla.geckoview.AllowOrDeny.ALLOW)
                                }
                                if (com.azokle.search.browser.handlers.ExternalSchemeHandler.isExternalScheme(uriStr)) {
                                    if (!request.hasUserGesture && request.target != GeckoSession.NavigationDelegate.TARGET_WINDOW_NONE) {
                                        return GeckoResult.fromValue(org.mozilla.geckoview.AllowOrDeny.DENY)
                                    }
                                    com.azokle.search.browser.handlers.ExternalSchemeHandler.handleExternalUri(ctx, uriStr, session)
                                    return GeckoResult.fromValue(org.mozilla.geckoview.AllowOrDeny.DENY)
                                }
                                return GeckoResult.fromValue(org.mozilla.geckoview.AllowOrDeny.DENY)
                            }
                        }

                        session.progressDelegate = object : GeckoSession.ProgressDelegate {
                            override fun onPageStart(session: GeckoSession, url: String) {
                                isLoading = true
                                progress = 0.1f
                            }

                            override fun onPageStop(session: GeckoSession, success: Boolean) {
                                isLoading = false
                                progress = 1.0f
                                isSplashVisible = false
                            }

                            override fun onProgressChange(session: GeckoSession, progressInt: Int) {
                                progress = progressInt / 100f
                                if (progressInt > 70) {
                                    isSplashVisible = false
                                }
                            }
                        }

                        session.contentDelegate = object : GeckoSession.ContentDelegate {
                            override fun onTitleChange(session: GeckoSession, title: String?) {
                                if (!title.isNullOrBlank()) {
                                    currentTitle = title
                                }
                            }
                        }

                        session.permissionDelegate = object : GeckoSession.PermissionDelegate {
                            override fun onContentPermissionRequest(
                                session: GeckoSession,
                                permRequest: GeckoSession.PermissionDelegate.ContentPermission
                            ): GeckoResult<Int>? {
                                val uriStr = permRequest.uri
                                val origin = permissionsManager.canonicalizeOrigin(uriStr)
                                val host = Uri.parse(uriStr).host ?: origin

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

                                if (mappedType == null) {
                                    return GeckoResult.fromValue(GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY)
                                }

                                val siteState = permissionsManager.getPermission(origin, mappedType, isPrivate = false)
                                if (siteState == SitePermissionState.BLOCK) {
                                    return GeckoResult.fromValue(GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY)
                                }
                                if (siteState == SitePermissionState.ALLOW) {
                                    return GeckoResult.fromValue(GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW)
                                }

                                val geckoResult = GeckoResult<Int>()
                                android.os.Handler(android.os.Looper.getMainLooper()).post {
                                    val requestPayload = SitePermissionRequest(
                                        host = host,
                                        origin = origin,
                                        uri = uriStr,
                                        isPrivate = false,
                                        requestedTypes = listOf(mappedType),
                                        contentCallback = { grantResult ->
                                            geckoResult.complete(grantResult)
                                        }
                                    )
                                    activePermissionRequest = requestPayload
                                }
                                return geckoResult
                            }
                        }

                        session.promptDelegate = com.azokle.search.browser.gecko.GeckoPromptDelegateImpl(
                            context = ctx,
                            onPopupRequested = { targetUri ->
                                val scheme = com.azokle.search.util.UrlHelper.extractScheme(targetUri)
                                val dangerousSchemes = setOf("file", "content", "chrome", "resource", "javascript", "data")
                                if (scheme == null || scheme !in dangerousSchemes) {
                                    onOpenInBrowser(targetUri)
                                }
                            }
                        )

                        setSession(session)
                        if (!session.isOpen) {
                            session.open(runtime)
                        }
                        session.loadUri(startUrl)
                    }
                },
                onRelease = { view ->
                    try {
                        view.releaseSession()
                    } catch (e: Exception) {}
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }

        // Custom High-End Branded Animated Splash Screen
        AnimatedVisibility(
            visible = isSplashVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor?.let { Color(it) } ?: Color(0xFF09090B)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (iconBitmap != null) {
                        Image(
                            bitmap = iconBitmap.asImageBitmap(),
                            contentDescription = pwaName,
                            modifier = Modifier
                                .size(84.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(84.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(themeColor?.let { Color(it) } ?: Color(0xFF1E293B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = pwaName.take(1).uppercase(),
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = pwaName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp,
                        color = themeColor?.let { Color(it) } ?: Color(0xFF38BDF8)
                    )
                }
            }
        }

        // Permission Prompt Modal in PWA Standalone Mode
        activePermissionRequest?.let { req ->
            SitePermissionPromptSheet(
                request = req,
                onAllowAlways = { handleAllowPermission(req, rememberChoice = true) },
                onAllowOnce = { handleAllowPermission(req, rememberChoice = false) },
                onDeny = { rememberChoice -> handleDenyPermission(req, rememberChoice = rememberChoice) },
                onDismiss = { handleDenyPermission(req, rememberChoice = false) }
            )
        }
    }
}
