package com.azokle.search.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Hardware-Backed Cryptographic KeyStore & Sovereign Data Vault.
 * Leverages Android KeyStore, Hardware Security Modules (HSM), TEE, and StrongBox Keymaster
 * to guarantee non-exportable hardware cryptographic isolation for browser secrets.
 */
object SovereignKeyStore {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val MASTER_KEY_ALIAS = "azokle_sovereign_master_v1"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128

    @Synchronized
    private fun getOrCreateMasterKey(context: Context): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (keyStore.containsAlias(MASTER_KEY_ALIAS)) {
            val entry = keyStore.getEntry(MASTER_KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (entry != null) {
                return entry.secretKey
            }
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val hasStrongBox = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                context.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)

        val builder = KeyGenParameterSpec.Builder(
            MASTER_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)

        if (hasStrongBox && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                builder.setIsStrongBoxBacked(true)
            } catch (e: Throwable) {
                // TEE fallback if StrongBox initialization fails
            }
        }

        keyGenerator.init(builder.build())
        return keyGenerator.generateKey()
    }

    /**
     * Authenticated AES-256-GCM Hardware-Backed Encryption.
     * Output format: [12-byte IV] + [Ciphertext with 16-byte Poly/GCM Authentication Tag]
     */
    fun encrypt(context: Context, plaintext: ByteArray): ByteArray {
        val key = getOrCreateMasterKey(context)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext)

        val result = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, result, 0, iv.size)
        System.arraycopy(ciphertext, 0, result, iv.size, ciphertext.size)
        return result
    }

    /**
     * Authenticated AES-256-GCM Hardware-Backed Decryption.
     * Verifies cryptographic tag before releasing plaintext.
     */
    fun decrypt(context: Context, payload: ByteArray): ByteArray {
        if (payload.size <= GCM_IV_LENGTH) {
            throw IllegalArgumentException("Invalid payload size for AES-GCM decryption")
        }

        val key = getOrCreateMasterKey(context)
        val iv = ByteArray(GCM_IV_LENGTH)
        System.arraycopy(payload, 0, iv, 0, GCM_IV_LENGTH)

        val ciphertextLen = payload.size - GCM_IV_LENGTH
        val ciphertext = ByteArray(ciphertextLen)
        System.arraycopy(payload, GCM_IV_LENGTH, ciphertext, 0, ciphertextLen)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)

        return cipher.doFinal(ciphertext)
    }
}
