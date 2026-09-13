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

    fun getOrCreateConfigFile(context: Context): File {
        val configFile = File(context.filesDir, CONFIG_FILE_NAME)
        try {
            FileWriter(configFile, false).use { writer ->
                writer.write(generatePerformanceConfigYaml())
            }
            Log.i(TAG, "Successfully generated performance config at: ${configFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write geckoview performance config", e)
        }
        return configFile
    }

    internal fun generatePerformanceConfigYaml(): String {
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

            // 3. High-Performance Network & Memory Caching
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
            appendLine("  media.block-autoplay-until-in-foreground: false")
            appendLine("  media.geckoview.autoplay.request: true")
            appendLine("  media.autoplay.default: 0")
            appendLine("  media.autoplay.blocking_policy: 0")
            appendLine("  media.eme.enabled: true")
            appendLine("  media.webspeech.recognition.enable: true")
            appendLine("  media.webspeech.recognition.force_enable: true")

            // 6. Advanced Web APIs: Async Clipboard, WebXR, WebAuthn
            appendLine("  dom.events.asyncClipboard.readText: true")
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
        }
    }
}
