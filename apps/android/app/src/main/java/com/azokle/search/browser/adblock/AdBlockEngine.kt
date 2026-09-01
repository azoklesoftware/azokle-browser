package com.azokle.search.browser.adblock

import android.content.Context
import com.azokle.search.core.AzokleCore
import com.azokle.search.core.FilterDecision
import java.util.concurrent.ConcurrentHashMap

/**
 * Sovereign Ad Block & Shield Manager backed by high-speed Rust Core.
 */
class AdBlockEngine private constructor() {
    private val disabledHosts = ConcurrentHashMap.newKeySet<String>()

    companion object {
        val instance: AdBlockEngine by lazy { AdBlockEngine() }
    }

    fun initialize(context: Context) {
        AzokleCore.initialize(context)
    }

    fun isShieldEnabled(host: String): Boolean {
        val cleanHost = host.lowercase().trim()
        if (disabledHosts.contains(cleanHost)) return false
        return AzokleCore.isShieldActiveForHost(cleanHost)
    }

    fun getDisabledHostsList(): List<String> = disabledHosts.toList()

    fun toggleShield(host: String): Boolean {
        val cleanHost = host.lowercase().trim()
        val newState = if (disabledHosts.contains(cleanHost)) {
            disabledHosts.remove(cleanHost)
            true
        } else {
            disabledHosts.add(cleanHost)
            false
        }
        AzokleCore.toggleShieldForHost(cleanHost, newState)
        return newState
    }

    fun checkUrl(url: String, sourceOrigin: String? = null, isThirdParty: Boolean = true): FilterDecision {
        return AzokleCore.shouldBlock(url, sourceOrigin, isThirdParty)
    }

    fun cleanUrl(url: String): String {
        return AzokleCore.cleanTrackingParameters(url)
    }

    fun getCosmeticCss(domain: String): String {
        return AzokleCore.getCosmeticStyles(domain)
    }

    fun getScriptlets(domain: String): String {
        return AzokleCore.getDefuserScriptlet(domain)
    }
}
