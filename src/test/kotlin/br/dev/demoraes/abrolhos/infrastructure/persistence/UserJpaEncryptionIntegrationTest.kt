package br.dev.demoraes.abrolhos.infrastructure.persistence

import br.dev.demoraes.abrolhos.application.services.EncryptionService
import br.dev.demoraes.abrolhos.domain.entities.Role
import br.dev.demoraes.abrolhos.infrastructure.persistence.converters.TotpSecretConverter
import br.dev.demoraes.abrolhos.infrastructure.persistence.entities.UserEntity
import br.dev.demoraes.abrolhos.infrastructure.web.config.SecurityProperties
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotContain
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ulid.ULID

/**
 * Integration test for JPA encryption of TOTP secrets.
 *
 * **Validates: Requirements 3.1, 3.2**
 *
 * This test verifies the complete encryption flow:
 * 1. Saving a User entity with TOTP secret encrypts the data before database persistence
 * 2. Database contains encrypted data (not plaintext)
 * 3. Loading a User entity decrypts the TOTP secret correctly
 * 4. Application receives decrypted data that matches the original plaintext
 *
 * Requirements:
 * - 3.1: TOTP secrets are encrypted before database persistence
 * - 3.2: TOTP secrets are decrypted when loading from database
 *
 * Note: This test uses the converter directly to simulate the JPA encryption flow without requiring
 * a full database setup. It validates that:
 * - The converter encrypts data before database persistence
 * - The encrypted data does not contain plaintext
 * - The converter decrypts data when loading from database
 * - The decrypted data matches the original plaintext
 */
class UserJpaEncryptionIntegrationTest {

    private lateinit var encryptionService: EncryptionService
    private lateinit var converter: TotpSecretConverter

    @BeforeEach
    fun setup() {
        // Create security properties with a test encryption key
        val securityProperties =
            SecurityProperties().apply {
                // Generate a proper 32-byte (256-bit) key for testing
                encryption.key =
                    "MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI=" // Base64 encoded 32-byte
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
     * Test: Save and load User with TOTP secret
     *
     * Verifies the complete encryption flow:
     * - User entity with TOTP secret is saved to database (simulated by converter)
     * - Database contains encrypted data (not plaintext)
     * - Loading the user entity decrypts the TOTP secret (simulated by converter)
     * - Application receives the original plaintext value
     */
    @Test
    fun `should encrypt TOTP secret when saving User and decrypt when loading`() {
        // Given: A User entity with a plaintext TOTP secret
        val plaintextSecret = "JBSWY3DPEHPK3PXP"
        val userId = ULID.randomULID().toString()
        val user =
            UserEntity(
                username = "testuser",
                totpSecret = plaintextSecret,
                passwordHash = null,
                isActive = true,
                role = Role.USER
            )
                .apply { id = userId }

        // When: Simulate saving to database (converter encrypts the value)
        val encryptedValueInDb = converter.convertToDatabaseColumn(user.totpSecret)

        // Then: Verify the database contains encrypted data (not plaintext)
        encryptedValueInDb shouldNotBe null
        encryptedValueInDb shouldNotBe plaintextSecret
        encryptedValueInDb!! shouldNotContain plaintextSecret

        // When: Simulate loading from database (converter decrypts the value)
        val decryptedValue = converter.convertToEntityAttribute(encryptedValueInDb)

        // Then: Verify the application receives decrypted data
        decryptedValue shouldNotBe null
        decryptedValue shouldBe plaintextSecret
    }

    /**
     * Test: Save User with null TOTP secret
     *
     * Verifies that:
     * - Null TOTP secrets are handled correctly
     * - No encryption occurs for null values
     * - Loading returns null as expected
     */
    @Test
    fun `should handle null TOTP secret correctly`() {
        // Given: A User entity with null TOTP secret
        val userId = ULID.randomULID().toString()
        val user =
            UserEntity(
                username = "userwithoutotp",
                totpSecret = null,
                passwordHash = null,
                isActive = true,
                role = Role.USER
            )
                .apply { id = userId }

        // When: Simulate saving to database (converter handles null)
        val encryptedValue = converter.convertToDatabaseColumn(user.totpSecret)

        // Then: Verify the result is null
        encryptedValue shouldBe null

        // When: Simulate loading from database (converter handles null)
        val decryptedValue = converter.convertToEntityAttribute(null)

        // Then: Verify the result is null
        decryptedValue shouldBe null
    }

    /**
     * Test: Update User TOTP secret
     *
     * Verifies that:
     * - Updating TOTP secret encrypts the new value
     * - Database contains the new encrypted value
     * - Loading returns the new decrypted value
     */
    @Test
    fun `should encrypt new TOTP secret when updating User`() {
        // Given: A User entity with an initial TOTP secret
        val initialSecret = "JBSWY3DPEHPK3PXP"
        val userId = ULID.randomULID().toString()
        val user =
            UserEntity(
                username = "updateuser",
                totpSecret = initialSecret,
                passwordHash = null,
                isActive = true,
                role = Role.USER
            )
                .apply { id = userId }

        // When: Simulate initial save
        val initialEncrypted = converter.convertToDatabaseColumn(user.totpSecret)

        // Then: Verify initial encryption
        initialEncrypted shouldNotBe null
        initialEncrypted shouldNotBe initialSecret

        // When: Update the TOTP secret
        val newSecret = "HXDMVJECJJWSRB3H"
        user.totpSecret = newSecret
        val newEncrypted = converter.convertToDatabaseColumn(user.totpSecret)

        // Then: Verify the new encrypted value is different
        newEncrypted shouldNotBe null
        newEncrypted shouldNotBe newSecret
        newEncrypted!! shouldNotContain newSecret
        newEncrypted shouldNotContain initialSecret

        // When: Simulate loading from database
        val decryptedValue = converter.convertToEntityAttribute(newEncrypted)

        // Then: Verify the application receives the new decrypted value
        decryptedValue shouldBe newSecret
    }

    /**
     * Test: Multiple users with different TOTP secrets
     *
     * Verifies that:
     * - Each user's TOTP secret is encrypted independently
     * - Database contains different encrypted values for different secrets
     * - Each user's secret is correctly decrypted
     */
    @Test
    fun `should encrypt different TOTP secrets independently for multiple users`() {
        // Given: Multiple users with different TOTP secrets
        val users =
            listOf(
                Triple("user1", "JBSWY3DPEHPK3PXP", ULID.randomULID().toString()),
                Triple("user2", "HXDMVJECJJWSRB3H", ULID.randomULID().toString()),
                Triple("user3", "GEZDGNBVGY3TQOJQ", ULID.randomULID().toString())
            )

        // When: Simulate saving all users (encrypt their secrets)
        val encryptedValues =
            users.map { (_, secret, _) -> converter.convertToDatabaseColumn(secret) }

        // Then: Verify each user's encrypted value in database is different
        encryptedValues.toSet().size shouldBe users.size

        // Then: Verify none of the encrypted values contain plaintext
        users.forEachIndexed { index, (_, secret, _) ->
            encryptedValues[index] shouldNotBe null
            encryptedValues[index]!! shouldNotContain secret
        }

        // When: Simulate loading all users (decrypt their secrets)
        val decryptedValues =
            encryptedValues.map { encrypted -> converter.convertToEntityAttribute(encrypted) }

        // Then: Verify each user's decrypted secret matches the original
        decryptedValues.forEachIndexed { index, decrypted ->
            decrypted shouldBe users[index].second
        }
    }
}
