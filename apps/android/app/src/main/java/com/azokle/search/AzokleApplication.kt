package com.azokle.search

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Process
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.FragmentActivity
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.azokle.search.common.util.PreferencesManager

class AzokleApplication : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.20) // Use at most 20% of app RAM for bitmap caching
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(64 * 1024 * 1024) // 64 MB bounded disk cache
                    .build()
            }
            .components {
                add(SvgDecoder.Factory())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .crossfade(true)
            .build()
    }

    override fun onCreate() {
        super.onCreate()

        // Critical: GeckoView runs multiple isolated child processes (e.g. :tab_*, :gpu_*).
        // Only the MAIN process should initialize UI systems, databases, and GeckoRuntime!
        if (!isMainProcess()) {
            return
        }

        val prefs = PreferencesManager.getInstance(this)
        val savedTheme = prefs.getThemeMode()
        applyThemeMode(savedTheme)
        com.azokle.search.core.DnsOverHttpsManager.activeProviderId = prefs.getDohProvider()

        // Initialize Native Rust Azokle Core & Privacy Shield Engine
        com.azokle.search.core.AzokleCore.initialize(this)
        com.azokle.search.adblock.AdBlockStats.init(this)

        // Initialize History, Download & Tab systems
        com.azokle.search.history.HistoryManager.init(this)
        com.azokle.search.downloads.DownloadManager.init(this)
        com.azokle.search.tabs.TabManager.init(this)

        // Automatically track active FragmentActivity for BiometricPrompt window binding
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (activity is FragmentActivity) {
                    com.azokle.search.core.BiometricAuthManager.setCurrentActivity(activity)
                }
            }
            override fun onActivityStarted(activity: Activity) {
                if (activity is FragmentActivity) {
                    com.azokle.search.core.BiometricAuthManager.setCurrentActivity(activity)
                }
            }
            override fun onActivityResumed(activity: Activity) {
                if (activity is FragmentActivity) {
                    com.azokle.search.core.BiometricAuthManager.setCurrentActivity(activity)
                }
            }
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {
                if (activity is FragmentActivity) {
                    com.azokle.search.core.BiometricAuthManager.onActivityDestroyed(activity)
                }
            }
        })
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        if (isMainProcess()) {
            try {
                com.azokle.search.widget.AzokleSearchWidgetProvider.updateAllWidgets(this)
                com.azokle.search.widget.AzokleTorSearchWidgetProvider.updateAllWidgets(this)
                com.azokle.search.widget.AzokleShieldsWidgetProvider.updateAllWidgets(this)
            } catch (e: Exception) {
                // Safe fallback
            }
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (isMainProcess()) {
            com.azokle.search.engine.GeckoEngineManager.onTrimMemory(level)
        }
    }

    private fun isMainProcess(): Boolean {
        return try {
            val currentProcess = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                getProcessName()
            } else {
                val pid = Process.myPid()
                val am = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                am?.runningAppProcesses?.find { it.pid == pid }?.processName ?: ""
            }
            currentProcess == packageName
        } catch (e: Exception) {
            true
        }
    }

    companion object {
        fun applyThemeMode(themeMode: String) {
            when (themeMode.lowercase()) {
                "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }
    }
}
