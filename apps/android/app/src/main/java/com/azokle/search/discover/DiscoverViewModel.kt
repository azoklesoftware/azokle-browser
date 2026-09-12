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

    fun selectCategory(category: DiscoverCategory) {
        _uiState.update { current ->
            val filtered = filterItems(category, current.searchQuery)
            current.copy(
                selectedCategory = category,
                displayedItems = filtered
            )
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { current ->
            val filtered = filterItems(current.selectedCategory, query)
            current.copy(
                searchQuery = query,
                displayedItems = filtered
            )
        }
    }

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
