package com.azokle.search.util

import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher

object DefaultBrowserManager {

    /**
     * Unwraps any ContextWrapper hierarchy to retrieve the underlying Activity instance.
     */
    fun unwrapActivity(context: Context): Activity? {
        var current = context
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext ?: break
        }
        return null
    }

    /**
     * Checks whether Azokle Search is currently the system's default browser.
     */
    fun isDefaultBrowser(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = context.getSystemService(RoleManager::class.java)
                roleManager?.isRoleHeld(RoleManager.ROLE_BROWSER) == true
            } else {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://azokle.com"))
                val resolveInfo = context.packageManager.resolveActivity(
                    intent,
                    PackageManager.MATCH_DEFAULT_ONLY
                )
                resolveInfo?.activityInfo?.packageName == context.packageName
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Launches the official Android system interface to set Azokle as the default browser.
     * Uses RoleManager via ActivityResultLauncher on Android 10+ with automatic multi-tier fallbacks.
     */
    fun launchSetDefaultBrowser(
        context: Context,
        launcher: ActivityResultLauncher<Intent>? = null
    ) {
        try {
            // Tier 1: Android 10+ (API 29+) RoleManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = context.getSystemService(RoleManager::class.java)
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_BROWSER)) {
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_BROWSER)
                    if (launcher != null) {
                        launcher.launch(intent)
                        return
                    } else {
                        val activity = unwrapActivity(context)
                        if (activity != null) {
                            activity.startActivity(intent)
                            return
                        }
                    }
                }
            }

            // Tier 2: Fallback to System Default Apps Settings (Android 7.0+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    val manageIntent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    if (launcher != null) {
                        launcher.launch(manageIntent)
                        return
                    } else {
                        context.startActivity(manageIntent)
                        return
                    }
                } catch (e: Exception) {}
            }

            // Tier 3: Fallback to Application Details Settings (Direct App Info)
            val appDetailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (launcher != null) {
                launcher.launch(appDetailsIntent)
            } else {
                context.startActivity(appDetailsIntent)
            }
        } catch (e: Exception) {
            // Tier 4: Generic System Settings
            try {
                val settingsIntent = Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(settingsIntent)
            } catch (e2: Exception) {}
        }
    }

    /**
     * Direct intent to open Android System Default Apps configuration.
     */
    fun createManageDefaultAppsIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
    }
}
