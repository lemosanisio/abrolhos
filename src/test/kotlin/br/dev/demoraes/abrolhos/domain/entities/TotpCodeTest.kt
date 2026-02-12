package br.dev.demoraes.abrolhos.domain.entities

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TotpCodeTest {

    @Test
    fun `TotpCode should be valid with 6 digits`() {
        val code = "123456"
        val totpCode = TotpCode(code)
        assertEquals(code, totpCode.value)
    }

    @Test
    fun `TotpCode should accept all numeric digits`() {
        val code = "000000"
        val totpCode = TotpCode(code)
        assertEquals(code, totpCode.value)
    }

    @Test
    fun `TotpCode should throw exception when less than 6 digits`() {
        assertThrows<IllegalArgumentException> {
            TotpCode("12345")
        }
    }

    @Test
    fun `TotpCode should throw exception when more than 6 digits`() {
        assertThrows<IllegalArgumentException> {
            TotpCode("1234567")
        }
    }

    @Test
    fun `TotpCode should throw exception when contains non-numeric characters`() {
        assertThrows<IllegalArgumentException> {
            TotpCode("12345a")
        }
        assertThrows<IllegalArgumentException> {
            TotpCode("abcdef")
        }
    }

    @Test
    fun `TotpCode should throw exception when blank`() {
        assertThrows<IllegalArgumentException> {
            TotpCode("")
        }
    }

    @Test
    fun `TotpCode should throw exception when contains spaces`() {
        assertThrows<IllegalArgumentException> {
            TotpCode("123 456")
        }
    }
}
