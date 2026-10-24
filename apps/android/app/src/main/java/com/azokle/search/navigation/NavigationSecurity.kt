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
import android.net.Uri

/**
 * Hardens navigation against intent injection, scheme hijacking, malicious redirects,
 * privilege escalation, and Tor/Private mode clearnet leakage.
 */
object NavigationSecurity {

    /**
     * Bitmask of dangerous intent flags that grant persistent or transient file/content URI permissions.
     * These flags are stripped from untrusted web-dispatched intents to prevent sandbox access theft.
     */
    const val DANGEROUS_FLAGS_MASK = (
        Intent.FLAG_GRANT_READ_URI_PERMISSION or
        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
        Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
    )

    /**
     * Whitelist of safe, standard Android intent actions permissible for web content to launch.
     * Note: ACTION_SEND and ACTION_MAIN are excluded to prevent confused-deputy share dialog attacks (H-3).
     */
    val ALLOWED_ACTIONS = setOf(
        Intent.ACTION_VIEW,
        Intent.ACTION_DIAL,
        Intent.ACTION_SENDTO,
        Intent.ACTION_WEB_SEARCH
    )

    /**
     * Sensitive system action prefixes that untrusted web contexts are strictly forbidden from broadcasting.
     */
    val FORBIDDEN_ACTION_PREFIXES = setOf(
        "android.intent.action.PACKAGE_",
        "android.intent.action.BOOT_",
        "android.intent.action.SHUTDOWN",
        "android.intent.action.REBOOT",
        "android.intent.action.MASTER_CLEAR",
        "android.intent.action.AIRPLANE_MODE",
        "android.intent.action.BATTERY_",
        "android.intent.action.DEVICE_INITIALIZATION"
    )

    /**
     * Sanitizes integer flags by stripping dangerous permission flags
     * and ensuring FLAG_ACTIVITY_NEW_TASK is present.
     */
    fun sanitizeFlags(flags: Int): Int {
        var f = flags and DANGEROUS_FLAGS_MASK.inv()
        f = f or Intent.FLAG_ACTIVITY_NEW_TASK
        return f
    }

    /**
     * Sanitizes action string, replacing forbidden system actions with ACTION_VIEW.
     */
    fun sanitizeAction(action: String?): String {
        if (action.isNullOrBlank()) return Intent.ACTION_VIEW
        return if (isForbiddenAction(action) || action !in ALLOWED_ACTIONS) {
            Intent.ACTION_VIEW
        } else {
            action
        }
    }

    /**
     * Sanitizes an Intent to eliminate injection attacks, privilege escalation,
     * and internal component targeting.
     *
     * Neutralizes intent selectors, explicit component destinations, and clip data.
     * Forces CATEGORY_BROWSABLE, strips all extras, and strictly prevents self-targeting (H-3).
     */
    fun sanitizeIntent(intent: Intent, context: Context? = null): Intent {
        intent.action = sanitizeAction(intent.action)
        intent.flags = sanitizeFlags(intent.flags)
        intent.addCategory(Intent.CATEGORY_BROWSABLE)
        intent.selector = null
        intent.component = null
        intent.clipData = null
        intent.replaceExtras(null as android.os.Bundle?)

        val appPkg = context?.packageName ?: "com.azokle.search"
        if (intent.`package` != null && (intent.`package`.equals(appPkg, ignoreCase = true) || intent.`package`.equals("com.azokle.browser", ignoreCase = true))) {
            intent.`package` = null
            intent.data = null
        }

        val dataScheme = intent.data?.scheme?.lowercase()
        if (dataScheme != null && isForbiddenWebScheme(dataScheme)) {
            intent.data = null
        }

        return intent
    }

    /**
     * Checks if the intent action is forbidden or dangerous for a web browser to dispatch.
     */
    fun isForbiddenAction(action: String?): Boolean {
        if (action.isNullOrBlank()) return false
        val lower = action.lowercase()
        for (prefix in FORBIDDEN_ACTION_PREFIXES) {
            if (lower.startsWith(prefix.lowercase())) return true
        }
        return false
    }

    /**
     * Verifies whether a navigation request is safe to execute based on user gesture.
     * AZK-MED-03: Strictly require hasUserGesture to prevent unprompted background external dispatches.
     */
    fun isGestureAllowed(hasUserGesture: Boolean, targetWindow: Int): Boolean {
        return hasUserGesture
    }

    /**
     * Returns true if dispatching navigation to an external native Android app would break Tor anonymity.
     *
     * External applications operate over the standard device network stack (clearnet),
     * completely bypassing the browser's Tor proxy tunnel and leaking the user's true IP.
     */
    fun isTorIsolationRisk(isTor: Boolean): Boolean {
        return isTor
    }

    /**
     * Identifies local and privileged URI schemes (`file`, `content`, `chrome`, `resource`, `jar`)
     * that untrusted web contexts are strictly prohibited from loading.
     */
    fun isForbiddenWebScheme(scheme: String?): Boolean {
        if (scheme.isNullOrBlank()) return false
        val s = scheme.lowercase()
        return s in setOf("file", "content", "chrome", "resource", "jar", "javascript", "view-source")
    }

    /**
     * Determines whether a package or component destination represents an attempt
     * to self-target the browser's own internal application components (AZK-BUG-07, H-3).
     */
    fun isSelfTargetingPackage(targetPkg: String?, appPkg: String = "com.azokle.search"): Boolean {
        if (targetPkg.isNullOrBlank()) return false
        val pkg = targetPkg.trim()
        return pkg.equals(appPkg, ignoreCase = true) ||
               pkg.equals("com.azokle.search", ignoreCase = true) ||
               pkg.equals("com.azokle.browser", ignoreCase = true)
    }
}
