package com.azokle.search.engine.delegates

import android.app.Activity
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import com.azokle.search.adblock.AdBlockEngine
import com.azokle.search.adblock.AdBlockStats
import com.azokle.search.engine.ui.BrowserErrorType
import com.azokle.search.engine.GeckoSwipeRefreshLayout
import com.azokle.search.pwa.PwaManager
import com.azokle.search.tabs.TabManager
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSession.ProgressDelegate.SecurityInformation

/**
 * Enterprise Progress & Security Delegate extracted from monolithic GeckoBrowserView.
 * Handles:
 * 1. Page load lifecycle (start/stop/progress pacing)
 * 2. Visual alpha unmasking to eliminate white/stale texture flashing
 * 3. Security certificate analysis (TLS/cipher extraction)
 * 4. Thumbnail capture for active tabs grid
 * 5. Plugin event dispatching
 */
class AzokleProgressDelegate(
    private val viewProvider: () -> View?,
    private val swipeRefreshProvider: () -> GeckoSwipeRefreshLayout?,
    private val tabId: String?,
    private val isPrivate: Boolean,
    private val isTor: Boolean,
    private val onLoadingChanged: (Boolean) -> Unit,
    private val onProgressChanged: (Int) -> Unit,
    private val onSecurityChanged: (SecurityInformation?) -> Unit,
    private val onLoadErrorChanged: (BrowserErrorType?) -> Unit,
    private val onCrashStateReset: () -> Unit,
    private val plugins: List<BrowserLifecyclePlugin> = emptyList()
) : GeckoSession.ProgressDelegate {

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onPageStart(session: GeckoSession, url: String) {
        val swipeRefresh = swipeRefreshProvider()
        swipeRefresh?.isPageLoading = true
        swipeRefresh?.currentUrl = url
        swipeRefresh?.currentScrollY = 0
        swipeRefresh?.isContentAtTop = true
        swipeRefresh?.resetGestureState()

        mainHandler.post {
            onCrashStateReset()
            val view = viewProvider()
            if (url.equals("about:blank", ignoreCase = true) && view != null) {
                view.visibility = View.INVISIBLE
                view.alpha = 0f
            }
        }

        onLoadingChanged(true)
        onProgressChanged(10)
        onLoadErrorChanged(null)

        val host = try { Uri.parse(url).host ?: "" } catch (e: Exception) { "" }
        val isShield = AdBlockEngine.instance.isShieldEnabled(host)
        AdBlockStats.resetForPage(url = url, host = host, isEnabled = isShield)

        if (!isPrivate && !isTor) {
            PwaManager.onPageStarted(tabId ?: "active", url)
        }

        plugins.forEach { it.onPageStart(session, url) }
    }

    override fun onPageStop(session: GeckoSession, success: Boolean) {
        val swipeRefresh = swipeRefreshProvider()
        swipeRefresh?.isPageLoading = false

        mainHandler.post {
            val view = viewProvider()
            if (view != null && view.visibility != View.VISIBLE) {
                view.visibility = View.VISIBLE
                view.alpha = 1f
            }
            swipeRefresh?.isRefreshing = false
        }

        onLoadingChanged(false)
        onProgressChanged(100)

        // Capture Lightweight Screenshot Thumbnail for Active Tabs Grid
        if (!isPrivate && !isTor) {
            captureThumbnail()
        }

        plugins.forEach { it.onPageStop(session, success) }
    }

    override fun onProgressChange(session: GeckoSession, progress: Int) {
        if (progress >= 20) {
            mainHandler.post {
                val view = viewProvider()
                if (view != null) {
                    if (view.visibility != View.VISIBLE) {
                        view.visibility = View.VISIBLE
                    }
                    if (view.alpha < 1f) {
                        view.animate().alpha(1f).setDuration(150).start()
                    }
                }
            }
        }
        if (progress >= 100) {
            mainHandler.post {
                swipeRefreshProvider()?.isRefreshing = false
            }
        }
        onProgressChanged(progress)
        plugins.forEach { it.onProgressChange(session, progress) }
    }

    override fun onSecurityChange(session: GeckoSession, securityInfo: SecurityInformation) {
        onSecurityChanged(securityInfo)
    }

    private fun captureThumbnail() {
        mainHandler.postDelayed({
            try {
                val view = viewProvider() ?: return@postDelayed
                val activity = view.context as? Activity
                if (activity != null && (activity.isFinishing || activity.isDestroyed)) return@postDelayed
                if (view.width <= 0 || view.height <= 0) return@postDelayed

                val targetW = 180
                val targetH = (180f * view.height / view.width).toInt().coerceIn(120, 260)
                val bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.RGB_565)

                var surfaceView: SurfaceView? = null
                var textureView: TextureView? = null
                fun findViews(parent: ViewGroup) {
                    for (i in 0 until parent.childCount) {
                        val child = parent.getChildAt(i)
                        if (child is SurfaceView) surfaceView = child
                        else if (child is TextureView) textureView = child
                        else if (child is ViewGroup) findViews(child)
                    }
                }
                (view as? ViewGroup)?.let { findViews(it) }

                var copyDispatched = false
                if (textureView != null) {
                    try {
                        val texBmp = textureView.getBitmap(targetW, targetH)
                        if (texBmp != null) {
                            if (tabId != null) TabManager.updateTabThumbnail(tabId, texBmp)
                            else TabManager.updateActiveTabThumbnail(texBmp)
                            copyDispatched = true
                        }
                    } catch (e: Exception) {}
                }
                val currentSurfaceView = surfaceView
                if (!copyDispatched && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && currentSurfaceView?.holder?.surface?.isValid == true) {
                    try {
                        PixelCopy.request(
                            currentSurfaceView,
                            bitmap,
                            { copyResult ->
                                if (copyResult == PixelCopy.SUCCESS) {
                                    if (tabId != null) TabManager.updateTabThumbnail(tabId, bitmap)
                                    else TabManager.updateActiveTabThumbnail(bitmap)
                                } else {
                                    bitmap.recycle()
                                }
                            },
                            mainHandler
                        )
                        copyDispatched = true
                    } catch (e: Exception) {
                        copyDispatched = false
                    }
                }
                if (!copyDispatched) {
                    bitmap.recycle()
                }
            } catch (e: Exception) {
                // Ignore
            }
        }, 450)
    }
}
