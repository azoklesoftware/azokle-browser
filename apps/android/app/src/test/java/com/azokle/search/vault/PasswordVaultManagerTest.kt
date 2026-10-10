package com.azokle.search.vault

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordVaultManagerTest {

    @Test
    fun testSavedLoginModelStructure() {
        val login = SavedLogin(
            origin = "https://github.com",
            username = "octocat",
            password = "supersecretpassword123"
        )
        assertEquals("https://github.com", login.origin)
        assertEquals("octocat", login.username)
        assertEquals("supersecretpassword123", login.password)
        assertTrue(login.id.isNotBlank())
        assertTrue(login.dateModified > 0)
    }

    @Test
    fun testOriginMatchingLogic() {
        fun matchesOrigin(loginOrigin: String, query: String): Boolean {
            return loginOrigin.contains(query, ignoreCase = true) || query.contains(loginOrigin, ignoreCase = true)
        }

        assertTrue(matchesOrigin("https://github.com", "github.com"))
        assertTrue(matchesOrigin("github.com", "https://github.com/login"))
        assertFalse(matchesOrigin("https://gitlab.com", "github.com"))
    }

    @Test
    fun testGenerateStrongPassword() {
        val pw = PasswordVaultManager.generateStrongPassword(20, true)
        assertEquals(20, pw.length)
        val pwShort = PasswordVaultManager.generateStrongPassword(4, true)
        assertEquals(8, pwShort.length) // coerced to min 8
    }
}
