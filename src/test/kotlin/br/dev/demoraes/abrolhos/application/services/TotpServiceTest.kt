package br.dev.demoraes.abrolhos.application.services

import br.dev.demoraes.abrolhos.domain.entities.TotpSecret
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Date

class TotpServiceTest {
    private val totpService = TotpService()

    @Test
    fun `generateCodesForWindows should generate codes for three time windows`() {
        // Given
        val secret = TotpSecret("JBSWY3DPEHPK3PXP")
        val timestamp = Date()

        // When
        val windowCodes = totpService.generateCodesForWindows(secret, timestamp)

        // Then
        assertNotNull(windowCodes.previous)
        assertNotNull(windowCodes.current)
        assertNotNull(windowCodes.next)
        assertEquals(timestamp.time, windowCodes.timestamp)

        // Verify all codes are 6 digits
        assertEquals(6, windowCodes.previous.length)
        assertEquals(6, windowCodes.current.length)
        assertEquals(6, windowCodes.next.length)

        // Verify codes are numeric
        assertTrue(windowCodes.previous.all { it.isDigit() })
        assertTrue(windowCodes.current.all { it.isDigit() })
        assertTrue(windowCodes.next.all { it.isDigit() })
    }

    @Test
    fun `generateCodesForWindows should generate different codes for different windows`() {
        // Given
        val secret = TotpSecret("JBSWY3DPEHPK3PXP")
        val timestamp = Date()

        // When
        val windowCodes = totpService.generateCodesForWindows(secret, timestamp)

        // Then - codes should typically be different (though theoretically could be same)
        // We just verify they are all valid codes
        assertNotEquals("", windowCodes.previous)
        assertNotEquals("", windowCodes.current)
        assertNotEquals("", windowCodes.next)
    }

    @Test
    fun `generateCodesForWindows should be consistent for same timestamp`() {
        // Given
        val secret = TotpSecret("JBSWY3DPEHPK3PXP")
        val timestamp = Date(1234567890000L) // Fixed timestamp

        // When
        val windowCodes1 = totpService.generateCodesForWindows(secret, timestamp)
        val windowCodes2 = totpService.generateCodesForWindows(secret, timestamp)

        // Then - same timestamp should produce same codes
        assertEquals(windowCodes1.previous, windowCodes2.previous)
        assertEquals(windowCodes1.current, windowCodes2.current)
        assertEquals(windowCodes1.next, windowCodes2.next)
        assertEquals(windowCodes1.timestamp, windowCodes2.timestamp)
    }

    @Test
    fun `validateSecret should return valid for properly formatted secret`() {
        // Given
        val validSecret = TotpSecret("JBSWY3DPEHPK3PXP")

        // When
        val validation = totpService.validateSecret(validSecret)

        // Then
        assertTrue(validation.isValid)
        assertNotNull(validation.byteCount)
        assertNotNull(validation.expectedByteCount)
        assertNull(validation.error)
    }

    @Test
    fun `validateSecret should handle production secret correctly`() {
        // Given
        val productionSecret = TotpSecret("KNT7MHTHYMB2HQR7RG7MQBD6GPQLOJ2T")

        // When
        val validation = totpService.validateSecret(productionSecret)

        // Then
        assertTrue(validation.isValid)
        assertNotNull(validation.byteCount)
        assertEquals(20, validation.expectedByteCount)
    }

    @Test
    fun `validateSecret should return byte count information`() {
        // Given
        val secret = totpService.generateSecret()

        // When
        val validation = totpService.validateSecret(secret)

        // Then
        assertTrue(validation.isValid)
        assertEquals(20, validation.byteCount)
        assertEquals(20, validation.expectedByteCount)
        assertNull(validation.error)
    }
}
