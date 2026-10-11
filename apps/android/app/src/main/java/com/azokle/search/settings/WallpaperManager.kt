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

package com.azokle.search.settings

import androidx.annotation.DrawableRes
import com.azokle.search.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Sovereign Wallpaper Manager for the Browser Home Screen.
 * Automatically cycles through high-resolution background wallpapers on every refresh or new tab.
 */
object WallpaperManager {
    val wallpapers: List<Int> = listOf(
        R.drawable.bg_home_1,
        R.drawable.bg_home_2,
        R.drawable.bg_home_3,
        R.drawable.bg_home_4,
        R.drawable.bg_home
    )

    private var currentIndex: Int = 0
    private val _currentWallpaper = MutableStateFlow(wallpapers[0])
    val currentWallpaper: StateFlow<Int> = _currentWallpaper.asStateFlow()

    /**
     * Executes the rotate wallpaper operation.
     */
    fun rotateWallpaper(): Int {
        currentIndex = (currentIndex + 1) % wallpapers.size
        val next = wallpapers[currentIndex]
        _currentWallpaper.value = next
        return next
    }

    /**
     * Retrieves the current wallpaper.
     */
    fun getCurrentWallpaper(): Int {
        return _currentWallpaper.value
    }
}
