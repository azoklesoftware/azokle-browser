package com.azokle.search.core

import android.content.Context
import com.azokle.search.common.util.PreferencesManager
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

    fun getProvider(providerId: String): DohProvider {
        return PROVIDERS.find { it.id.equals(providerId, ignoreCase = true) } ?: PROVIDERS.first()
    }

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
        override fun lookup(hostname: String): List<InetAddress> {
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

    fun createDohDns(context: Context): Dns {
        val prefs = PreferencesManager(context)
        val selectedId = prefs.getDohProvider()
        return getDnsForProvider(selectedId)
    }
}
