package com.azokle.search.downloads

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Base64
import android.util.Base64InputStream
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
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

object DownloadManager {
    private const val TAG = "AzokleDownloadManager"
    private const val DOWNLOADS_FILE = "downloads_history.json"
    private const val PRIVATE_DOWNLOADS_DIR = "downloads_private"

    private val _downloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloads: StateFlow<List<DownloadItem>> = _downloads.asStateFlow()

    private val activeJobs = java.util.concurrent.ConcurrentHashMap<String, Job>()
    private val scope = CoroutineScope(Dispatchers.IO)
    private var isInitialized = false

    // HTTP Response Headers that must NEVER be forwarded as outbound Request Headers
    private val FORBIDDEN_OUTBOUND_HEADERS = hashSetOf(
        "content-length", "content-type", "content-disposition",
        "transfer-encoding", "set-cookie", "server", "date",
        "connection", "keep-alive", "host", "user-agent",
        "content-encoding", "vary", "age", "etag", "last-modified"
    )

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true
        loadHistory(context)
    }

    /**
     * Extracts or sanitizes a safe filename from a URL or Content-Disposition using native Rust engine.
     */
    fun extractFilename(urlStr: String, contentDisposition: String? = null, mimeType: String? = null): String {
        if (urlStr.startsWith("data:", ignoreCase = true)) {
            val detectedMime = mimeType?.takeIf { it.isNotBlank() }
                ?: urlStr.substringAfter("data:").substringBefore(';').substringBefore(',')
            val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(detectedMime) ?: "bin"
            return "download_${System.currentTimeMillis()}.$ext"
        }
        var name = com.azokle.search.core.AzokleCore.extractDownloadFilename(
            urlStr,
            contentDisposition ?: "",
            mimeType ?: ""
        )
        // If MIME type indicates PDF, enforce .pdf extension so Android viewers recognize it
        val lowerMime = (mimeType ?: "").lowercase()
        if ((lowerMime.contains("pdf") || lowerMime == "application/pdf") && !name.lowercase().endsWith(".pdf")) {
            name = "$name.pdf"
        }
        return name
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
     * Returns true if the file extension or MIME type represents an executable application package.
     */
    fun isDangerousExecutable(filename: String, mimeType: String? = null): Boolean {
        val lowerName = filename.lowercase().trim()
        val lowerMime = (mimeType ?: "").lowercase().trim()
        val dangerousExts = listOf(".apk", ".dex", ".sh", ".bat", ".exe", ".bin", ".vbs", ".cmd", ".ps1")
        return dangerousExts.any { lowerName.endsWith(it) } ||
                lowerMime == "application/vnd.android.package-archive" ||
                lowerMime == "application/x-executable" ||
                lowerMime == "application/x-msdos-program"
    }

    /**
     * Initiates a new high-speed sovereign download.
     * Isolates Private and Tor downloads into a sandboxed internal directory without MediaStore leakage.
     */
    fun startDownload(
        context: Context,
        url: String,
        filename: String? = null,
        mimeType: String? = null,
        contentLength: Long = 0L,
        headers: Map<String, String>? = null,
        isPrivate: Boolean = false,
        isTor: Boolean = false,
        stagedFilePath: String? = null
    ): String {
        init(context)
        val rawName = filename?.takeIf { it.isNotBlank() } ?: extractFilename(url, mimeType = mimeType)
        val safeName = sanitizeFilename(rawName)

        // Tor & Private Tab Downloads: Enforce strict isolated internal sandbox storage
        val downloadDir = if (isPrivate || isTor) {
            File(context.filesDir, PRIVATE_DOWNLOADS_DIR).apply { mkdirs() }
        } else {
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        }

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

        // Prevent filename collisions
        var counter = 1
        val baseName = targetFile.nameWithoutExtension
        val ext = targetFile.extension
        val extSuffix = if (ext.isNotBlank()) ".$ext" else ""
        while (targetFile.exists()) {
            targetFile = File(downloadDir, "${baseName}_$counter$extSuffix")
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

        startDownloadJob(context, item, headers, stagedFilePath)
        return item.id
    }

    private fun startDownloadJob(
        context: Context,
        item: DownloadItem,
        headers: Map<String, String>? = null,
        stagedFilePath: String? = null
    ) {
        val job = scope.launch {
            var inputStream: InputStream? = null
            var outputStream: FileOutputStream? = null
            val targetFile = File(item.filePath)
            var downloaded = targetFile.length()

            try {
                // 0. Pre-Staged Stream from GeckoView onExternalResponse (0ms instant move/copy!)
                // Completely eliminates authentication/cookie/token loss and handles client-side blob: URLs
                if (!stagedFilePath.isNullOrBlank()) {
                    val stagedFile = File(stagedFilePath)
                    if (stagedFile.exists() && stagedFile.length() > 0L) {
                        try {
                            stagedFile.copyTo(targetFile, overwrite = true)
                            val finalSize = targetFile.length()
                            if (!item.isPrivate && !item.isTor) {
                                publishToPublicDownloads(context, targetFile, item.mimeType, targetFile.name)
                            }
                            updateItemStatus(item.id, DownloadStatus.COMPLETED, finalSize, finalSize)
                            postCompletionNotification(context, item, isSuccess = true)
                            saveHistory(context)
                            try { stagedFile.delete() } catch (_: Exception) {}
                            return@launch
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to copy staged download file, falling back to network: ${e.message}")
                        }
                    }
                }

                // 1. Client-Side Data URI Stream (prevents JVM heap OOM on large Base64 files)
                if (item.url.startsWith("data:", ignoreCase = true)) {
                    val commaIndex = item.url.indexOf(',')
                    if (commaIndex != -1) {
                        val metadata = item.url.substring(5, commaIndex)
                        val dataPart = item.url.substring(commaIndex + 1)
                        val isBase64 = metadata.contains(";base64", ignoreCase = true)

                        if (isBase64) {
                            val byteStream = ByteArrayInputStream(dataPart.toByteArray(Charsets.US_ASCII))
                            inputStream = Base64InputStream(byteStream, Base64.DEFAULT)
                        } else {
                            val decodedString = java.net.URLDecoder.decode(dataPart, "UTF-8")
                            inputStream = ByteArrayInputStream(decodedString.toByteArray(Charsets.UTF_8))
                        }

                        outputStream = FileOutputStream(targetFile, false)
                        val buffer = ByteArray(32 * 1024)
                        var bytesWritten = 0L
                        var readLen: Int

                        while (inputStream.read(buffer).also { readLen = it } != -1) {
                            outputStream.write(buffer, 0, readLen)
                            bytesWritten += readLen
                        }
                        outputStream.flush()

                        // Only publish to public MediaStore if NOT Private/Tor
                        if (!item.isPrivate && !item.isTor) {
                            publishToPublicDownloads(context, targetFile, item.mimeType, targetFile.name)
                        }

                        updateItemStatus(item.id, DownloadStatus.COMPLETED, bytesWritten, bytesWritten)
                        postCompletionNotification(context, item, isSuccess = true)
                        saveHistory(context)
                        return@launch
                    }
                }

                // 2. Reject unresolvable blob: URLs gracefully if not staged
                if (item.url.startsWith("blob:", ignoreCase = true)) {
                    throw Exception("Blob data stream timed out or was not supplied by page context.")
                }

                val isTorActive = item.isTor || com.azokle.search.tor.TorCircuitManager.isTorActive || item.url.contains(".onion", ignoreCase = true)

                val client = if (isTorActive) {
                    val socksPort = com.azokle.search.tor.TorCircuitManager.socksPort.takeIf { it > 0 } ?: 9050
                    val socksProxy = java.net.Proxy(
                        java.net.Proxy.Type.SOCKS,
                        java.net.InetSocketAddress.createUnresolved("127.0.0.1", socksPort)
                    )
                    okhttp3.OkHttpClient.Builder()
                        .proxy(socksProxy)
                        .dns(com.azokle.search.tor.TorRemoteDns)
                        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                        .followRedirects(true)
                        .followSslRedirects(true)
                        .build()
                } else {
                    // Clearnet modern OkHttpClient: supports HTTP/2, TLS 1.3, and automatic HTTP -> HTTPS redirects!
                    okhttp3.OkHttpClient.Builder()
                        .connectTimeout(25, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(45, java.util.concurrent.TimeUnit.SECONDS)
                        .followRedirects(true)
                        .followSslRedirects(true)
                        .build()
                }

                val requestBuilder = okhttp3.Request.Builder()
                    .url(item.url)
                    .header("User-Agent", com.azokle.search.engine.GeckoEngineManager.MODERN_MOBILE_UA)

                // Forward only safe client request headers; strip response reflection headers
                headers?.forEach { (k, v) ->
                    if (!FORBIDDEN_OUTBOUND_HEADERS.contains(k.lowercase().trim())) {
                        requestBuilder.header(k, v)
                    }
                }

                if (downloaded > 0) {
                    requestBuilder.header("Range", "bytes=$downloaded-")
                }

                val response = client.newCall(requestBuilder.build()).execute()
                val responseCode = response.code

                if (!response.isSuccessful && responseCode != 206) {
                    throw Exception("HTTP error code $responseCode")
                }

                // Strict Range 206 Resumption Check
                val isRange206 = (responseCode == 206)
                if (!isRange206) {
                    downloaded = 0L // Reset if server sent full file from byte 0
                }

                val body = response.body ?: throw Exception("Empty response body from server")
                val bodyLen = body.contentLength()
                val totalLength = if (isRange206) {
                    downloaded + bodyLen
                } else {
                    if (bodyLen > 0L) bodyLen else item.totalBytes
                }

                val stream = body.byteStream()
                inputStream = stream
                outputStream = FileOutputStream(targetFile, isRange206)

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
                        val pct = if (totalLength > 0) ((downloaded * 100) / totalLength).toInt() else 0
                        postProgressNotification(context, item, pct, totalLength <= 0)
                    }
                    bytesRead = stream.read(buffer)
                }

                outputStream.flush()

                // Only publish to public MediaStore if NOT Private/Tor
                if (!item.isPrivate && !item.isTor) {
                    publishToPublicDownloads(context, targetFile, item.mimeType, targetFile.name)
                }

                updateItemStatus(item.id, DownloadStatus.COMPLETED, downloaded, downloaded)
                postCompletionNotification(context, item, isSuccess = true)
                saveHistory(context)
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
            if (file.exists()) {
                file.delete()
            }
        }
        com.azokle.search.history.AzokleHistoryDatabase.getInstance(context).deleteDownload(id)
        _downloads.update { list -> list.filterNot { it.id == id } }
        saveHistory(context)
    }

    fun clearAllHistory(context: Context) {
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()
        _downloads.value = emptyList()
        com.azokle.search.history.AzokleHistoryDatabase.getInstance(context).clearDownloads()
        saveHistory(context)
    }

    /**
     * Anti-forensics: Cryptographically shreds and zero-fills all files in private sandboxed downloads directory.
     * Invoked automatically by the Incinerator during "Burn Data".
     */
    fun shredPrivateDownloads(context: Context) {
        try {
            val privateDir = File(context.filesDir, PRIVATE_DOWNLOADS_DIR)
            if (privateDir.exists()) {
                val files = privateDir.listFiles() ?: emptyArray()
                for (f in files) {
                    try {
                        if (f.isFile) {
                            val len = f.length()
                            if (len > 0) {
                                FileOutputStream(f).use { out ->
                                    val zeroes = ByteArray(8192)
                                    var remaining = len
                                    while (remaining > 0) {
                                        val writeLen = minOf(remaining, zeroes.size.toLong()).toInt()
                                        out.write(zeroes, 0, writeLen)
                                        remaining -= writeLen
                                    }
                                    out.flush()
                                }
                            }
                            f.delete()
                        }
                    } catch (e: Exception) {
                        f.delete()
                    }
                }
                privateDir.delete()
            }
            // Remove any in-memory private download records
            _downloads.update { list -> list.filterNot { it.isPrivate || it.isTor } }
        } catch (e: Exception) {
            Log.e(TAG, "Error during private download shredding: ${e.message}")
        }
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
        publishToPublicDownloads(context, file, mimeType, file.name)
        saveHistory(context)
        postCompletionNotification(context, item, isSuccess = true)
    }

    /**
     * Publishes a completed download file to the public Android MediaStore (Android 10+ / Q+)
     * or scans it via MediaScannerConnection (Android 9-).
     * Returns the destination public file path or URI string.
     */
    fun publishToPublicDownloads(context: Context, sourceFile: File, mimeType: String, displayName: String): String {
        if (!sourceFile.exists() || sourceFile.length() == 0L) return sourceFile.absolutePath

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Azokle")
                    put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val targetUri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (targetUri != null) {
                    resolver.openOutputStream(targetUri)?.use { outStream ->
                        sourceFile.inputStream().use { inStream ->
                            inStream.copyTo(outStream)
                        }
                    }
                    contentValues.clear()
                    contentValues.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(targetUri, contentValues, null, null)
                    Log.i(TAG, "Successfully published download to MediaStore: $targetUri")
                }
            } else {
                try {
                    val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (publicDir.exists() || publicDir.mkdirs()) {
                        val publicFile = generateUniqueFile(publicDir, displayName)
                        sourceFile.copyTo(publicFile, overwrite = false)
                        android.media.MediaScannerConnection.scanFile(
                            context,
                            arrayOf(publicFile.absolutePath),
                            arrayOf(mimeType),
                            null
                        )
                        return publicFile.absolutePath
                    }
                } catch (e: Exception) {
                    android.media.MediaScannerConnection.scanFile(
                        context,
                        arrayOf(sourceFile.absolutePath),
                        arrayOf(mimeType),
                        null
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to publish to MediaStore: ${e.message}")
        }
        return sourceFile.absolutePath
    }

    private fun generateUniqueFile(directory: File, fileName: String): File {
        var target = File(directory, fileName)
        if (!target.exists()) return target

        val dotIdx = fileName.lastIndexOf('.')
        val baseName = if (dotIdx != -1) fileName.substring(0, dotIdx) else fileName
        val ext = if (dotIdx != -1) fileName.substring(dotIdx) else ""

        var count = 1
        while (target.exists() && count < 10000) {
            target = File(directory, "$baseName ($count)$ext")
            count++
        }
        return target
    }

    /**
     * Safely opens a completed file using Android FileProvider.
     */
    fun openFile(context: Context, item: DownloadItem) {
        try {
            val file = File(item.filePath)
            if (!file.exists()) return

            val resolvedMime = resolveEffectiveMimeType(file, item.mimeType)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, resolvedMime)
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

            val resolvedMime = resolveEffectiveMimeType(file, item.mimeType)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = resolvedMime
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

    private fun resolveEffectiveMimeType(file: File, fallbackMime: String): String {
        if (fallbackMime.isNotBlank() && fallbackMime != "application/octet-stream" && fallbackMime != "*/*") {
            return fallbackMime
        }
        val ext = file.extension.lowercase().trim()
        if (ext.isNotBlank()) {
            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
            if (!mime.isNullOrBlank()) return mime
            if (ext == "pdf") return "application/pdf"
            if (ext == "apk") return "application/vnd.android.package-archive"
        }
        return fallbackMime.ifBlank { "application/octet-stream" }
    }

    private fun saveHistory(context: Context) {
        scope.launch {
            try {
                val db = com.azokle.search.history.AzokleHistoryDatabase.getInstance(context)
                val standardList = _downloads.value.take(200).filter { !it.isPrivate && !it.isTor }

                // 1. Sync directly to SQLite
                for (item in standardList) {
                    db.recordOrUpdateDownload(item)
                }

                // 2. Synchronize Atomic JSON file backup
                val array = JSONArray()
                standardList.forEach { item ->
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
                val db = com.azokle.search.history.AzokleHistoryDatabase.getInstance(context)
                val sqliteDownloads = db.getAllDownloads()

                if (sqliteDownloads.isNotEmpty()) {
                    _downloads.value = sqliteDownloads
                    Log.d(TAG, "Restored ${sqliteDownloads.size} download records from SQLite")
                    return@launch
                }

                // Fallback to JSON file backup and back-populate SQLite
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
                        DownloadStatus.PAUSED
                    } else {
                        try { DownloadStatus.valueOf(rawStatus) } catch (e: Exception) { DownloadStatus.COMPLETED }
                    }

                    val dlItem = DownloadItem(
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
                    list.add(dlItem)
                    db.recordOrUpdateDownload(dlItem)
                }
                _downloads.value = list
                Log.d(TAG, "Migrated ${list.size} downloads from JSON to SQLite")
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
