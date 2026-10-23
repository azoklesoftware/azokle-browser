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

package com.azokle.search.core

import android.content.Context
import com.azokle.search.common.util.PreferencesManager
import com.azokle.search.tor.TorCircuitManager
import com.azokle.search.tor.TorRemoteDns
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import java.net.InetAddress
import java.util.concurrent.TimeUnit

data class DohProvider(
    val id: String,
    val name: String,
    val url: String,
    val description: String,
    val flagEmoji: String,
    val bootstrapIps: List<String> = emptyList(),
    val bootstrapAddress: String = ""
)

object DnsOverHttpsManager {
    val PROVIDERS = listOf(
        DohProvider(
            id = "cloudflare",
            name = "Cloudflare (1.1.1.1)",
            url = "https://cloudflare-dns.com/dns-query",
            description = "Fastest privacy-first encrypted DNS with zero logging.",
            flagEmoji = "⚡",
            bootstrapIps = listOf("1.1.1.1", "1.0.0.1"),
            bootstrapAddress = "1.1.1.1"
        ),
        DohProvider(
            id = "quad9",
            name = "Quad9 (9.9.9.9)",
            url = "https://dns.quad9.net/dns-query",
            description = "Swiss non-profit with malicious domain blocking.",
            flagEmoji = "🛡️",
            bootstrapIps = listOf("9.9.9.9", "149.112.112.112"),
            bootstrapAddress = "9.9.9.9"
        ),
        DohProvider(
            id = "adguard",
            name = "AdGuard DNS",
            url = "https://dns.adguard-dns.com/dns-query",
            description = "Built-in ad, tracker, and malware DNS filtering.",
            flagEmoji = "🚫",
            bootstrapIps = listOf("94.140.14.14", "94.140.15.15"),
            bootstrapAddress = "94.140.14.14"
        ),
        DohProvider(
            id = "system",
            name = "System Default DNS",
            url = "",
            description = "Standard operating system DNS provider.",
            flagEmoji = "📱",
            bootstrapIps = emptyList(),
            bootstrapAddress = ""
        )
    )

    private val bootstrapClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private val providerDnsMap = java.util.concurrent.ConcurrentHashMap<String, Dns>()

    @Volatile
    var activeProviderId: String = "cloudflare"

    /**
     * Retrieves the provider.
     */
    fun getProvider(providerId: String): DohProvider {
        return PROVIDERS.find { it.id.equals(providerId, ignoreCase = true) } ?: PROVIDERS.first()
    }

    /**
     * Retrieves the dns for provider.
     */
    fun getDnsForProvider(providerId: String): Dns {
        if (providerId.equals("system", ignoreCase = true)) return Dns.SYSTEM
        return providerDnsMap.getOrPut(providerId) {
            val provider = getProvider(providerId)
            try {
                val bootstrapAddresses = provider.bootstrapIps.mapNotNull { ip ->
                    try {
                        InetAddress.getByName(ip)
                    } catch (e: Exception) {
                        null
                    }
                }.ifEmpty {
                    listOfNotNull(
                        try { InetAddress.getByName("1.1.1.1") } catch (e: Exception) { null },
                        try { InetAddress.getByName("1.0.0.1") } catch (e: Exception) { null }
                    )
                }

                DnsOverHttps.Builder()
                    .client(bootstrapClient)
                    .url(provider.url.toHttpUrl())
                    .bootstrapDnsHosts(bootstrapAddresses)
                    .includeIPv6(false)
                    .build()
            } catch (e: Exception) {
                Dns.SYSTEM
            }
        }
    }

    val sharedDns: Dns = object : Dns {
        /**
         * Executes the lookup operation.
         */
        override fun lookup(hostname: String): List<InetAddress> {
            if (TorCircuitManager.isTorActive) {
                return TorRemoteDns.lookup(hostname)
            }
            val dns = getDnsForProvider(activeProviderId)
            return try {
                dns.lookup(hostname)
            } catch (e: Exception) {
                try {
                    Dns.SYSTEM.lookup(hostname)
                } catch (e2: Exception) {
                    emptyList()
                }
            }
        }
    }

    private data class CnameCacheEntry(val canonicalHost: String?, val expiresAtMs: Long)
    private val cnameCache = java.util.concurrent.ConcurrentHashMap<String, CnameCacheEntry>()

    /**
     * Resolves the canonical CNAME alias of a domain over encrypted DNS (DoH).
     * Used for uncovering CNAME-cloaked third-party trackers disguised under first-party subdomains.
     */
    fun resolveCname(hostname: String): String? {
        // AZK-VULN-15: Never query clearnet DoH during active Tor sessions to prevent DNS leaks
        if (TorCircuitManager.isTorActive) {
            return null
        }
        val cleanHost = hostname.lowercase().trim().trimEnd('.')
        if (cleanHost.isBlank() || cleanHost == "localhost" || cleanHost.endsWith(".local") || cleanHost.contains(":") || cleanHost.all { it.isDigit() || it == '.' }) {
            return null
        }

        val now = System.currentTimeMillis()
        val cached = cnameCache[cleanHost]
        if (cached != null) {
            if (now < cached.expiresAtMs) {
                return cached.canonicalHost
            } else {
                cnameCache.remove(cleanHost)
            }
        }

        val provider = getProvider(activeProviderId)
        val endpoint = if (provider.id == "system" || provider.url.isBlank()) {
            "https://cloudflare-dns.com/dns-query"
        } else {
            provider.url
        }

        return try {
            val url = "$endpoint?name=$cleanHost&type=CNAME".toHttpUrl()
            val request = okhttp3.Request.Builder()
                .url(url)
                .addHeader("Accept", "application/dns-json")
                .build()

            bootstrapClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    cnameCache[cleanHost] = CnameCacheEntry(null, now + 60_000)
                    return null
                }
                val body = response.body.string()
                val json = org.json.JSONObject(body)
                val answers = json.optJSONArray("Answer")
                if (answers != null && answers.length() > 0) {
                    for (i in 0 until answers.length()) {
                        val ans = answers.optJSONObject(i) ?: continue
                        val type = ans.optInt("type", 0)
                        if (type == 5) { // CNAME type
                            val data = ans.optString("data", "").trim().trimEnd('.').lowercase()
                            if (data.isNotBlank() && data != cleanHost) {
                                val ttl = ans.optLong("TTL", 300L)
                                val ttlMs = maxOf(60_000L, minOf(ttl * 1000L, 3600_000L))
                                cnameCache[cleanHost] = CnameCacheEntry(data, now + ttlMs)
                                return data
                            }
                        }
                    }
                }
                cnameCache[cleanHost] = CnameCacheEntry(null, now + 300_000)
                null
            }
        } catch (e: Exception) {
            cnameCache[cleanHost] = CnameCacheEntry(null, now + 60_000)
            null
        }
    }

    /**
     * Clears all cname cache.
     */
    fun clearCnameCache() {
        cnameCache.clear()
    }

    /**
     * Executes the create doh dns operation.
     */
    fun createDohDns(context: Context): Dns {
        val prefs = PreferencesManager(context)
        val selectedId = prefs.getDohProvider()
        return getDnsForProvider(selectedId)
    }
}
