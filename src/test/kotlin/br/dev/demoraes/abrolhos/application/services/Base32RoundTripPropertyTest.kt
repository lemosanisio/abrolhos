package br.dev.demoraes.abrolhos.application.services

import io.kotest.matchers.shouldBe
import org.apache.commons.codec.binary.Base32
import org.junit.jupiter.api.Test

/**
 * Property-based test for Base32 encoding/decoding round-trip consistency.
 *
 * **Property 1: Base32 Round-Trip Consistency**
 * **Validates: Requirements 3.1**
 *
 * This test verifies that Base32 encoding and decoding produces consistent results:
 * For any byte array, encoding it to Base32 and then decoding it back should produce
 * the original byte array.
 *
 * Property: ∀ bytes. decode(encode(bytes)) = bytes
 */
class Base32RoundTripPropertyTest {
    private val base32 = Base32()

    /**
     * Property: For any byte array, Base32 encode → decode should return the original bytes.
     *
     * This property verifies that the Base32 encoding/decoding implementation is consistent
     * and doesn't lose or corrupt data during the round-trip transformation.
     */
    @Test
    fun `property - Base32 encode then decode returns original bytes`() {
        var successfulTests = 0

        while (successfulTests < 100) {
            // Generate arbitrary byte array (10-40 bytes)
            val size = (10..40).random()
            val byteArray = ByteArray(size) { kotlin.random.Random.nextBytes(1)[0] }

            // When: Encode to Base32 and then decode back
            val encoded = base32.encodeToString(byteArray)
            val decoded = base32.decode(encoded)

            // Then: The decoded bytes should match the original bytes
            decoded shouldBe byteArray

            successfulTests++
        }
    }

    /**
     * Property: For any byte array, Base32 encode (without padding) → decode should return the original bytes.
     *
     * This property verifies that unpadded Base32 strings (as used in TOTP secrets) can be
     * correctly decoded back to the original bytes.
     */
    @Test
    fun `property - Base32 encode without padding then decode returns original bytes`() {
        var successfulTests = 0

        while (successfulTests < 100) {
            // Generate arbitrary byte array (10-40 bytes)
            val size = (10..40).random()
            val byteArray = ByteArray(size) { kotlin.random.Random.nextBytes(1)[0] }

            // When: Encode to Base32, remove padding, and then decode back
            val encoded = base32.encodeToString(byteArray).replace("=", "")
            val decoded = base32.decode(encoded)

            // Then: The decoded bytes should match the original bytes
            decoded shouldBe byteArray

            successfulTests++
        }
    }

    /**
     * Property: For any byte array of TOTP secret size (20 bytes), round-trip should be consistent.
     *
     * This property specifically tests the byte size used for TOTP secrets (160 bits = 20 bytes)
     * to ensure the encoding/decoding works correctly for the actual use case.
     */
    @Test
    fun `property - Base32 round-trip is consistent for TOTP secret size (20 bytes)`() {
        repeat(100) {
            // Given: A 20-byte array (TOTP secret size)
            val byteArray = ByteArray(20) { kotlin.random.Random.nextBytes(1)[0] }

            // When: Encode to Base32 (without padding) and decode back
            val encoded = base32.encodeToString(byteArray).replace("=", "")
            val decoded = base32.decode(encoded)

            // Then: The decoded bytes should match the original bytes
            decoded shouldBe byteArray

            // And: The decoded byte count should be exactly 20
            decoded.size shouldBe 20
        }
    }

    /**
     * Property: For any byte array, encoding is case-insensitive during decoding.
     *
     * This property verifies that Base32 decoding works correctly regardless of the case
     * of the encoded string, which is important for TOTP secrets that may be entered manually.
     */
    @Test
    fun `property - Base32 decoding is case-insensitive`() {
        repeat(100) {
            // Generate arbitrary byte array (10-40 bytes)
            val size = (10..40).random()
            val byteArray = ByteArray(size) { kotlin.random.Random.nextBytes(1)[0] }

            // When: Encode to Base32 and decode with different cases
            val encoded = base32.encodeToString(byteArray).replace("=", "")
            val decodedUpper = base32.decode(encoded.uppercase())
            val decodedLower = base32.decode(encoded.lowercase())

            // Then: Both should decode to the original bytes
            decodedUpper shouldBe byteArray
            decodedLower shouldBe byteArray
        }
    }
}
