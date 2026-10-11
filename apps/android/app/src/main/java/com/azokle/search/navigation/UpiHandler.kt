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

package com.azokle.search.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import org.mozilla.geckoview.GeckoSession

/**
 * Handles Unified Payments Interface (UPI) payment links according to NPCI standards.
 */
object UpiHandler {

    data class UpiPaymentDetails(
        val payeeAddress: String,
        val payeeName: String?,
        val amount: Double?,
        val currency: String,
        val transactionNote: String?,
        val referenceId: String?,
        val rawUri: Uri?
    )

    /**
     * Parses and validates a UPI URI (upi://pay?...).
     * Uses pure string parsing so it is 100% reliable in both unit tests and Android runtime.
     */
    fun parseUpiUri(uriStr: String): UpiPaymentDetails? {
        val trimmed = uriStr.trim()
        val scheme = UriClassifier.extractScheme(trimmed)
        if (scheme != "upi") return null

        val queryIdx = trimmed.indexOf('?')
        if (queryIdx == -1) return null
        val queryString = trimmed.substring(queryIdx + 1)
        val params = queryString.split('&').filter { it.isNotBlank() }.associate { param ->
            val eqIdx = param.indexOf('=')
            if (eqIdx != -1) {
                val key = param.substring(0, eqIdx)
                val value = param.substring(eqIdx + 1)
                key to try { java.net.URLDecoder.decode(value, "UTF-8") } catch (e: Exception) { value }
            } else {
                param to ""
            }
        }

        val pa = params["pa"] ?: return null
        if (!pa.contains('@') || pa.length < 3) return null

        val pn = params["pn"]
        val amStr = params["am"]
        val amount = amStr?.toDoubleOrNull()?.takeIf { it > 0.0 }
        val cu = params["cu"] ?: "INR"
        val tn = params["tn"]
        val tr = params["tr"]

        val uri = try { Uri.parse(trimmed) } catch (e: Throwable) { null }

        return UpiPaymentDetails(
            payeeAddress = pa,
            payeeName = pn,
            amount = amount,
            currency = cu,
            transactionNote = tn,
            referenceId = tr,
            rawUri = uri
        )
    }

    /**
     * Dispatches a UPI payment request to an installed UPI application using Android's chooser.
     */
    fun handlePayment(
        context: Context,
        uriStr: String,
        session: GeckoSession? = null
    ): Boolean {
        // Validate structure
        val paymentDetails = parseUpiUri(uriStr)
        if (paymentDetails == null && uriStr.trim().startsWith("upi:", ignoreCase = true)) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "Invalid UPI payment link", Toast.LENGTH_SHORT).show()
            }
            return true
        }

        val uri = try { Uri.parse(uriStr.trim()) } catch (e: Exception) { return false }
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        // Dynamically query matching UPI apps
        val handlers = IntentResolver.resolveHandlers(context, intent)
        if (handlers.isEmpty()) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "No UPI payment app (GPay, PhonePe, Paytm, BHIM) installed", Toast.LENGTH_LONG).show()
            }
            return true
        }

        // If multiple apps installed, use Android system chooser with clear title
        val targetIntent = if (handlers.size > 1) {
            Intent.createChooser(intent, "Pay using UPI").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            intent
        }

        return ExternalAppLauncher.launch(
            context = context,
            intent = targetIntent,
            session = session,
            showToastOnError = true
        ) == ExternalAppLauncher.LaunchResult.SUCCESS
    }
}
