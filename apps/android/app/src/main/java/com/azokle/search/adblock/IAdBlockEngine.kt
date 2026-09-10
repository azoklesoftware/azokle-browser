package com.azokle.search.adblock

import android.content.Context
import com.azokle.search.core.FilterDecision

/**
 * Public Contract Interface for AdBlock & Shield Engine.
 * Follows DuckDuckGo's -api pattern to decouple consumers from concrete implementations.
 */
interface IAdBlockEngine {
    fun initialize(context: Context)
    fun isShieldEnabled(host: String): Boolean
    fun getDisabledHostsList(): List<String>
    fun toggleShield(host: String): Boolean
    fun checkUrl(url: String, sourceOrigin: String? = null, isThirdParty: Boolean = true): FilterDecision
    fun cleanUrl(url: String): String
    fun getCosmeticCss(domain: String): String
    fun getScriptlets(domain: String): String
}
