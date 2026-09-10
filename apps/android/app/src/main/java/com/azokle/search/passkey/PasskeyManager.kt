package com.azokle.search.passkey

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Sovereign Passkey & WebAuthn Controller for Android 14+ / AndroidX Credentials.
 */
object PasskeyManager {
    private const val TAG = "AzoklePasskeyManager"

    /**
     * Executes Passkey Registration (FIDO2 / WebAuthn navigator.credentials.create()).
     */
    suspend fun createPasskey(
        activity: Activity,
        requestJson: String,
        rpId: String? = null,
        userName: String? = null
    ): PasskeyAuthResult = withContext(Dispatchers.Main) {
        try {
            val credentialManager = CredentialManager.create(activity)
            val createRequest = CreatePublicKeyCredentialRequest(requestJson)

            val response = credentialManager.createCredential(activity, createRequest)
            if (response is CreatePublicKeyCredentialResponse) {
                val responseJson = response.registrationResponseJson

                // Parse details to save in Passkey Vault
                val effectiveRpId = rpId ?: extractRpIdFromJson(requestJson)
                val effectiveUserName = userName ?: extractUserNameFromJson(requestJson)
                if (effectiveRpId.isNotBlank()) {
                    PasskeyVaultManager.getInstance(activity).recordPasskey(
                        rpId = effectiveRpId,
                        userName = effectiveUserName.ifBlank { "Passkey User" },
                        isSovereign = false
                    )
                }

                Log.i(TAG, "Passkey created successfully for $effectiveRpId ($effectiveUserName)")
                PasskeyAuthResult.Success(responseJson)
            } else {
                PasskeyAuthResult.Error("Unsupported credential response type")
            }
        } catch (e: CreateCredentialCancellationException) {
            Log.d(TAG, "Passkey creation cancelled by user.")
            PasskeyAuthResult.Cancelled
        } catch (e: CreateCredentialException) {
            Log.w(TAG, "Passkey creation failed: ${e.message}", e)
            PasskeyAuthResult.Error(e.message ?: "Failed to create passkey", e.type)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error creating passkey: ${e.message}", e)
            PasskeyAuthResult.Error(e.message ?: "Passkey creation error")
        }
    }

    /**
     * Executes Passkey Authentication (FIDO2 / WebAuthn navigator.credentials.get()).
     */
    suspend fun getPasskeyAssertion(
        activity: Activity,
        requestJson: String
    ): PasskeyAuthResult = withContext(Dispatchers.Main) {
        try {
            val credentialManager = CredentialManager.create(activity)
            val getOption = GetPublicKeyCredentialOption(requestJson)
            val getRequest = GetCredentialRequest.Builder()
                .addCredentialOption(getOption)
                .build()

            val response = credentialManager.getCredential(activity, getRequest)
            val credential = response.credential

            if (credential is PublicKeyCredential) {
                val responseJson = credential.authenticationResponseJson

                val rpId = extractRpIdFromJson(requestJson)
                if (rpId.isNotBlank()) {
                    PasskeyVaultManager.getInstance(activity).recordPasskey(
                        rpId = rpId,
                        userName = "Verified User",
                        isSovereign = false
                    )
                }

                Log.i(TAG, "Passkey assertion signed successfully for $rpId")
                PasskeyAuthResult.Success(responseJson)
            } else {
                PasskeyAuthResult.Error("Unsupported assertion response type")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.d(TAG, "Passkey assertion cancelled by user.")
            PasskeyAuthResult.Cancelled
        } catch (e: GetCredentialException) {
            Log.w(TAG, "Passkey assertion failed: ${e.message}", e)
            PasskeyAuthResult.Error(e.message ?: "Failed to verify passkey", e.type)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during passkey sign-in: ${e.message}", e)
            PasskeyAuthResult.Error(e.message ?: "Passkey assertion error")
        }
    }

    private fun extractRpIdFromJson(jsonStr: String): String {
        return try {
            val root = JSONObject(jsonStr)
            val pk = root.optJSONObject("publicKey") ?: root
            val rp = pk.optJSONObject("rp")
            rp?.optString("id", "") ?: pk.optString("rpId", "")
        } catch (e: Exception) {
            ""
        }
    }

    private fun extractUserNameFromJson(jsonStr: String): String {
        return try {
            val root = JSONObject(jsonStr)
            val pk = root.optJSONObject("publicKey") ?: root
            val user = pk.optJSONObject("user")
            user?.optString("name", "") ?: user?.optString("displayName", "") ?: ""
        } catch (e: Exception) {
            ""
        }
    }
}
