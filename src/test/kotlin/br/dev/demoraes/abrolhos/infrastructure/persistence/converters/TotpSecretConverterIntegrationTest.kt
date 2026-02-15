package br.dev.demoraes.abrolhos.infrastructure.persistence.converters

import br.dev.demoraes.abrolhos.application.config.SecurityProperties
import br.dev.demoraes.abrolhos.application.services.EncryptionService
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotContain
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Integration test for JPA encryption of TOTP secrets.
 *
 * **Validates: Requirements 3.1, 3.2**
 *
 * This test verifies that:
 * 1. TOTP secrets are encrypted when converting to database column
 * 2. Database column contains encrypted data (not plaintext)
 * 3. Application receives decrypted data when converting from database
 * 4. The encryption/decryption process works correctly through the converter
 *
 * Requirements:
 * - 3.1: TOTP secrets are encrypted before database persistence
 * - 3.2: TOTP secrets are decrypted when loading from database
 */
class TotpSecretConverterIntegrationTest {

    private lateinit var encryptionService: EncryptionService
    private lateinit var converter: TotpSecretConverter

    @BeforeEach
    fun setup() {
        // Create security properties with a test encryption key
        val securityProperties =
                SecurityProperties().apply {
                    encryption.key =
                            "t7rCVzHlcS8FLq2CE4M6YAmv7HIVjh/dWhZLr+JWJ+o=" // Base64 encoded 32-byte
                    // key
                    encryption.oldKeys = ""
                    cors.allowedOrigins = "http://localhost:3000"
                    rateLimit.maxRequests = 5
                    rateLimit.windowMinutes = 15
                }

        // Create encryption service
        val meterRegistry = SimpleMeterRegistry()
        encryptionService = EncryptionService(securityProperties, meterRegistry)
        encryptionService.validateKeys()

        // Create converter
        converter = TotpSecretConverter(encryptionService)
    }

    /**
     * Test: Converting TOTP secret to database column and back
     *
     * Verifies that:
     * - TOTP secret is encrypted when converting to database column
     * - The encrypted value is not the plaintext
     * - The encrypted value does not contain the plaintext
     * - Converting back from database column decrypts correctly
     * - The decrypted value matches the original plaintext
     */
    @Test
    fun `should encrypt TOTP secret when converting to database column and decrypt when converting back`() {
        // Given: A plaintext TOTP secret
        val plaintextSecret = "JBSWY3DPEHPK3PXP"

        // When: Convert to database column (encrypt)
        val encryptedValue = converter.convertToDatabaseColumn(plaintextSecret)

        // Then: Verify the encrypted value is not the plaintext
        encryptedValue shouldNotBe null
        encryptedValue shouldNotBe plaintextSecret
        encryptedValue!! shouldNotContain plaintextSecret

        // When: Convert from database column (decrypt)
        val decryptedValue = converter.convertToEntityAttribute(encryptedValue)

        // Then: Verify the decrypted value matches the original
        decryptedValue shouldNotBe null
        decryptedValue shouldBe plaintextSecret
    }

    /**
     * Test: Converting null TOTP secret
     *
     * Verifies that:
     * - Null values are handled correctly by the converter
     * - No encryption/decryption occurs for null values
     * - Null in produces null out
     */
    @Test
    fun `should handle null TOTP secret correctly`() {
        // Given: A null TOTP secret
        val nullSecret: String? = null

        // When: Convert to database column
        val encryptedValue = converter.convertToDatabaseColumn(nullSecret)

        // Then: Verify the result is null
        encryptedValue shouldBe null

        // When: Convert from database column
        val decryptedValue = converter.convertToEntityAttribute(null)

        // Then: Verify the result is null
        decryptedValue shouldBe null
    }

    /**
     * Test: Multiple different TOTP secrets
     *
     * Verifies that:
     * - Each secret is encrypted independently
     * - Different secrets produce different encrypted values
     * - Each secret is correctly decrypted
     */
    @Test
    fun `should encrypt different secrets independently`() {
        // Given: Multiple different TOTP secrets
        val secrets = listOf("JBSWY3DPEHPK3PXP", "HXDMVJECJJWSRB3H", "GEZDGNBVGY3TQOJQ")

        // When: Convert each to database column (encrypt)
        val encryptedValues = secrets.map { converter.convertToDatabaseColumn(it) }

        // Then: Verify all encrypted values are different
        encryptedValues.toSet().size shouldBe secrets.size

        // Then: Verify none of the encrypted values contain the plaintext
        encryptedValues.forEachIndexed { index, encrypted ->
            encrypted shouldNotBe null
            encrypted!! shouldNotContain secrets[index]
        }

        // When: Convert each from database column (decrypt)
        val decryptedValues = encryptedValues.map { converter.convertToEntityAttribute(it) }

        // Then: Verify each decrypted value matches the original
        decryptedValues.forEachIndexed { index, decrypted -> decrypted shouldBe secrets[index] }
    }

    /**
     * Test: Round-trip encryption consistency
     *
     * Verifies that:
     * - Encrypting and decrypting produces the original value
     * - The process is consistent across multiple conversions
     */
    @Test
    fun `should maintain consistency across multiple round-trip conversions`() {
        // Given: A TOTP secret
        val originalSecret = "JBSWY3DPEHPK3PXP"

        // When: Perform multiple round-trip conversions
        repeat(5) {
            val encrypted = converter.convertToDatabaseColumn(originalSecret)
            val decrypted = converter.convertToEntityAttribute(encrypted)

            // Then: Verify the decrypted value matches the original
            decrypted shouldBe originalSecret
        }
    }

    /**
     * Test: Same secret encrypted multiple times produces different ciphertexts
     *
     * Verifies that:
     * - Each encryption uses a unique IV
     * - Same plaintext produces different ciphertexts
     * - All ciphertexts decrypt to the same plaintext
     *
     * This is a critical security property - if the same plaintext always produces the same
     * ciphertext, it would be vulnerable to pattern analysis.
     */
    @Test
    fun `should produce different ciphertexts for same plaintext due to unique IVs`() {
        // Given: A TOTP secret
        val plaintextSecret = "JBSWY3DPEHPK3PXP"

        // When: Encrypt the same secret multiple times
        val encryptedValues = (1..5).map { converter.convertToDatabaseColumn(plaintextSecret) }

        // Then: Verify all encrypted values are different (unique IVs)
        encryptedValues.toSet().size shouldBe 5

        // Then: Verify all decrypt to the same plaintext
        encryptedValues.forEach { encrypted ->
            val decrypted = converter.convertToEntityAttribute(encrypted)
            decrypted shouldBe plaintextSecret
        }
    }

    /**
     * Test: Empty string encryption
     *
     * Verifies that:
     * - Empty strings can be encrypted
     * - Empty strings decrypt correctly
     */
    @Test
    fun `should handle empty string correctly`() {
        // Given: An empty string
        val emptySecret = ""

        // When: Convert to database column (encrypt)
        val encryptedValue = converter.convertToDatabaseColumn(emptySecret)

        // Then: Verify encryption succeeded
        encryptedValue shouldNotBe null
        encryptedValue shouldNotBe emptySecret

        // When: Convert from database column (decrypt)
        val decryptedValue = converter.convertToEntityAttribute(encryptedValue)

        // Then: Verify the decrypted value is empty string
        decryptedValue shouldBe emptySecret
    }

    /**
     * Test: Long TOTP secret encryption
     *
     * Verifies that:
     * - Long secrets can be encrypted
     * - Long secrets decrypt correctly
     */
    @Test
    fun `should handle long secrets correctly`() {
        // Given: A long TOTP secret (longer than typical)
        val longSecret = "JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP"

        // When: Convert to database column (encrypt)
        val encryptedValue = converter.convertToDatabaseColumn(longSecret)

        // Then: Verify encryption succeeded
        encryptedValue shouldNotBe null
        encryptedValue shouldNotBe longSecret

        // When: Convert from database column (decrypt)
        val decryptedValue = converter.convertToEntityAttribute(encryptedValue)

        // Then: Verify the decrypted value matches the original
        decryptedValue shouldBe longSecret
    }
}
