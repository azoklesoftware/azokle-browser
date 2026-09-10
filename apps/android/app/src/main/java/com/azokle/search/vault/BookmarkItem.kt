package com.azokle.search.vault

data class BookmarkItem(
    val id: String,
    val title: String,
    val url: String,
    val dateAdded: String,
    val favicon: String? = null
)
