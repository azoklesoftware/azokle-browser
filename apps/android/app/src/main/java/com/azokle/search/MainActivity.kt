package com.azokle.search

import android.app.Activity
import android.content.Intent
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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import com.azokle.search.common.util.PreferencesManager
import com.azokle.search.common.theme.AzokleSearchTheme
import com.azokle.search.common.util.LocaleHelper
import com.azokle.search.search.HomeViewModel

class MainActivity : FragmentActivity() {
    private val viewModel: HomeViewModel by viewModels()

    override fun attachBaseContext(newBase: Context) {
        val prefs = PreferencesManager(newBase)
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
                viewModel.onQueryChange(spokenText)
                viewModel.executeSearch(spokenText)
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

    fun launchFilePicker(
        prompt: org.mozilla.geckoview.GeckoSession.PromptDelegate.FilePrompt,
        result: org.mozilla.geckoview.GeckoResult<org.mozilla.geckoview.GeckoSession.PromptDelegate.PromptResponse>
    ) {
        pendingFilePrompt = Pair(prompt, result)
        try {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
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
            filePickerLauncher.launch(Intent.createChooser(intent, "Select File"))
        } catch (e: Exception) {
            pendingFilePrompt = null
            result.complete(prompt.dismiss())
        }
    }

    private var currentConfig by androidx.compose.runtime.mutableStateOf(android.content.res.Configuration())

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        currentConfig = Configuration(newConfig)
        val prefs = PreferencesManager(this)
        val savedTheme = prefs.getThemeMode().lowercase()
        val isDark = when (savedTheme) {
            "dark" -> true
            "light" -> false
            else -> (newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        }
        updateSystemBars(isDark)
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
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_USER
        currentConfig = Configuration(resources.configuration)
        val prefs = PreferencesManager(this)
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

        // Cinema-grade exit transition: smooth alpha fade
        splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
            try {
                val splashView = splashScreenViewProvider.view
                val alphaAnimator = android.animation.ObjectAnimator.ofFloat(splashView, android.view.View.ALPHA, 1f, 0f).apply {
                    duration = 240
                    interpolator = android.view.animation.AccelerateDecelerateInterpolator()
                }
                alphaAnimator.addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        splashScreenViewProvider.remove()
                        updateSystemBars(isInitialDark)
                    }
                })
                alphaAnimator.start()
            } catch (e: Throwable) {
                splashScreenViewProvider.remove()
                updateSystemBars(isInitialDark)
            }
        }

        com.azokle.search.core.BiometricAuthManager.setCurrentActivity(this)
        AzokleApplication.applyThemeMode(prefs.getThemeMode())

        com.azokle.search.media.MediaPlaybackManager.initialize(this)

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
    }

    override fun onResume() {
        super.onResume()
        val prefs = PreferencesManager(this)
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
            if (viewModel.uiState.value.inAppBrowserUrl != null) {
                viewModel.closeInAppBrowser()
            }
            viewModel.setVoiceModalOpen(true)
            return
        }
        if (intent.getBooleanExtra(com.azokle.search.widget.AzokleSearchWidgetProvider.EXTRA_OPEN_TOR, false)) {
            viewModel.openInAppBrowser("about:tor", isTor = true)
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
