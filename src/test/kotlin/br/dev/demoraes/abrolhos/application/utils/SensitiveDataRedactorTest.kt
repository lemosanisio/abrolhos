package br.dev.demoraes.abrolhos.application.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SensitiveDataRedactorTest {

    @Test
    fun `should redact email`() {
        val input = "Log contains someone@example.com inside"
        val expected = "Log contains [REDACTED_EMAIL] inside"
        assertEquals(expected, SensitiveDataRedactor.redact(input))
    }

    @Test
    fun `should redact password field`() {
        val input = "user logged in password: mySecretPassword123"
        val expected = "user logged in password: [REDACTED_PASSWORD]"
        assertEquals(expected, SensitiveDataRedactor.redact(input))
    }

    @Test
    fun `should redact token field`() {
        val input = "{\"token\": \"abc123XYZ\"}"
        val expected = "{\"token\": \"[REDACTED_TOKEN]\"}"
        assertEquals(expected, SensitiveDataRedactor.redact(input))
    }
}
