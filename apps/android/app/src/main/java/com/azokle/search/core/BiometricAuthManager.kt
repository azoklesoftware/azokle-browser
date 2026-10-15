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

package com.azokle.search.core

import android.app.KeyguardManager
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.lang.ref.WeakReference

object BiometricAuthManager {
    private const val TAG = "AzokleBiometricAuth"

    @Volatile
    private var activeActivityRef: WeakReference<FragmentActivity>? = null

    /**
     * Updates the current activity.
     */
    fun setCurrentActivity(activity: FragmentActivity?) {
        activeActivityRef = if (activity != null) WeakReference(activity) else null
    }

    /**
     * Handles on activity destroyed events.
     */
    fun onActivityDestroyed(activity: FragmentActivity) {
        if (activeActivityRef?.get() == activity) {
            activeActivityRef = null
        }
    }

    /**
     * Unwraps context hierarchy to locate the host FragmentActivity.
     * Includes lifecycle-aware fallback to active FragmentActivity if context is configuration-wrapped.
     */
    fun unwrapFragmentActivity(context: Context?): FragmentActivity? {
        var current: Context? = context
        while (current != null) {
            if (current is FragmentActivity) return current
            if (current is ContextWrapper) {
                current = current.baseContext
            } else {
                break
            }
        }
        val fallback = activeActivityRef?.get()
        if (fallback != null && !fallback.isFinishing && !fallback.isDestroyed) {
            return fallback
        }
        return null
    }

    /**
     * Checks if the Android device has a secure lock screen (PIN, Pattern, Password, Biometric).
     */
    fun isDeviceSecurityConfigured(context: Context): Boolean {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val isKeyguardSecure = keyguardManager?.isDeviceSecure == true

        val biometricManager = BiometricManager.from(context)
        val canBio = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
        } else {
            biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
        }

        return isKeyguardSecure || canBio
    }

    /**
     * Requests official Android Biometric / Device-Credential authentication for sensitive vault actions.
     * Enforces STRICT DEFAULT-DENY: Callback onDenied is invoked if authentication fails, is cancelled, or errors.
     * Guarantees Main/UI thread execution for BiometricPrompt window attachment.
     */
    fun authenticate(
        context: Context,
        title: String = "Authorize Access",
        subtitle: String = "Verify identity to proceed",
        onAuthenticated: () -> Unit,
        onDenied: (String) -> Unit
    ) {
        val activity = unwrapFragmentActivity(context)
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            Handler(Looper.getMainLooper()).post {
                onDenied("Device authentication required.")
            }
            return
        }

        if (!isDeviceSecurityConfigured(activity)) {
            // AZK-MED-05: Strict default-deny: fail closed when no screen lock or biometrics are configured
            Handler(Looper.getMainLooper()).post {
                onDenied("Device security (PIN, pattern, password, or biometric) must be configured in Android Settings before accessing protected data.")
            }
            return
        }

        // Guaranteed Main/UI Thread execution for BiometricPrompt
        activity.runOnUiThread {
            if (activity.isFinishing || activity.isDestroyed) {
                onDenied("Activity closed.")
                return@runOnUiThread
            }

            val executor = ContextCompat.getMainExecutor(activity)
            val callback = object : BiometricPrompt.AuthenticationCallback() {
                /**
                 * Handles on authentication succeeded events.
                 */
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Log.d(TAG, "Biometric authentication SUCCEEDED: $title")
                    onAuthenticated()
                }

                /**
                 * Handles on authentication error events.
                 */
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Log.w(TAG, "Biometric authentication ERROR ($errorCode): $errString")
                    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_CANCELED
                    ) {
                        onDenied("Authentication cancelled.")
                    } else {
                        onDenied("Authentication failed: $errString")
                    }
                }

                /**
                 * Handles on authentication failed events.
                 */
                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Log.w(TAG, "Biometric attempt failed - user may retry on prompt.")
                }
            }

            try {
                val prompt = BiometricPrompt(activity, executor, callback)
                val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setConfirmationRequired(false)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // API 30+: Supports Biometrics + PIN/Pattern/Password via DEVICE_CREDENTIAL
                    promptInfoBuilder.setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
                    )
                } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                    // API 29: setDeviceCredentialAllowed(true)
                    @Suppress("DEPRECATION")
                    promptInfoBuilder.setDeviceCredentialAllowed(true)
                } else {
                    // API 28: Strong biometric with negative button
                    promptInfoBuilder.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                    promptInfoBuilder.setNegativeButtonText("Cancel")
                }

                prompt.authenticate(promptInfoBuilder.build())
            } catch (e: Exception) {
                Log.e(TAG, "Failed to launch BiometricPrompt: ${e.message}", e)
                onDenied("Authentication error: ${e.message ?: "Failed to verify identity"}")
            }
        }
    }

    /**
     * Backward-compatible alias for authentication.
     */
    fun authenticateForDownload(
        context: Context,
        filename: String,
        onAuthenticated: () -> Unit,
        onDenied: (String) -> Unit
    ) {
        authenticate(
            context = context,
            title = "Authorize Download",
            subtitle = "Verify identity to download \"$filename\"",
            onAuthenticated = onAuthenticated,
            onDenied = onDenied
        )
    }
}
