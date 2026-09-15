package com.azokle.search.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.azokle.search.MainActivity
import com.azokle.search.R

/**
 * Dedicated Home Screen Widget Provider for Azokle Tor Anonymous Search.
 * Dispatches 1-tap private Tor search requests with high-contrast violet glassmorphic styling.
 */
class AzokleTorSearchWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        const val EXTRA_FOCUS_SEARCH = "com.azokle.search.EXTRA_FOCUS_SEARCH"
        const val EXTRA_START_VOICE = "com.azokle.search.EXTRA_START_VOICE"
        const val EXTRA_OPEN_TOR = "com.azokle.search.EXTRA_OPEN_TOR"

        fun updateAllWidgets(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = android.content.ComponentName(context, AzokleTorSearchWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                    for (appWidgetId in appWidgetIds) {
                        updateAppWidget(context, appWidgetManager, appWidgetId)
                    }
                }
            } catch (e: Exception) {
                // Safe fallback
            }
        }

        private fun isWidgetDark(context: Context): Boolean {
            return try {
                val prefs = com.azokle.search.common.util.PreferencesManager(context)
                val mode = prefs.getThemeMode().lowercase()
                when (mode) {
                    "dark" -> true
                    "light" -> false
                    else -> {
                        val nightModeFlags = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
                        nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES
                    }
                }
            } catch (e: Exception) {
                true
            }
        }

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_azokle_tor_search)

            val isDark = isWidgetDark(context)
            val bgRes = if (isDark) R.drawable.widget_tor_bg_dark else R.drawable.widget_tor_bg_light
            val emblemBgRes = if (isDark) R.drawable.widget_tor_emblem_bg_dark else R.drawable.widget_tor_emblem_bg_light
            val chipBgRes = if (isDark) R.drawable.widget_tor_chip_bg_dark else R.drawable.widget_tor_chip_bg_light

            val primaryTextColor = if (isDark) android.graphics.Color.parseColor("#F3E8FF") else android.graphics.Color.parseColor("#3B0764")
            val actionIconTint = if (isDark) android.graphics.Color.parseColor("#F3E8FF") else android.graphics.Color.parseColor("#6B21A8")
            val dividerColor = if (isDark) android.graphics.Color.parseColor("#33A855F7") else android.graphics.Color.parseColor("#259333EA")

            views.setInt(R.id.widget_tor_root, "setBackgroundResource", bgRes)
            views.setInt(R.id.widget_tor_brand_container, "setBackgroundResource", emblemBgRes)
            views.setTextColor(R.id.widget_tor_search_text, primaryTextColor)
            views.setInt(R.id.widget_tor_divider, "setBackgroundColor", dividerColor)

            views.setInt(R.id.widget_tor_btn_voice, "setBackgroundResource", chipBgRes)
            views.setInt(R.id.widget_tor_voice_icon, "setColorFilter", actionIconTint)

            views.setTextViewText(R.id.widget_tor_search_text, "Search with Tor")

            // 1. Click Search Bar -> Opens Azokle with Instant Tor Mode & Search Focus
            val torSearchIntent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_OPEN_TOR, true)
                putExtra(EXTRA_FOCUS_SEARCH, true)
            }
            val searchPendingIntent = PendingIntent.getActivity(
                context,
                10,
                torSearchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_tor_root, searchPendingIntent)

            // 2. Click Voice Button -> Opens Instant Tor Voice Recognition
            val torVoiceIntent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_OPEN_TOR, true)
                putExtra(EXTRA_START_VOICE, true)
            }
            val voicePendingIntent = PendingIntent.getActivity(
                context,
                11,
                torVoiceIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_tor_btn_voice, voicePendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
