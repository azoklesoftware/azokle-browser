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
import com.azokle.search.data.PreferencesManager

class AzokleApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Critical: GeckoView runs multiple isolated child processes (e.g. :tab_*, :gpu_*).
        // Only the MAIN process should initialize UI systems, databases, and GeckoRuntime!
        if (!isMainProcess()) {
            return
        }

        val prefs = PreferencesManager(this)
        val savedTheme = prefs.getThemeMode()
        applyThemeMode(savedTheme)

        // Initialize Native Rust Azokle Core & Privacy Shield Engine
        com.azokle.search.core.AzokleCore.initialize(this)

        // Initialize History, Download & Tab systems
        com.azokle.search.browser.history.HistoryManager.init(this)
        com.azokle.search.browser.downloads.DownloadManager.init(this)
        com.azokle.search.browser.tabs.TabManager.init(this)

        // Automatically track active FragmentActivity for BiometricPrompt window binding
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (activity is FragmentActivity) {
                    com.azokle.search.security.BiometricAuthManager.setCurrentActivity(activity)
                }
            }
            override fun onActivityStarted(activity: Activity) {
                if (activity is FragmentActivity) {
                    com.azokle.search.security.BiometricAuthManager.setCurrentActivity(activity)
                }
            }
            override fun onActivityResumed(activity: Activity) {
                if (activity is FragmentActivity) {
                    com.azokle.search.security.BiometricAuthManager.setCurrentActivity(activity)
                }
            }
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {
                if (activity is FragmentActivity) {
                    com.azokle.search.security.BiometricAuthManager.onActivityDestroyed(activity)
                }
            }
        })

        // Pre-warm GeckoView Quantum Engine & WebExtensions on background thread in main process only
        Thread {
            try {
                com.azokle.search.browser.gecko.GeckoEngineManager.getRuntime(this)
            } catch (e: Throwable) {
                // Safe fallback
            }
        }.start()
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
