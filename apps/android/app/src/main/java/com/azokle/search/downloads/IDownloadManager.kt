package com.azokle.search.downloads

import android.content.Context
import kotlinx.coroutines.flow.StateFlow

/**
 * Public Contract Interface for Downloads Management.
 * Follows DuckDuckGo's -api pattern to decouple consumers from concrete implementations.
 */
interface IDownloadManager {
    val downloads: StateFlow<List<DownloadItem>>

    fun init(context: Context)
    fun extractFilename(urlStr: String, contentDisposition: String? = null, mimeType: String? = null): String
    fun sanitizeFilename(name: String): String
    fun startDownload(
        context: Context,
        url: String,
        filename: String? = null,
        mimeType: String? = null,
        contentLength: Long = 0L,
        headers: Map<String, String>? = null,
        isPrivate: Boolean = false,
        isTor: Boolean = false
    ): String
    fun pauseDownload(id: String, context: Context)
    fun resumeDownload(id: String, context: Context)
    fun cancelDownload(id: String, context: Context)
    fun deleteDownload(id: String, context: Context, deleteFile: Boolean = true)
    fun openDownloadedFile(context: Context, item: DownloadItem)
}
