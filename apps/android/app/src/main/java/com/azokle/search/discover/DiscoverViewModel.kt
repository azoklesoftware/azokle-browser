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

package com.azokle.search.discover

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class DiscoverUiState(
    val selectedCategory: DiscoverCategory = DiscoverCategory.ALL,
    val searchQuery: String = "",
    val topPickItems: List<DiscoverItem> = emptyList(),
    val displayedItems: List<DiscoverItem> = emptyList(),
    val favoriteIds: Set<String> = emptySet()
)

class DiscoverViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("azokle_discover_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(
        DiscoverUiState(
            topPickItems = DiscoverCatalog.topPickIds.mapNotNull { id -> DiscoverCatalog.items.find { it.id == id } },
            displayedItems = DiscoverCatalog.items,
            favoriteIds = prefs.getStringSet("favorite_portal_ids", emptySet()) ?: emptySet()
        )
    )
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    /**
     * Executes the select category operation.
     */
    fun selectCategory(category: DiscoverCategory) {
        _uiState.update { current ->
            val filtered = filterItems(category, current.searchQuery)
            current.copy(
                selectedCategory = category,
                displayedItems = filtered
            )
        }
    }

    /**
     * Updates search query.
     */
    fun updateSearchQuery(query: String) {
        _uiState.update { current ->
            val filtered = filterItems(current.selectedCategory, query)
            current.copy(
                searchQuery = query,
                displayedItems = filtered
            )
        }
    }

    /**
     * Toggles the state of favorite.
     */
    fun toggleFavorite(id: String) {
        _uiState.update { current ->
            val updated = if (current.favoriteIds.contains(id)) {
                current.favoriteIds - id
            } else {
                current.favoriteIds + id
            }
            prefs.edit().putStringSet("favorite_portal_ids", updated).apply()
            current.copy(favoriteIds = updated)
        }
    }

    /**
     * Executes the filter items operation.
     */
    private fun filterItems(category: DiscoverCategory, query: String): List<DiscoverItem> {
        val base = if (category == DiscoverCategory.ALL) {
            DiscoverCatalog.items
        } else {
            DiscoverCatalog.items.filter { it.category == category }
        }

        if (query.isBlank()) return base

        val trimmed = query.trim().lowercase()
        return base.filter {
            it.title.lowercase().contains(trimmed) ||
            it.subtitle.lowercase().contains(trimmed) ||
            it.domain.lowercase().contains(trimmed) ||
            it.category.label.lowercase().contains(trimmed)
        }
    }
}
