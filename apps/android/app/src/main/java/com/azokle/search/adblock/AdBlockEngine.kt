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

    /**
     * Initializes ialize.
     */
    fun initialize(context: Context) {
        AzokleCore.initialize(context)
    }

    /**
     * Retrieves the shield state.
     */
    fun getShieldState(host: String): ShieldHostState {
        val cleanHost = host.lowercase().trim()
        return when {
            sessionDisabledHosts.contains(cleanHost) -> ShieldHostState.DisabledUntilRelaunch
            permanentDisabledHosts.contains(cleanHost) -> ShieldHostState.DisabledPermanently
            !AzokleCore.isShieldActiveForHost(cleanHost) -> ShieldHostState.DisabledPermanently
            else -> ShieldHostState.Enabled
        }
    }

    /**
     * Checks whether shield enabled.
     */
    fun isShieldEnabled(host: String): Boolean {
        val cleanHost = host.lowercase().trim()
        if (sessionDisabledHosts.contains(cleanHost) || permanentDisabledHosts.contains(cleanHost)) {
            return false
        }
        return AzokleCore.isShieldActiveForHost(cleanHost)
    }

    /**
     * Checks whether shield paused until relaunch.
     */
    fun isShieldPausedUntilRelaunch(host: String): Boolean {
        return sessionDisabledHosts.contains(host.lowercase().trim())
    }

    /**
     * Retrieves the disabled hosts list.
     */
    fun getDisabledHostsList(): List<String> {
        val all = HashSet<String>()
        all.addAll(permanentDisabledHosts)
        all.addAll(sessionDisabledHosts)
        return all.toList()
    }

    /**
     * Executes the enable shield operation.
     */
    fun enableShield(host: String) {
        val cleanHost = host.lowercase().trim()
        sessionDisabledHosts.remove(cleanHost)
        permanentDisabledHosts.remove(cleanHost)
        AzokleCore.toggleShieldForHost(cleanHost, true)
        com.azokle.search.engine.GeckoEngineManager.notifyDisabledHostsChanged(getDisabledHostsList())
    }

    /**
     * Executes the pause shield until relaunch operation.
     */
    fun pauseShieldUntilRelaunch(host: String) {
        val cleanHost = host.lowercase().trim()
        permanentDisabledHosts.remove(cleanHost)
        sessionDisabledHosts.add(cleanHost)
        com.azokle.search.engine.GeckoEngineManager.notifyDisabledHostsChanged(getDisabledHostsList())
    }

    /**
     * Executes the disable shield permanently operation.
     */
    fun disableShieldPermanently(host: String) {
        val cleanHost = host.lowercase().trim()
        sessionDisabledHosts.remove(cleanHost)
        permanentDisabledHosts.add(cleanHost)
        AzokleCore.toggleShieldForHost(cleanHost, false)
        com.azokle.search.engine.GeckoEngineManager.notifyDisabledHostsChanged(getDisabledHostsList())
    }

    /**
     * Toggles the state of shield.
     */
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

    /**
     * Validates check url according to security policies.
     */
    fun checkUrl(url: String, sourceOrigin: String? = null, requestType: String? = null, isThirdParty: Boolean = true): FilterDecision {
        return AzokleCore.shouldBlock(url, sourceOrigin, requestType, isThirdParty)
    }

    /**
     * Executes the clean url operation.
     */
    fun cleanUrl(url: String): String {
        return AzokleCore.cleanTrackingParameters(url)
    }

    /**
     * Retrieves the cosmetic css.
     */
    fun getCosmeticCss(domain: String): String {
        return AzokleCore.getCosmeticStyles(domain)
    }

    /**
     * Retrieves the scriptlets.
     */
    fun getScriptlets(domain: String): String {
        return AzokleCore.getDefuserScriptlet(domain)
    }
}
