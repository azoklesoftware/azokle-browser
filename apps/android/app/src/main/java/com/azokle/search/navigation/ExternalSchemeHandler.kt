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
import com.azokle.search.navigation.ExternalNavigationHandler
import com.azokle.search.navigation.UriClassifier
import org.mozilla.geckoview.GeckoSession

/**
 * Handles external URL schemes by delegating to the production-grade [ExternalNavigationHandler].
 * Maintains backwards compatibility for existing callers.
 */
object ExternalSchemeHandler {

    val SAFE_EXTERNAL_SCHEMES = setOf(
        "tel",
        "telprompt",
        "callto",
        "mailto",
        "sms",
        "smsto",
        "mms",
        "mmsto",
        "geo",
        "market",
        "webcal",
        "upi",
        "paytmmp",
        "intent"
    )

    /**
     * Checks whether external scheme.
     */
    fun isExternalScheme(uriStr: String): Boolean {
        return ExternalNavigationHandler.isExternalNavigation(uriStr)
    }

    /**
     * Dispatches an external URI to the appropriate native Android application.
     * Returns true if the scheme was recognized and processed, false otherwise.
     */
    fun handleExternalUri(
        context: Context,
        uriStr: String,
        session: GeckoSession? = null,
        showToastOnError: Boolean = true,
        hasUserGesture: Boolean = true,
        isPrivate: Boolean = false,
        isTor: Boolean = false
    ): Boolean {
        return ExternalNavigationHandler.handleNavigation(
            context = context,
            uriStr = uriStr,
            session = session,
            hasUserGesture = hasUserGesture,
            isPrivate = isPrivate,
            isTor = isTor
        )
    }
}

