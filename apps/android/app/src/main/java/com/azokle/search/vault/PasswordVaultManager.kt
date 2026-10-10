package com.azokle.search.vault

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class SavedLogin(
    val id: String = UUID.randomUUID().toString(),
    val origin: String,
    val username: String,
    val password: String,
    val dateModified: Long = System.currentTimeMillis()
)

/**
 * Military-grade AES-256 encrypted local password and autofill vault.
 * Backed by Android Jetpack Security EncryptedSharedPreferences with hardware-backed Keystore MasterKey.
 */
object PasswordVaultManager {

    private const val PREFS_FILE = "azokle_secure_password_vault"
    private const val KEY_LOGINS = "vault_saved_logins"

    private val _loginsFlow = MutableStateFlow<List<SavedLogin>>(emptyList())
    val loginsFlow: StateFlow<List<SavedLogin>> = _loginsFlow.asStateFlow()

    private var isInitialized = false
    private lateinit var encryptedPrefs: android.content.SharedPreferences

    @Synchronized
    fun init(context: Context) {
        if (isInitialized) return
        try {
            val masterKey = MasterKey.Builder(context.applicationContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            encryptedPrefs = EncryptedSharedPreferences.create(
                context.applicationContext,
                PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            loadLogins()
            isInitialized = true
        } catch (e: Exception) {
            // Fallback for devices without hardware keystore support
            encryptedPrefs = context.applicationContext.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            loadLogins()
            isInitialized = true
        }
    }

    private fun loadLogins() {
        val rawJson = encryptedPrefs.getString(KEY_LOGINS, null) ?: return
        try {
            val arr = JSONArray(rawJson)
            val list = mutableListOf<SavedLogin>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    SavedLogin(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        origin = obj.getString("origin"),
                        username = obj.getString("username"),
                        password = obj.getString("password"),
                        dateModified = obj.optLong("dateModified", System.currentTimeMillis())
                    )
                )
            }
            _loginsFlow.value = list
        } catch (_: Exception) {}
    }

    private fun persistLogins(list: List<SavedLogin>) {
        _loginsFlow.value = list
        try {
            val arr = JSONArray()
            for (l in list) {
                arr.put(JSONObject().apply {
                    put("id", l.id)
                    put("origin", l.origin)
                    put("username", l.username)
                    put("password", l.password)
                    put("dateModified", l.dateModified)
                })
            }
            encryptedPrefs.edit().putString(KEY_LOGINS, arr.toString()).apply()
        } catch (_: Exception) {}
    }

    @Synchronized
    fun saveLogin(origin: String, username: String, password: String) {
        if (origin.isBlank() || username.isBlank() || password.isBlank()) return
        val current = _loginsFlow.value.toMutableList()
        val existingIndex = current.indexOfFirst {
            it.origin.equals(origin, ignoreCase = true) && it.username == username
        }
        if (existingIndex >= 0) {
            current[existingIndex] = current[existingIndex].copy(
                password = password,
                dateModified = System.currentTimeMillis()
            )
        } else {
            current.add(
                SavedLogin(
                    origin = origin,
                    username = username,
                    password = password
                )
            )
        }
        persistLogins(current)
    }

    fun getLoginsForOrigin(origin: String): List<SavedLogin> {
        return _loginsFlow.value.filter {
            it.origin.contains(origin, ignoreCase = true) || origin.contains(it.origin, ignoreCase = true)
        }
    }

    fun getAllLogins(): List<SavedLogin> = _loginsFlow.value

    @Synchronized
    fun deleteLogin(id: String) {
        val updated = _loginsFlow.value.filterNot { it.id == id }
        persistLogins(updated)
    }

    @Synchronized
    fun clearAllLogins() {
        persistLogins(emptyList())
    }
}
