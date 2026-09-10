package com.azokle.search.common.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import android.text.TextUtils
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LocaleHelper {

    fun getLocale(languageCode: String): Locale {
        if (languageCode.isBlank() || languageCode.equals("system", ignoreCase = true) || languageCode.equals("auto", ignoreCase = true)) {
            return Locale.getDefault()
        }
        return try {
            val cleanCode = languageCode.replace("values-", "").replace("b+", "")
            val (lang, region) = if (cleanCode.contains("-r")) {
                val parts = cleanCode.split("-r")
                parts[0] to parts.getOrNull(1)
            } else if (cleanCode.contains("-")) {
                val parts = cleanCode.split("-")
                parts[0] to parts.getOrNull(1)
            } else if (cleanCode.contains("_")) {
                val parts = cleanCode.split("_")
                parts[0] to parts.getOrNull(1)
            } else {
                cleanCode to null
            }

            val builder = Locale.Builder().setLanguage(lang)
            if (!region.isNullOrBlank()) {
                builder.setRegion(region)
            }
            builder.build()
        } catch (e: Exception) {
            try {
                Locale.forLanguageTag(languageCode.replace("-r", "-").replace("b+", ""))
            } catch (e2: Exception) {
                Locale.getDefault()
            }
        }
    }

    fun isRtl(locale: Locale): Boolean {
        return TextUtils.getLayoutDirectionFromLocale(locale) == View.LAYOUT_DIRECTION_RTL
    }

    fun applyLocale(context: Context, languageCode: String): Context {
        val locale = getLocale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
        }

        try {
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
        } catch (e: Exception) {}

        try {
            @Suppress("DEPRECATION")
            context.applicationContext?.resources?.updateConfiguration(config, context.resources.displayMetrics)
        } catch (e: Exception) {}

        return context.createConfigurationContext(config)
    }

    fun setAppLocale(languageCode: String) {
        val locale = getLocale(languageCode)
        Locale.setDefault(locale)
    }
}
