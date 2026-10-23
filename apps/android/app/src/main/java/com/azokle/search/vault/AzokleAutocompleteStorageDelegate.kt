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

package com.azokle.search.vault

import org.mozilla.geckoview.Autocomplete
import org.mozilla.geckoview.GeckoResult

/**
 * Autocomplete storage delegate bridging Mozilla GeckoView Quantum autofill engine
 * with the encrypted hardware-backed Azokle PasswordVaultManager.
 */
object AzokleAutocompleteStorageDelegate : Autocomplete.StorageDelegate {

    /**
     * Resolves matching logins from the vault for the given domain.
     * Fails closed: never returns all logins on blank/empty domain queries (H-4).
     */
    fun resolveMatchingLogins(domain: String?): List<SavedLogin> {
        if (domain.isNullOrBlank()) return emptyList()
        val host = OriginMatcher.extractHost(domain)
        return if (host.isNotEmpty()) {
            PasswordVaultManager.getLoginsForOrigin(host)
        } else {
            emptyList()
        }
    }

    /**
     * Fetches saved login credentials for the given origin domain.
     */
    override fun onLoginFetch(domain: String): GeckoResult<Array<Autocomplete.LoginEntry>>? {
        if (domain.isBlank()) {
            return GeckoResult.fromValue(emptyArray())
        }
        return try {
            val logins = resolveMatchingLogins(domain)
            val entries = logins.map { login ->
                Autocomplete.LoginEntry.Builder()
                    .guid(login.id)
                    .origin(login.origin)
                    .username(login.username)
                    .password(login.password)
                    .build()
            }.toTypedArray()
            GeckoResult.fromValue(entries)
        } catch (e: Exception) {
            android.util.Log.w("AzokleAutofill", "Error fetching logins for domain $domain: ${e.message}")
            GeckoResult.fromValue(emptyArray())
        }
    }

    /**
     * AZK-VULN-17: Least-privilege credential protection.
     * Unbounded zero-argument login fetch is deliberately disabled to prevent leaking the entire
     * decrypted password vault into GeckoView engine memory or exposing it across domains.
     * Login retrieval is strictly restricted to origin-scoped onLoginFetch(domain).
     */
    override fun onLoginFetch(): GeckoResult<Array<Autocomplete.LoginEntry>>? {
        return GeckoResult.fromValue(emptyArray())
    }

    /**
     * Saves or updates a login entry from the Gecko autocomplete pipeline.
     */
    override fun onLoginSave(entry: Autocomplete.LoginEntry) {
        try {
            val origin = entry.origin
            val user = entry.username
            val pass = entry.password
            if (origin.isNotBlank() && user.isNotBlank() && pass.isNotBlank()) {
                PasswordVaultManager.saveLogin(origin, user, pass)
            }
        } catch (e: Exception) {
            android.util.Log.w("AzokleAutofill", "Error saving autocomplete login entry: ${e.message}")
        }
    }

    /**
     * Receives notification when a saved login entry was filled into a web form.
     */
    override fun onLoginUsed(entry: Autocomplete.LoginEntry, field: Int) {
        // No-op for privacy: we do not log login usage timestamps or web form tracking telemetry
    }
}
