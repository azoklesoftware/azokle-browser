package com.azokle.search.engine.delegates

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Toast
import com.azokle.search.downloads.DownloadManager
import com.azokle.search.engine.DownloadConfirmationPayload
import com.azokle.search.engine.GeckoSwipeRefreshLayout
import com.azokle.search.history.HistoryManager
import com.azokle.search.media.MediaPlaybackManager
import com.azokle.search.pwa.PwaManager
import com.azokle.search.tabs.TabManager
import com.azokle.search.engine.ui.ContextMediaType
import com.azokle.search.engine.ui.LinkContextTarget
import org.json.JSONObject
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.SlowScriptResponse
import org.mozilla.geckoview.WebResponse
import java.net.URI

/**
 * Enterprise Content Delegate extracted from monolithic GeckoBrowserView.
 * Handles:
 * 1. Document title changes & tab synchronization
 * 2. Fullscreen mode coordination
 * 3. Context menu item classification (Images, Videos, Links)
 * 4. Downloads & external response routing
 * 5. Content process crash / OOM kill resilience
 * 6. Slow script ANR prevention
 * 7. PWA manifest discovery
 */
class AzokleContentDelegate(
    private val context: Context,
    private val viewProvider: () -> View?,
    private val swipeRefreshProvider: () -> GeckoSwipeRefreshLayout?,
    private val tabId: String?,
    private val isPrivate: Boolean,
    private val isTor: Boolean,
    private val currentUrlProvider: () -> String,
    private val onTitleChanged: (String) -> Unit,
    private val onFullScreenChanged: (Boolean) -> Unit,
    private val onLinkLongPressed: (LinkContextTarget) -> Unit,
    private val onDownloadConfirmationRequested: ((DownloadConfirmationPayload) -> Unit)?,
    private val onCrashStateChanged: (isCrashed: Boolean, isOomKill: Boolean) -> Unit,
    private val plugins: List<BrowserLifecyclePlugin> = emptyList()
) : GeckoSession.ContentDelegate {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var crashCount = 0
    private var lastCrashTime = 0L

    override fun onTitleChange(session: GeckoSession, title: String?) {
        if (com.azokle.search.incineration.DataBurnEngine.isBurning() ||
            com.azokle.search.history.HistoryManager.isIncinerating()
        ) {
            return
        }

        title?.let {
            onTitleChanged(it)
            TabManager.updateActiveTab("", it)
            val currentUrl = currentUrlProvider()
            if (!isPrivate && !isTor && currentUrl.isNotBlank()) {
                HistoryManager.updateTitle(url = currentUrl, title = it)
                PwaManager.onTitleChanged(tabId ?: "active", it)
            }
            plugins.forEach { plugin -> plugin.onTitleChange(session, it) }
        }
    }

    override fun onFullScreen(session: GeckoSession, fullScreen: Boolean) {
        mainHandler.post {
            swipeRefreshProvider()?.isFullScreen = fullScreen
            MediaPlaybackManager.updateFullscreenState(
                tabId = tabId ?: session.hashCode().toString(),
                isFullscreen = fullScreen,
                videoAspectRatio = null
            )
            onFullScreenChanged(fullScreen)
        }
    }

    override fun onContextMenu(
        session: GeckoSession,
        screenX: Int,
        screenY: Int,
        element: GeckoSession.ContentDelegate.ContextElement
    ) {
        if (com.azokle.search.incineration.DataBurnEngine.isBurning() ||
            com.azokle.search.history.HistoryManager.isIncinerating()
        ) {
            return
        }

        // 1. Guard against inactive background tabs
        if (tabId != null && TabManager.activeTabId.value != null && tabId != TabManager.activeTabId.value) {
            return
        }

        // 2. Guard against edge back gestures (Android system gesture navigation zone: left/right edge within 32dp)
        val displayMetrics = context.resources.displayMetrics
        val edgeThresholdPx = (32 * displayMetrics.density).toInt()
        val isEdgeBackGesture = screenX <= edgeThresholdPx || screenX >= (displayMetrics.widthPixels - edgeThresholdPx)
        if (isEdgeBackGesture) {
            return
        }

        val baseUri = element.baseUri
        val rawLink = element.linkUri
        val srcUri = element.srcUri
        val title = element.title?.takeIf { it.isNotBlank() }
            ?: element.linkText?.takeIf { it.isNotBlank() }
            ?: element.altText?.takeIf { it.isNotBlank() }
            ?: ""

        val effectiveBaseUri = if (!baseUri.isNullOrBlank()) baseUri else currentUrlProvider()
        val resolvedLink = if (!rawLink.isNullOrBlank() && !rawLink.startsWith("http://") && !rawLink.startsWith("https://") && !effectiveBaseUri.isNullOrBlank()) {
            try {
                URI(effectiveBaseUri).resolve(rawLink).toString()
            } catch (e: Exception) {
                try {
                    Uri.parse(effectiveBaseUri).buildUpon().encodedPath(rawLink).build().toString()
                } catch (e2: Exception) {
                    rawLink
                }
            }
        } else {
            rawLink
        }

        // Sanitize resolved link
        val safeLink = if (!resolvedLink.isNullOrBlank()) {
            val scheme = try { Uri.parse(resolvedLink).scheme?.lowercase() ?: "" } catch (e: Exception) { "" }
            val dangerousSchemes = setOf("javascript", "file", "content", "chrome", "resource", "data")
            if (scheme in dangerousSchemes) "" else resolvedLink
        } else {
            ""
        }

        // Map ContextElement.type to ContextMediaType
        val mediaType = when (element.type) {
            GeckoSession.ContentDelegate.ContextElement.TYPE_IMAGE -> ContextMediaType.IMAGE
            GeckoSession.ContentDelegate.ContextElement.TYPE_VIDEO -> ContextMediaType.VIDEO
            GeckoSession.ContentDelegate.ContextElement.TYPE_AUDIO -> ContextMediaType.AUDIO
            else -> {
                if (!srcUri.isNullOrBlank()) ContextMediaType.IMAGE
                else ContextMediaType.NONE
            }
        }

        if (safeLink.isNotBlank() || !srcUri.isNullOrBlank()) {
            val target = LinkContextTarget(
                url = safeLink,
                title = title,
                isImage = mediaType == ContextMediaType.IMAGE,
                imageSrc = srcUri,
                mediaType = mediaType,
                mediaSrc = srcUri
            )
            mainHandler.post {
                // Ensure tab and session are still active before displaying context popup
                if (tabId != null && TabManager.activeTabId.value != null && tabId != TabManager.activeTabId.value) {
                    return@post
                }
                if (!session.isOpen) {
                    return@post
                }
                try {
                    viewProvider()?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                } catch (e: Exception) {}
                onLinkLongPressed(target)
            }
        }
    }

    override fun onExternalResponse(session: GeckoSession, response: WebResponse) {
        val downloadUrl = response.uri
        val contentType = response.headers["Content-Type"] ?: response.headers["content-type"]
        val contentDisposition = response.headers["Content-Disposition"] ?: response.headers["content-disposition"]
        val filename = DownloadManager.extractFilename(
            urlStr = downloadUrl,
            contentDisposition = contentDisposition,
            mimeType = contentType
        )
        val mimeType = contentType ?: "application/octet-stream"
        val headerLength = (response.headers["Content-Length"] ?: response.headers["content-length"])?.toLongOrNull() ?: 0L

        // STAGE THE STREAM IMMEDIATELY:
        // GeckoView gives us response.body (InputStream) directly from the authenticated session.
        // Reading response.body directly preserves session cookies, authentication headers,
        // single-use download tokens, POST-generated streams, and client-side blob: URLs!
        var stagedFile: java.io.File? = null
        try {
            val stagingDir = java.io.File(context.cacheDir, "download_staging").apply { mkdirs() }
            val tempFile = java.io.File(stagingDir, "stage_${java.util.UUID.randomUUID().toString().take(8)}.tmp")
            response.body?.use { inStream ->
                tempFile.outputStream().use { outStream ->
                    inStream.copyTo(outStream)
                }
            }
            if (tempFile.exists() && tempFile.length() > 0L) {
                stagedFile = tempFile
            }
        } catch (e: Throwable) {
            Log.w("AzokleContentDelegate", "Error staging download response stream: ${e.message}")
        }

        val effectiveLength = stagedFile?.length()?.takeIf { it > 0L } ?: headerLength

        val payload = DownloadConfirmationPayload(
            url = downloadUrl,
            filename = filename,
            mimeType = mimeType,
            contentLength = effectiveLength,
            headers = response.headers,
            isPrivate = isPrivate,
            isTor = isTor,
            stagedFilePath = stagedFile?.absolutePath
        )

        if (onDownloadConfirmationRequested != null) {
            mainHandler.post {
                onDownloadConfirmationRequested.invoke(payload)
            }
        } else {
            DownloadManager.startDownload(
                context = context,
                url = downloadUrl,
                filename = filename,
                mimeType = mimeType,
                contentLength = effectiveLength,
                headers = response.headers,
                isPrivate = isPrivate,
                isTor = isTor,
                stagedFilePath = stagedFile?.absolutePath
            )
            Toast.makeText(context, "Downloading $filename...", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCrash(session: GeckoSession) {
        Log.w("AzokleContentDelegate", "Gecko content process crashed.")
        val now = System.currentTimeMillis()
        if (now - lastCrashTime < 10000L) {
            crashCount++
        } else {
            crashCount = 1
        }
        lastCrashTime = now

        mainHandler.post {
            swipeRefreshProvider()?.isRefreshing = false
            if (crashCount > 1) {
                onCrashStateChanged(true, false)
            } else {
                try {
                    if (session.isOpen) {
                        session.reload(GeckoSession.LOAD_FLAGS_NONE)
                    } else {
                        onCrashStateChanged(true, false)
                    }
                } catch (e: Exception) {
                    onCrashStateChanged(true, false)
                }
            }
        }
        plugins.forEach { it.onCrash(session) }
    }

    override fun onKill(session: GeckoSession) {
        Log.w("AzokleContentDelegate", "Gecko content process killed by OS due to memory pressure.")
        mainHandler.post {
            swipeRefreshProvider()?.isRefreshing = false
            onCrashStateChanged(true, true)
        }
        plugins.forEach { it.onKill(session) }
    }

    override fun onSlowScript(session: GeckoSession, scriptFileName: String): GeckoResult<SlowScriptResponse>? {
        Log.w("AzokleContentDelegate", "Slow script detected: $scriptFileName, stopping execution to prevent ANR")
        return GeckoResult.fromValue(SlowScriptResponse.STOP)
    }

    override fun onCloseRequest(session: GeckoSession) {
        mainHandler.post {
            if (tabId != null) {
                TabManager.closeTab(tabId)
            }
        }
    }

    override fun onWebAppManifest(session: GeckoSession, manifest: JSONObject) {
        mainHandler.post {
            try {
                val activeTabId = tabId ?: "active"
                val name = manifest.optString("name", "").ifBlank { manifest.optString("short_name", "") }
                val startUrl = manifest.optString("start_url", "").takeIf { it.isNotBlank() }
                val themeColorStr = manifest.optString("theme_color", "").takeIf { it.isNotBlank() }
                val themeColor = themeColorStr?.let {
                    try { Color.parseColor(it) } catch (e: Exception) { null }
                }
                PwaManager.onManifestDiscovered(
                    tabId = activeTabId,
                    manifestUrl = startUrl,
                    manifestJson = manifest.toString(),
                    documentUrl = currentUrlProvider(),
                    pageTitle = name,
                    fallbackFaviconUrl = null,
                    fallbackThemeColor = themeColor
                )
            } catch (e: Exception) {
                // Safe fallback
            }
        }
        plugins.forEach { it.onWebAppManifest(session, manifest) }
    }
}
