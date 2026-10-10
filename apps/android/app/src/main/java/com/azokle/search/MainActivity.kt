package com.azokle.search

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import android.content.Context
import android.content.res.Configuration
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import com.azokle.search.common.util.PreferencesManager
import com.azokle.search.common.theme.AzokleSearchTheme
import com.azokle.search.common.util.LocaleHelper
import com.azokle.search.search.HomeViewModel

import com.azokle.search.engine.FilePromptHost

class MainActivity : FragmentActivity(), FilePromptHost {
    private val viewModel: HomeViewModel by viewModels()

    override fun attachBaseContext(newBase: Context) {
        val prefs = PreferencesManager.getInstance(newBase)
        val lang = prefs.getAppLanguage()
        val wrappedContext = LocaleHelper.applyLocale(newBase, lang)
        super.attachBaseContext(wrappedContext)
    }

    private val voiceSearchLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()

            if (!spokenText.isNullOrBlank()) {
                val cleanQuery = spokenText
                    .filter { it >= ' ' && it != '\u007F' }
                    .trim()
                    .take(500)
                if (cleanQuery.isNotBlank()) {
                    viewModel.onQueryChange(cleanQuery)
                    viewModel.executeSearch(cleanQuery)
                }
            }
        }
    }

    private var pendingFilePrompt: Pair<org.mozilla.geckoview.GeckoSession.PromptDelegate.FilePrompt, org.mozilla.geckoview.GeckoResult<org.mozilla.geckoview.GeckoSession.PromptDelegate.PromptResponse>>? = null

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

    override fun launchFilePicker(
        prompt: org.mozilla.geckoview.GeckoSession.PromptDelegate.FilePrompt,
        result: org.mozilla.geckoview.GeckoResult<org.mozilla.geckoview.GeckoSession.PromptDelegate.PromptResponse>
    ) {
        pendingFilePrompt = Pair(prompt, result)
        try {
            val intent = if (prompt.type == org.mozilla.geckoview.GeckoSession.PromptDelegate.FilePrompt.Type.FOLDER) {
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
                    if (prompt.type == org.mozilla.geckoview.GeckoSession.PromptDelegate.FilePrompt.Type.MULTIPLE) {
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

    private val pendingAndroidPermissions = java.util.concurrent.ConcurrentLinkedQueue<Pair<Array<String>, org.mozilla.geckoview.GeckoSession.PermissionDelegate.Callback>>()
    private var isRequestingAndroidPermission = false

    private val androidPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val current = pendingAndroidPermissions.poll()
        if (current != null) {
            val (perms, cb) = current
            val isLocationReq = perms.any {
                it == android.Manifest.permission.ACCESS_FINE_LOCATION || it == android.Manifest.permission.ACCESS_COARSE_LOCATION
            }
            val locationOk = if (isLocationReq) {
                (permissionsMap[android.Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
                (permissionsMap[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true) ||
                (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) ||
                (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED)
            } else true

            val otherPerms = perms.filter {
                it != android.Manifest.permission.ACCESS_FINE_LOCATION && it != android.Manifest.permission.ACCESS_COARSE_LOCATION
            }
            val otherOk = otherPerms.all {
                permissionsMap[it] == true ||
                androidx.core.content.ContextCompat.checkSelfPermission(this, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }

            if (locationOk && otherOk) {
                cb.grant()
            } else {
                cb.reject()
            }
        }
        isRequestingAndroidPermission = false
        processNextAndroidPermissionRequest()
    }

    fun requestAndroidPermissions(
        permissions: Array<String>,
        callback: org.mozilla.geckoview.GeckoSession.PermissionDelegate.Callback
    ) {
        val isLocationReq = permissions.any {
            it == android.Manifest.permission.ACCESS_FINE_LOCATION || it == android.Manifest.permission.ACCESS_COARSE_LOCATION
        }
        val locationSatisfied = if (isLocationReq) {
            androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else true

        val nonLocationUngranted = permissions.filter {
            it != android.Manifest.permission.ACCESS_FINE_LOCATION &&
            it != android.Manifest.permission.ACCESS_COARSE_LOCATION &&
            androidx.core.content.ContextCompat.checkSelfPermission(this, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        if (locationSatisfied && nonLocationUngranted.isEmpty()) {
            callback.grant()
            return
        }

        pendingAndroidPermissions.offer(Pair(permissions, callback))
        if (!isRequestingAndroidPermission) {
            processNextAndroidPermissionRequest()
        }
    }

    private fun processNextAndroidPermissionRequest() {
        val next = pendingAndroidPermissions.peek() ?: return
        isRequestingAndroidPermission = true
        val (perms, _) = next
        val isLocationReq = perms.any {
            it == android.Manifest.permission.ACCESS_FINE_LOCATION || it == android.Manifest.permission.ACCESS_COARSE_LOCATION
        }
        val hasLocation = androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                          androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED

        val ungranted = perms.filter { perm ->
            if (perm == android.Manifest.permission.ACCESS_FINE_LOCATION || perm == android.Manifest.permission.ACCESS_COARSE_LOCATION) {
                !hasLocation
            } else {
                androidx.core.content.ContextCompat.checkSelfPermission(this, perm) != android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        }.distinct()

        if (ungranted.isEmpty()) {
            val polled = pendingAndroidPermissions.poll()
            polled?.second?.grant()
            isRequestingAndroidPermission = false
            processNextAndroidPermissionRequest()
        } else {
            androidPermissionsLauncher.launch(ungranted.toTypedArray())
        }
    }

    private var currentConfig by androidx.compose.runtime.mutableStateOf(android.content.res.Configuration())

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        currentConfig = Configuration(newConfig)
        val prefs = PreferencesManager.getInstance(this)
        val savedTheme = prefs.getThemeMode().lowercase()
        val isDark = when (savedTheme) {
            "dark" -> true
            "light" -> false
            else -> (newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        }
        updateSystemBars(isDark)
        try {
            com.azokle.search.widget.AzokleSearchWidgetProvider.updateAllWidgets(this)
            com.azokle.search.widget.AzokleTorSearchWidgetProvider.updateAllWidgets(this)
            com.azokle.search.widget.AzokleShieldsWidgetProvider.updateAllWidgets(this)
        } catch (e: Exception) {
            // Safe ignore
        }
    }

    fun updateSystemBars(isDark: Boolean) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ) { isDark },
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ) { isDark }
        )

        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = !isDark
        insetsController.isAppearanceLightNavigationBars = !isDark

        // API 30+ explicit system bar appearance (for Motorola Hello UI / AOSP Android 11-15+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(
                if (!isDark) {
                    android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                    android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                } else {
                    0
                },
                android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            )
        } else {
            // Legacy compatibility strictly for OEM manufacturer skins on API 26-29
            @Suppress("DEPRECATION")
            if (!isDark) {
                window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or
                    android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                    android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            } else {
                window.decorView.systemUiVisibility = window.decorView.systemUiVisibility and
                    android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv() and
                    android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentConfig = Configuration(resources.configuration)
        val prefs = PreferencesManager.getInstance(this)
        val savedTheme = prefs.getThemeMode().lowercase()
        val isInitialDark = when (savedTheme) {
            "dark" -> true
            "light" -> false
            else -> {
                val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                currentNightMode == Configuration.UI_MODE_NIGHT_YES
            }
        }
        updateSystemBars(isInitialDark)

        com.azokle.search.core.BiometricAuthManager.setCurrentActivity(this)
        AzokleApplication.applyThemeMode(prefs.getThemeMode())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            lifecycleScope.launch {
                repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                    com.azokle.search.media.MediaPlaybackManager.mediaState.collect { state ->
                        val isPlaying = state.status == com.azokle.search.media.PlaybackStatus.PLAYING
                        val hasVideo = state.metadata.hasVideo || state.isFullscreen
                        if (com.azokle.search.media.PictureInPictureHelper.supportsPictureInPicture(this@MainActivity)) {
                            try {
                                val params = com.azokle.search.media.PictureInPictureHelper.buildPipParams(
                                    activity = this@MainActivity,
                                    isPlaying = isPlaying && hasVideo,
                                    isFullscreen = state.isFullscreen,
                                    aspectRatio = state.metadata.videoAspectRatio
                                )
                                setPictureInPictureParams(params)
                            } catch (e: Exception) {
                                // Ignore transient PiP params errors safely
                            }
                        }
                    }
                }
            }
        }

        handleIncomingIntent(intent)

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val currentContext = LocalContext.current

            val localizedContext = remember(uiState.selectedLanguage) {
                LocaleHelper.applyLocale(currentContext, uiState.selectedLanguage)
            }
            val localizedConfiguration = remember(uiState.selectedLanguage, currentConfig) {
                val config = Configuration(currentConfig)
                val locale = LocaleHelper.getLocale(uiState.selectedLanguage)
                config.setLocale(locale)
                config.setLayoutDirection(locale)
                config
            }
            val layoutDirection = remember(uiState.selectedLanguage) {
                val locale = LocaleHelper.getLocale(uiState.selectedLanguage)
                if (LocaleHelper.isRtl(locale)) LayoutDirection.Rtl else LayoutDirection.Ltr
            }

            val isSystemDark = (currentConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            val isDark = when (uiState.selectedThemeMode.lowercase()) {
                "dark" -> true
                "light" -> false
                else -> isSystemDark
            }

            androidx.compose.runtime.DisposableEffect(isDark) {
                updateSystemBars(isDark)
                onDispose {}
            }

            androidx.compose.runtime.LaunchedEffect(uiState.selectedThemeMode) {
                AzokleApplication.applyThemeMode(uiState.selectedThemeMode)
                com.azokle.search.engine.GeckoEngineManager.updateThemeMode(uiState.selectedThemeMode)
            }

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedConfiguration,
                LocalLayoutDirection provides layoutDirection,
                androidx.activity.compose.LocalActivityResultRegistryOwner provides this@MainActivity
            ) {
                AzokleSearchTheme(themeMode = if (isDark) "dark" else "light") {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
                            if (uiState.isOnboardingVisible) {
                                com.azokle.search.onboarding.OnboardingScreen(
                                    selectedSearchEngineId = uiState.selectedSearchEngine,
                                    onSelectSearchEngine = { viewModel.setSearchEngine(it) },
                                    onFinish = { viewModel.completeOnboarding() }
                                )
                            } else {
                                com.azokle.search.engine.ui.BrowserScreen(
                                    initialUrl = com.azokle.search.tabs.TabManager.activeTab?.url ?: "",
                                    onClose = {
                                        this@MainActivity.moveTaskToBack(true)
                                    },
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }
            }
        }

        // Warm up GeckoView Quantum Engine & MediaPlaybackManager strictly AFTER the window is attached
        // and the initial Home Screen frame is drawn, ensuring sub-100ms cold start to interactive UI.
        window.decorView.post {
            try {
                com.azokle.search.media.MediaPlaybackManager.initialize(applicationContext)
                com.azokle.search.engine.GeckoEngineManager.getRuntime(applicationContext)
            } catch (_: Throwable) {}
        }
    }

    override fun onResume() {
        super.onResume()
        val prefs = PreferencesManager.getInstance(this)
        val savedTheme = prefs.getThemeMode().lowercase()
        val isDark = when (savedTheme) {
            "dark" -> true
            "light" -> false
            else -> {
                val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                currentNightMode == Configuration.UI_MODE_NIGHT_YES
            }
        }
        updateSystemBars(isDark)
        com.azokle.search.core.BiometricAuthManager.setCurrentActivity(this)
        com.azokle.search.widget.AzokleSearchWidgetProvider.updateAllWidgets(this)
        com.azokle.search.widget.AzokleTorSearchWidgetProvider.updateAllWidgets(this)
        com.azokle.search.widget.AzokleShieldsWidgetProvider.updateAllWidgets(this)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        enterPipIfEligible()
    }

    private fun enterPipIfEligible() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val state = com.azokle.search.media.MediaPlaybackManager.mediaState.value
            val isPlaying = state.status == com.azokle.search.media.PlaybackStatus.PLAYING
            val hasVideo = state.metadata.hasVideo || state.isFullscreen
            if (isPlaying && hasVideo && com.azokle.search.media.PictureInPictureHelper.supportsPictureInPicture(this)) {
                try {
                    val params = com.azokle.search.media.PictureInPictureHelper.buildPipParams(
                        activity = this,
                        isPlaying = true,
                        isFullscreen = state.isFullscreen,
                        aspectRatio = state.metadata.videoAspectRatio
                    )
                    enterPictureInPictureMode(params)
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "enterPictureInPictureMode failed: ${e.message}")
                }
            }
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        viewModel.setPipMode(isInPictureInPictureMode)
    }

    override fun onStop() {
        super.onStop()
        if (isFinishing) {
            com.azokle.search.media.MediaPlaybackManager.stop()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        com.azokle.search.core.BiometricAuthManager.onActivityDestroyed(this)
        if (isFinishing) {
            com.azokle.search.tabs.TabManager.closeEphemeralAndTorTabs()
            com.azokle.search.tor.TorCircuitManager.stopTorService(this)
            com.azokle.search.media.MediaPlaybackManager.release(this)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return

        if (intent.getBooleanExtra(com.azokle.search.widget.AzokleSearchWidgetProvider.EXTRA_START_VOICE, false)) {
            if (com.azokle.search.common.util.VoiceSearchHelper.isVoiceSearchAvailable(this)) {
                if (viewModel.uiState.value.inAppBrowserUrl != null) {
                    viewModel.closeInAppBrowser()
                }
                viewModel.setVoiceModalOpen(true)
            } else {
                Toast.makeText(this, "Voice search is not supported on this device", Toast.LENGTH_SHORT).show()
                viewModel.setSearchExpanded(true)
            }
            return
        }
        if (intent.getBooleanExtra(com.azokle.search.widget.AzokleSearchWidgetProvider.EXTRA_OPEN_TOR, false)) {
            viewModel.openInAppBrowser("about:tor", isTor = true)
            return
        }
        if (intent.getBooleanExtra(com.azokle.search.widget.AzokleSearchWidgetProvider.EXTRA_SCAN_QR, false)) {
            if (viewModel.uiState.value.inAppBrowserUrl != null) {
                viewModel.closeInAppBrowser()
            }
            viewModel.setQrScannerOpen(true)
            return
        }
        if (intent.getBooleanExtra(com.azokle.search.widget.AzokleSearchWidgetProvider.EXTRA_FOCUS_SEARCH, false)) {
            viewModel.setSearchExpanded(true)
            return
        }

        val action = intent.action
        val dataString = intent.dataString ?: intent.data?.toString()

        if (action == Intent.ACTION_VIEW && !dataString.isNullOrBlank()) {
            val rawTrimmed = dataString.trim()
            val unwrapped = com.azokle.search.common.util.UrlHelper.unwrapLinkShim(rawTrimmed)
            val sanitized = com.azokle.search.common.util.UrlHelper.stripTrackingParameters(unwrapped)

            // Direct external dispatch for Play Store links, market links, or external app schemes
            if (com.azokle.search.navigation.ExternalNavigationHandler.isExternalNavigation(sanitized)) {
                val handled = com.azokle.search.navigation.ExternalNavigationHandler.handleNavigation(
                    context = this,
                    uriStr = sanitized,
                    session = null,
                    hasUserGesture = true,
                    isPrivate = false,
                    isTor = false
                )
                if (handled) {
                    return
                }
            }

            if (com.azokle.search.common.util.UrlHelper.isAllowedScheme(sanitized)) {
                val resolved = com.azokle.search.common.util.UrlHelper.resolveSearchOrUrl(this, sanitized)
                viewModel.openInAppBrowser(resolved)
            }
        } else if (action == Intent.ACTION_WEB_SEARCH) {
            val query = intent.getStringExtra(android.app.SearchManager.QUERY)
                ?: intent.getStringExtra("query")
                ?: dataString
            if (!query.isNullOrBlank()) {
                viewModel.onQueryChange(query)
                viewModel.executeSearch(query)
            }
        }
    }

    private fun startVoiceRecognition() {
        if (!com.azokle.search.common.util.VoiceSearchHelper.isVoiceSearchAvailable(this)) {
            Toast.makeText(this, "Voice search is not supported on this device", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH
                )
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to search privately on Azokle...")
            }
            voiceSearchLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Voice search is not available on this device", Toast.LENGTH_SHORT).show()
        }
    }
}
