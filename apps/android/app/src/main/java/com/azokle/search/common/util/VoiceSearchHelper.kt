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
