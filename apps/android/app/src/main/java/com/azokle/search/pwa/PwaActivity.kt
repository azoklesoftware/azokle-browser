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

package com.azokle.search.pwa

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.Lock
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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import com.azokle.search.AzokleApplication
import com.azokle.search.MainActivity
import com.azokle.search.adblock.AdBlockEngine
import com.azokle.search.common.theme.AzokleRadius
import com.azokle.search.common.theme.AzokleSearchTheme
import com.azokle.search.common.theme.AzokleTheme
import com.azokle.search.common.util.PreferencesManager
import com.azokle.search.engine.GeckoEngineManager
import com.azokle.search.engine.delegates.AzoklePermissionDelegate
import com.azokle.search.permissions.SitePermissionPromptSheet
import com.azokle.search.permissions.SitePermissionRequest
import com.azokle.search.permissions.SitePermissionState
import com.azokle.search.permissions.SitePermissionsManager
import org.json.JSONObject
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView

import android.app.Activity
import androidx.activity.result.contract.ActivityResultContracts
import com.azokle.search.engine.FilePromptHost

class PwaActivity : FragmentActivity(), FilePromptHost {

    private var pendingFilePrompt: Pair<GeckoSession.PromptDelegate.FilePrompt, GeckoResult<GeckoSession.PromptDelegate.PromptResponse>>? = null

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        val pending = pendingFilePrompt
        pendingFilePrompt = null
        if (pending == null) return@registerForActivityResult
        val (prompt, result) = pending

        if (res.resultCode == Activity.RESULT_OK) {
            val data = res.data
            val singleUri = data?.data
            val clipData = data?.clipData
            if (clipData != null && clipData.itemCount > 0) {
                val uris = Array(clipData.itemCount) { clipData.getItemAt(it).uri }
                result.complete(prompt.confirm(this, uris))
            } else if (singleUri != null) {
                result.complete(prompt.confirm(this, singleUri))
            } else {
                result.complete(prompt.dismiss())
            }
        } else {
            result.complete(prompt.dismiss())
        }
    }

    /**
     * Executes the launch file picker operation.
     */
    override fun launchFilePicker(
        prompt: GeckoSession.PromptDelegate.FilePrompt,
        result: GeckoResult<GeckoSession.PromptDelegate.PromptResponse>
    ) {
        pendingFilePrompt = Pair(prompt, result)
        try {
            val intent = if (prompt.type == GeckoSession.PromptDelegate.FilePrompt.Type.FOLDER) {
                Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            } else {
                Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    val types = prompt.mimeTypes?.filter { it.isNotBlank() }
                    if (!types.isNullOrEmpty()) {
                        type = types.first()
                        if (types.size > 1) {
                            putExtra(Intent.EXTRA_MIME_TYPES, types.toTypedArray())
                        }
                    } else {
                        type = "*/*"
                    }
                    if (prompt.type == GeckoSession.PromptDelegate.FilePrompt.Type.MULTIPLE) {
                        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                    }
                }
            }
            filePickerLauncher.launch(Intent.createChooser(intent, "Select File"))
        } catch (e: Exception) {
            pendingFilePrompt = null
            result.complete(prompt.dismiss())
        }
    }

    private var geckoSession: GeckoSession? = null
    private var pwaId: String = ""
    private var startUrl: String = ""
    private var pwaScope: String = ""
    private var pwaName: String = "Web App"
    private var displayMode: DisplayMode = DisplayMode.STANDALONE
    private var themeColor: Int? = null
    private var backgroundColor: Int? = null
    private var activeThemeColor: Int? = null
    private var activeBgColor: Int? = null
    private var cachedIconBitmap: Bitmap? = null

    /**
     * Initializes dependencies and components when created.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Parse incoming intent extras with scheme sanitization
        pwaId = intent.getStringExtra(PwaShortcutManager.EXTRA_PWA_ID) ?: ""
        startUrl = sanitizeStartUrl(
            intent.getStringExtra(PwaShortcutManager.EXTRA_PWA_START_URL) ?: intent.dataString
        )
        pwaScope = intent.getStringExtra(PwaShortcutManager.EXTRA_PWA_SCOPE) ?: startUrl
        pwaName = intent.getStringExtra(PwaShortcutManager.EXTRA_PWA_NAME) ?: "Web App"
        displayMode = DisplayMode.fromString(intent.getStringExtra(PwaShortcutManager.EXTRA_PWA_DISPLAY_MODE))

        if (intent.hasExtra(PwaShortcutManager.EXTRA_PWA_THEME_COLOR)) {
            val tc = intent.getIntExtra(PwaShortcutManager.EXTRA_PWA_THEME_COLOR, AndroidColor.BLACK)
            if (tc != 0) themeColor = tc
        }
        if (intent.hasExtra(PwaShortcutManager.EXTRA_PWA_BG_COLOR)) {
            val bg = intent.getIntExtra(PwaShortcutManager.EXTRA_PWA_BG_COLOR, AndroidColor.BLACK)
            if (bg != 0) backgroundColor = bg
        }

        // AZK-SEC-02: Enforce PWA registration and origin authentication.
        // Prevent external apps or malicious intents from launching chromeless standalone
        // webviews for unregistered domains (phishing / origin spoofing defense).
        val storedPwa = PwaStorageManager.getInstance(this).getPwaById(pwaId)
            ?: PwaStorageManager.getInstance(this).findPwaForUrl(startUrl)

        if (storedPwa == null) {
            val browserIntent = Intent(this, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse(startUrl)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(browserIntent)
            finish()
            return
        }

        // Enforce verified parameters from persistent PWA storage to defeat intent parameter spoofing
        pwaId = storedPwa.id
        pwaName = storedPwa.title
        pwaScope = storedPwa.scope
        displayMode = storedPwa.displayMode
        cachedIconBitmap = storedPwa.getIconBitmap()
        if (themeColor == null) themeColor = storedPwa.themeColor
        if (backgroundColor == null) backgroundColor = storedPwa.backgroundColor

        // Constrain startUrl to registered scope
        if (!PwaInstallabilityValidator.isUrlInScope(startUrl, pwaScope)) {
            startUrl = storedPwa.startUrl
        }

        activeThemeColor = themeColor
        activeBgColor = backgroundColor

        // Configure dynamic TaskDescription for Android Recents
        updateTaskDescription()

        val prefs = PreferencesManager(this)
        val appTheme = prefs.getThemeMode()
        AzokleApplication.applyThemeMode(appTheme)

        // Configure edge-to-edge & system bar colors with contrast-accurate icons
        updateSystemBars(activeThemeColor, activeBgColor, isDarkThemeActive(appTheme))

        setContent {
            AzokleSearchTheme(themeMode = appTheme) {
                androidx.compose.runtime.key(pwaId) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = (activeBgColor ?: activeThemeColor)?.let { Color(it) } ?: MaterialTheme.colorScheme.background
                    ) {
                        PwaContainerScreen(
                            pwaId = pwaId,
                            pwaName = pwaName,
                            startUrl = startUrl,
                            scope = pwaScope,
                            displayMode = displayMode,
                            themeColor = themeColor,
                            backgroundColor = backgroundColor,
                            iconBitmap = cachedIconBitmap,
                            onClose = { finish() },
                            onSessionReady = { s -> geckoSession = s },
                            onFullScreenChange = { isFull ->
                                setFullScreen(isFull)
                            },
                            onThemeColorUpdated = { newTheme, newBg ->
                                activeThemeColor = newTheme ?: activeThemeColor
                                if (newBg != null) activeBgColor = newBg
                                updateSystemBars(activeThemeColor, activeBgColor)
                                updateTaskDescription()
                            },
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
    }

    /**
     * Executes the sanitize start url operation.
     */
    private fun sanitizeStartUrl(rawUrl: String?): String {
        if (rawUrl.isNullOrBlank()) return "https://azokle.com"
        val trimmed = rawUrl.trim()
        val scheme = try { Uri.parse(trimmed).scheme?.lowercase() ?: "" } catch (e: Exception) { "" }
        val dangerousSchemes = setOf("file", "content", "javascript", "data", "chrome", "resource", "about", "jar", "blob", "view-source", "ws", "wss")
        if (scheme in dangerousSchemes) {
            return "https://azokle.com"
        }
        if (trimmed.startsWith("azokle-pwa://", ignoreCase = true)) {
            val queryUrl = try { Uri.parse(trimmed).getQueryParameter("url") } catch (e: Exception) { null }
            if (!queryUrl.isNullOrBlank()) {
                return sanitizeStartUrl(queryUrl)
            }
            val withoutScheme = trimmed.removePrefix("azokle-pwa://")
            return if (withoutScheme.startsWith("http://", ignoreCase = true) || withoutScheme.startsWith("https://", ignoreCase = true)) {
                sanitizeStartUrl(withoutScheme)
            } else {
                "https://$withoutScheme"
            }
        }
        if (scheme == "http" || scheme == "https") {
            return trimmed
        }
        return "https://azokle.com"
    }

    /**
     * Handles on new intent events.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val newPwaId = intent.getStringExtra(PwaShortcutManager.EXTRA_PWA_ID) ?: ""
        val newStartUrl = sanitizeStartUrl(
            intent.getStringExtra(PwaShortcutManager.EXTRA_PWA_START_URL) ?: intent.dataString
        )
        val newScope = intent.getStringExtra(PwaShortcutManager.EXTRA_PWA_SCOPE) ?: newStartUrl
        val newName = intent.getStringExtra(PwaShortcutManager.EXTRA_PWA_NAME) ?: "Web App"
        val newDisplay = DisplayMode.fromString(intent.getStringExtra(PwaShortcutManager.EXTRA_PWA_DISPLAY_MODE))

        val storedPwa = PwaStorageManager.getInstance(this).getPwaById(newPwaId)
            ?: PwaStorageManager.getInstance(this).findPwaForUrl(newStartUrl)

        if (storedPwa == null) {
            val browserIntent = Intent(this, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse(newStartUrl)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(browserIntent)
            finish()
            return
        }

        pwaId = storedPwa.id
        pwaName = storedPwa.title
        pwaScope = storedPwa.scope
        displayMode = storedPwa.displayMode
        cachedIconBitmap = storedPwa.getIconBitmap()
        themeColor = storedPwa.themeColor
        backgroundColor = storedPwa.backgroundColor
        startUrl = if (PwaInstallabilityValidator.isUrlInScope(newStartUrl, pwaScope)) {
            newStartUrl
        } else {
            storedPwa.startUrl
        }

        activeThemeColor = themeColor
        activeBgColor = backgroundColor
        updateTaskDescription()
        updateSystemBars(activeThemeColor, activeBgColor)

        geckoSession?.let { s ->
            if (s.isOpen) {
                s.loadUri(startUrl)
            }
        }
    }

    /**
     * Handles on configuration changed events.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val isDark = isDarkThemeActive()
        updateSystemBars(activeThemeColor, activeBgColor, isDark)
    }

    /**
     * Checks whether dark theme active.
     */
    private fun isDarkThemeActive(themeMode: String? = null): Boolean {
        val mode = themeMode ?: PreferencesManager(this).getThemeMode()
        return when (mode.lowercase()) {
            "dark" -> true
            "light" -> false
            else -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        }
    }

    /**
     * Updates task description.
     */
    private fun updateTaskDescription() {
        try {
            val primaryColor = activeThemeColor ?: AndroidColor.parseColor("#18181B")
            @Suppress("DEPRECATION")
            val taskDesc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityManager.TaskDescription.Builder()
                    .setLabel(pwaName)
                    .setPrimaryColor(primaryColor)
                    .build()
            } else {
                ActivityManager.TaskDescription(pwaName, cachedIconBitmap, primaryColor)
            }
            setTaskDescription(taskDesc)
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    /**
     * Updates system bars.
     */
    fun updateSystemBars(theme: Int?, bg: Int?, isDark: Boolean = isDarkThemeActive()) {
        val effectiveColor = theme
            ?: bg
            ?: if (isDark) AndroidColor.parseColor("#09090B") else AndroidColor.parseColor("#FFFFFF")

        val isLightBar = androidx.core.graphics.ColorUtils.calculateLuminance(effectiveColor) > 0.5

        enableEdgeToEdge(
            statusBarStyle = if (isLightBar) {
                SystemBarStyle.light(effectiveColor, effectiveColor)
            } else {
                SystemBarStyle.dark(effectiveColor)
            },
            navigationBarStyle = if (isLightBar) {
                SystemBarStyle.light(effectiveColor, effectiveColor)
            } else {
                SystemBarStyle.dark(effectiveColor)
            }
        )

        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = isLightBar
        insetsController.isAppearanceLightNavigationBars = isLightBar

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val appearance = if (isLightBar) {
                android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            } else {
                0
            }
            window.insetsController?.setSystemBarsAppearance(
                appearance,
                android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            )
        }
    }

    /**
     * Updates the full screen.
     */
    private fun setFullScreen(fullScreen: Boolean) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (fullScreen) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            updateSystemBars(activeThemeColor, activeBgColor)
        }
    }





    /**
     * Cleans up resources and listeners when destroyed.
     */
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

/**
 * Composable that renders the pwa container screen UI component.
 */
@Composable
fun PwaContainerScreen(
    pwaId: String = "",
    pwaName: String,
    startUrl: String,
    scope: String,
    displayMode: DisplayMode,
    themeColor: Int?,
    backgroundColor: Int?,
    iconBitmap: Bitmap?,
    onClose: () -> Unit,
    onSessionReady: (GeckoSession) -> Unit = {},
    onFullScreenChange: (Boolean) -> Unit,
    onThemeColorUpdated: (Int?, Int?) -> Unit = { _, _ -> },
    onOpenInBrowser: (String) -> Unit
) {
    val context = LocalContext.current
    val colors = AzokleTheme.colors
    val runtime = remember { GeckoEngineManager.getRuntime(context) }
    val isTor = remember(startUrl) {
        startUrl.contains(".onion", ignoreCase = true) || com.azokle.search.tor.TorCircuitManager.isTorActive
    }
    val session = remember(pwaId, isTor) {
        GeckoEngineManager.createSession(
            context = context,
            isPrivate = isTor,
            isTor = isTor,
            isDesktopMode = false
        )
    }

    LaunchedEffect(session) {
        onSessionReady(session)
    }

    DisposableEffect(session) {
        onDispose {
            try {
                session.close()
            } catch (e: Exception) {}
        }
    }

    var currentThemeColor by remember(pwaId, themeColor) { mutableStateOf(themeColor) }
    var currentBgColor by remember(pwaId, backgroundColor) { mutableStateOf(backgroundColor) }
    var currentUrl by remember(pwaId, startUrl) { mutableStateOf(startUrl) }
    var currentTitle by remember(pwaId, pwaName) { mutableStateOf(pwaName) }
    var progress by remember(pwaId) { mutableFloatStateOf(0f) }
    var isLoading by remember(pwaId) { mutableStateOf(true) }
    var isSplashVisible by remember(pwaId) { mutableStateOf(true) }
    var canGoBack by remember(pwaId) { mutableStateOf(false) }
    var isOutOfScope by remember(pwaId) { mutableStateOf(false) }
    var isFullScreen by remember(pwaId) { mutableStateOf(false) }

    LaunchedEffect(scope, currentUrl) {
        if (currentUrl.isNotBlank() && !currentUrl.equals("about:blank", ignoreCase = true)) {
            isOutOfScope = !PwaInstallabilityValidator.isUrlInScope(currentUrl, scope)
        }
    }

    var activePermissionRequest by remember(pwaId) { mutableStateOf<SitePermissionRequest?>(null) }
    val permissionsManager = remember { SitePermissionsManager.getInstance(context) }

    /**
     * Handles allow permission.
     */
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

    /**
     * Handles deny permission.
     */
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
        if (isFullScreen) {
            session.exitFullScreen()
            isFullScreen = false
            onFullScreenChange(false)
        } else if (session.isOpen && canGoBack) {
            session.goBack()
        } else {
            onClose()
        }
    }

    val effectiveBarColor = currentThemeColor ?: currentBgColor
    val barColor = effectiveBarColor?.let { Color(it) } ?: (if (colors.isDark) Color(0xFF09090B) else Color(0xFFFFFFFF))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(barColor)
    ) {
        Column(
            modifier = if (isFullScreen) {
                Modifier.fillMaxSize()
            } else {
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            }
        ) {
            // ==========================================
            // 1. OUT OF SCOPE WARNING & ACTION BAR
            // ==========================================
            AnimatedVisibility(
                visible = isOutOfScope && !isFullScreen,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E1B4B))
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
                                .size(30.dp)
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
                                text = "External: ${Uri.parse(currentUrl).host ?: currentUrl}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Tap arrow to return to $pwaName",
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
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.OpenInBrowser,
                                contentDescription = "Open in Browser",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Browser",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // ==========================================
            // 2. MINIMALIST PROGRESS INDICATOR
            // ==========================================
            if (isLoading && !isSplashVisible && !isFullScreen) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp),
                    color = themeColor?.let { Color(it) } ?: Color(0xFF38BDF8),
                    trackColor = Color.Transparent
                )
            }

            // ==========================================
            // 3. GECKOVIEW QUANTUM ENGINE CONTAINER
            // ==========================================
            AndroidView(
                factory = { ctx ->
                    GeckoView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        session.navigationDelegate = object : GeckoSession.NavigationDelegate {
                            /**
                             * Handles on location change events.
                             */
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

                            /**
                             * Handles on can go back events.
                             */
                            override fun onCanGoBack(session: GeckoSession, canGoBackVal: Boolean) {
                                canGoBack = canGoBackVal
                            }

                            /**
                             * Handles on new session events.
                             */
                            override fun onNewSession(session: GeckoSession, uri: String): GeckoResult<GeckoSession>? {
                                val scheme = try { Uri.parse(uri).scheme?.lowercase() ?: "" } catch (e: Exception) { "" }
                                if (com.azokle.search.navigation.NavigationSecurity.isForbiddenWebScheme(scheme)) {
                                    return GeckoResult.fromValue(null)
                                }
                                if (PwaInstallabilityValidator.isUrlInScope(uri, scope)) {
                                    session.loadUri(uri)
                                } else {
                                    onOpenInBrowser(uri)
                                }
                                return GeckoResult.fromValue(null)
                            }

                            /**
                             * Handles on load request events.
                             */
                            override fun onLoadRequest(
                                session: GeckoSession,
                                request: GeckoSession.NavigationDelegate.LoadRequest
                            ): GeckoResult<org.mozilla.geckoview.AllowOrDeny>? {
                                val uriStr = request.uri
                                val scheme = try { Uri.parse(uriStr).scheme?.lowercase() ?: "" } catch (e: Exception) { "" }
                                if (com.azokle.search.navigation.NavigationSecurity.isForbiddenWebScheme(scheme)) {
                                    return GeckoResult.fromValue(org.mozilla.geckoview.AllowOrDeny.DENY)
                                }
                                if (scheme == "http" || scheme == "https" || scheme == "about" || scheme == "blob" || scheme == "data" || scheme == "javascript") {
                                    return GeckoResult.fromValue(org.mozilla.geckoview.AllowOrDeny.ALLOW)
                                }
                                if (com.azokle.search.navigation.ExternalNavigationHandler.isExternalNavigation(uriStr)) {
                                    com.azokle.search.navigation.ExternalNavigationHandler.handleNavigation(
                                        context = ctx,
                                        uriStr = uriStr,
                                        session = session,
                                        hasUserGesture = request.hasUserGesture,
                                        targetWindow = request.target,
                                        isPrivate = isTor,
                                        isTor = isTor
                                    )
                                    return GeckoResult.fromValue(org.mozilla.geckoview.AllowOrDeny.DENY)
                                }
                                return GeckoResult.fromValue(org.mozilla.geckoview.AllowOrDeny.DENY)
                            }
                        }

                        session.progressDelegate = object : GeckoSession.ProgressDelegate {
                            /**
                             * Handles on page start events.
                             */
                            override fun onPageStart(session: GeckoSession, url: String) {
                                isLoading = true
                                progress = 0.1f
                            }

                            /**
                             * Handles on page stop events.
                             */
                            override fun onPageStop(session: GeckoSession, success: Boolean) {
                                isLoading = false
                                progress = 1.0f
                                isSplashVisible = false
                            }

                            /**
                             * Handles on progress change events.
                             */
                            override fun onProgressChange(session: GeckoSession, progressInt: Int) {
                                progress = progressInt / 100f
                                if (progressInt > 65) {
                                    isSplashVisible = false
                                }
                            }
                        }

                        session.contentDelegate = object : GeckoSession.ContentDelegate {
                            /**
                             * Handles on title change events.
                             */
                            override fun onTitleChange(session: GeckoSession, title: String?) {
                                if (!title.isNullOrBlank()) {
                                    currentTitle = title
                                }
                            }

                            /**
                             * Handles on full screen events.
                             */
                            override fun onFullScreen(session: GeckoSession, fullScreen: Boolean) {
                                isFullScreen = fullScreen
                                onFullScreenChange(fullScreen)
                            }

                            /**
                             * Handles on web app manifest events.
                             */
                            override fun onWebAppManifest(session: GeckoSession, manifest: JSONObject) {
                                try {
                                    val manifestThemeColorStr = manifest.optString("theme_color", "").takeIf { it.isNotBlank() }
                                    val parsedTheme = manifestThemeColorStr?.let {
                                        try { AndroidColor.parseColor(it) } catch (e: Exception) { null }
                                    }
                                    val manifestBgColorStr = manifest.optString("background_color", "").takeIf { it.isNotBlank() }
                                    val parsedBg = manifestBgColorStr?.let {
                                        try { AndroidColor.parseColor(it) } catch (e: Exception) { null }
                                    }
                                    if (parsedTheme != null || parsedBg != null) {
                                        if (parsedTheme != null) currentThemeColor = parsedTheme
                                        if (parsedBg != null) currentBgColor = parsedBg
                                        onThemeColorUpdated(parsedTheme, parsedBg)
                                    }
                                } catch (e: Exception) {}
                            }

                            /**
                             * Handles on external response events.
                             */
                            override fun onExternalResponse(session: GeckoSession, response: org.mozilla.geckoview.WebResponse) {
                                try {
                                    val downloadUrl = response.uri
                                    val contentType = response.headers["Content-Type"] ?: response.headers["content-type"]
                                    val contentDisposition = response.headers["Content-Disposition"] ?: response.headers["content-disposition"]
                                    val filename = com.azokle.search.downloads.DownloadManager.extractFilename(
                                        urlStr = downloadUrl,
                                        contentDisposition = contentDisposition,
                                        mimeType = contentType
                                    )
                                    val mimeType = contentType ?: "application/octet-stream"
                                    val headerLength = (response.headers["Content-Length"] ?: response.headers["content-length"])?.toLongOrNull() ?: 0L

                                    var stagedFile: java.io.File? = null
                                    try {
                                        val stagingDir = java.io.File(ctx.cacheDir, "download_staging").apply { mkdirs() }
                                        val tempFile = java.io.File(stagingDir, "stage_${java.util.UUID.randomUUID().toString().take(8)}.tmp")
                                        response.body?.use { inStream ->
                                            tempFile.outputStream().use { outStream ->
                                                inStream.copyTo(outStream)
                                            }
                                        }
                                        if (tempFile.exists() && tempFile.length() > 0L) {
                                            stagedFile = tempFile
                                        }
                                    } catch (_: Throwable) {}

                                    val effectiveLength = stagedFile?.length()?.takeIf { it > 0L } ?: headerLength

                                    // AZK-MED-04: Show user confirmation and dangerous executable warning in PWA context
                                    val isDangerous = com.azokle.search.downloads.DownloadManager.isDangerousExecutable(filename)
                                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                                        val act = (ctx as? android.app.Activity)
                                            ?: ((ctx as? android.content.ContextWrapper)?.baseContext as? android.app.Activity)
                                        if (act == null || act.isFinishing || act.isDestroyed) return@post
                                        android.app.AlertDialog.Builder(act)
                                            .setTitle(if (isDangerous) "Warning: Executable File" else "Confirm Download")
                                            .setMessage(
                                                (if (isDangerous) "WARNING: This file ($filename) can execute code and may harm your device.\n\n" else "") +
                                                "Do you want to download $filename?"
                                            )
                                            .setPositiveButton("Download") { _, _ ->
                                                com.azokle.search.downloads.DownloadManager.startDownload(
                                                    context = ctx,
                                                    url = downloadUrl,
                                                    filename = filename,
                                                    mimeType = mimeType,
                                                    contentLength = effectiveLength,
                                                    headers = response.headers,
                                                    isPrivate = isTor,
                                                    isTor = isTor,
                                                    stagedFilePath = stagedFile?.absolutePath
                                                )
                                            }
                                            .setNegativeButton("Cancel") { _, _ ->
                                                stagedFile?.delete()
                                            }
                                            .show()
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("PwaActivity", "Download routing error: ${e.message}")
                                }
                            }
                        }

                        session.permissionDelegate = AzoklePermissionDelegate(
                            context = ctx,
                            isPrivate = isTor,
                            isTor = isTor,
                            onPermissionPromptRequested = { req ->
                                activePermissionRequest = req
                            }
                        )

                        session.promptDelegate = com.azokle.search.engine.GeckoPromptDelegateImpl(
                            context = ctx,
                            isPrivate = isTor,
                            isTor = isTor,
                            onPopupRequested = { targetUri ->
                                val scheme = com.azokle.search.common.util.UrlHelper.extractScheme(targetUri)
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
                        (session.promptDelegate as? com.azokle.search.engine.GeckoPromptDelegateImpl)?.dismissAllPendingDialogs()
                        view.releaseSession()
                    } catch (e: Exception) {}
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .focusProperties { canFocus = false }
            )
        }

        // ==========================================
        // 4. BRANDED HIGH-END SPLASH SCREEN
        // ==========================================
        AnimatedVisibility(
            visible = isSplashVisible,
            enter = fadeIn(),
            exit = fadeOut(tween(350)),
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
                                .size(88.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(22.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(themeColor?.let { Color(it) } ?: Color(0xFF1E293B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = pwaName.take(1).uppercase(),
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = pwaName,
                        fontSize = 19.sp,
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

        // ==========================================
        // 5. PERMISSION PROMPT MODAL
        // ==========================================
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
