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

    fun rotateWallpaper(): Int {
        currentIndex = (currentIndex + 1) % wallpapers.size
        val next = wallpapers[currentIndex]
        _currentWallpaper.value = next
        return next
    }

    fun getCurrentWallpaper(): Int {
        return _currentWallpaper.value
    }
}
