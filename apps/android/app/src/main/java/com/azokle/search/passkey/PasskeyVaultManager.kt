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

@file:Suppress("DEPRECATION")

package com.azokle.search.passkey

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
        private const val FALLBACK_PREFS_FILE = "azokle_passkey_vault_fallback"
        private var instance: PasskeyVaultManager? = null

        @Volatile
        var isEncryptedStoreActive: Boolean = false
            private set

        /**
         * Retrieves the instance.
         */
        fun getInstance(context: Context): PasskeyVaultManager {
            return instance ?: synchronized(this) {
                instance ?: PasskeyVaultManager(context.applicationContext).also { instance = it }
            }
        }

        /**
         * Executes the create encrypted prefs operation.
         */
        private fun createEncryptedPrefs(context: Context): SharedPreferences {
            var lastException: Exception? = null
            for (attempt in 1..3) {
                try {
                    val masterKey = MasterKey.Builder(context)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build()

                    val p = EncryptedSharedPreferences.create(
                        context,
                        PREFS_FILE,
                        masterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                    )
                    isEncryptedStoreActive = true
                    return p
                } catch (e: Exception) {
                    lastException = e
                    android.util.Log.w("PasskeyVaultManager", "Keystore init attempt $attempt/3 failed: ${e.message}")
                    try {
                        Thread.sleep(50L * attempt)
                    } catch (_: InterruptedException) {
                        Thread.currentThread().interrupt()
                    }
                }
            }

            android.util.Log.e(
                "PasskeyVaultManager",
                "Android Keystore unavailable. Preserving encrypted vault without deletion: ${lastException?.message}"
            )
            isEncryptedStoreActive = false
            return context.getSharedPreferences(FALLBACK_PREFS_FILE, Context.MODE_PRIVATE)
        }
    }

    init {
        loadPasskeys()
    }

    /**
     * Loads passkeys.
     */
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

    /**
     * Executes the record passkey operation.
     */
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

    /**
     * Removes the specified passkey.
     */
    fun deletePasskey(id: String) {
        val updated = _passkeysFlow.value.filter { it.id != id }
        _passkeysFlow.value = updated
        savePasskeys(updated)
    }

    /**
     * Retrieves the passkeys for domain.
     */
    fun getPasskeysForDomain(domain: String): List<PasskeyItem> {
        val clean = domain.lowercase().trim()
        return _passkeysFlow.value.filter {
            it.rpId.equals(clean, ignoreCase = true) || clean.endsWith(".${it.rpId}") || it.rpId.endsWith(".$clean")
        }
    }

    /**
     * Persists passkeys to storage.
     */
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
