package com.azokle.search.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.azokle.search.MainActivity
import com.azokle.search.R

class AzokleSearchWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        if (action == Intent.ACTION_CONFIGURATION_CHANGED ||
            action == Intent.ACTION_LOCALE_CHANGED ||
            action == "android.intent.action.USER_PRESENT" ||
            action == AppWidgetManager.ACTION_APPWIDGET_UPDATE
        ) {
            updateAllWidgets(context)
        }
    }

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
        const val EXTRA_SCAN_QR = "com.azokle.search.EXTRA_SCAN_QR"
        const val EXTRA_OPEN_TOR = "com.azokle.search.EXTRA_OPEN_TOR"

        fun updateAllWidgets(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, AzokleSearchWidgetProvider::class.java)
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
                    else -> { // "system"
                        val currentConfig = context.applicationContext?.resources?.configuration ?: context.resources.configuration
                        val nightModeFlags = currentConfig.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
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
            val views = RemoteViews(context.packageName, R.layout.widget_azokle_search)

            // Dynamic Theme Styling (System / Dark / Light)
            val isDark = isWidgetDark(context)
            val bgRes = if (isDark) R.drawable.widget_bg_dark else R.drawable.widget_bg_light
            val emblemBgRes = if (isDark) R.drawable.widget_emblem_bg_dark else R.drawable.widget_emblem_bg_light
            val iconBgRes = if (isDark) R.drawable.widget_icon_bg_dark else R.drawable.widget_icon_bg_light

            val primaryTextColor = if (isDark) android.graphics.Color.parseColor("#F1F5F9") else android.graphics.Color.parseColor("#0F172A")
            val searchIconTint = if (isDark) android.graphics.Color.parseColor("#FF38BDF8") else android.graphics.Color.parseColor("#FF0284C7")
            val actionIconTint = if (isDark) android.graphics.Color.parseColor("#F1F5F9") else android.graphics.Color.parseColor("#334155")
            val dividerColor = if (isDark) android.graphics.Color.parseColor("#2438BDF8") else android.graphics.Color.parseColor("#1A0F172A")

            views.setInt(R.id.widget_root, "setBackgroundResource", bgRes)
            views.setInt(R.id.widget_brand_container, "setBackgroundResource", emblemBgRes)
            views.setInt(R.id.widget_search_icon, "setColorFilter", searchIconTint)
            views.setTextColor(R.id.widget_search_text, primaryTextColor)
            views.setInt(R.id.widget_divider, "setBackgroundColor", dividerColor)

            views.setInt(R.id.widget_btn_voice, "setBackgroundResource", iconBgRes)
            views.setInt(R.id.widget_voice_icon, "setColorFilter", actionIconTint)

            // Universal clean search prompt text
            views.setTextViewText(R.id.widget_search_text, "Search with Azokle")

            // 1. Main Search Bar Click -> Opens Azokle with Search Focus
            val searchIntent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_FOCUS_SEARCH, true)
            }
            val searchPendingIntent = PendingIntent.getActivity(
                context,
                0,
                searchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, searchPendingIntent)

            // 2. Voice Search Action Click -> Opens Instant Voice Recognition
            val voiceIntent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_START_VOICE, true)
            }
            val voicePendingIntent = PendingIntent.getActivity(
                context,
                1,
                voiceIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_voice, voicePendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
