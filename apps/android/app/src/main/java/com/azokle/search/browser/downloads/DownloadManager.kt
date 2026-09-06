package com.azokle.search.browser.downloads

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

object DownloadManager {
    private const val TAG = "AzokleDownloadManager"
    private const val DOWNLOADS_FILE = "downloads_history.json"

    private val _downloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloads: StateFlow<List<DownloadItem>> = _downloads.asStateFlow()

    private val activeJobs = java.util.concurrent.ConcurrentHashMap<String, Job>()
    private val scope = CoroutineScope(Dispatchers.IO)
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true
        loadHistory(context)
    }

    /**
     * Extracts or sanitizes a safe filename from a URL or Content-Disposition using native Rust engine.
     */
    fun extractFilename(urlStr: String, contentDisposition: String? = null, mimeType: String? = null): String {
        return com.azokle.search.core.AzokleCore.extractDownloadFilename(
            urlStr,
            contentDisposition ?: "",
            mimeType ?: ""
        )
    }

    /**
     * Sanitizes incoming filenames from Content-Disposition, URL paths, or external delegates.
     * Prevents path traversal (e.g. "../../", "..\", null bytes, control chars, reserved names)
     * and strictly keeps files within the target download directory.
     */
    fun sanitizeFilename(name: String): String {
        var cleaned = name
            .replace("\u0000", "")
            .replace('\\', '/')
            .substringAfterLast('/')
            .trim()

        while (cleaned.contains("..")) {
            cleaned = cleaned.replace("..", "_")
        }

        cleaned = cleaned.trimStart('.', ' ', '\t')
        cleaned = cleaned.replace(Regex("""[/\?%*:|"<>\\#\x00-\x1F\x7F]"""), "_")

        val hasAlphanumeric = cleaned.any { it.isLetterOrDigit() }
        if (!hasAlphanumeric || cleaned.isBlank() || cleaned == "." || cleaned == "..") {
            cleaned = "download_${System.currentTimeMillis()}"
        }

        return cleaned.take(128)
    }

    /**
     * Initiates a new high-speed sovereign download.
     */
    fun startDownload(
        context: Context,
        url: String,
        filename: String? = null,
        mimeType: String? = null,
        contentLength: Long = 0L,
        headers: Map<String, String>? = null,
        isPrivate: Boolean = false,
        isTor: Boolean = false
    ): String {
        init(context)
        val rawName = filename?.takeIf { it.isNotBlank() } ?: extractFilename(url, mimeType = mimeType)
        val safeName = sanitizeFilename(rawName)
        val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        var targetFile = File(downloadDir, safeName)

        // Enforce strict canonical containment to prevent directory traversal
        try {
            val downloadCanonical = downloadDir.canonicalFile
            val targetCanonical = targetFile.canonicalFile
            if (!targetCanonical.path.startsWith(downloadCanonical.path + File.separator)) {
                val fallbackName = "download_${UUID.randomUUID().toString().take(8)}" +
                    (targetFile.extension.takeIf { it.isNotBlank() }?.let { ".$it" } ?: "")
                targetFile = File(downloadDir, fallbackName)
            }
        } catch (e: Exception) {
            targetFile = File(downloadDir, "download_${UUID.randomUUID().toString().take(8)}")
        }

        // Prevent collisions
        var counter = 1
        val baseName = targetFile.nameWithoutExtension
        val ext = targetFile.extension
        while (targetFile.exists()) {
            targetFile = File(downloadDir, "${baseName}_$counter.$ext")
            counter++
        }

        val item = DownloadItem(
            id = UUID.randomUUID().toString(),
            url = url,
            filename = targetFile.name,
            filePath = targetFile.absolutePath,
            mimeType = mimeType ?: "application/octet-stream",
            totalBytes = contentLength,
            downloadedBytes = 0L,
            status = DownloadStatus.DOWNLOADING,
            timestamp = System.currentTimeMillis(),
            isPrivate = isPrivate,
            isTor = isTor
        )

        _downloads.update { listOf(item) + it }
        saveHistory(context)

        startDownloadJob(context, item, headers)
        return item.id
    }

    private fun startDownloadJob(context: Context, item: DownloadItem, headers: Map<String, String>? = null) {
        val job = scope.launch {
            var inputStream: InputStream? = null
            var outputStream: FileOutputStream? = null
            val targetFile = File(item.filePath)
            var downloaded = targetFile.length()

            try {
                val isTorActive = item.isTor || com.azokle.search.security.TorCircuitManager.isTorActive || item.url.contains(".onion", ignoreCase = true)
                if (isTorActive) {
                    val socksProxy = java.net.Proxy(
                        java.net.Proxy.Type.SOCKS,
                        java.net.InetSocketAddress.createUnresolved("127.0.0.1", com.azokle.search.security.TorCircuitManager.socksPort)
                    )
                    val client = okhttp3.OkHttpClient.Builder()
                        .proxy(socksProxy)
                        .dns(com.azokle.search.security.TorRemoteDns)
                        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                        .followRedirects(true)
                        .build()

                    val requestBuilder = okhttp3.Request.Builder()
                        .url(item.url)
                        .header("User-Agent", com.azokle.search.browser.gecko.GeckoEngineManager.MODERN_MOBILE_UA)

                    headers?.forEach { (k, v) ->
                        if (!k.equals("User-Agent", ignoreCase = true) && !k.equals("Host", ignoreCase = true)) {
                            requestBuilder.header(k, v)
                        }
                    }

                    if (downloaded > 0) {
                        requestBuilder.header("Range", "bytes=$downloaded-")
                    }

                    val response = client.newCall(requestBuilder.build()).execute()
                    if (!response.isSuccessful && response.code != 206) {
                        throw Exception("Tor HTTP error code ${response.code}")
                    }

                    val body = response.body ?: throw Exception("Empty response body from Tor")
                    val bodyLen = body.contentLength()
                    val totalLength = if (response.code == 206) {
                        downloaded + bodyLen
                    } else {
                        if (bodyLen > 0L) bodyLen else item.totalBytes
                    }

                    val stream = body.byteStream()
                    inputStream = stream
                    outputStream = FileOutputStream(targetFile, downloaded > 0)

                    val buffer = ByteArray(32 * 1024)
                    var bytesRead: Int = stream.read(buffer)
                    var lastUpdate = System.currentTimeMillis()

                    while (bytesRead != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        downloaded += bytesRead

                        val now = System.currentTimeMillis()
                        if (now - lastUpdate > 250) {
                            lastUpdate = now
                            updateItemStatus(item.id, DownloadStatus.DOWNLOADING, downloaded, totalLength)
                        }
                        bytesRead = stream.read(buffer)
                    }

                    outputStream.flush()
                    updateItemStatus(item.id, DownloadStatus.COMPLETED, downloaded, downloaded)
                    saveHistory(context)
                } else {
                    val url = URL(item.url)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 25000
                    connection.readTimeout = 45000
                    connection.instanceFollowRedirects = true
                    connection.setRequestProperty("User-Agent", com.azokle.search.browser.gecko.GeckoEngineManager.MODERN_MOBILE_UA)

                    headers?.forEach { (k, v) ->
                        if (!k.equals("User-Agent", ignoreCase = true) && !k.equals("Host", ignoreCase = true)) {
                            connection.setRequestProperty(k, v)
                        }
                    }

                    if (downloaded > 0) {
                        connection.setRequestProperty("Range", "bytes=$downloaded-")
                    }

                    connection.connect()
                    val responseCode = connection.responseCode
                    if (responseCode !in 200..299 && responseCode != 206) {
                        throw Exception("HTTP error code $responseCode")
                    }

                    val totalLength = if (responseCode == 206) {
                        downloaded + connection.contentLengthLong
                    } else {
                        connection.contentLengthLong.takeIf { it > 0 } ?: item.totalBytes
                    }

                    inputStream = connection.inputStream
                    outputStream = FileOutputStream(targetFile, downloaded > 0)

                    val buffer = ByteArray(32 * 1024)
                    var bytesRead: Int
                    var lastUpdate = System.currentTimeMillis()

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        downloaded += bytesRead

                        val now = System.currentTimeMillis()
                        if (now - lastUpdate > 250) {
                            lastUpdate = now
                            updateItemStatus(item.id, DownloadStatus.DOWNLOADING, downloaded, totalLength)
                            val pct = if (totalLength > 0) ((downloaded * 100) / totalLength).toInt() else 0
                            postProgressNotification(context, item, pct, totalLength <= 0)
                        }
                    }

                    outputStream.flush()
                    updateItemStatus(item.id, DownloadStatus.COMPLETED, downloaded, downloaded)
                    postCompletionNotification(context, item, isSuccess = true)
                    saveHistory(context)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Download error for ${item.filename}: ${e.message}")
                updateItemStatus(item.id, DownloadStatus.FAILED, item.downloadedBytes, item.totalBytes, e.message)
                postCompletionNotification(context, item, isSuccess = false)
                saveHistory(context)
            } finally {
                try { inputStream?.close() } catch (e: Exception) {}
                try { outputStream?.close() } catch (e: Exception) {}
                activeJobs.remove(item.id)
            }
        }

        activeJobs[item.id] = job
    }

    private fun updateItemStatus(
        id: String,
        status: DownloadStatus,
        downloaded: Long,
        total: Long,
        error: String? = null
    ) {
        _downloads.update { list ->
            list.map {
                if (it.id == id) {
                    it.copy(
                        status = status,
                        downloadedBytes = downloaded,
                        totalBytes = if (total > 0) total else it.totalBytes,
                        errorMessage = error
                    )
                } else it
            }
        }
    }

    fun pauseDownload(context: Context, id: String) {
        activeJobs[id]?.cancel()
        activeJobs.remove(id)
        _downloads.update { list ->
            list.map { if (it.id == id) it.copy(status = DownloadStatus.PAUSED) else it }
        }
        saveHistory(context)
    }

    fun resumeDownload(context: Context, id: String) {
        val item = _downloads.value.find { it.id == id } ?: return
        _downloads.update { list ->
            list.map { if (it.id == id) it.copy(status = DownloadStatus.DOWNLOADING) else it }
        }
        startDownloadJob(context, item)
    }

    fun retryDownload(context: Context, id: String) {
        val item = _downloads.value.find { it.id == id } ?: return
        val targetFile = File(item.filePath)
        if (targetFile.exists()) targetFile.delete()
        _downloads.update { list ->
            list.map { if (it.id == id) it.copy(status = DownloadStatus.DOWNLOADING, downloadedBytes = 0L, errorMessage = null) else it }
        }
        startDownloadJob(context, item.copy(downloadedBytes = 0L))
    }

    fun cancelDownload(context: Context, id: String) {
        activeJobs[id]?.cancel()
        activeJobs.remove(id)
        val item = _downloads.value.find { it.id == id }
        if (item != null) {
            val file = File(item.filePath)
            if (file.exists()) file.delete()
        }
        _downloads.update { list ->
            list.map { if (it.id == id) it.copy(status = DownloadStatus.CANCELLED) else it }
        }
        saveHistory(context)
    }

    fun deleteDownload(context: Context, id: String, deleteFile: Boolean = true) {
        activeJobs[id]?.cancel()
        activeJobs.remove(id)
        val item = _downloads.value.find { it.id == id }
        if (item != null && deleteFile) {
            val file = File(item.filePath)
            if (file.exists()) file.delete()
        }
        _downloads.update { list -> list.filterNot { it.id == id } }
        saveHistory(context)
    }

    fun clearAllHistory(context: Context) {
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()
        _downloads.value = emptyList()
        saveHistory(context)
    }

    fun recordCompletedDownload(
        context: Context,
        url: String,
        file: File,
        mimeType: String = "application/pdf"
    ) {
        init(context)
        val item = DownloadItem(
            id = UUID.randomUUID().toString(),
            url = url,
            filename = file.name,
            filePath = file.absolutePath,
            mimeType = mimeType,
            totalBytes = file.length(),
            downloadedBytes = file.length(),
            status = DownloadStatus.COMPLETED,
            timestamp = System.currentTimeMillis()
        )
        _downloads.update { listOf(item) + it }
        saveHistory(context)
        postCompletionNotification(context, item, isSuccess = true)
    }

    /**
     * Safely opens a completed file using Android FileProvider.
     */
    fun openFile(context: Context, item: DownloadItem) {
        try {
            val file = File(item.filePath)
            if (!file.exists()) return

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, item.mimeType)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(Intent.createChooser(intent, "Open with").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error opening file: ${e.message}")
        }
    }

    /**
     * Shares a downloaded file using Android Share Sheet.
     */
    fun shareFile(context: Context, item: DownloadItem) {
        try {
            val file = File(item.filePath)
            if (!file.exists()) return

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = item.mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(Intent.createChooser(intent, "Share file").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing file: ${e.message}")
        }
    }

    private fun getMimeTypeForFile(filename: String): String {
        val ext = filename.substringAfterLast('.', "").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
    }

    private fun saveHistory(context: Context) {
        scope.launch {
            try {
                val array = JSONArray()
                _downloads.value.take(200).filter { !it.isPrivate && !it.isTor }.forEach { item ->
                    val obj = JSONObject().apply {
                        put("id", item.id)
                        put("url", item.url)
                        put("filename", item.filename)
                        put("filePath", item.filePath)
                        put("mimeType", item.mimeType)
                        put("totalBytes", item.totalBytes)
                        put("downloadedBytes", item.downloadedBytes)
                        put("status", item.status.name)
                        put("timestamp", item.timestamp)
                        put("errorMessage", item.errorMessage ?: "")
                    }
                    array.put(obj)
                }
                withContext(Dispatchers.IO) {
                    val atomicFile = androidx.core.util.AtomicFile(File(context.filesDir, DOWNLOADS_FILE))
                    val stream = atomicFile.startWrite()
                    try {
                        stream.write(array.toString().toByteArray(Charsets.UTF_8))
                        atomicFile.finishWrite(stream)
                    } catch (e: Exception) {
                        atomicFile.failWrite(stream)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save downloads: ${e.message}")
            }
        }
    }

    private fun loadHistory(context: Context) {
        scope.launch(Dispatchers.IO) {
            try {
                val atomicFile = androidx.core.util.AtomicFile(File(context.filesDir, DOWNLOADS_FILE))
                if (!atomicFile.baseFile.exists()) return@launch
                val bytes = atomicFile.readFully()
                if (bytes.isEmpty()) return@launch
                val jsonStr = String(bytes, Charsets.UTF_8)
                val array = JSONArray(jsonStr)
                val list = mutableListOf<DownloadItem>()

                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val rawStatus = obj.optString("status", DownloadStatus.COMPLETED.name)
                    val status = if (rawStatus == DownloadStatus.DOWNLOADING.name) {
                        DownloadStatus.PAUSED // Resumeable on restart
                    } else {
                        try { DownloadStatus.valueOf(rawStatus) } catch (e: Exception) { DownloadStatus.COMPLETED }
                    }

                    list.add(
                        DownloadItem(
                            id = obj.getString("id"),
                            url = obj.getString("url"),
                            filename = obj.getString("filename"),
                            filePath = obj.getString("filePath"),
                            mimeType = obj.getString("mimeType"),
                            totalBytes = obj.optLong("totalBytes", 0L),
                            downloadedBytes = obj.optLong("downloadedBytes", 0L),
                            status = status,
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            errorMessage = obj.optString("errorMessage", "").takeIf { it.isNotBlank() }
                        )
                    )
                }
                _downloads.value = list
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load downloads: ${e.message}")
            }
        }
    }

    private const val NOTIFICATION_CHANNEL_ID = "azokle_downloads"
    private const val NOTIFICATION_CHANNEL_NAME = "Browser Downloads"

    private fun ensureNotificationChannel(context: Context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
            val channel = android.app.NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                android.app.NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Download progress and status notifications"
                setShowBadge(false)
            }
            manager?.createNotificationChannel(channel)
        }
    }

    private fun postProgressNotification(context: Context, item: DownloadItem, progress: Int, isIndeterminate: Boolean = false) {
        try {
            ensureNotificationChannel(context)
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager ?: return
            val notifId = item.id.hashCode()

            val notif = androidx.core.app.NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(item.filename)
                .setContentText(if (isIndeterminate) "Downloading..." else "$progress%")
                .setProgress(100, progress, isIndeterminate)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build()

            manager.notify(notifId, notif)
        } catch (e: Exception) {
            // Safe fallback if notifications are disabled or restricted
        }
    }

    private fun postCompletionNotification(context: Context, item: DownloadItem, isSuccess: Boolean) {
        try {
            ensureNotificationChannel(context)
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager ?: return
            val notifId = item.id.hashCode()

            if (isSuccess) {
                val file = File(item.filePath)
                val uri = try {
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                } catch (e: Exception) {
                    null
                }
                val pendingIntent = if (uri != null) {
                    val openIntent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, item.mimeType ?: "application/octet-stream")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    android.app.PendingIntent.getActivity(
                        context,
                        notifId,
                        openIntent,
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                    )
                } else null

                val builder = androidx.core.app.NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.stat_sys_download_done)
                    .setContentTitle("Download Complete")
                    .setContentText(item.filename)
                    .setAutoCancel(true)

                if (pendingIntent != null) {
                    builder.setContentIntent(pendingIntent)
                }

                manager.notify(notifId, builder.build())
            } else {
                val notif = androidx.core.app.NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.stat_notify_error)
                    .setContentTitle("Download Failed")
                    .setContentText(item.filename)
                    .setAutoCancel(true)
                    .build()

                manager.notify(notifId, notif)
            }
        } catch (e: Exception) {
            // Safe fallback
        }
    }
}
