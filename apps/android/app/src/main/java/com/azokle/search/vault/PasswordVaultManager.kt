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

@file:Suppress("DEPRECATION")

package com.azokle.search.vault

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class SavedLogin(
    val id: String = UUID.randomUUID().toString(),
    val origin: String,
    val username: String,
    val password: String,
    val dateModified: Long = System.currentTimeMillis()
)

/**
 * Enterprise Public Suffix and Host isolation utilities.
 * Hardened against CWE-346 (Origin Confusion) and credential autofill phishing (H-4).
 */
object OriginMatcher {

    val MULTI_PART_PUBLIC_SUFFIXES = setOf(
        // Turkey
        "com.tr", "org.tr", "net.tr", "edu.tr", "gov.tr", "info.tr", "biz.tr", "tv.tr",
        // Korea
        "co.kr", "ne.kr", "or.kr", "re.kr", "pe.kr", "go.kr", "mil.kr", "ac.kr", "hs.kr",
        // UK
        "co.uk", "org.uk", "me.uk", "gov.uk", "ac.uk", "ltd.uk", "plc.uk", "net.uk", "sch.uk",
        // Australia
        "com.au", "net.au", "org.au", "edu.au", "gov.au", "asn.au", "id.au",
        // New Zealand
        "co.nz", "net.nz", "org.nz", "govt.nz", "ac.nz", "geek.nz", "gen.nz", "school.nz",
        // Japan
        "co.jp", "ne.jp", "ac.jp", "go.jp", "or.jp", "ed.jp", "gr.jp", "lg.jp",
        // India
        "co.in", "net.in", "org.in", "gen.in", "firm.in", "ind.in", "nic.in", "ac.in", "edu.in", "res.in", "gov.in", "mil.in",
        // Brazil
        "com.br", "net.br", "org.br", "gov.br", "edu.br", "art.br", "eco.br", "emp.br",
        // Singapore
        "com.sg", "edu.sg", "gov.sg", "net.sg", "org.sg", "per.sg",
        // South Africa
        "co.za", "org.za", "web.za", "net.za", "gov.za", "ac.za", "edu.za",
        // China
        "com.cn", "net.cn", "org.cn", "gov.cn", "edu.cn",
        // Taiwan
        "com.tw", "org.tw", "club.tw", "net.tw", "gov.tw", "edu.tw", "idv.tw",
        // Hong Kong
        "com.hk", "org.hk", "edu.hk", "net.hk", "gov.hk", "idv.hk",
        // Mexico
        "com.mx", "org.mx", "edu.mx", "net.mx", "gob.mx",
        // Additional ccTLDs
        "com.ar", "net.ar", "org.ar", "gov.ar", "gob.ar",
        "com.co", "net.co", "org.co", "edu.co", "gov.co",
        "co.id", "web.id", "or.id", "ac.id", "sch.id", "go.id", "biz.id", "my.id",
        "co.il", "org.il", "net.il", "ac.il", "gov.il",
        "com.my", "net.my", "org.my", "gov.my", "edu.my",
        "com.ph", "net.ph", "org.ph", "gov.ph", "edu.ph",
        "com.pk", "net.pk", "org.pk", "gov.pk", "edu.pk",
        "com.ng", "net.ng", "org.ng", "gov.ng", "edu.ng",
        "com.eg", "net.eg", "org.eg", "gov.eg", "edu.eg",
        "com.vn", "net.vn", "org.vn", "gov.vn", "edu.vn",
        // Shared Hosting and PaaS Multi-Tenant Suffixes
        "herokuapp.com",
        "github.io",
        "gitlab.io",
        "pages.dev",
        "vercel.app",
        "netlify.app",
        "blogspot.com",
        "wordpress.com",
        "azurewebsites.net",
        "firebaseapp.com",
        "web.app",
        "appspot.com",
        "cloudfront.net",
        "workers.dev",
        "fly.dev",
        "onrender.com",
        "myshopify.com"
    )

    /**
     * Converts a raw origin, domain, or URL string into a normalized okhttp3.HttpUrl.
     * Uses real URI/URL parsing instead of naive string splitting to avoid userinfo parsing vulnerabilities.
     */
    fun toNormalizedHttpUrl(raw: String?): HttpUrl? {
        if (raw.isNullOrBlank()) return null
        val trimmed = raw.trim()
        val withScheme = if (!trimmed.startsWith("http://", ignoreCase = true) &&
            !trimmed.startsWith("https://", ignoreCase = true)
        ) {
            "https://$trimmed"
        } else {
            trimmed
        }
        return try {
            withScheme.toHttpUrlOrNull()
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Extracts canonical hostname from a URL or raw origin string using safe URL parsing.
     * Accurately parses RFC 3986 userinfo (e.g., https://evil.example/x@bank.com -> evil.example)
     * and normalizes leading "www.".
     */
    fun extractHost(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        val url = toNormalizedHttpUrl(raw)
        val host = if (url != null) {
            url.host
        } else {
            var str = raw.trim()
            if (str.contains("://")) str = str.substringAfter("://")
            str = str.substringBefore('/').substringBefore('?').substringBefore('#')
            if (str.contains('@')) str = str.substringAfterLast('@')
            str = str.substringBefore(':')
            str.lowercase()
        }
        return if (host.startsWith("www.")) host.removePrefix("www.") else host
    }

    /**
     * Extracts registrable domain (eTLD+1) using multi-part Public Suffix List resolution.
     * Accurately handles multi-part TLDs (e.g. .com.tr, .co.kr) and shared hosting (e.g. .herokuapp.com, .github.io).
     */
    fun extractRegistrableDomain(rawOrHost: String?): String {
        val host = extractHost(rawOrHost)
        if (host.isEmpty()) return ""
        if (host.all { it.isDigit() || it == '.' } || host.contains(':') || host == "localhost") {
            return host
        }

        // 1. Attempt OkHttp built-in Public Suffix Database if available
        try {
            val tpd = toNormalizedHttpUrl(host)?.topPrivateDomain()
            if (tpd != null && tpd.isNotEmpty()) {
                return tpd
            }
        } catch (_: Throwable) {
            // Fall back to pure Kotlin PSL table below
        }

        // 2. Pure Kotlin PSL resolution
        val parts = host.split('.').filter { it.isNotEmpty() }
        if (parts.size <= 1) return host

        if (parts.size >= 3) {
            val lastTwo = "${parts[parts.size - 2]}.${parts.last()}"
            if (MULTI_PART_PUBLIC_SUFFIXES.contains(lastTwo)) {
                return "${parts[parts.size - 3]}.$lastTwo"
            }
        }
        if (parts.size >= 4) {
            val lastThree = "${parts[parts.size - 3]}.${parts[parts.size - 2]}.${parts.last()}"
            if (MULTI_PART_PUBLIC_SUFFIXES.contains(lastThree)) {
                return "${parts[parts.size - 4]}.$lastThree"
            }
        }

        val lastTwo = "${parts[parts.size - 2]}.${parts.last()}"
        if (MULTI_PART_PUBLIC_SUFFIXES.contains(lastTwo)) {
            return host
        }

        return "${parts[parts.size - 2]}.${parts.last()}"
    }

    /**
     * Determines whether a saved login origin matches a target page origin for credential autofill.
     *
     * Security constraints (H-4):
     * - Disallows cross-scheme autofill (https:// credentials are never offered on http://).
     * - Requires exact port matching.
     * - Disallows blank or wildcard queries.
     * - Enforces exact host matching or strictly parent/child matching on the same topPrivateDomain.
     * - Strictly prevents sibling subdomain cross-talk (e.g. accounts.example.com vs uploads.example.com)
     *   and shared-hosting co-tenant leakage (e.g. victim.herokuapp.com vs evil.herokuapp.com).
     */
    fun matches(savedOrigin: String?, pageOrigin: String?): Boolean {
        if (savedOrigin.isNullOrBlank() || pageOrigin.isNullOrBlank()) return false

        val savedUrl = toNormalizedHttpUrl(savedOrigin) ?: return false
        val pageUrl = toNormalizedHttpUrl(pageOrigin) ?: return false

        // 1. Strict Scheme Matching
        if (savedUrl.scheme != pageUrl.scheme) return false

        // 2. Disallow plain HTTP for credential autofill (allow HTTP only on local loopback)
        val isLoopback = savedUrl.host == "localhost" || savedUrl.host == "127.0.0.1" || savedUrl.host == "::1"
        if (savedUrl.scheme != "https" && !isLoopback) return false

        // 3. Port Matching (treat standard ports as matching if implicit)
        val savedPort = if (savedUrl.port != -1) savedUrl.port else if (savedUrl.scheme == "https") 443 else 80
        val pagePort = if (pageUrl.port != -1) pageUrl.port else if (pageUrl.scheme == "https") 443 else 80
        if (savedPort != pagePort) return false

        // 4. Exact Host Match (ignoring leading www.)
        val savedHost = extractHost(savedOrigin)
        val pageHost = extractHost(pageOrigin)
        if (savedHost.isNotEmpty() && savedHost.equals(pageHost, ignoreCase = true)) return true

        // 5. Registrable Domain check using Public Suffix List
        val savedTpd = extractRegistrableDomain(savedHost)
        val pageTpd = extractRegistrableDomain(pageHost)

        if (savedTpd.isEmpty() || pageTpd.isEmpty() || !savedTpd.equals(pageTpd, ignoreCase = true)) {
            return false
        }

        // Only allow parent domain to child subdomain or vice-versa (e.g. login.example.com and example.com)
        // Disallow sibling subdomains (e.g. accounts.example.com vs uploads.example.com)
        val savedIsParent = savedHost.equals(savedTpd, ignoreCase = true)
        val pageIsParent = pageHost.equals(pageTpd, ignoreCase = true)

        return savedIsParent || pageIsParent
    }
}

/**
 * AES-256 encrypted local password and autofill vault.
 * Backed by Android Jetpack Security EncryptedSharedPreferences with hardware-backed Keystore MasterKey.
 * Fails closed without plaintext fallback or silent credential wipes (H-7).
 */
object PasswordVaultManager {

    private const val PREFS_FILE = "azokle_secure_password_vault"
    private const val KEY_LOGINS = "vault_saved_logins"

    private val _loginsFlow = MutableStateFlow<List<SavedLogin>>(emptyList())
    val loginsFlow: StateFlow<List<SavedLogin>> = _loginsFlow.asStateFlow()

    @Volatile
    var isVaultAvailable: Boolean = false
        private set

    @Volatile
    private var isInitialized = false

    @Volatile
    private var encryptedPrefs: android.content.SharedPreferences? = null

    /**
     * Initializes the encrypted password vault.
     * Fail closed: never wipes master keys automatically and never falls back to plaintext SharedPreferences (H-7).
     */
    @Synchronized
    fun init(context: Context) {
        if (isInitialized && encryptedPrefs != null) return
        val appContext = context.applicationContext
        var lastException: Exception? = null

        for (attempt in 1..3) {
            try {
                val masterKey = MasterKey.Builder(appContext)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

                encryptedPrefs = EncryptedSharedPreferences.create(
                    appContext,
                    PREFS_FILE,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
                loadLogins()
                isInitialized = true
                isVaultAvailable = true
                return
            } catch (e: Exception) {
                lastException = e
                android.util.Log.w("PasswordVaultManager", "Keystore init attempt $attempt/3 failed: ${e.message}")
                try {
                    Thread.sleep(50L * attempt)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            }
        }

        // FAIL CLOSED (H-7): Do NOT delete the Keystore master key, do NOT delete the preference file,
        // and NEVER create an unencrypted plaintext SharedPreferences file.
        android.util.Log.e(
            "PasswordVaultManager",
            "Keystore master key error or hardware unavailable. Vault remaining locked in encrypted state: ${lastException?.message}"
        )
        encryptedPrefs = null
        isInitialized = true
        isVaultAvailable = false
    }

    /**
     * Loads logins from encrypted storage.
     */
    private fun loadLogins() {
        val prefs = encryptedPrefs ?: return
        val rawJson = prefs.getString(KEY_LOGINS, null) ?: return
        try {
            val arr = JSONArray(rawJson)
            val list = mutableListOf<SavedLogin>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    SavedLogin(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        origin = obj.getString("origin"),
                        username = obj.getString("username"),
                        password = obj.getString("password"),
                        dateModified = obj.optLong("dateModified", System.currentTimeMillis())
                    )
                )
            }
            _loginsFlow.value = list
        } catch (e: Exception) {
            android.util.Log.e("PasswordVaultManager", "Failed to deserialize encrypted vault logins: ${e.message}")
            // Do NOT overwrite _loginsFlow on parse error to prevent data destruction
        }
    }

    /**
     * Persists logins to secure storage atomically.
     */
    private fun persistLogins(list: List<SavedLogin>) {
        val prefs = encryptedPrefs
        if (prefs == null) {
            android.util.Log.e("PasswordVaultManager", "Cannot persist logins: secure encrypted vault unavailable")
            return
        }
        try {
            val arr = JSONArray()
            for (l in list) {
                arr.put(JSONObject().apply {
                    put("id", l.id)
                    put("origin", l.origin)
                    put("username", l.username)
                    put("password", l.password)
                    put("dateModified", l.dateModified)
                })
            }
            // Use commit() for synchronous durability confirmation before publishing state update
            val committed = prefs.edit().putString(KEY_LOGINS, arr.toString()).commit()
            if (committed) {
                _loginsFlow.value = list
            } else {
                android.util.Log.e("PasswordVaultManager", "Failed to atomically commit encrypted logins to disk")
            }
        } catch (e: Exception) {
            android.util.Log.e("PasswordVaultManager", "Exception during encrypted logins commit: ${e.message}")
        }
    }

    /**
     * Persists login to storage.
     */
    @Synchronized
    fun saveLogin(origin: String, username: String, password: String) {
        if (origin.isBlank() || username.isBlank() || password.isBlank()) return
        val canonicalOrigin = OriginMatcher.extractHost(origin).ifEmpty { origin.trim() }
        val cleanUsername = username.trim()
        val current = _loginsFlow.value.toMutableList()
        val existingIndex = current.indexOfFirst {
            OriginMatcher.matches(it.origin, canonicalOrigin) && it.username.equals(cleanUsername, ignoreCase = true)
        }
        if (existingIndex >= 0) {
            current[existingIndex] = current[existingIndex].copy(
                origin = canonicalOrigin,
                password = password,
                dateModified = System.currentTimeMillis()
            )
        } else {
            current.add(
                SavedLogin(
                    origin = canonicalOrigin,
                    username = cleanUsername,
                    password = password
                )
            )
        }
        persistLogins(current)
    }

    /**
     * Updates login.
     */
    @Synchronized
    fun updateLogin(id: String, origin: String, username: String, password: String) {
        if (origin.isBlank() || username.isBlank() || password.isBlank()) return
        val current = _loginsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0) {
            val canonicalOrigin = OriginMatcher.extractHost(origin).ifEmpty { origin.trim() }
            current[index] = current[index].copy(
                origin = canonicalOrigin,
                username = username.trim(),
                password = password,
                dateModified = System.currentTimeMillis()
            )
            persistLogins(current)
        }
    }

    /**
     * Retrieves the logins for origin.
     */
    fun getLoginsForOrigin(origin: String): List<SavedLogin> {
        if (origin.isBlank()) return emptyList()
        return _loginsFlow.value.filter {
            OriginMatcher.matches(it.origin, origin)
        }
    }

    /**
     * Retrieves the all logins.
     */
    fun getAllLogins(): List<SavedLogin> = _loginsFlow.value

    /**
     * Removes the specified login.
     */
    @Synchronized
    fun deleteLogin(id: String) {
        val updated = _loginsFlow.value.filterNot { it.id == id }
        persistLogins(updated)
    }

    /**
     * Clears all all logins.
     */
    @Synchronized
    fun clearAllLogins() {
        persistLogins(emptyList())
    }

    /**
     * Executes the export vault json operation.
     */
    fun exportVaultJson(): String {
        val arr = JSONArray()
        for (l in _loginsFlow.value) {
            arr.put(JSONObject().apply {
                put("id", l.id)
                put("origin", l.origin)
                put("username", l.username)
                put("password", l.password)
                put("dateModified", l.dateModified)
            })
        }
        return arr.toString(2)
    }

    /**
     * Executes the generate strong password operation.
     */
    fun generateStrongPassword(length: Int = 16, includeSymbols: Boolean = true): String {
        val upper = "ABCDEFGHJKLMNPQRSTUVWXYZ" // exclude ambiguous O, I
        val lower = "abcdefghijkmnopqrstuvwxyz" // exclude ambiguous l
        val digits = "23456789" // exclude ambiguous 0, 1
        val symbols = "!@#$%^&*()-_=+[]{}?"
        val secureRandom = java.security.SecureRandom()

        val guaranteed = mutableListOf<Char>()
        guaranteed.add(upper[secureRandom.nextInt(upper.length)])
        guaranteed.add(lower[secureRandom.nextInt(lower.length)])
        guaranteed.add(digits[secureRandom.nextInt(digits.length)])
        if (includeSymbols) {
            guaranteed.add(symbols[secureRandom.nextInt(symbols.length)])
        }

        val pool = if (includeSymbols) upper + lower + digits + symbols else upper + lower + digits
        val totalLength = length.coerceIn(8, 64)
        val remaining = totalLength - guaranteed.size
        for (i in 0 until remaining) {
            guaranteed.add(pool[secureRandom.nextInt(pool.length)])
        }

        for (i in guaranteed.indices.reversed()) {
            val j = secureRandom.nextInt(i + 1)
            val temp = guaranteed[i]
            guaranteed[i] = guaranteed[j]
            guaranteed[j] = temp
        }

        return guaranteed.joinToString("")
    }
}
