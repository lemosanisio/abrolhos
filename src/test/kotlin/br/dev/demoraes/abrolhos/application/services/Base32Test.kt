package br.dev.demoraes.abrolhos.application.services

import org.apache.commons.codec.binary.Base32
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test
import java.security.SecureRandom

class Base32Test {

    @Test
    fun testUnpaddedDecoding() {
        val base32 = Base32()

        // Test Case 1: Known padded secret
        val original = "Hello!".toByteArray()
        val padded = base32.encodeToString(original) // "NBSWY3DPEBGC4==="
        val unpadded = padded.replace("=", "")

        println("Original: ${String(original)}")
        println("Padded: $padded")
        println("Unpadded: $unpadded")

        val decodedPadded = base32.decode(padded)
        val decodedUnpadded = base32.decode(unpadded)

        assertArrayEquals(original, decodedPadded, "Padded decoding failed")
        assertArrayEquals(original, decodedUnpadded, "Unpadded decoding failed")
    }

    @Test
    fun testRandomSecretDecoding() {
        val base32 = Base32()
        val randomBytes = ByteArray(20)
        SecureRandom().nextBytes(randomBytes)

        val padded = base32.encodeToString(randomBytes)
        val unpadded = padded.replace("=", "")

        val decodedPadded = base32.decode(padded)
        val decodedUnpadded = base32.decode(unpadded)

        assertArrayEquals(randomBytes, decodedPadded, "Padded random decoding failed")
        assertArrayEquals(randomBytes, decodedUnpadded, "Unpadded random decoding failed")
    }

    /**
     * Unit test for production secret round-trip consistency.
     *
     * **Validates: Requirements 3.4**
     *
     * This test verifies that the production secret "KNT7MHTHYMB2HQR7RG7MQBD6GPQLOJ2T"
     * can be decoded and re-encoded consistently, ensuring the Base32 implementation
     * handles this specific secret correctly.
     */
    @Test
    fun testProductionSecretRoundTrip() {
        val base32 = Base32()
        val productionSecret = "KNT7MHTHYMB2HQR7RG7MQBD6GPQLOJ2T"

        // Decode the production secret
        val decodedBytes = base32.decode(productionSecret)

        // Re-encode the decoded bytes
        val reencoded = base32.encodeToString(decodedBytes).replace("=", "")

        // Verify the re-encoded secret matches the original
        assertArrayEquals(
            productionSecret.toByteArray(),
            reencoded.toByteArray(),
            "Production secret round-trip failed: expected '$productionSecret' but got '$reencoded'"
        )

        // Verify the decoded byte count is correct (20 bytes for TOTP secrets)
        assert(decodedBytes.size == 20) {
            "Expected 20 bytes for TOTP secret, but got ${decodedBytes.size}"
        }

        println("Production secret round-trip test passed:")
        println("  Original: $productionSecret")
        println("  Re-encoded: $reencoded")
        println("  Byte count: ${decodedBytes.size}")
    }
}
