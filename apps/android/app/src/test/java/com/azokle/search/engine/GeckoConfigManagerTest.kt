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

        // Media & Hardware Video Decoding
        assertTrue(yaml.contains("media.mediasource.enabled: true"))
        assertTrue(yaml.contains("media.mediasource.mp4.enabled: true"))
        assertTrue(yaml.contains("media.mediasource.webm.enabled: true"))
        assertTrue(yaml.contains("media.mp4.enabled: true"))
        assertTrue(yaml.contains("media.hardware-video-decoding.enabled: true"))
        assertTrue(yaml.contains("media.autoplay.default: 0"))
        assertTrue(yaml.contains("media.eme.enabled: true"))
    }
}
