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
import android.util.Log
import java.io.File
import java.io.FileWriter

/**
 * Generates and manages the high-performance GeckoView configuration file (geckoview-config.yaml).
 * Injected into GeckoRuntime via GeckoRuntimeSettings.Builder.configFilePath().
 *
 * Configures SpiderMonkey JIT, hardware-accelerated WebRender, multi-threaded GC marking,
 * aggressive in-memory caching, and sub-millisecond layout timing.
 */
object GeckoConfigManager {

    private const val TAG = "GeckoConfigManager"
    private const val CONFIG_FILE_NAME = "geckoview-config.yaml"

    /**
     * Retrieves the or create config file.
     */
    fun getOrCreateConfigFile(context: Context): File {
        val configFile = File(context.filesDir, CONFIG_FILE_NAME)
        try {
            val prefs = com.azokle.search.common.util.PreferencesManager.getInstance(context)
            val savedDoh = try {
                prefs.getDohProvider()
            } catch (e: Exception) {
                com.azokle.search.core.DnsOverHttpsManager.activeProviderId
            }
            val fpEnabled = prefs.isFingerprintProtectionEnabled()
            val fontEnabled = prefs.isFontFingerprintProtectionEnabled()
            val screenEnabled = prefs.isScreenFingerprintProtectionEnabled()
            val uaEnabled = prefs.isUserAgentFingerprintProtectionEnabled()
            val cookieCtrl = prefs.getCookieControl()
            val blockingMode = prefs.getShieldBlockingMode()
            val cookieBannerEnabled = prefs.isCookieBannerHandlingEnabled()

            FileWriter(configFile, false).use { writer ->
                writer.write(
                    generatePerformanceConfigYaml(
                        dohProviderId = savedDoh,
                        fingerprintProtection = fpEnabled,
                        fontProtection = fontEnabled,
                        screenProtection = screenEnabled,
                        userAgentProtection = uaEnabled,
                        cookieControl = cookieCtrl,
                        blockingMode = blockingMode,
                        cookieBannerHandling = cookieBannerEnabled
                    )
                )
            }
            Log.i(TAG, "Successfully generated performance config at: ${configFile.absolutePath} (DoH: $savedDoh, FP: $fpEnabled, Mode: $blockingMode, Cookies: $cookieCtrl)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write geckoview performance config", e)
        }
        return configFile
    }

    /**
     * Executes the generate performance config yaml operation.
     */
    internal fun generatePerformanceConfigYaml(
        dohProviderId: String = com.azokle.search.core.DnsOverHttpsManager.activeProviderId,
        fingerprintProtection: Boolean = true,
        fontProtection: Boolean = true,
        screenProtection: Boolean = true,
        userAgentProtection: Boolean = true,
        cookieControl: String = com.azokle.search.common.util.PreferencesManager.COOKIE_CONTROL_BLOCK_THIRD_PARTY,
        blockingMode: String = com.azokle.search.common.util.PreferencesManager.BLOCKING_MODE_STANDARD,
        cookieBannerHandling: Boolean = true
    ): String {
        val provider = com.azokle.search.core.DnsOverHttpsManager.getProvider(dohProviderId)
        val isDohEnabled = !dohProviderId.equals("system", ignoreCase = true) && provider.url.isNotBlank()

        return buildString {
            appendLine("env:")
            appendLine("  MOZ_ACCELERATED: \"1\"")
            appendLine("  MOZ_WEBRENDER: \"1\"")
            appendLine("prefs:")
            // 1. SpiderMonkey JavaScript JIT & Compiler Optimizations
            appendLine("  javascript.options.ion: true")
            appendLine("  javascript.options.baselinejit: true")
            appendLine("  javascript.options.warp: true")
            appendLine("  javascript.options.wasm: true")
            appendLine("  javascript.options.wasm_baselinejit: true")
            appendLine("  javascript.options.wasm_optimizingjit: true")
            appendLine("  javascript.options.mem.gc_parallel_marking: true")
            appendLine("  javascript.options.mem.gc_incremental: true")
            appendLine("  javascript.options.mem.gc_compacting: true")
            appendLine("  javascript.options.mem.gc_generational: true")
            appendLine("  javascript.options.mem.nursery.max_kb: 65536")
            appendLine("  javascript.options.mem.gc_incremental_slice_ms: 10")
            appendLine("  javascript.options.mem.gc_high_frequency_time_limit_ms: 1000")

            // 2. WebRender Hardware Acceleration & Compositor
            appendLine("  gfx.webrender.all: true")
            appendLine("  gfx.webrender.compositor: true")
            appendLine("  gfx.webrender.multithreading: true")
            appendLine("  gfx.webrender.pbo-uploads: true")
            appendLine("  layers.acceleration.force-enabled: true")

            // 3. High-Performance Network & Memory Caching & DNS-over-HTTPS (TRR)
            appendLine("  browser.cache.memory.enable: true")
            appendLine("  browser.cache.memory.capacity: 65536")
            appendLine("  browser.cache.disk.enable: true")
            appendLine("  network.http.max-connections: 128")
            appendLine("  network.http.max-persistent-connections-per-server: 8")
            appendLine("  network.http.pipelining: true")
            appendLine("  network.http.pipelining.maxrequests: 8")
            appendLine("  network.ssl_tokens_cache_enabled: true")
            appendLine("  network.dnsCacheEntries: 1024")
            appendLine("  network.dnsCacheExpiration: 3600")

            // Quantum Engine TRR (Trusted Recursive Resolver - DNS-over-HTTPS) & Remote SOCKS DNS
            appendLine("  network.proxy.socks_remote_dns: true")
            if (isDohEnabled) {
                appendLine("  network.trr.mode: 2")
                appendLine("  network.trr.uri: \"${provider.url}\"")
                if (provider.bootstrapAddress.isNotBlank()) {
                    appendLine("  network.trr.bootstrapAddress: \"${provider.bootstrapAddress}\"")
                }
                appendLine("  network.trr.max-fails: 5")
                appendLine("  network.trr.clear-cache-on-mode-change: true")
            } else {
                appendLine("  network.trr.mode: 0")
                appendLine("  network.trr.uri: \"\"")
                appendLine("  network.trr.bootstrapAddress: \"\"")
            }

            // 4. Layout, Rendering & Frame Pacing
            appendLine("  content.notify.ontimer: true")
            appendLine("  content.notify.interval: 15000")
            appendLine("  content.notify.backoffcount: 5")
            appendLine("  content.sink.enable_perf_mode: true")
            appendLine("  layout.css.individual-transform.enabled: true")
            appendLine("  layout.css.font-display.enabled: true")
            appendLine("  layout.css.webkit-scrollbar.enabled: true")
            appendLine("  dom.input.autofocus: true")
            appendLine("  dom.disable_open_during_load: true")
            appendLine("  dom.vibrator.enabled: true")

            // 5. Media, Video & Audio Hardware Decoding
            appendLine("  media.mediasource.enabled: true")
            appendLine("  media.mediasource.mp4.enabled: true")
            appendLine("  media.mediasource.webm.enabled: true")
            appendLine("  media.mp4.enabled: true")
            appendLine("  media.av1.enabled: true")
            appendLine("  media.hls.enabled: true")
            appendLine("  media.hardware-video-decoding.enabled: true")
            appendLine("  media.suspend-bkgnd-video.enabled: false")
            appendLine("  media.block-autoplay-until-in-foreground: true")
            appendLine("  media.geckoview.autoplay.request: true")
            appendLine("  media.autoplay.default: 1") // AZK-LOW-02: Block audible autoplay by default
            appendLine("  media.autoplay.blocking_policy: 0")
            appendLine("  media.eme.enabled: true")
            appendLine("  media.webspeech.recognition.enable: false") // AZK-LOW-02: Do not force enable speech recognition
            appendLine("  media.webspeech.recognition.force_enable: false")

            // 6. Advanced Web APIs: Async Clipboard, WebXR, WebAuthn
            appendLine("  dom.events.asyncClipboard.readText: false") // AZK-LOW-02: Prevent unprompted clipboard snooping
            appendLine("  dom.events.asyncClipboard.clipboardItem: true")
            appendLine("  dom.events.asyncClipboard.dataTransfer: true")
            appendLine("  dom.vr.enabled: true")
            appendLine("  dom.vr.webxr.enabled: true")
            appendLine("  security.webauth.webauthn: true")
            appendLine("  security.webauth.webauthn.enable_android_fido2: true")

            // 7. WebRTC Leak Prevention & Local IP Shield (RFC 8828 / LibreWolf / Tor Standards)
            appendLine("  media.peerconnection.ice.no_host: true")
            appendLine("  media.peerconnection.ice.default_address_only: true")
            appendLine("  media.peerconnection.ice.obfuscate_host_addresses: true")
            appendLine("  media.peerconnection.ice.obfuscate_host_addresses.sync: true")
            appendLine("  media.peerconnection.ice.link_local_ips: false")
            appendLine("  media.peerconnection.ice.proxy_only: true")
            appendLine("  network.proxy.failover_direct: false")
            appendLine("  network.dns.disablePrefetch: true")
            appendLine("  network.prefetch-next: false")
            appendLine("  network.http.speculative-parallel-limit: 0")

            // 8. Privacy & Engine-Level Anti-Tracking & Fingerprinting Shield
            appendLine("  privacy.reduceTimerPrecision: true")
            appendLine("  privacy.reduceTimerPrecision.microseconds: 2000")
            appendLine("  privacy.partition.serviceWorkers: true")
            appendLine("  privacy.partition.blobURL: true")
            appendLine("  privacy.query_stripping.enabled: true")
            appendLine("  privacy.query_stripping.strip_list: \"__hsfp __hssc __hstc __s _hsenc _openstat dclid fbclid gbraid gclid igshid mc_cid mc_eid ml_subscriber ml_subscriber_hash msclkid oly_anon_id oly_enc_id rb_clickid s_cid twclid utm_campaign utm_content utm_medium utm_name utm_reader utm_source utm_term utm_viz_id vgo_ee wbraid yclid\"")
            appendLine("  dom.security.https_only_mode_pbm: true")

            // Fingerprinting Protection (RFC / Tor / GeckoView Master & Fine-Grained Controls)
            if (fingerprintProtection) {
                appendLine("  privacy.fingerprintingProtection: true")
                appendLine("  privacy.fingerprintingProtection.overrides: \"+AllTargets,-CSSPrefersColorScheme\"")
                if (fontProtection) {
                    appendLine("  layout.css.font-visibility.level: 1")
                } else {
                    appendLine("  layout.css.font-visibility.level: 3")
                }
                if (screenProtection) {
                    appendLine("  privacy.resistFingerprinting.letterboxing: true")
                    appendLine("  privacy.window.maxInnerWidth: 1000")
                    appendLine("  privacy.window.maxInnerHeight: 1000")
                } else {
                    appendLine("  privacy.resistFingerprinting.letterboxing: false")
                }
                if (userAgentProtection) {
                    appendLine("  dom.maxHardwareConcurrency: 4")
                    appendLine("  privacy.spoof_english: 1")
                } else {
                    appendLine("  privacy.spoof_english: 0")
                }
            } else {
                appendLine("  privacy.fingerprintingProtection: false")
                appendLine("  layout.css.font-visibility.level: 3")
                appendLine("  privacy.resistFingerprinting.letterboxing: false")
                appendLine("  privacy.spoof_english: 0")
            }

            // Tracker & Ad Blocking Engine Modes
            when (blockingMode) {
                com.azokle.search.common.util.PreferencesManager.BLOCKING_MODE_AGGRESSIVE -> {
                    appendLine("  privacy.trackingprotection.enabled: true")
                    appendLine("  privacy.trackingprotection.socialtracking.enabled: true")
                    appendLine("  privacy.trackingprotection.cryptomining.enabled: true")
                    appendLine("  privacy.trackingprotection.fingerprinting.enabled: true")
                    appendLine("  privacy.trackingprotection.emailtracking.enabled: true")
                    appendLine("  urlclassifier.trackingAnnotationTable: \"ads-track-digest256,social-track-digest256,tracker-track-digest256,email-track-digest256\"")
                }
                com.azokle.search.common.util.PreferencesManager.BLOCKING_MODE_OFF -> {
                    appendLine("  privacy.trackingprotection.enabled: false")
                    appendLine("  privacy.trackingprotection.socialtracking.enabled: false")
                    appendLine("  privacy.trackingprotection.cryptomining.enabled: false")
                    appendLine("  privacy.trackingprotection.fingerprinting.enabled: false")
                }
                else -> { // BLOCKING_MODE_STANDARD
                    appendLine("  privacy.trackingprotection.enabled: true")
                    appendLine("  privacy.trackingprotection.socialtracking.enabled: true")
                    appendLine("  privacy.trackingprotection.cryptomining.enabled: true")
                    appendLine("  privacy.trackingprotection.fingerprinting.enabled: true")
                }
            }

            // 8.5 Cookie Isolation, SameSite & Storage Access API (RFC 6265bis & W3C Storage Access API)
            val cookieBehaviorValue = when (cookieControl) {
                com.azokle.search.common.util.PreferencesManager.COOKIE_CONTROL_BLOCK_ALL -> 2 // REJECT ALL
                com.azokle.search.common.util.PreferencesManager.COOKIE_CONTROL_ALLOW_ALL -> 0 // ACCEPT ALL
                else -> 5 // Total Cookie Protection / Isolate Third-Party (Default)
            }
            appendLine("  network.cookie.cookieBehavior: $cookieBehaviorValue")
            appendLine("  dom.storage_access.enabled: true")
            appendLine("  dom.storage_access.forward_declares: true")
            appendLine("  privacy.restrict3rdpartystorage.heuristics: true")
            appendLine("  network.cookie.sameSite.laxByDefault: true")
            appendLine("  network.cookie.sameSite.noneRequiresSecure: true")
            appendLine("  network.cookie.sameSite.schemeful: true")
            appendLine("  privacy.partition.always_partition_third_party_non_cookie_storage: true")
            appendLine("  privacy.partition.always_partition_third_party_non_cookie_storage.exempt_sessionstorage: false")

            // Engine-Level Cookie Banner Reducer & Auto-Reject
            if (cookieBannerHandling) {
                appendLine("  cookiebanners.service.mode: 2")
                appendLine("  cookiebanners.service.mode.privateBrowsing: 2")
                appendLine("  cookiebanners.bannerClicking.enabled: true")
                appendLine("  cookiebanners.cookieInjection.enabled: true")
            } else {
                appendLine("  cookiebanners.service.mode: 0")
                appendLine("  cookiebanners.service.mode.privateBrowsing: 2")
                appendLine("  cookiebanners.bannerClicking.enabled: false")
                appendLine("  cookiebanners.cookieInjection.enabled: false")
            }

            // 9. Post-Quantum Cryptography & Encrypted Client Hello (ECH)
            appendLine("  security.tls.enable_kyber: true")
            appendLine("  network.dns.echconfig.enabled: true")
            appendLine("  network.dns.echconfig.fallback_to_origin_when_ech_fails: false")
            appendLine("  network.http.referer.trimmingPolicy: 2")
            appendLine("  network.http.referer.XOriginPolicy: 1")

            // 10. Native PDF.js In-Tab Document Rendering
            appendLine("  pdfjs.disabled: false")
        }
    }
}
