package com.azokle.search.security.passkey

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Encrypted Manager for Passkey Metadata & Credential Records.
 */
class PasskeyVaultManager private constructor(context: Context) {
    private val prefs: SharedPreferences = createEncryptedPrefs(context)
    private val _passkeysFlow = MutableStateFlow<List<PasskeyItem>>(emptyList())
    val passkeysFlow: StateFlow<List<PasskeyItem>> = _passkeysFlow.asStateFlow()

    companion object {
        private const val KEY_PASSKEYS = "azokle_passkeys_v1"
        private const val PREFS_FILE = "azokle_passkey_vault_secure"
        private var instance: PasskeyVaultManager? = null

        fun getInstance(context: Context): PasskeyVaultManager {
            return instance ?: synchronized(this) {
                instance ?: PasskeyVaultManager(context.applicationContext).also { instance = it }
            }
        }

        private fun createEncryptedPrefs(context: Context): SharedPreferences {
            return try {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

                EncryptedSharedPreferences.create(
                    context,
                    PREFS_FILE,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e: Exception) {
                android.util.Log.e("PasskeyVaultManager", "Keystore init error: ${e.message}, resetting key")
                context.deleteSharedPreferences(PREFS_FILE)
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

                EncryptedSharedPreferences.create(
                    context,
                    PREFS_FILE,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            }
        }
    }

    init {
        loadPasskeys()
    }

    private fun loadPasskeys() {
        val rawStr = prefs.getString(KEY_PASSKEYS, null)
        if (rawStr.isNullOrBlank()) {
            _passkeysFlow.value = emptyList()
            return
        }

        try {
            val arr = JSONArray(rawStr)
            val list = mutableListOf<PasskeyItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    PasskeyItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        rpId = obj.optString("rpId", ""),
                        userName = obj.optString("userName", ""),
                        userHandle = obj.optString("userHandle", ""),
                        dateCreated = obj.optString("dateCreated", ""),
                        lastUsed = obj.optString("lastUsed", "Recently"),
                        isSovereign = obj.optBoolean("isSovereign", false)
                    )
                )
            }
            _passkeysFlow.value = list
        } catch (e: Exception) {
            _passkeysFlow.value = emptyList()
        }
    }

    fun recordPasskey(rpId: String, userName: String, userHandle: String = "", isSovereign: Boolean = false) {
        val current = _passkeysFlow.value.toMutableList()
        val existingIdx = current.indexOfFirst { it.rpId.equals(rpId, ignoreCase = true) && it.userName.equals(userName, ignoreCase = true) }

        val df = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val dateStr = df.format(Date())

        if (existingIdx >= 0) {
            val existing = current[existingIdx]
            current[existingIdx] = existing.copy(lastUsed = "Today")
        } else {
            val item = PasskeyItem(
                id = UUID.randomUUID().toString(),
                rpId = rpId.lowercase().trim(),
                userName = userName,
                userHandle = userHandle,
                dateCreated = dateStr,
                lastUsed = "Today",
                isSovereign = isSovereign
            )
            current.add(0, item)
        }

        _passkeysFlow.value = current
        savePasskeys(current)
    }

    fun deletePasskey(id: String) {
        val updated = _passkeysFlow.value.filter { it.id != id }
        _passkeysFlow.value = updated
        savePasskeys(updated)
    }

    fun getPasskeysForDomain(domain: String): List<PasskeyItem> {
        val clean = domain.lowercase().trim()
        return _passkeysFlow.value.filter {
            it.rpId.equals(clean, ignoreCase = true) || clean.endsWith(".${it.rpId}") || it.rpId.endsWith(".$clean")
        }
    }

    private fun savePasskeys(list: List<PasskeyItem>) {
        try {
            val arr = JSONArray()
            for (p in list) {
                val obj = JSONObject().apply {
                    put("id", p.id)
                    put("rpId", p.rpId)
                    put("userName", p.userName)
                    put("userHandle", p.userHandle)
                    put("dateCreated", p.dateCreated)
                    put("lastUsed", p.lastUsed)
                    put("isSovereign", p.isSovereign)
                }
                arr.put(obj)
            }
            prefs.edit().putString(KEY_PASSKEYS, arr.toString()).apply()
        } catch (e: Exception) {
            // Ignore
        }
    }
}
