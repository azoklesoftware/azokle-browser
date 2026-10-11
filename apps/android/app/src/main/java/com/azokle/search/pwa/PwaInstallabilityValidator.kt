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

package com.azokle.search.pwa

import java.net.URI

data class PwaInstallEligibility(
    val isInstallablePwa: Boolean,
    val isAddShortcutEligible: Boolean,
    val title: String,
    val startUrl: String,
    val scope: String,
    val displayMode: DisplayMode,
    val iconUrl: String?,
    val themeColor: Int?,
    val backgroundColor: Int?,
    val verifiedOrigin: String,
    val isHttps: Boolean,
    val reason: String = ""
)

object PwaInstallabilityValidator {

    /**
     * Checks whether secure origin.
     */
    fun isSecureOrigin(url: String): Boolean {
        if (url.isBlank()) return false
        val uri = try { URI(url) } catch (e: Exception) { return false }
        val scheme = uri.scheme?.lowercase() ?: return false
        val host = uri.host?.lowercase() ?: return false

        if (scheme == "https") return true
        if (scheme == "http" && (host == "localhost" || host == "127.0.0.1" || host == "::1")) return true
        return false
    }

    /**
     * Checks whether same origin.
     */
    fun isSameOrigin(url1: String, url2: String): Boolean {
        if (url1.isBlank() || url2.isBlank()) return false
        val uri1 = try { URI(url1) } catch (e: Exception) { return false }
        val uri2 = try { URI(url2) } catch (e: Exception) { return false }

        val scheme1 = uri1.scheme?.lowercase() ?: return false
        val scheme2 = uri2.scheme?.lowercase() ?: return false
        if (scheme1 != scheme2) return false

        val host1 = uri1.host?.lowercase() ?: return false
        val host2 = uri2.host?.lowercase() ?: return false
        if (host1 != host2) return false

        val port1 = if (uri1.port != -1) uri1.port else (if (scheme1 == "https") 443 else 80)
        val port2 = if (uri2.port != -1) uri2.port else (if (scheme2 == "https") 443 else 80)
        return port1 == port2
    }

    /**
     * Checks whether url in scope.
     */
    fun isUrlInScope(url: String, scope: String): Boolean {
        if (url.isBlank() || scope.isBlank()) return false
        if (!isSameOrigin(url, scope)) return false

        val targetUri = try { URI(url) } catch (e: Exception) { return false }
        val scopeUri = try { URI(scope) } catch (e: Exception) { return false }

        val scopePath = scopeUri.path ?: "/"
        val targetPath = targetUri.path ?: "/"

        val normalizedScope = if (scopePath.endsWith("/")) scopePath else "$scopePath/"
        val normalizedTarget = if (targetPath.endsWith("/")) targetPath else "$targetPath/"

        return normalizedTarget.startsWith(normalizedScope) || targetPath == scopePath
    }

    /**
     * Executes the evaluate operation.
     */
    fun evaluate(
        currentUrl: String,
        pageTitle: String,
        manifest: WebAppManifest?,
        fallbackFaviconUrl: String? = null,
        fallbackThemeColor: Int? = null
    ): PwaInstallEligibility {
        val uri = try { URI(currentUrl) } catch (e: Exception) { null }
        val host = uri?.host ?: ""
        val scheme = uri?.scheme?.lowercase() ?: ""
        val isHttps = isSecureOrigin(currentUrl)

        // Cannot install internal or invalid scheme pages
        if (scheme in setOf("about", "file", "content", "javascript", "data", "chrome", "resource") || host.isBlank()) {
            return PwaInstallEligibility(
                isInstallablePwa = false,
                isAddShortcutEligible = false,
                title = pageTitle,
                startUrl = currentUrl,
                scope = "",
                displayMode = DisplayMode.BROWSER,
                iconUrl = null,
                themeColor = null,
                backgroundColor = null,
                verifiedOrigin = host,
                isHttps = false,
                reason = "Internal scheme not eligible for shortcuts"
            )
        }

        val verifiedOrigin = if (uri != null) {
            val portPart = if (uri.port != -1 && !((scheme == "https" && uri.port == 443) || (scheme == "http" && uri.port == 80))) ":${uri.port}" else ""
            "${uri.scheme}://${uri.host}$portPart"
        } else host

        if (manifest != null && isHttps) {
            val bestIcon = manifest.getBestIconUrl() ?: fallbackFaviconUrl
            val title = if (manifest.name.isNotBlank()) manifest.name else (if (manifest.shortName.isNotBlank()) manifest.shortName else pageTitle.ifBlank { host })
            
            // W3C Same-Origin Enforcement: manifest scope must be same-origin as the document
            val rawScope = manifest.scope.ifBlank { verifiedOrigin }
            val scope = if (isSameOrigin(rawScope, currentUrl)) rawScope else verifiedOrigin

            // Ensure start_url is same-origin and within scope
            val rawStartUrl = manifest.startUrl.ifBlank { currentUrl }
            val validStartUrl = if (isSameOrigin(rawStartUrl, currentUrl) && isUrlInScope(rawStartUrl, scope)) {
                rawStartUrl
            } else {
                currentUrl
            }

            return PwaInstallEligibility(
                isInstallablePwa = true,
                isAddShortcutEligible = true,
                title = title,
                startUrl = validStartUrl,
                scope = scope,
                displayMode = manifest.display,
                iconUrl = bestIcon,
                themeColor = manifest.themeColor ?: fallbackThemeColor,
                backgroundColor = manifest.backgroundColor,
                verifiedOrigin = verifiedOrigin,
                isHttps = true,
                reason = "Valid Web App Manifest detected"
            )
        }

        // Fallback standard shortcut eligibility
        val fallbackTitle = pageTitle.ifBlank { host }
        return PwaInstallEligibility(
            isInstallablePwa = false,
            isAddShortcutEligible = true,
            title = fallbackTitle,
            startUrl = currentUrl,
            scope = verifiedOrigin,
            displayMode = DisplayMode.BROWSER,
            iconUrl = fallbackFaviconUrl,
            themeColor = fallbackThemeColor,
            backgroundColor = null,
            verifiedOrigin = verifiedOrigin,
            isHttps = isHttps,
            reason = "Standard webpage (No manifest found)"
        )
    }
}
