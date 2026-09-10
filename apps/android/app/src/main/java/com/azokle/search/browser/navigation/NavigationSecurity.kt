package com.azokle.search.browser.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Hardens navigation against intent injection, scheme hijacking, malicious redirects,
 * privilege escalation, and Tor/Private mode clearnet leakage.
 */
object NavigationSecurity {

    // Dangerous intent flags that allow granting file/content access or persistent permissions
    const val DANGEROUS_FLAGS_MASK = (
        Intent.FLAG_GRANT_READ_URI_PERMISSION or
        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
        Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
    )

    val ALLOWED_ACTIONS = setOf(
        Intent.ACTION_VIEW,
        Intent.ACTION_DIAL,
        Intent.ACTION_SENDTO,
        Intent.ACTION_SEND,
        Intent.ACTION_WEB_SEARCH,
        Intent.ACTION_MAIN
    )

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
     * and component targeting.
     */
    fun sanitizeIntent(intent: Intent): Intent {
        intent.action = sanitizeAction(intent.action)
        intent.flags = sanitizeFlags(intent.flags)
        intent.selector = null
        intent.component = null
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
     */
    fun isGestureAllowed(hasUserGesture: Boolean, targetWindow: Int): Boolean {
        return hasUserGesture || targetWindow == 0 // TARGET_WINDOW_NONE
    }

    /**
     * Returns true if external navigation would break Tor mode anonymity.
     */
    fun isTorIsolationRisk(isTor: Boolean): Boolean {
        return isTor
    }

    /**
     * Rejects local and content URIs from untrusted web contexts.
     */
    fun isForbiddenWebScheme(scheme: String?): Boolean {
        if (scheme.isNullOrBlank()) return false
        val s = scheme.lowercase()
        return s in setOf("file", "content", "chrome", "resource")
    }
}
