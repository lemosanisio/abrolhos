package br.dev.demoraes.abrolhos.domain.entities

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TotpSecretTest {

    @Test
    fun `TotpSecret should be valid with base32 characters`() {
        val secret = "JBSWY3DPEHPK3PXP"
        val totpSecret = TotpSecret(secret)
        assertEquals(secret, totpSecret.value)
    }

    @Test
    fun `TotpSecret should throw exception when blank`() {
        assertThrows<IllegalArgumentException> {
            TotpSecret("")
        }
        assertThrows<IllegalArgumentException> {
            TotpSecret("   ")
        }
    }

    @Test
    fun `TotpSecret should throw exception with lowercase characters`() {
        assertThrows<IllegalArgumentException> {
            TotpSecret("jbswy3dpehpk3pxp")
        }
    }

    @Test
    fun `TotpSecret should throw exception with invalid base32 characters`() {
        assertThrows<IllegalArgumentException> {
            TotpSecret("INVALID1890")
        }
        assertThrows<IllegalArgumentException> {
            TotpSecret("ABCD=")
        }
    }

    @Test
    fun `TotpSecret should accept all valid base32 characters`() {
        val secret = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        val totpSecret = TotpSecret(secret)
        assertEquals(secret, totpSecret.value)
    }
}
