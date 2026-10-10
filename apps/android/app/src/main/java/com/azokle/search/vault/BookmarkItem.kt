package com.azokle.search.vault

import java.util.UUID

data class BookmarkItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val url: String,
    val dateAdded: String = "",
    val favicon: String? = null
)
