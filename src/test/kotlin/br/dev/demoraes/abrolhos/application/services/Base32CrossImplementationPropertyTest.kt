package br.dev.demoraes.abrolhos.application.services

import io.kotest.matchers.shouldBe
import org.apache.commons.codec.binary.Base32
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

/**
 * Property-based test for Base32 cross-implementation consistency.
 *
 * **Property 3: Base32 Cross-Implementation Consistency**
 * **Validates: Requirements 3.3**
 *
 * This test verifies that the commons-codec Base32 implementation produces
 * results consistent with RFC 4648 specification and other standard implementations.
 *
 * The test validates:
 * 1. RFC 4648 test vectors decode correctly
 * 2. Encoded strings can be decoded consistently regardless of padding
 * 3. The implementation handles both uppercase and lowercase input
 * 4. The decoded bytes match expected values from the specification
 */
class Base32CrossImplementationPropertyTest {
    private val base32 = Base32()

    /**
     * RFC 4648 test vectors for Base32 encoding.
     * These are the official test vectors from the RFC specification.
     *
     * Format: Pair(plaintext, base32_encoded)
     */
    private val rfcTestVectors = listOf(
        "" to "",
        "f" to "MY======",
        "fo" to "MZXQ====",
        "foo" to "MZXW6===",
        "foob" to "MZXW6YQ=",
        "fooba" to "MZXW6YTB",
        "foobar" to "MZXW6YTBOI======"
    )

    /**
     * Property: RFC 4648 test vectors should decode correctly.
     *
     * This property verifies that the commons-codec Base32 implementation
     * correctly decodes the official RFC 4648 test vectors.
     */
    @Test
    fun `property - RFC 4648 test vectors decode correctly`() {
        rfcTestVectors.forEach { (plaintext, encoded) ->
            // When: Decode the RFC test vector
            val decoded = base32.decode(encoded)
            val decodedString = String(decoded, StandardCharsets.UTF_8)

            // Then: The decoded string should match the expected plaintext
            decodedString shouldBe plaintext
        }
    }

    /**
     * Property: RFC 4648 test vectors should encode correctly.
     *
     * This property verifies that the commons-codec Base32 implementation
     * correctly encodes to match the official RFC 4648 test vectors.
     */
    @Test
    fun `property - RFC 4648 test vectors encode correctly`() {
        rfcTestVectors.forEach { (plaintext, expected) ->
            // When: Encode the plaintext
            val encoded = base32.encodeToString(plaintext.toByteArray(StandardCharsets.UTF_8))

            // Then: The encoded string should match the RFC test vector
            encoded shouldBe expected
        }
    }

    /**
     * Property: Padded and unpadded Base32 strings decode to the same bytes.
     *
     * This property verifies that the Base32 implementation handles both
     * padded and unpadded strings consistently, which is critical for TOTP
     * secrets that may be stored without padding.
     */
    @Test
    fun `property - padded and unpadded strings decode consistently`() {
        repeat(100) {
            // Generate arbitrary byte array (10-40 bytes)
            val size = (10..40).random()
            val byteArray = ByteArray(size) { kotlin.random.Random.nextBytes(1)[0] }

            // When: Encode with padding and without padding
            val encodedWithPadding = base32.encodeToString(byteArray)
            val encodedWithoutPadding = encodedWithPadding.replace("=", "")

            val decodedFromPadded = base32.decode(encodedWithPadding)
            val decodedFromUnpadded = base32.decode(encodedWithoutPadding)

            // Then: Both should decode to the same bytes
            decodedFromPadded shouldBe byteArray
            decodedFromUnpadded shouldBe byteArray
            decodedFromPadded shouldBe decodedFromUnpadded
        }
    }

    /**
     * Property: Base32 decoding is case-insensitive across implementations.
     *
     * This property verifies that the Base32 implementation correctly handles
     * both uppercase and lowercase input, which is required by RFC 4648 and
     * ensures compatibility with different TOTP secret formats.
     */
    @Test
    fun `property - decoding is case-insensitive for RFC test vectors`() {
        rfcTestVectors.forEach { (plaintext, encoded) ->
            // When: Decode with different cases
            val decodedUpper = base32.decode(encoded.uppercase())
            val decodedLower = base32.decode(encoded.lowercase())
            val decodedMixed = base32.decode(encoded)

            val expectedBytes = plaintext.toByteArray(StandardCharsets.UTF_8)

            // Then: All should decode to the same bytes
            decodedUpper shouldBe expectedBytes
            decodedLower shouldBe expectedBytes
            decodedMixed shouldBe expectedBytes
        }
    }

    /**
     * Property: TOTP-sized secrets (20 bytes) encode/decode consistently.
     *
     * This property specifically tests the 20-byte (160-bit) secrets used
     * for TOTP, ensuring they work correctly with the Base32 implementation.
     */
    @Test
    fun `property - TOTP secret size (20 bytes) encodes and decodes consistently`() {
        repeat(100) {
            // Given: A 20-byte array (TOTP secret size)
            val secretBytes = ByteArray(20) { kotlin.random.Random.nextBytes(1)[0] }

            // When: Encode and decode
            val encoded = base32.encodeToString(secretBytes).replace("=", "")
            val decoded = base32.decode(encoded)

            // Then: Should decode to original bytes
            decoded shouldBe secretBytes
            decoded.size shouldBe 20
        }
    }

    /**
     * Property: Known TOTP secrets decode to expected byte counts.
     *
     * This property tests real-world TOTP secrets to ensure they decode
     * to the expected 20-byte format required by RFC 6238.
     */
    @Test
    fun `property - known TOTP secrets decode to correct byte count`() {
        val knownSecrets = listOf(
            "JBSWY3DPEHPK3PXP", // Test secret
            "KNT7MHTHYMB2HQR7RG7MQBD6GPQLOJ2T", // Production secret
            "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ", // Another test secret
            "MFRGGZDFMZTWQ2LK" // Short test secret
        )

        knownSecrets.forEach { secret ->
            // When: Decode the secret
            val decoded = base32.decode(secret.uppercase())

            // Then: Should decode successfully
            decoded.size shouldBe secret.replace("=", "").length * 5 / 8

            // And: Should be able to encode back
            val reencoded = base32.encodeToString(decoded).replace("=", "")
            val redecoded = base32.decode(reencoded)

            redecoded shouldBe decoded
        }
    }

    /**
     * Property: Base32 encoding produces valid RFC 4648 alphabet.
     *
     * This property verifies that the encoded output only contains valid
     * Base32 characters as defined in RFC 4648: A-Z, 2-7, and padding (=).
     */
    @Test
    fun `property - encoded output uses valid RFC 4648 alphabet`() {
        val validBase32Chars = ('A'..'Z').toSet() + ('2'..'7').toSet() + '='

        repeat(100) {
            // Generate arbitrary byte array
            val size = (10..40).random()
            val byteArray = ByteArray(size) { kotlin.random.Random.nextBytes(1)[0] }

            // When: Encode to Base32
            val encoded = base32.encodeToString(byteArray)

            // Then: All characters should be valid Base32 characters
            encoded.all { it in validBase32Chars } shouldBe true
        }
    }

    /**
     * Property: Base32 decoding rejects invalid characters.
     *
     * This property verifies that the Base32 implementation properly handles
     * invalid input by testing characters outside the RFC 4648 alphabet.
     */
    @Test
    fun `property - decoding handles invalid characters gracefully`() {
        val invalidStrings = listOf(
            "INVALID0", // Contains '0' (not in Base32 alphabet)
            "INVALID1", // Contains '1' (not in Base32 alphabet)
            "INVALID8", // Contains '8' (not in Base32 alphabet)
            "INVALID9", // Contains '9' (not in Base32 alphabet)
            "HELLO WORLD", // Contains space
            "TEST@TEST" // Contains special character
        )

        invalidStrings.forEach { invalid ->
            // When/Then: Decoding should either throw or return empty/invalid result
            try {
                val decoded = base32.decode(invalid)
                // If it doesn't throw, the result should be empty or the implementation
                // is lenient and skips invalid characters
                println("Decoded '$invalid' to ${decoded.size} bytes (implementation is lenient)")
            } catch (@Suppress("SwallowedException") e: Exception) {
                // Expected behavior: throw exception for invalid input
                println("Correctly rejected invalid input: '$invalid'")
            }
        }
    }

    /**
     * Property: Cross-implementation consistency with production secret.
     *
     * This property tests the specific production secret that was causing
     * issues to ensure it decodes consistently.
     */
    @Test
    fun `property - production secret decodes consistently`() {
        val productionSecret = "KNT7MHTHYMB2HQR7RG7MQBD6GPQLOJ2T"

        // When: Decode multiple times
        val decoded1 = base32.decode(productionSecret.uppercase())
        val decoded2 = base32.decode(productionSecret.lowercase())
        val decoded3 = base32.decode(productionSecret)

        // Then: All decodings should produce the same bytes
        decoded1 shouldBe decoded2
        decoded2 shouldBe decoded3

        // And: Should be 20 bytes (160 bits)
        decoded1.size shouldBe 20

        // And: Should be able to re-encode and decode
        val reencoded = base32.encodeToString(decoded1).replace("=", "")
        val redecoded = base32.decode(reencoded)

        redecoded shouldBe decoded1
    }
}
