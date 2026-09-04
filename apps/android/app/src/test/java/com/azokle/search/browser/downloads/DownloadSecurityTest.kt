package com.azokle.search.browser.downloads

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DownloadSecurityTest {

    @Test
    fun testPathTraversalWithForwardSlashes() {
        val malicious = "../../etc/passwd"
        val sanitized = DownloadManager.sanitizeFilename(malicious)

        assertFalse(sanitized.contains(".."))
        assertFalse(sanitized.contains("/"))
        assertTrue(sanitized.contains("passwd"))
    }

    @Test
    fun testPathTraversalWithBackslashes() {
        val malicious = "..\\..\\windows\\system32\\cmd.exe"
        val sanitized = DownloadManager.sanitizeFilename(malicious)

        assertFalse(sanitized.contains(".."))
        assertFalse(sanitized.contains("\\"))
        assertTrue(sanitized.contains("cmd.exe"))
    }

    @Test
    fun testAbsolutePathEscape() {
        val malicious = "/data/data/com.azokle.search/databases/secret.db"
        val sanitized = DownloadManager.sanitizeFilename(malicious)

        assertFalse(sanitized.startsWith("/"))
        assertTrue(sanitized == "secret.db")
    }

    @Test
    fun testRecursiveDotDotTraversal() {
        val malicious = "....//....//payload.bin"
        val sanitized = DownloadManager.sanitizeFilename(malicious)

        assertFalse(sanitized.contains(".."))
        assertFalse(sanitized.contains("/"))
        assertTrue(sanitized.contains("payload.bin"))
    }

    @Test
    fun testNullByteInjection() {
        val malicious = "innocent.pdf\u0000.sh"
        val sanitized = DownloadManager.sanitizeFilename(malicious)

        assertFalse(sanitized.contains("\u0000"))
        assertTrue(sanitized.contains("innocent.pdf"))
    }

    @Test
    fun testEmptyOrAllDotsFallback() {
        val dotInput = "...."
        val sanitized = DownloadManager.sanitizeFilename(dotInput)

        assertTrue(sanitized.startsWith("download_"))
        assertFalse(sanitized.contains(".."))
    }

    @Test
    fun testCanonicalDirectoryContainment() {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "azokle_test_downloads").apply { mkdirs() }
        try {
            val maliciousFilename = "../../escaped.txt"
            val sanitized = DownloadManager.sanitizeFilename(maliciousFilename)
            val resolvedFile = File(tempDir, sanitized)

            val dirCanonical = tempDir.canonicalFile
            val fileCanonical = resolvedFile.canonicalFile

            assertTrue(
                "File must be strictly contained inside the download directory",
                fileCanonical.path.startsWith(dirCanonical.path + File.separator)
            )
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
