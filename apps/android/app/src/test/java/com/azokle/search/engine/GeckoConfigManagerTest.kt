package com.azokle.search.engine

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class GeckoConfigManagerTest {

    @Test
    fun testGeneratePerformanceConfigContainsCriticalFlags() {
        val yaml = GeckoConfigManager.generatePerformanceConfigYaml()

        // SpiderMonkey JIT
        assertTrue(yaml.contains("javascript.options.ion: true"))
        assertTrue(yaml.contains("javascript.options.baselinejit: true"))
        assertTrue(yaml.contains("javascript.options.warp: true"))
        assertTrue(yaml.contains("javascript.options.wasm: true"))

        // Garbage Collection
        assertTrue(yaml.contains("javascript.options.mem.gc_parallel_marking: true"))
        assertTrue(yaml.contains("javascript.options.mem.gc_compacting: true"))
        assertTrue(yaml.contains("javascript.options.mem.nursery.max_kb: 65536"))
        assertTrue(yaml.contains("javascript.options.mem.gc_incremental_slice_ms: 10"))

        // WebRender
        assertTrue(yaml.contains("gfx.webrender.all: true"))
        assertTrue(yaml.contains("gfx.webrender.compositor: true"))
        assertTrue(yaml.contains("gfx.webrender.multithreading: true"))

        // Network & Cache
        assertTrue(yaml.contains("browser.cache.memory.capacity: 65536"))
        assertTrue(yaml.contains("content.notify.interval: 15000"))
        assertTrue(yaml.contains("content.notify.ontimer: true"))
        assertTrue(yaml.contains("layout.css.font-display.enabled: true"))
        assertTrue(yaml.contains("layout.css.webkit-scrollbar.enabled: true"))
        assertTrue(yaml.contains("dom.vibrator.enabled: true"))

        // Media & Hardware Video Decoding
        assertTrue(yaml.contains("media.mediasource.enabled: true"))
        assertTrue(yaml.contains("media.mediasource.mp4.enabled: true"))
        assertTrue(yaml.contains("media.mediasource.webm.enabled: true"))
        assertTrue(yaml.contains("media.mp4.enabled: true"))
        assertTrue(yaml.contains("media.hardware-video-decoding.enabled: true"))
        assertTrue(yaml.contains("media.autoplay.default: 0"))
        assertTrue(yaml.contains("media.eme.enabled: true"))
        assertTrue(yaml.contains("media.webspeech.recognition.enable: true"))
        assertTrue(yaml.contains("media.webspeech.recognition.force_enable: true"))

        // WebRTC Leak Prevention & Local IP Protection
        assertTrue(yaml.contains("media.peerconnection.ice.no_host: true"))
        assertTrue(yaml.contains("media.peerconnection.ice.default_address_only: true"))
        assertTrue(yaml.contains("media.peerconnection.ice.obfuscate_host_addresses: true"))
        assertTrue(yaml.contains("media.peerconnection.ice.obfuscate_host_addresses.sync: true"))
        assertTrue(yaml.contains("media.peerconnection.ice.link_local_ips: false"))

        // Trusted Recursive Resolver (TRR) DoH
        assertTrue(yaml.contains("network.trr.mode: 2"))
        assertTrue(yaml.contains("network.trr.uri: \"https://cloudflare-dns.com/dns-query\""))
        assertTrue(yaml.contains("network.trr.bootstrapAddress: \"1.1.1.1\""))
        assertTrue(yaml.contains("network.trr.max-fails: 5"))
        assertTrue(yaml.contains("network.trr.clear-cache-on-mode-change: true"))

        // Post-Quantum Kyber768 & Encrypted Client Hello (ECH)
        assertTrue(yaml.contains("security.tls.enable_kyber: true"))
        assertTrue(yaml.contains("network.dns.echconfig.enabled: true"))
        assertTrue(yaml.contains("network.http.referer.trimmingPolicy: 2"))
    }

    @Test
    fun testGeneratePerformanceConfigWithQuad9() {
        val yaml = GeckoConfigManager.generatePerformanceConfigYaml("quad9")
        assertTrue(yaml.contains("network.trr.mode: 2"))
        assertTrue(yaml.contains("network.trr.uri: \"https://dns.quad9.net/dns-query\""))
        assertTrue(yaml.contains("network.trr.bootstrapAddress: \"9.9.9.9\""))
    }

    @Test
    fun testGeneratePerformanceConfigWithAdGuard() {
        val yaml = GeckoConfigManager.generatePerformanceConfigYaml("adguard")
        assertTrue(yaml.contains("network.trr.mode: 2"))
        assertTrue(yaml.contains("network.trr.uri: \"https://dns.adguard-dns.com/dns-query\""))
        assertTrue(yaml.contains("network.trr.bootstrapAddress: \"94.140.14.14\""))
    }

    @Test
    fun testGeneratePerformanceConfigWithSystemDns() {
        val yaml = GeckoConfigManager.generatePerformanceConfigYaml("system")
        assertTrue(yaml.contains("network.trr.mode: 0"))
        assertTrue(yaml.contains("network.trr.uri: \"\""))
        assertTrue(yaml.contains("network.trr.bootstrapAddress: \"\""))
    }
}
