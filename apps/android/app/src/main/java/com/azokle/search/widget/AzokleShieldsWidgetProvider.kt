package com.azokle.search.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.azokle.search.MainActivity
import com.azokle.search.R

class AzokleShieldsWidgetProvider : AppWidgetProvider() {

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
        fun updateAllWidgets(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, AzokleShieldsWidgetProvider::class.java)
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
                        val currentConfig = context.applicationContext?.resources?.configuration ?: context.resources.configuration
                        val nightModeFlags = currentConfig.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
                        nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES
                    }
                }
            } catch (e: Exception) {
                true
            }
        }

        private fun formatCount(count: Long): String {
            return when {
                count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
                count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
                else -> count.toString()
            }
        }

        private fun formatBandwidth(totalBlocked: Long): String {
            val bytes = totalBlocked * 50L * 1024L // ~50KB per ad/tracker blocked
            return when {
                bytes >= 1024L * 1024L * 1024L -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
                bytes >= 1024L * 1024L -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
                bytes >= 1024L -> String.format("%.0f KB", bytes / 1024.0)
                else -> "0 KB"
            }
        }

        private fun formatTimeSaved(totalBlocked: Long): String {
            val seconds = totalBlocked * 50L / 1000L // ~50ms per network tracker saved
            return when {
                seconds >= 3600 -> String.format("%.1fh", seconds / 3600.0)
                seconds >= 60 -> String.format("%.1fm", seconds / 60.0)
                else -> "${seconds}s"
            }
        }

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_azokle_shields_hub)

            val isDark = isWidgetDark(context)

            // Dynamic Theme Styling (Solid Minimalist Surfaces)
            val bgRes = if (isDark) R.drawable.widget_bg_shields_dark else R.drawable.widget_bg_shields_light
            val cardBgRes = if (isDark) R.drawable.widget_metric_card_bg_dark else R.drawable.widget_metric_card_bg_light
            val badgeBgRes = if (isDark) R.drawable.widget_shield_badge_bg_dark else R.drawable.widget_shield_badge_bg_light
            val pillBgRes = if (isDark) R.drawable.widget_status_pill_dark else R.drawable.widget_status_pill_light

            val titleColor = if (isDark) Color.parseColor("#F3F3F3") else Color.parseColor("#1C1917")
            val subtitleColor = if (isDark) Color.parseColor("#A0A0A0") else Color.parseColor("#57534E")
            val labelColor = if (isDark) Color.parseColor("#A0A0A0") else Color.parseColor("#78716C")
            val emeraldColor = if (isDark) Color.parseColor("#22C55E") else Color.parseColor("#16A34A")

            val adsColor = if (isDark) Color.parseColor("#38BDF8") else Color.parseColor("#0284C7")
            val trackersColor = if (isDark) Color.parseColor("#F97316") else Color.parseColor("#EA580C")
            val bandwidthColor = if (isDark) Color.parseColor("#A855F7") else Color.parseColor("#9333EA")
            val timeColor = if (isDark) Color.parseColor("#22C55E") else Color.parseColor("#16A34A")

            // Apply Background Surfaces
            views.setInt(R.id.widget_shields_root, "setBackgroundResource", bgRes)
            views.setInt(R.id.widget_shield_badge_container, "setBackgroundResource", badgeBgRes)
            views.setInt(R.id.widget_shield_icon, "setColorFilter", emeraldColor)
            views.setInt(R.id.widget_shield_status_pill, "setBackgroundResource", pillBgRes)

            // Apply Text Colors
            views.setTextColor(R.id.widget_shield_title, titleColor)
            views.setTextColor(R.id.widget_shield_subtitle, subtitleColor)
            views.setTextColor(R.id.widget_shield_status, emeraldColor)

            // Apply Card Surfaces
            views.setInt(R.id.widget_card_ads, "setBackgroundResource", cardBgRes)
            views.setInt(R.id.widget_card_trackers, "setBackgroundResource", cardBgRes)
            views.setInt(R.id.widget_card_bandwidth, "setBackgroundResource", cardBgRes)
            views.setInt(R.id.widget_card_time, "setBackgroundResource", cardBgRes)

            // Apply Metric Value Colors
            views.setTextColor(R.id.widget_metric_ads, adsColor)
            views.setTextColor(R.id.widget_metric_trackers, trackersColor)
            views.setTextColor(R.id.widget_metric_bandwidth, bandwidthColor)
            views.setTextColor(R.id.widget_metric_time, timeColor)

            // Apply Label Colors
            views.setTextColor(R.id.widget_label_ads, labelColor)
            views.setTextColor(R.id.widget_label_trackers, labelColor)
            views.setTextColor(R.id.widget_label_bandwidth, labelColor)
            views.setTextColor(R.id.widget_label_time, labelColor)

            // Read stats from SharedPreferences safely
            val sp = context.getSharedPreferences("azokle_shield_stats", Context.MODE_PRIVATE)
            val ads = sp.getLong("lifetime_ads_blocked", 0L)
            val trackers = sp.getLong("lifetime_trackers_blocked", 0L)
            val total = ads + trackers

            // Bind the 4 privacy metrics
            views.setTextViewText(R.id.widget_metric_ads, formatCount(ads))
            views.setTextViewText(R.id.widget_metric_trackers, formatCount(trackers))
            views.setTextViewText(R.id.widget_metric_bandwidth, formatBandwidth(total))
            views.setTextViewText(R.id.widget_metric_time, formatTimeSaved(total))

            // Root Widget Click -> Opens Azokle Browser
            val mainIntent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val mainPendingIntent = PendingIntent.getActivity(
                context,
                20,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_shields_root, mainPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
