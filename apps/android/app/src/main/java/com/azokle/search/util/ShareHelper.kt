package com.azokle.search.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import com.azokle.search.ui.components.ToastManager

/**
 * Resolved share target application installed on the device.
 */
data class ShareTargetApp(
    val packageName: String,
    val activityName: String,
    val label: String,
    val icon: Drawable,
    val launchIntent: Intent
)

/**
 * Resilient, multi-context safe Share & Intent Dispatcher.
 * Guarantees crash-free system share sheet presentation across all Context types
 * (ContextWrapper, Activity, Application, ContextImpl).
 */
object ShareHelper {

    private const val TAG = "ShareHelper"

    /**
     * Builds a standard plain-text share Intent.
     */
    fun createShareIntent(text: String, title: String? = null): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            title?.takeIf { it.isNotBlank() }?.let {
                putExtra(Intent.EXTRA_SUBJECT, it)
                putExtra(Intent.EXTRA_TITLE, it)
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    /**
     * Safely launches the Android System Share Chooser without crashing on non-Activity contexts.
     */
    fun launchSystemShare(
        context: Context,
        text: String,
        title: String? = null,
        chooserTitle: String = "Share Webpage"
    ) {
        try {
            val shareIntent = createShareIntent(text, title)
            val chooser = Intent.createChooser(shareIntent, chooserTitle).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            val activity = context.findActivity()
            if (activity != null && !activity.isFinishing && !activity.isDestroyed) {
                activity.startActivity(chooser)
            } else {
                context.startActivity(chooser)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch system share sheet", e)
            ToastManager.show("Unable to open share sheet")
        }
    }

    /**
     * Launches a specific installed share target application directly.
     */
    fun launchDirectAppShare(
        context: Context,
        targetApp: ShareTargetApp
    ) {
        try {
            val activity = context.findActivity()
            if (activity != null && !activity.isFinishing && !activity.isDestroyed) {
                activity.startActivity(targetApp.launchIntent)
            } else {
                context.startActivity(targetApp.launchIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch direct share target ${targetApp.packageName}", e)
            // Fallback to system share
            launchSystemShare(context, targetApp.launchIntent.getStringExtra(Intent.EXTRA_TEXT) ?: "")
        }
    }

    /**
     * Queries the device PackageManager for installed applications capable of receiving text shares.
     * Filters out Azokle itself and orders common messaging apps first.
     */
    fun getInstalledShareApps(
        context: Context,
        text: String,
        title: String? = null,
        limit: Int = 6
    ): List<ShareTargetApp> {
        val shareIntent = createShareIntent(text, title)
        val pm = context.packageManager
        val resolvedActivities = try {
            pm.queryIntentActivities(shareIntent, 0)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to query share intent activities", e)
            return emptyList()
        }

        val myPackage = context.packageName

        val priorityPackages = listOf(
            "com.whatsapp",
            "org.telegram.messenger",
            "com.google.android.gm",
            "com.google.android.apps.messaging",
            "com.twitter.android",
            "com.instagram.android",
            "com.facebook.orca",
            "com.linkedin.android",
            "com.discord",
            "com.slack"
        )

        return resolvedActivities
            .filter { it.activityInfo != null && it.activityInfo.packageName != myPackage }
            .map { resolveInfo ->
                val pkg = resolveInfo.activityInfo.packageName
                val cls = resolveInfo.activityInfo.name
                val appLabel = resolveInfo.loadLabel(pm).toString()
                val appIcon = resolveInfo.loadIcon(pm)

                val directIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                    title?.takeIf { it.isNotBlank() }?.let {
                        putExtra(Intent.EXTRA_SUBJECT, it)
                    }
                    component = ComponentName(pkg, cls)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }

                ShareTargetApp(
                    packageName = pkg,
                    activityName = cls,
                    label = appLabel,
                    icon = appIcon,
                    launchIntent = directIntent
                )
            }
            .distinctBy { it.packageName }
            .sortedWith(compareBy<ShareTargetApp> { target ->
                val idx = priorityPackages.indexOf(target.packageName)
                if (idx != -1) idx else 999
            }.thenBy { it.label })
            .take(limit)
    }

    /**
     * Copies the given text to clipboard with haptic feedback and user feedback toast.
     */
    fun copyToClipboard(context: Context, text: String, successMessage: String = "Link copied to clipboard") {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText("URL", text)
            clipboard?.setPrimaryClip(clip)
            ToastManager.showCopy(successMessage)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy text to clipboard", e)
        }
    }

    /**
     * Opens the target URL in an external app/browser.
     */
    fun openInExternalBrowser(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val activity = context.findActivity()
            if (activity != null && !activity.isFinishing && !activity.isDestroyed) {
                activity.startActivity(intent)
            } else {
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open in external browser", e)
            ToastManager.show("No external browser available")
        }
    }
}
