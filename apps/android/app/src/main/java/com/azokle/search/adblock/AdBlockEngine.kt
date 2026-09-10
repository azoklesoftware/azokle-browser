package com.azokle.search.adblock

import android.content.Context
import com.azokle.search.core.AzokleCore
import com.azokle.search.core.FilterDecision
import java.util.concurrent.ConcurrentHashMap

/**
 * Granular state of Azokle Shield protection on a specific host.
 */
sealed interface ShieldHostState {
    data object Enabled : ShieldHostState
    data object DisabledUntilRelaunch : ShieldHostState
    data object DisabledPermanently : ShieldHostState
}

/**
 * Sovereign Ad Block & Shield Manager backed by high-speed Rust Core.
 * Supports permanent site allowlisting and session-temporary pausing ("Until Relaunch").
 */
class AdBlockEngine private constructor() {
    private val permanentDisabledHosts = ConcurrentHashMap.newKeySet<String>()
    private val sessionDisabledHosts = ConcurrentHashMap.newKeySet<String>()

    companion object {
        val instance: AdBlockEngine by lazy { AdBlockEngine() }
    }

    fun initialize(context: Context) {
        AzokleCore.initialize(context)
    }

    fun getShieldState(host: String): ShieldHostState {
        val cleanHost = host.lowercase().trim()
        return when {
            sessionDisabledHosts.contains(cleanHost) -> ShieldHostState.DisabledUntilRelaunch
            permanentDisabledHosts.contains(cleanHost) -> ShieldHostState.DisabledPermanently
            !AzokleCore.isShieldActiveForHost(cleanHost) -> ShieldHostState.DisabledPermanently
            else -> ShieldHostState.Enabled
        }
    }

    fun isShieldEnabled(host: String): Boolean {
        val cleanHost = host.lowercase().trim()
        if (sessionDisabledHosts.contains(cleanHost) || permanentDisabledHosts.contains(cleanHost)) {
            return false
        }
        return AzokleCore.isShieldActiveForHost(cleanHost)
    }

    fun isShieldPausedUntilRelaunch(host: String): Boolean {
        return sessionDisabledHosts.contains(host.lowercase().trim())
    }

    fun getDisabledHostsList(): List<String> {
        val all = HashSet<String>()
        all.addAll(permanentDisabledHosts)
        all.addAll(sessionDisabledHosts)
        return all.toList()
    }

    fun enableShield(host: String) {
        val cleanHost = host.lowercase().trim()
        sessionDisabledHosts.remove(cleanHost)
        permanentDisabledHosts.remove(cleanHost)
        AzokleCore.toggleShieldForHost(cleanHost, true)
        com.azokle.search.engine.GeckoEngineManager.notifyDisabledHostsChanged(getDisabledHostsList())
    }

    fun pauseShieldUntilRelaunch(host: String) {
        val cleanHost = host.lowercase().trim()
        permanentDisabledHosts.remove(cleanHost)
        sessionDisabledHosts.add(cleanHost)
        com.azokle.search.engine.GeckoEngineManager.notifyDisabledHostsChanged(getDisabledHostsList())
    }

    fun disableShieldPermanently(host: String) {
        val cleanHost = host.lowercase().trim()
        sessionDisabledHosts.remove(cleanHost)
        permanentDisabledHosts.add(cleanHost)
        AzokleCore.toggleShieldForHost(cleanHost, false)
        com.azokle.search.engine.GeckoEngineManager.notifyDisabledHostsChanged(getDisabledHostsList())
    }

    fun toggleShield(host: String): Boolean {
        val cleanHost = host.lowercase().trim()
        val currentlyActive = isShieldEnabled(cleanHost)
        if (currentlyActive) {
            disableShieldPermanently(cleanHost)
            return false
        } else {
            enableShield(cleanHost)
            return true
        }
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
