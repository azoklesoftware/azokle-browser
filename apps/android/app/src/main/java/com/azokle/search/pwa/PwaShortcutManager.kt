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

package com.azokle.search.pwa

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import coil.ImageLoader
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI
import java.util.UUID

object PwaShortcutManager {
    const val EXTRA_PWA_ID = "com.azokle.search.extra.PWA_ID"
    const val EXTRA_PWA_START_URL = "com.azokle.search.extra.PWA_START_URL"
    const val EXTRA_PWA_SCOPE = "com.azokle.search.extra.PWA_SCOPE"
    const val EXTRA_PWA_DISPLAY_MODE = "com.azokle.search.extra.PWA_DISPLAY_MODE"
    const val EXTRA_PWA_NAME = "com.azokle.search.extra.PWA_NAME"
    const val EXTRA_PWA_THEME_COLOR = "com.azokle.search.extra.PWA_THEME_COLOR"
    const val EXTRA_PWA_BG_COLOR = "com.azokle.search.extra.PWA_BG_COLOR"

    /**
     * Checks whether pin shortcut supported.
     */
    fun isPinShortcutSupported(context: Context): Boolean {
        return ShortcutManagerCompat.isRequestPinShortcutSupported(context)
    }

    /**
     * Executes the pin pwa shortcut operation.
     */
    suspend fun pinPwaShortcut(
        context: Context,
        eligibility: PwaInstallEligibility,
        customTitle: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val title = (customTitle ?: eligibility.title).trim().ifBlank { "Web App" }
            val pwaId = UUID.nameUUIDFromBytes(eligibility.startUrl.toByteArray()).toString()

            // Fetch or render real high-resolution logo bitmap
            val iconBitmap = loadIconBitmap(context, eligibility.iconUrl, eligibility.startUrl, title, eligibility.themeColor)

            val displayMode = eligibility.displayMode
            val isStandalone = displayMode == DisplayMode.STANDALONE || displayMode == DisplayMode.FULLSCREEN || displayMode == DisplayMode.MINIMAL_UI

            // Construct Dedicated Target Intent
            val targetIntent = createPwaLaunchIntent(
                context = context,
                pwaId = pwaId,
                startUrl = eligibility.startUrl,
                scope = eligibility.scope,
                displayMode = displayMode,
                title = title,
                themeColor = eligibility.themeColor,
                backgroundColor = eligibility.backgroundColor
            )

            val iconCompat = IconCompat.createWithBitmap(iconBitmap)

            val shortcutInfo = ShortcutInfoCompat.Builder(context, pwaId)
                .setShortLabel(title.take(25))
                .setLongLabel(title.take(50))
                .setIcon(iconCompat)
                .setIntent(targetIntent)
                .build()

            val success = ShortcutManagerCompat.requestPinShortcut(context, shortcutInfo, null)

            // Record to persistent storage
            val iconPath = PwaStorageManager.getInstance(context).saveIconToFile(pwaId, iconBitmap)
            val installedPwa = InstalledPwa(
                id = pwaId,
                title = title,
                startUrl = eligibility.startUrl,
                scope = eligibility.scope,
                displayMode = displayMode,
                themeColor = eligibility.themeColor,
                backgroundColor = eligibility.backgroundColor,
                iconBase64 = null,
                iconPath = iconPath
            )
            PwaStorageManager.getInstance(context).recordInstalled(installedPwa)

            success
        } catch (e: Exception) {
            android.util.Log.e("PwaShortcutManager", "Failed to pin shortcut: ${e.message}", e)
            false
        }
    }

    /**
     * Loads icon bitmap.
     */
    private suspend fun loadIconBitmap(
        context: Context,
        iconUrl: String?,
        pageUrl: String,
        title: String,
        themeColor: Int?
    ): Bitmap = withContext(Dispatchers.IO) {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val targetSize = Math.max(am?.launcherLargeIconSize ?: 192, 192)

        val isTor = com.azokle.search.tor.TorCircuitManager.isTorActive ||
                pageUrl.contains(".onion", ignoreCase = true) ||
                (iconUrl?.contains(".onion", ignoreCase = true) == true)

        // If Tor is active or on onion service, avoid unproxied clearnet Coil requests to prevent IP/DNS leaks
        if (isTor) {
            return@withContext generateFallbackIcon(title, targetSize, themeColor)
        }

        val imageLoader = ImageLoader.Builder(context)
            .components {
                add(SvgDecoder.Factory())
            }
            .allowHardware(false)
            .build()

        // Candidate URLs in priority order
        val candidates = mutableListOf<String>()
        if (!iconUrl.isNullOrBlank()) candidates.add(iconUrl)
        try {
            val uri = URI(pageUrl)
            val origin = "${uri.scheme}://${uri.authority}"
            candidates.add("$origin/apple-touch-icon.png")
            candidates.add("$origin/apple-touch-icon-precomposed.png")
            candidates.add("$origin/favicon.ico")
        } catch (e: Exception) {}

        for (candidateUrl in candidates) {
            try {
                val request = ImageRequest.Builder(context)
                    .data(candidateUrl)
                    .size(targetSize, targetSize)
                    .allowHardware(false)
                    .bitmapConfig(Bitmap.Config.ARGB_8888)
                    .addHeader("User-Agent", "Mozilla/5.0 (Android; Mobile) AzokleSearch/1.0")
                    .addHeader("Referer", pageUrl)
                    .build()

                val result = imageLoader.execute(request)
                if (result is SuccessResult) {
                    val drawable = result.drawable
                    val bitmap = drawableToBitmap(drawable, targetSize)
                    if (bitmap != null) {
                        return@withContext bitmap
                    }
                }
            } catch (e: Exception) {
                // Try next candidate
            }
        }

        // Fallback branded initial squircle
        generateFallbackIcon(title, targetSize, themeColor)
    }

    /**
     * Executes the drawable to bitmap operation.
     */
    private fun drawableToBitmap(drawable: Drawable, targetSize: Int): Bitmap? {
        return try {
            if (drawable is BitmapDrawable && drawable.bitmap != null) {
                val src = drawable.bitmap
                scaleAndPadBitmap(src, targetSize)
            } else {
                val w = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else targetSize
                val h = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else targetSize
                val bitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, targetSize, targetSize)
                drawable.draw(canvas)
                bitmap
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Executes the scale and pad bitmap operation.
     */
    private fun scaleAndPadBitmap(source: Bitmap, targetSize: Int): Bitmap {
        val output = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val srcW = source.width
        val srcH = source.height
        val scale = targetSize.toFloat() / Math.max(srcW, srcH)
        val scaledW = srcW * scale
        val scaledH = srcH * scale
        val left = (targetSize - scaledW) / 2f
        val top = (targetSize - scaledH) / 2f

        canvas.drawBitmap(source, null, RectF(left, top, left + scaledW, top + scaledH), paint)
        return output
    }

    /**
     * Executes the create pwa launch intent operation.
     */
    fun createPwaLaunchIntent(
        context: Context,
        pwaId: String,
        startUrl: String,
        scope: String,
        displayMode: DisplayMode,
        title: String,
        themeColor: Int?,
        backgroundColor: Int?
    ): Intent {
        val isStandalone = displayMode == DisplayMode.STANDALONE || displayMode == DisplayMode.FULLSCREEN || displayMode == DisplayMode.MINIMAL_UI
        return if (isStandalone) {
            Intent(context, PwaActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                // Unique data URI per PWA id prevents task-merging collisions in Android Recents
                data = Uri.parse("azokle-pwa://$pwaId?url=${Uri.encode(startUrl)}")
                setComponent(android.content.ComponentName(context, PwaActivity::class.java))
                addCategory(Intent.CATEGORY_DEFAULT)
                addCategory(Intent.CATEGORY_BROWSABLE)
                putExtra(EXTRA_PWA_ID, pwaId)
                putExtra(EXTRA_PWA_START_URL, startUrl)
                putExtra(EXTRA_PWA_SCOPE, scope)
                putExtra(EXTRA_PWA_DISPLAY_MODE, displayMode.name)
                putExtra(EXTRA_PWA_NAME, title)
                if (themeColor != null) {
                    putExtra(EXTRA_PWA_THEME_COLOR, themeColor)
                }
                if (backgroundColor != null) {
                    putExtra(EXTRA_PWA_BG_COLOR, backgroundColor)
                }
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                        Intent.FLAG_ACTIVITY_MULTIPLE_TASK
            }
        } else {
            Intent(context, com.azokle.search.MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse(startUrl)
                addCategory(Intent.CATEGORY_DEFAULT)
                addCategory(Intent.CATEGORY_BROWSABLE)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
    }

    /**
     * Executes the launch pwa operation.
     */
    fun launchPwa(context: Context, installedPwa: InstalledPwa) {
        val intent = createPwaLaunchIntent(
            context = context,
            pwaId = installedPwa.id,
            startUrl = installedPwa.startUrl,
            scope = installedPwa.scope,
            displayMode = installedPwa.displayMode,
            title = installedPwa.title,
            themeColor = installedPwa.themeColor,
            backgroundColor = installedPwa.backgroundColor
        )
        context.startActivity(intent)
    }

    /**
     * Executes the generate fallback icon operation.
     */
    private fun generateFallbackIcon(title: String, targetSize: Int, themeColor: Int?): Bitmap {
        val bitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Background Squircle
        paint.color = themeColor ?: Color.parseColor("#18181B")
        val rect = RectF(0f, 0f, targetSize.toFloat(), targetSize.toFloat())
        canvas.drawRoundRect(rect, targetSize * 0.22f, targetSize * 0.22f, paint)

        // Letter initial
        val initial = title.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "W"
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = targetSize * 0.48f
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }

        val textY = (targetSize / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText(initial, targetSize / 2f, textY, textPaint)

        return bitmap
    }
}
