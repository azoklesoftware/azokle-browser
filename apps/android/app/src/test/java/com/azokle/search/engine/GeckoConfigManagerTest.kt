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
        assertTrue(yaml.contains("media.autoplay.default: 1"))
        assertTrue(yaml.contains("media.eme.enabled: true"))
        assertTrue(yaml.contains("media.webspeech.recognition.enable: false"))
        assertTrue(yaml.contains("media.webspeech.recognition.force_enable: false"))
        assertTrue(yaml.contains("dom.events.asyncClipboard.readText: false"))

        // WebRTC Leak Prevention & Local IP Protection
        assertTrue(yaml.contains("media.peerconnection.ice.no_host: true"))
        assertTrue(yaml.contains("media.peerconnection.ice.default_address_only: true"))
        assertTrue(yaml.contains("media.peerconnection.ice.obfuscate_host_addresses: true"))
        assertTrue(yaml.contains("media.peerconnection.ice.obfuscate_host_addresses.sync: true"))
        assertTrue(yaml.contains("media.peerconnection.ice.link_local_ips: false"))
        assertTrue(yaml.contains("media.peerconnection.ice.proxy_only: true"))
        assertTrue(yaml.contains("network.proxy.failover_direct: false"))
        assertTrue(yaml.contains("network.dns.disablePrefetch: true"))
        assertTrue(yaml.contains("network.prefetch-next: false"))
        assertTrue(yaml.contains("network.http.speculative-parallel-limit: 0"))

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

        // Total Cookie Protection Storage Partitioning, Query Stripping & PDF.js
        assertTrue(yaml.contains("privacy.partition.serviceWorkers: true"))
        assertTrue(yaml.contains("privacy.partition.blobURL: true"))
        assertTrue(yaml.contains("privacy.query_stripping.enabled: true"))
        assertTrue(yaml.contains("dom.security.https_only_mode_pbm: true"))
        assertTrue(yaml.contains("dom.storage_access.enabled: true"))
        assertTrue(yaml.contains("dom.storage_access.forward_declares: true"))
        assertTrue(yaml.contains("privacy.restrict3rdpartystorage.heuristics: true"))
        assertTrue(yaml.contains("network.cookie.sameSite.laxByDefault: true"))
        assertTrue(yaml.contains("network.cookie.sameSite.noneRequiresSecure: true"))
        assertTrue(yaml.contains("network.cookie.sameSite.schemeful: true"))
        assertTrue(yaml.contains("privacy.partition.always_partition_third_party_non_cookie_storage: true"))
        assertTrue(yaml.contains("pdfjs.disabled: false"))

        // Certificate Security, CRLite & Static Key Pinning Enforcement (eMudhra incident mitigations)
        assertTrue(yaml.contains("security.cert_pinning.enforcement_level: 2"))
        assertTrue(yaml.contains("security.OCSP.enabled: 1"))
        assertTrue(yaml.contains("security.ssl.enable_ocsp_stapling: true"))
        assertTrue(yaml.contains("security.ssl.enable_ocsp_must_staple: true"))
        assertTrue(yaml.contains("security.pki.crlite_mode: 2"))
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

    @Test
    fun testGeneratePerformanceConfigCookieOptions() {
        // 1. Block Third-Party Cookies (Default: 5 - Total Cookie Protection)
        val defaultYaml = GeckoConfigManager.generatePerformanceConfigYaml(
            cookieControl = com.azokle.search.common.util.PreferencesManager.COOKIE_CONTROL_BLOCK_THIRD_PARTY
        )
        assertTrue(defaultYaml.contains("network.cookie.cookieBehavior: 5"))

        // 2. Block All Cookies (2 - ACCEPT_NONE)
        val blockAllYaml = GeckoConfigManager.generatePerformanceConfigYaml(
            cookieControl = com.azokle.search.common.util.PreferencesManager.COOKIE_CONTROL_BLOCK_ALL
        )
        assertTrue(blockAllYaml.contains("network.cookie.cookieBehavior: 2"))

        // 3. Allow Cookies (0 - ACCEPT_ALL)
        val allowAllYaml = GeckoConfigManager.generatePerformanceConfigYaml(
            cookieControl = com.azokle.search.common.util.PreferencesManager.COOKIE_CONTROL_ALLOW_ALL
        )
        assertTrue(allowAllYaml.contains("network.cookie.cookieBehavior: 0"))
    }

    @Test
    fun testGeneratePerformanceConfigFingerprintingProtections() {
        // Default: Master enabled with all sub-flags active
        val defaultYaml = GeckoConfigManager.generatePerformanceConfigYaml()
        assertTrue(defaultYaml.contains("privacy.fingerprintingProtection: true"))
        assertTrue(defaultYaml.contains("layout.css.font-visibility.level: 1"))
        assertTrue(defaultYaml.contains("privacy.resistFingerprinting.letterboxing: true"))
        assertTrue(defaultYaml.contains("privacy.spoof_english: 1"))
        assertTrue(defaultYaml.contains("dom.maxHardwareConcurrency: 4"))

        // Configurable font protection disabled
        val fontDisabledYaml = GeckoConfigManager.generatePerformanceConfigYaml(fontProtection = false)
        assertTrue(fontDisabledYaml.contains("layout.css.font-visibility.level: 3"))
        assertTrue(fontDisabledYaml.contains("privacy.resistFingerprinting.letterboxing: true"))

        // Configurable screen resolution protection disabled
        val screenDisabledYaml = GeckoConfigManager.generatePerformanceConfigYaml(screenProtection = false)
        assertTrue(screenDisabledYaml.contains("privacy.resistFingerprinting.letterboxing: false"))

        // Configurable user agent protection disabled
        val uaDisabledYaml = GeckoConfigManager.generatePerformanceConfigYaml(userAgentProtection = false)
        assertTrue(uaDisabledYaml.contains("privacy.spoof_english: 0"))

        // Master fingerprinting disabled
        val masterDisabledYaml = GeckoConfigManager.generatePerformanceConfigYaml(fingerprintProtection = false)
        assertTrue(masterDisabledYaml.contains("privacy.fingerprintingProtection: false"))
        assertTrue(masterDisabledYaml.contains("layout.css.font-visibility.level: 3"))
        assertTrue(masterDisabledYaml.contains("privacy.resistFingerprinting.letterboxing: false"))
        assertTrue(masterDisabledYaml.contains("privacy.spoof_english: 0"))
    }

    @Test
    fun testGeneratePerformanceConfigBlockingModes() {
        // Standard Mode (Default)
        val standardYaml = GeckoConfigManager.generatePerformanceConfigYaml(
            blockingMode = com.azokle.search.common.util.PreferencesManager.BLOCKING_MODE_STANDARD
        )
        assertTrue(standardYaml.contains("privacy.trackingprotection.enabled: true"))
        assertTrue(standardYaml.contains("privacy.trackingprotection.socialtracking.enabled: true"))

        // Aggressive Mode
        val aggressiveYaml = GeckoConfigManager.generatePerformanceConfigYaml(
            blockingMode = com.azokle.search.common.util.PreferencesManager.BLOCKING_MODE_AGGRESSIVE
        )
        assertTrue(aggressiveYaml.contains("privacy.trackingprotection.enabled: true"))
        assertTrue(aggressiveYaml.contains("privacy.trackingprotection.emailtracking.enabled: true"))
        assertTrue(aggressiveYaml.contains("urlclassifier.trackingAnnotationTable"))

        // Off Mode
        val offYaml = GeckoConfigManager.generatePerformanceConfigYaml(
            blockingMode = com.azokle.search.common.util.PreferencesManager.BLOCKING_MODE_OFF
        )
        assertTrue(offYaml.contains("privacy.trackingprotection.enabled: false"))
        assertTrue(offYaml.contains("privacy.trackingprotection.socialtracking.enabled: false"))
    }

    @Test
    fun testGeneratePerformanceConfigCookieBannerHandling() {
        // Enabled (Default: mode 2, bannerClicking true)
        val enabledYaml = GeckoConfigManager.generatePerformanceConfigYaml(cookieBannerHandling = true)
        assertTrue(enabledYaml.contains("cookiebanners.service.mode: 2"))
        assertTrue(enabledYaml.contains("cookiebanners.service.mode.privateBrowsing: 2"))
        assertTrue(enabledYaml.contains("cookiebanners.bannerClicking.enabled: true"))
        assertTrue(enabledYaml.contains("cookiebanners.cookieInjection.enabled: true"))

        // Disabled (mode 0, bannerClicking false)
        val disabledYaml = GeckoConfigManager.generatePerformanceConfigYaml(cookieBannerHandling = false)
        assertTrue(disabledYaml.contains("cookiebanners.service.mode: 0"))
        assertTrue(disabledYaml.contains("cookiebanners.service.mode.privateBrowsing: 2"))
        assertTrue(disabledYaml.contains("cookiebanners.bannerClicking.enabled: false"))
        assertTrue(disabledYaml.contains("cookiebanners.cookieInjection.enabled: false"))
    }
}

