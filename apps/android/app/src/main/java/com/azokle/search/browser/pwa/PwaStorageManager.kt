package com.azokle.search.browser.pwa

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class InstalledPwa(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val startUrl: String,
    val scope: String,
    val displayMode: DisplayMode = DisplayMode.STANDALONE,
    val themeColor: Int? = null,
    val backgroundColor: Int? = null,
    val iconBase64: String? = null,
    val installedAt: Long = System.currentTimeMillis()
) {
    fun getIconBitmap(reqWidth: Int = 192, reqHeight: Int = 192): Bitmap? {
        if (iconBase64.isNullOrBlank()) return null
        return try {
            val bytes = Base64.decode(iconBase64, Base64.DEFAULT)
            // First decode with inJustDecodeBounds=true to check dimensions without allocating memory
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

            // Calculate inSampleSize to downsample large bitmaps and prevent memory exhaustion
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.ARGB_8888

            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        } catch (e: Exception) {
            null
        }
    }

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("title", title)
            put("startUrl", startUrl)
            put("scope", scope)
            put("displayMode", displayMode.name)
            if (themeColor != null) put("themeColor", themeColor)
            if (backgroundColor != null) put("backgroundColor", backgroundColor)
            if (iconBase64 != null) put("iconBase64", iconBase64)
            put("installedAt", installedAt)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): InstalledPwa {
            return InstalledPwa(
                id = json.optString("id", UUID.randomUUID().toString()),
                title = json.optString("title", "Web App"),
                startUrl = json.optString("startUrl", ""),
                scope = json.optString("scope", ""),
                displayMode = DisplayMode.fromString(json.optString("displayMode", "standalone")),
                themeColor = if (json.has("themeColor")) json.getInt("themeColor") else null,
                backgroundColor = if (json.has("backgroundColor")) json.getInt("backgroundColor") else null,
                iconBase64 = json.optString("iconBase64", "").takeIf { it.isNotBlank() },
                installedAt = json.optLong("installedAt", System.currentTimeMillis())
            )
        }

        fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
            val height = options.outHeight
            val width = options.outWidth
            var inSampleSize = 1

            if (height > reqHeight || width > reqWidth) {
                val halfHeight = height / 2
                val halfWidth = width / 2
                while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                    inSampleSize *= 2
                }
            }
            return inSampleSize
        }
    }
}

class PwaStorageManager private constructor(context: Context) {
    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val pwaMap = ConcurrentHashMap<String, InstalledPwa>()
    private val _installedPwasFlow = MutableStateFlow<List<InstalledPwa>>(emptyList())
    val installedPwasFlow: StateFlow<List<InstalledPwa>> = _installedPwasFlow.asStateFlow()

    init {
        loadFromDisk()
    }

    private fun loadFromDisk() {
        try {
            val jsonStr = prefs.getString(KEY_PWAS, null) ?: return
            val arr = JSONArray(jsonStr)
            pwaMap.clear()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val item = InstalledPwa.fromJson(obj)
                pwaMap[item.id] = item
            }
            _installedPwasFlow.value = pwaMap.values.toList().sortedByDescending { it.installedAt }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error loading installed PWAs: ${e.message}")
        }
    }

    @Synchronized
    private fun saveToDisk() {
        try {
            val arr = JSONArray()
            for (item in pwaMap.values) {
                arr.put(item.toJson())
            }
            prefs.edit().putString(KEY_PWAS, arr.toString()).apply()
            _installedPwasFlow.value = pwaMap.values.toList().sortedByDescending { it.installedAt }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error saving installed PWAs: ${e.message}")
        }
    }

    fun recordInstalled(item: InstalledPwa) {
        pwaMap[item.id] = item
        saveToDisk()
    }

    fun getPwaById(id: String): InstalledPwa? = pwaMap[id]

    fun findPwaForUrl(url: String): InstalledPwa? {
        if (url.isBlank()) return null
        return pwaMap.values.find { pwa ->
            PwaInstallabilityValidator.isUrlInScope(url, pwa.scope)
        }
    }

    fun removePwa(id: String) {
        if (pwaMap.remove(id) != null) {
            saveToDisk()
        }
    }

    companion object {
        private const val TAG = "PwaStorageManager"
        private const val PREFS_NAME = "azokle_pwa_registry"
        private const val KEY_PWAS = "installed_pwas_v1"

        @Volatile
        private var INSTANCE: PwaStorageManager? = null

        fun getInstance(context: Context): PwaStorageManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PwaStorageManager(context).also { INSTANCE = it }
            }
        }

        fun bitmapToBase64(bitmap: Bitmap): String {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)
            val byteArray = stream.toByteArray()
            return Base64.encodeToString(byteArray, Base64.NO_WRAP)
        }
    }
}
