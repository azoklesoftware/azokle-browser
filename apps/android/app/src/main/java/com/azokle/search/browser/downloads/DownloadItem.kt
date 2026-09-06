package com.azokle.search.browser.downloads

enum class DownloadStatus {
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

enum class FileCategory {
    ALL,
    ACTIVE,
    IMAGES,
    MEDIA,
    DOCUMENTS,
    OTHER
}

data class DownloadItem(
    val id: String,
    val url: String,
    val filename: String,
    val filePath: String,
    val mimeType: String,
    val totalBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val status: DownloadStatus = DownloadStatus.DOWNLOADING,
    val timestamp: Long = System.currentTimeMillis(),
    val errorMessage: String? = null,
    val isPrivate: Boolean = false,
    val isTor: Boolean = false
) {
    val progress: Float
        get() = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f

    val formattedSize: String
        get() {
            if (totalBytes <= 0 && downloadedBytes <= 0) return "Unknown size"
            val bytes = if (totalBytes > 0) totalBytes else downloadedBytes
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format("%.2f GB", gb)
                mb >= 1.0 -> String.format("%.1f MB", mb)
                kb >= 1.0 -> String.format("%.0f KB", kb)
                else -> "$bytes B"
            }
        }

    val formattedProgress: String
        get() {
            val dMb = downloadedBytes / (1024.0 * 1024.0)
            val tMb = totalBytes / (1024.0 * 1024.0)
            return if (totalBytes > 0) {
                String.format("%.1f / %.1f MB (%.0f%%)", dMb, tMb, progress * 100)
            } else {
                String.format("%.1f MB", dMb)
            }
        }

    val category: FileCategory
        get() {
            val lower = filename.lowercase()
            val mime = mimeType.lowercase()
            return when {
                status == DownloadStatus.DOWNLOADING || status == DownloadStatus.PAUSED -> FileCategory.ACTIVE
                mime.startsWith("image/") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp") || lower.endsWith(".gif") || lower.endsWith(".svg") -> FileCategory.IMAGES
                mime.startsWith("video/") || mime.startsWith("audio/") || lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".mp3") || lower.endsWith(".wav") || lower.endsWith(".webm") || lower.endsWith(".m4a") -> FileCategory.MEDIA
                mime.startsWith("text/") || mime.contains("pdf") || mime.contains("document") || mime.contains("msword") || lower.endsWith(".pdf") || lower.endsWith(".docx") || lower.endsWith(".txt") || lower.endsWith(".epub") || lower.endsWith(".csv") || lower.endsWith(".xlsx") -> FileCategory.DOCUMENTS
                else -> FileCategory.OTHER
            }
        }
}
