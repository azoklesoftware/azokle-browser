package com.azokle.search.pwa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PwaLogicTest {

    @Test
    fun testManifestParsingComplete() {
        val json = """
        {
            "name": "Azokle Test App",
            "short_name": "AzokleApp",
            "description": "A sovereign test progressive web app",
            "start_url": "/app/home",
            "scope": "/app/",
            "display": "standalone",
            "orientation": "portrait",
            "theme_color": "#18181B",
            "background_color": "#000000",
            "icons": [
                {
                    "src": "/icons/icon-192.png",
                    "sizes": "192x192",
                    "type": "image/png",
                    "purpose": "any"
                },
                {
                    "src": "/icons/icon-512.png",
                    "sizes": "512x512",
                    "type": "image/png",
                    "purpose": "maskable"
                }
            ],
            "shortcuts": [
                {
                    "name": "Search",
                    "url": "/app/search"
                }
            ]
        }
        """.trimIndent()

        val manifest = WebAppManifest.parse(
            jsonString = json,
            manifestUrl = "https://example.com/manifest.json",
            documentUrl = "https://example.com/app/"
        )

        assertNotNull(manifest)
        assertEquals("Azokle Test App", manifest?.name)
        assertEquals("AzokleApp", manifest?.shortName)
        assertEquals("https://example.com/app/home", manifest?.startUrl)
        assertEquals("https://example.com/app/", manifest?.scope)
        assertEquals(DisplayMode.STANDALONE, manifest?.display)
        assertEquals("portrait", manifest?.orientation)
        assertEquals(2, manifest?.icons?.size)
        assertEquals("https://example.com/icons/icon-192.png", manifest?.icons?.get(0)?.src)
        assertEquals("https://example.com/icons/icon-512.png", manifest?.icons?.get(1)?.src)
        assertEquals("https://example.com/icons/icon-512.png", manifest?.getBestIconUrl(preferMaskable = true))
        assertEquals(1, manifest?.shortcuts?.size)
        assertEquals("https://example.com/app/search", manifest?.shortcuts?.get(0)?.url)
    }

    @Test
    fun testInstallabilityValidatorSecureOrigin() {
        assertTrue(PwaInstallabilityValidator.isSecureOrigin("https://example.com/app"))
        assertTrue(PwaInstallabilityValidator.isSecureOrigin("http://localhost:8080/"))
        assertTrue(PwaInstallabilityValidator.isSecureOrigin("http://127.0.0.1:3000/"))
        assertFalse(PwaInstallabilityValidator.isSecureOrigin("http://insecure-site.com/"))
        assertFalse(PwaInstallabilityValidator.isSecureOrigin("about:blank"))
    }

    @Test
    fun testScopeJailMatching() {
        val scope = "https://example.com/app/"
        assertTrue(PwaInstallabilityValidator.isUrlInScope("https://example.com/app/home", scope))
        assertTrue(PwaInstallabilityValidator.isUrlInScope("https://example.com/app/settings/profile", scope))
        assertTrue(PwaInstallabilityValidator.isUrlInScope("https://example.com/app/", scope))
        assertFalse(PwaInstallabilityValidator.isUrlInScope("https://example.com/blog/article", scope))
        assertFalse(PwaInstallabilityValidator.isUrlInScope("https://otherdomain.com/app/home", scope))
        assertFalse(PwaInstallabilityValidator.isUrlInScope("http://example.com/app/home", scope))
    }

    @Test
    fun testInstallabilityEvaluation() {
        val json = """
        {
            "name": "Social App",
            "start_url": "/feed",
            "scope": "/",
            "display": "standalone",
            "icons": [{"src": "/icon.png", "sizes": "192x192"}]
        }
        """.trimIndent()

        val manifest = WebAppManifest.parse(
            jsonString = json,
            manifestUrl = "https://social.example.com/manifest.json",
            documentUrl = "https://social.example.com/feed"
        )

        // 1. Valid HTTPS PWA
        val pwaEligibility = PwaInstallabilityValidator.evaluate(
            currentUrl = "https://social.example.com/feed",
            pageTitle = "Feed",
            manifest = manifest
        )
        assertTrue(pwaEligibility.isInstallablePwa)
        assertTrue(pwaEligibility.isAddShortcutEligible)
        assertEquals(DisplayMode.STANDALONE, pwaEligibility.displayMode)
        assertEquals("Social App", pwaEligibility.title)

        // 2. Standard Webpage (No Manifest)
        val webEligibility = PwaInstallabilityValidator.evaluate(
            currentUrl = "https://news.example.com/article",
            pageTitle = "Breaking News",
            manifest = null,
            fallbackFaviconUrl = "https://news.example.com/favicon.ico"
        )
        assertFalse(webEligibility.isInstallablePwa)
        assertTrue(webEligibility.isAddShortcutEligible)
        assertEquals("Breaking News", webEligibility.title)

        // 3. Internal scheme (Not Eligible)
        val internalEligibility = PwaInstallabilityValidator.evaluate(
            currentUrl = "about:blank",
            pageTitle = "Blank",
            manifest = null
        )
        assertFalse(internalEligibility.isInstallablePwa)
        assertFalse(internalEligibility.isAddShortcutEligible)
    }

    @Test
    fun testInstalledPwaJsonSerialization() {
        val item = InstalledPwa(
            id = "test-pwa-id",
            title = "Test PWA",
            startUrl = "https://test.com/app",
            scope = "https://test.com/",
            displayMode = DisplayMode.STANDALONE,
            themeColor = 0xFF00FF,
            backgroundColor = 0x000000,
            installedAt = 1700000000000L
        )

        val json = item.toJson()
        val parsed = InstalledPwa.fromJson(json)

        assertEquals(item.id, parsed.id)
        assertEquals(item.title, parsed.title)
        assertEquals(item.startUrl, parsed.startUrl)
        assertEquals(item.scope, parsed.scope)
        assertEquals(item.displayMode, parsed.displayMode)
        assertEquals(item.themeColor, parsed.themeColor)
        assertEquals(item.backgroundColor, parsed.backgroundColor)
        assertEquals(item.installedAt, parsed.installedAt)
    }
}
