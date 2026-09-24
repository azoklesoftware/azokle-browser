package com.azokle.search.common.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * Utility to verify speech recognition availability.
 * Seamlessly detects custom privacy ROMs (GrapheneOS, LineageOS, CalyxOS, /e/OS),
 * de-Googled Android builds without speech services, and legacy devices.
 */
object VoiceSearchHelper {

    /**
     * Returns true if speech recognition / voice search is available and supported on this device.
     */
    fun isVoiceSearchAvailable(context: Context): Boolean {
        return try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                return true
            }

            val pm = context.packageManager
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            val activities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            }
            activities.isNotEmpty()
        } catch (_: Throwable) {
            false
        }
    }
}
