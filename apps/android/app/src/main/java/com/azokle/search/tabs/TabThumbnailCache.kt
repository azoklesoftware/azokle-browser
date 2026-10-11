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

package com.azokle.search.tabs

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

    /**
     * Retrieves the .
     */
    @Synchronized
    fun get(tabId: String): Bitmap? {
        return cache.get(tabId)
    }

    /**
     * Executes the put operation.
     */
    @Synchronized
    fun put(tabId: String, bitmap: Bitmap) {
        if (bitmap.isRecycled) return
        cache.put(tabId, bitmap)
    }

    /**
     * Removes the specified remove.
     */
    @Synchronized
    fun remove(tabId: String) {
        cache.remove(tabId)
    }

    /**
     * Clears all .
     */
    @Synchronized
    fun clear() {
        cache.evictAll()
    }

    /**
     * Executes the size operation.
     */
    @Synchronized
    fun size(): Int = cache.size()
}
