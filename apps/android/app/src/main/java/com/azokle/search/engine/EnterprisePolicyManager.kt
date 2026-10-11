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

package com.azokle.search.engine

import android.content.Context
import android.os.Bundle
import android.os.UserManager
import android.util.Log
import org.json.JSONObject

/**
 * Enterprise Policy Manager for Azokle Browser.
 * Reads policies from Android Enterprise Managed Configurations (MDM)
 * and optional deployment asset `assets/policies.json`.
 */
object EnterprisePolicyManager {

    private const val TAG = "EnterprisePolicy"

    private var isManaged = false
    private var disablePrivateBrowsing = false
    private var enforcedDohProvider: String? = null
    private var blockedDomains: Set<String> = emptySet()
    private var enforcedHomeUrl: String? = null

    /**
     * Initializes .
     */
    fun init(context: Context) {
        try {
            // 1. Android Enterprise Restrictions (MDM)
            val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager
            val restrictions: Bundle? = userManager?.getApplicationRestrictions(context.packageName)

            if (restrictions != null && !restrictions.isEmpty) {
                isManaged = true
                disablePrivateBrowsing = restrictions.getBoolean("disable_private_browsing", false)
                enforcedDohProvider = restrictions.getString("enforce_doh_provider")
                enforcedHomeUrl = restrictions.getString("enforced_homepage")
                val blockedArr = restrictions.getStringArray("blocked_domains")
                if (!blockedArr.isNullOrEmpty()) {
                    blockedDomains = blockedArr.toSet()
                }
                Log.i(TAG, "Loaded Android Enterprise MDM configuration.")
                return
            }

            // 2. Deployment Asset fallback (assets/policies.json)
            val assetManager = context.assets
            val hasPolicyAsset = try {
                assetManager.list("")?.contains("policies.json") == true
            } catch (_: Exception) { false }

            if (hasPolicyAsset) {
                val jsonStr = assetManager.open("policies.json").bufferedReader().use { it.readText() }
                val root = JSONObject(jsonStr)
                if (root.has("policies")) {
                    val p = root.getJSONObject("policies")
                    isManaged = true
                    disablePrivateBrowsing = p.optBoolean("DisablePrivateBrowsing", false)
                    enforcedDohProvider = p.optString("EnforceDohProvider").takeIf { it.isNotBlank() }
                    enforcedHomeUrl = p.optString("Homepage").takeIf { it.isNotBlank() }

                    if (p.has("BlocklistedDomains")) {
                        val arr = p.getJSONArray("BlocklistedDomains")
                        val list = mutableSetOf<String>()
                        for (i in 0 until arr.length()) {
                            list.add(arr.getString(i).lowercase())
                        }
                        blockedDomains = list
                    }
                    Log.i(TAG, "Loaded policies.json deployment asset.")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error evaluating enterprise policies: ${e.message}")
        }
    }

    /**
     * Checks whether enterprise managed.
     */
    fun isEnterpriseManaged(): Boolean = isManaged
    /**
     * Checks whether private browsing disabled.
     */
    fun isPrivateBrowsingDisabled(): Boolean = disablePrivateBrowsing
    /**
     * Retrieves the enforced doh.
     */
    fun getEnforcedDoh(): String? = enforcedDohProvider
    /**
     * Retrieves the enforced home url.
     */
    fun getEnforcedHomeUrl(): String? = enforcedHomeUrl
    /**
     * Checks whether domain blocked by policy.
     */
    fun isDomainBlockedByPolicy(host: String): Boolean {
        if (blockedDomains.isEmpty() || host.isBlank()) return false
        val lowerHost = host.lowercase()
        return blockedDomains.any { lowerHost == it || lowerHost.endsWith(".$it") }
    }
}
