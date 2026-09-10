package com.azokle.search.navigation

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build

/**
 * Manages Android App Links (HTTP/HTTPS links with associated native apps).
 */
object AppLinkHandler {

    /**
     * Finds native non-browser applications that handle this HTTP/HTTPS URL.
     */
    fun getNativeAppHandlers(context: Context, url: String): List<String> {
        val uri = try { Uri.parse(url) } catch (e: Exception) { return emptyList() }
        val scheme = uri.scheme?.lowercase() ?: return emptyList()
        if (scheme != "http" && scheme != "https") return emptyList()

        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }

        val allHandlers = IntentResolver.resolveHandlers(context, intent)
        return allHandlers
            .mapNotNull { it.activityInfo?.packageName }
            .filter { pkg -> !IntentResolver.isBrowserPackage(context, pkg) }
    }

    /**
     * Checks if a specialized native app (not a generic browser) exists for this URL.
     */
    fun hasSpecializedNativeHandler(context: Context, url: String): Boolean {
        return getNativeAppHandlers(context, url).isNotEmpty()
    }

    /**
     * Launches the specialized native app handler for an App Link URL.
     */
    fun openInNativeApp(context: Context, url: String): Boolean {
        val uri = try { Uri.parse(url) } catch (e: Exception) { return false }
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            addCategory(Intent.CATEGORY_BROWSABLE)
        }

        val nonBrowserHandlers = getNativeAppHandlers(context, url)
        if (nonBrowserHandlers.isEmpty()) return false

        if (nonBrowserHandlers.size == 1) {
            intent.`package` = nonBrowserHandlers.first()
        }

        return ExternalAppLauncher.launch(
            context = context,
            intent = intent,
            showToastOnError = false
        ) == ExternalAppLauncher.LaunchResult.SUCCESS
    }
}
