package com.azokle.search

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import android.content.Context
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import com.azokle.search.data.PreferencesManager
import com.azokle.search.ui.screens.MainContainerScreen
import com.azokle.search.ui.theme.AzokleSearchTheme
import com.azokle.search.util.LocaleHelper
import com.azokle.search.viewmodel.HomeViewModel

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

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

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
                    }
                })
                alphaAnimator.start()
            } catch (e: Throwable) {
                splashScreenViewProvider.remove()
            }
        }

        com.azokle.search.security.BiometricAuthManager.setCurrentActivity(this)
        val prefs = PreferencesManager(this)
        AzokleApplication.applyThemeMode(prefs.getThemeMode())

        com.azokle.search.browser.media.MediaPlaybackManager.initialize(this)

        handleIncomingIntent(intent)

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val currentContext = LocalContext.current

            val localizedContext = remember(uiState.selectedLanguage) {
                LocaleHelper.applyLocale(currentContext, uiState.selectedLanguage)
            }
            val localizedConfiguration = remember(uiState.selectedLanguage, localizedContext) {
                localizedContext.resources.configuration
            }
            val layoutDirection = remember(uiState.selectedLanguage) {
                val locale = LocaleHelper.getLocale(uiState.selectedLanguage)
                if (LocaleHelper.isRtl(locale)) LayoutDirection.Rtl else LayoutDirection.Ltr
            }

            androidx.compose.runtime.LaunchedEffect(uiState.selectedThemeMode) {
                AzokleApplication.applyThemeMode(uiState.selectedThemeMode)
                com.azokle.search.browser.gecko.GeckoEngineManager.updateThemeMode(uiState.selectedThemeMode)
            }

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedConfiguration,
                LocalLayoutDirection provides layoutDirection,
                androidx.activity.compose.LocalActivityResultRegistryOwner provides this@MainActivity
            ) {
                AzokleSearchTheme(themeMode = uiState.selectedThemeMode) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
                            if (uiState.isOnboardingVisible) {
                                com.azokle.search.ui.screens.OnboardingScreen(
                                    onFinish = { viewModel.completeOnboarding() }
                                )
                            } else {
                                MainContainerScreen(
                                    viewModel = viewModel,
                                    onVoiceSearchRequest = { viewModel.setVoiceModalOpen(true) }
                                )
                            }
                            com.azokle.search.ui.components.AzokleToastHost()
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        com.azokle.search.security.BiometricAuthManager.setCurrentActivity(this)
        com.azokle.search.widget.AzokleSearchWidgetProvider.updateAllWidgets(this)
    }

    override fun onStop() {
        super.onStop()
        if (isFinishing) {
            com.azokle.search.browser.media.MediaPlaybackManager.stop()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        com.azokle.search.security.BiometricAuthManager.onActivityDestroyed(this)
        if (isFinishing) {
            com.azokle.search.browser.tabs.TabManager.closeEphemeralAndTorTabs()
            com.azokle.search.security.TorCircuitManager.stopTorService(this)
            com.azokle.search.browser.media.MediaPlaybackManager.release(this)
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
            val sanitized = com.azokle.search.util.UrlHelper.stripTrackingParameters(dataString.trim())
            if (com.azokle.search.util.UrlHelper.isAllowedScheme(sanitized)) {
                val resolved = com.azokle.search.util.UrlHelper.resolveSearchOrUrl(this, sanitized)
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
