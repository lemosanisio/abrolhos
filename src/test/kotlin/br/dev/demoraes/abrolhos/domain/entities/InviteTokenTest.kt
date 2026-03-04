package br.dev.demoraes.abrolhos.domain.entities

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class InviteTokenTest {
    @Test
    fun `should create valid invite token with minimum length`() {
        val tokenValue = "a".repeat(32)
        val token = InviteToken(tokenValue)
        assertEquals(tokenValue, token.value)
    }

    @Test
    fun `should create valid invite token with length greater than minimum`() {
        val tokenValue = "a".repeat(64)
        val token = InviteToken(tokenValue)
        assertEquals(tokenValue, token.value)
    }

    @Test
    fun `should reject blank invite token`() {
        val exception = assertThrows<IllegalArgumentException> {
            InviteToken("")
        }
        assertEquals("Invite token cannot be blank.", exception.message)
    }

    @Test
    fun `should reject invite token shorter than minimum length`() {
        val tokenValue = "a".repeat(31)
        val exception = assertThrows<IllegalArgumentException> {
            InviteToken(tokenValue)
        }
        assertEquals("Invite token must be at least 32 characters.", exception.message)
    }

    @Test
    fun `should convert to string correctly`() {
        val tokenValue = "a".repeat(32)
        val token = InviteToken(tokenValue)
        assertEquals(tokenValue, token.toString())
    }
}
