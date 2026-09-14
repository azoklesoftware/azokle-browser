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

class AzokleShieldsWidgetProvider : AppWidgetProvider() {

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
        const val EXTRA_OPEN_TOR = "com.azokle.search.EXTRA_OPEN_TOR"

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

            // Read stats from SharedPreferences safely
            val sp = context.getSharedPreferences("azokle_shield_stats", Context.MODE_PRIVATE)
            val ads = sp.getLong("lifetime_ads_blocked", 0L)
            val trackers = sp.getLong("lifetime_trackers_blocked", 0L)
            val total = ads + trackers

            views.setTextViewText(R.id.widget_metric_trackers, formatCount(if (total > 0) total else 0L))
            views.setTextViewText(R.id.widget_metric_bandwidth, formatBandwidth(total))
            views.setTextViewText(R.id.widget_metric_time, formatTimeSaved(total))

            // Main Widget Click -> Opens Browser
            val mainIntent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val mainPendingIntent = PendingIntent.getActivity(
                context,
                10,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_shields_root, mainPendingIntent)

            // Open Private Tab Click
            val privateIntent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_OPEN_TOR, true)
            }
            val privatePendingIntent = PendingIntent.getActivity(
                context,
                11,
                privateIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_new_private_tab, privatePendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
