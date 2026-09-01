package com.azokle.search.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector
import com.azokle.search.R

enum class AppNavTab(
    @param:StringRes val titleRes: Int,
    val icon: ImageVector
) {
    SEARCH(R.string.nav_search, Icons.Default.Search),
    DISCOVER(R.string.nav_discover, Icons.Default.Explore),
    TABS(R.string.nav_tabs, Icons.Default.Layers),
    HISTORY(R.string.history_title, Icons.Default.History)
}
