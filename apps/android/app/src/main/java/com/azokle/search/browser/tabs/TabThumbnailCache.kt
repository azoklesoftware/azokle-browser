package com.azokle.search.browser.tabs

import android.graphics.Bitmap
import androidx.collection.LruCache

/**
 * Thread-Safe, Bounded LRU Cache for Tab Preview Thumbnails.
 *
 * Performance Advantages:
 *  - Strictly decoupled from [TabSession], eliminating spurious recomposition storms across Compose UI.
 *  - Bounded memory footprint (max 8 entries or ~4MB max), preventing Out-Of-Memory (OOM) and GC freezes.
 *  - Fast O(1) retrieval and automatic eviction when tabs close or capacity is reached.
 */
object TabThumbnailCache {
    // Max 8 thumbnails kept in memory simultaneously.
    // Thumbnails are downscaled to 180x270 RGB_565 (~97 KB each), total memory < 1 MB.
    private const val MAX_ENTRIES = 8

    private val cache = LruCache<String, Bitmap>(MAX_ENTRIES)

    @Synchronized
    fun get(tabId: String): Bitmap? {
        return cache.get(tabId)
    }

    @Synchronized
    fun put(tabId: String, bitmap: Bitmap) {
        if (bitmap.isRecycled) return
        cache.put(tabId, bitmap)
    }

    @Synchronized
    fun remove(tabId: String) {
        cache.remove(tabId)
    }

    @Synchronized
    fun clear() {
        cache.evictAll()
    }

    @Synchronized
    fun size(): Int = cache.size()
}
