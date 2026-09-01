package com.azokle.search.browser.network

import android.content.Context
import com.azokle.search.data.PreferencesManager
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
    val flagEmoji: String
)

object DnsOverHttpsManager {
    val PROVIDERS = listOf(
        DohProvider(
            id = "cloudflare",
            name = "Cloudflare (1.1.1.1)",
            url = "https://cloudflare-dns.com/dns-query",
            description = "Fastest privacy-first encrypted DNS with zero logging.",
            flagEmoji = "⚡"
        ),
        DohProvider(
            id = "quad9",
            name = "Quad9 (9.9.9.9)",
            url = "https://dns.quad9.net/dns-query",
            description = "Swiss non-profit with malicious domain blocking.",
            flagEmoji = "🛡️"
        ),
        DohProvider(
            id = "adguard",
            name = "AdGuard DNS",
            url = "https://dns.adguard-dns.com/dns-query",
            description = "Built-in ad, tracker, and malware DNS filtering.",
            flagEmoji = "🚫"
        ),
        DohProvider(
            id = "system",
            name = "System Default DNS",
            url = "",
            description = "Standard operating system DNS provider.",
            flagEmoji = "📱"
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

    fun getDnsForProvider(providerId: String): Dns {
        if (providerId == "system") return Dns.SYSTEM
        return providerDnsMap.getOrPut(providerId) {
            val provider = PROVIDERS.find { it.id == providerId } ?: PROVIDERS.first()
            try {
                DnsOverHttps.Builder()
                    .client(bootstrapClient)
                    .url(provider.url.toHttpUrl())
                    .bootstrapDnsHosts(
                        InetAddress.getByName("1.1.1.1"),
                        InetAddress.getByName("1.0.0.1"),
                        InetAddress.getByName("9.9.9.9")
                    )
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

        if (selectedId == "system") {
            return Dns.SYSTEM
        }

        val provider = PROVIDERS.find { it.id == selectedId } ?: PROVIDERS.first()
        return try {
            DnsOverHttps.Builder()
                .client(bootstrapClient)
                .url(provider.url.toHttpUrl())
                .bootstrapDnsHosts(
                    InetAddress.getByName("1.1.1.1"),
                    InetAddress.getByName("1.0.0.1"),
                    InetAddress.getByName("9.9.9.9")
                )
                .includeIPv6(false)
                .build()
        } catch (e: Exception) {
            Dns.SYSTEM
        }
    }
}
