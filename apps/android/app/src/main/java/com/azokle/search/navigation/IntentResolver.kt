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

package com.azokle.search.navigation

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build

/**
 * Resolves Intent handlers dynamically using Android PackageManager, avoiding hardcoded apps.
 */
object IntentResolver {

    /**
     * Queries matching activities for an intent, filtering out Azokle Browser itself.
     */
    fun resolveHandlers(context: Context, intent: Intent): List<ResolveInfo> {
        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PackageManager.MATCH_DEFAULT_ONLY
        } else {
            0
        }

        val allHandlers = try {
            pm.queryIntentActivities(intent, flags)
        } catch (e: Exception) {
            emptyList()
        }

        val ownPackage = context.packageName
        return allHandlers.filter { info ->
            info.activityInfo?.packageName != ownPackage
        }
    }

    /**
     * Checks if any external application can handle the given intent.
     */
    fun canHandle(context: Context, intent: Intent): Boolean {
        val handlers = resolveHandlers(context, intent)
        return handlers.isNotEmpty()
    }

    /**
     * Resolves whether an HTTP/HTTPS Intent has a dedicated, non-browser native app handler
     * (e.g. YouTube app, Google Play Store app, Reddit, Instagram, etc.).
     */
    fun hasNativeAppHandler(context: Context, intent: Intent): Boolean {
        val handlers = resolveHandlers(context, intent)
        if (handlers.isEmpty()) return false
        val ownPackage = context.packageName
        val nonBrowserHandlers = handlers.filter { info ->
            val pkg = info.activityInfo?.packageName ?: return@filter false
            pkg != ownPackage && !isBrowserPackage(context, pkg)
        }
        return nonBrowserHandlers.isNotEmpty()
    }

    /**
     * Resolves the single best handler or returns null if no handler or ambiguous.
     */
    fun resolveSingleHandler(context: Context, intent: Intent): ResolveInfo? {
        val handlers = resolveHandlers(context, intent)
        return if (handlers.size == 1) handlers.first() else null
    }

    /**
     * Checks if a target package belongs to a web browser rather than a native app.
     */
    fun isBrowserPackage(context: Context, packageName: String): Boolean {
        if (packageName == context.packageName) return true
        val browserIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://azokle.com")).apply {
            `package` = packageName
        }
        val pm = context.packageManager
        val matches = try {
            pm.queryIntentActivities(browserIntent, 0)
        } catch (e: Exception) {
            emptyList()
        }
        return matches.isNotEmpty()
    }
}
