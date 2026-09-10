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

    fun isSecureOrigin(url: String): Boolean {
        if (url.isBlank()) return false
        val uri = try { URI(url) } catch (e: Exception) { return false }
        val scheme = uri.scheme?.lowercase() ?: return false
        val host = uri.host?.lowercase() ?: return false

        if (scheme == "https") return true
        if (scheme == "http" && (host == "localhost" || host == "127.0.0.1" || host == "::1")) return true
        return false
    }

    fun isUrlInScope(url: String, scope: String): Boolean {
        if (url.isBlank() || scope.isBlank()) return false
        val targetUri = try { URI(url) } catch (e: Exception) { return false }
        val scopeUri = try { URI(scope) } catch (e: Exception) { return false }

        // Must match scheme and host
        if (targetUri.scheme?.lowercase() != scopeUri.scheme?.lowercase()) return false
        if (targetUri.host?.lowercase() != scopeUri.host?.lowercase()) return false

        val scopePath = scopeUri.path ?: "/"
        val targetPath = targetUri.path ?: "/"

        val normalizedScope = if (scopePath.endsWith("/")) scopePath else "$scopePath/"
        val normalizedTarget = if (targetPath.endsWith("/")) targetPath else "$targetPath/"

        return normalizedTarget.startsWith(normalizedScope) || targetPath == scopePath
    }

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

        val verifiedOrigin = if (uri != null) "${uri.scheme}://${uri.authority ?: uri.host}" else host

        if (manifest != null && isHttps) {
            val bestIcon = manifest.getBestIconUrl() ?: fallbackFaviconUrl
            val title = if (manifest.name.isNotBlank()) manifest.name else (if (manifest.shortName.isNotBlank()) manifest.shortName else pageTitle.ifBlank { host })
            val startUrl = manifest.startUrl.ifBlank { currentUrl }
            val scope = manifest.scope.ifBlank { verifiedOrigin }

            // Ensure start_url is within scope / origin
            val validStartUrl = if (isUrlInScope(startUrl, scope)) startUrl else currentUrl

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
