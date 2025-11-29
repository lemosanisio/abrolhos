
package br.dev.demoraes.abrolhos.domain.entities

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class UsernameTest {
    @Test
    fun `should create a valid username`() {
        val username = Username("valid_user_123")
        assertEquals("valid_user_123", username.value)
    }

    @Test
    fun `should throw exception for blank username`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                Username(" ")
            }
        assertEquals("Username cannot be blank.", exception.message)
    }

    @Test
    fun `should throw exception for username with invalid characters`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                Username("invalid-user!")
            }
        assertEquals("Username can only contain lowercase letters, numbers, and underscores.", exception.message)
    }

    @Test
    fun `should throw exception for username that is too short`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                Username("a")
            }
        assertEquals("Username must be between 3 and 20 characters.", exception.message)
    }

    @Test
    fun `should throw exception for username that is too long`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                Username("a_very_long_username_that_is_not_valid")
            }
        assertEquals("Username must be between 3 and 20 characters.", exception.message)
    }

    @Test
    fun `should throw exception for reserved username`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                Username("admin")
            }
        assertEquals("Username 'admin' is a reserved word and cannot be used.", exception.message)
    }

    @Test
    fun `should handle and normalize uppercase usernames`() {
        val username = Username("VALIDUSER")
        assertEquals("validuser", username.toString())
    }
}
