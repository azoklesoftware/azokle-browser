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

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class AzokleApplication : Application(), ImageLoaderFactory {
    /**
     * Executes the new image loader operation.
     */
    override fun newImageLoader(): ImageLoader {
        val safeOkHttpClient = OkHttpClient.Builder()
            .dns(com.azokle.search.downloads.DownloadManager.ClearnetSafeDns)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(safeOkHttpClient)
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


    /**
     * Initializes dependencies and components when created.
     */
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

        // Initialize History, Download, Tab & Password Vault systems
        com.azokle.search.history.HistoryManager.init(this)
        com.azokle.search.downloads.DownloadManager.init(this)
        com.azokle.search.tabs.TabManager.init(this)
        com.azokle.search.vault.PasswordVaultManager.init(this)
        com.azokle.search.engine.EnterprisePolicyManager.init(this)

        // Automatically track active FragmentActivity for BiometricPrompt window binding
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            /**
             * Handles on activity created events.
             */
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (activity is FragmentActivity) {
                    com.azokle.search.core.BiometricAuthManager.setCurrentActivity(activity)
                }
            }
            /**
             * Handles on activity started events.
             */
            override fun onActivityStarted(activity: Activity) {
                if (activity is FragmentActivity) {
                    com.azokle.search.core.BiometricAuthManager.setCurrentActivity(activity)
                }
            }
            /**
             * Handles on activity resumed events.
             */
            override fun onActivityResumed(activity: Activity) {
                if (activity is FragmentActivity) {
                    com.azokle.search.core.BiometricAuthManager.setCurrentActivity(activity)
                }
            }
            /**
             * Handles on activity paused events.
             */
            override fun onActivityPaused(activity: Activity) {}
            /**
             * Handles on activity stopped events.
             */
            override fun onActivityStopped(activity: Activity) {}
            /**
             * Handles on activity save instance state events.
             */
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            /**
             * Handles on activity destroyed events.
             */
            override fun onActivityDestroyed(activity: Activity) {
                if (activity is FragmentActivity) {
                    com.azokle.search.core.BiometricAuthManager.onActivityDestroyed(activity)
                }
            }
        })
    }

    /**
     * Handles on configuration changed events.
     */
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

    /**
     * Handles on trim memory events.
     */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (isMainProcess()) {
            com.azokle.search.engine.GeckoEngineManager.onTrimMemory(level)
        }
    }

    /**
     * Checks whether main process.
     */
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
        /**
         * Executes the apply theme mode operation.
         */
        fun applyThemeMode(themeMode: String) {
            when (themeMode.lowercase()) {
                "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }
    }
}
