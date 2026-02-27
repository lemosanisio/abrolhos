package br.dev.demoraes.abrolhos.application.services

import br.dev.demoraes.abrolhos.domain.exceptions.EncryptionException
import br.dev.demoraes.abrolhos.infrastructure.web.config.SecurityProperties
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.security.SecureRandom
import java.util.Base64

/**
 * Unit tests for EncryptionService.
 * Tests requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7
 */
class EncryptionServiceTest {

    // Requirement 3.4: Key validation in @PostConstruct
    @Test
    fun `should validate encryption key successfully with 256-bit key`() {
        val key = generateValidKey()
        val securityProperties = createSecurityProperties(key)
        val meterRegistry = SimpleMeterRegistry()
        val service = EncryptionService(securityProperties, meterRegistry)

        // Should not throw exception
        service.validateKeys()
    }

    @Test
    fun `should reject encryption key smaller than 256 bits`() {
        val smallKey = Base64.getEncoder().encodeToString(ByteArray(16)) // 128 bits
        val securityProperties = createSecurityProperties(smallKey)
        val meterRegistry = SimpleMeterRegistry()
        val service = EncryptionService(securityProperties, meterRegistry)

        val exception = assertThrows<IllegalStateException> {
            service.validateKeys()
        }
        assertTrue(exception.message!!.contains("at least 256 bits"))
    }

    @Test
    fun `should reject invalid Base64 encryption key`() {
        val securityProperties = createSecurityProperties("not-valid-base64!!!")
        val meterRegistry = SimpleMeterRegistry()
        val service = EncryptionService(securityProperties, meterRegistry)

        val exception = assertThrows<IllegalStateException> {
            service.validateKeys()
        }
        assertTrue(exception.message!!.contains("Invalid encryption key configuration"))
    }

    @Test
    fun `should load old keys for rotation support`() {
        val currentKey = generateValidKey()
        val oldKey1 = generateValidKey()
        val oldKey2 = generateValidKey()
        val securityProperties = createSecurityProperties(currentKey, "$oldKey1,$oldKey2")
        val meterRegistry = SimpleMeterRegistry()
        val service = EncryptionService(securityProperties, meterRegistry)

        // Should not throw exception
        service.validateKeys()
    }

    @Test
    fun `should reject old key smaller than 256 bits`() {
        val currentKey = generateValidKey()
        val smallOldKey = Base64.getEncoder().encodeToString(ByteArray(16)) // 128 bits
        val securityProperties = createSecurityProperties(currentKey, smallOldKey)
        val meterRegistry = SimpleMeterRegistry()
        val service = EncryptionService(securityProperties, meterRegistry)

        val exception = assertThrows<IllegalStateException> {
            service.validateKeys()
        }
        assertTrue(exception.message!!.contains("at least 256 bits"))
    }

    // Requirement 3.1: AES-256-GCM encryption
    @Test
    fun `should encrypt plaintext successfully`() {
        val service = createValidService()
        val plaintext = "sensitive data"

        val ciphertext = service.encrypt(plaintext)

        assertNotNull(ciphertext)
        assertTrue(ciphertext.isNotBlank())
        assertNotEquals(plaintext, ciphertext)
    }

    @Test
    fun `should decrypt ciphertext successfully`() {
        val service = createValidService()
        val plaintext = "sensitive data"

        val ciphertext = service.encrypt(plaintext)
        val decrypted = service.decrypt(ciphertext)

        assertEquals(plaintext, decrypted)
    }

    @Test
    fun `should handle empty string encryption`() {
        val service = createValidService()
        val plaintext = ""

        val ciphertext = service.encrypt(plaintext)
        val decrypted = service.decrypt(ciphertext)

        assertEquals(plaintext, decrypted)
    }

    @Test
    fun `should handle long text encryption`() {
        val service = createValidService()
        val plaintext = "a".repeat(10000)

        val ciphertext = service.encrypt(plaintext)
        val decrypted = service.decrypt(ciphertext)

        assertEquals(plaintext, decrypted)
    }

    @Test
    fun `should handle special characters encryption`() {
        val service = createValidService()
        val plaintext = "!@#$%^&*()_+-=[]{}|;':\",./<>?`~"

        val ciphertext = service.encrypt(plaintext)
        val decrypted = service.decrypt(ciphertext)

        assertEquals(plaintext, decrypted)
    }

    @Test
    fun `should handle unicode characters encryption`() {
        val service = createValidService()
        val plaintext = "Hello 世界 🌍 Привет"

        val ciphertext = service.encrypt(plaintext)
        val decrypted = service.decrypt(ciphertext)

        assertEquals(plaintext, decrypted)
    }

    // Requirement 3.2: Unique IV generation
    @Test
    fun `should generate unique IV for each encryption`() {
        val service = createValidService()
        val plaintext = "same plaintext"

        val ciphertext1 = service.encrypt(plaintext)
        val ciphertext2 = service.encrypt(plaintext)

        // Same plaintext should produce different ciphertexts due to unique IVs
        assertNotEquals(ciphertext1, ciphertext2)

        // Both should decrypt to the same plaintext
        assertEquals(plaintext, service.decrypt(ciphertext1))
        assertEquals(plaintext, service.decrypt(ciphertext2))
    }

    // Requirement 3.3: Key rotation support
    @Test
    fun `should decrypt with current key`() {
        val service = createValidService()
        val plaintext = "test data"

        val ciphertext = service.encrypt(plaintext)
        val decrypted = service.decrypt(ciphertext)

        assertEquals(plaintext, decrypted)
    }

    @Test
    fun `should decrypt with old key after key rotation`() {
        // Create service with initial key
        val oldKey = generateValidKey()
        val securityProperties1 = createSecurityProperties(oldKey)
        val meterRegistry1 = SimpleMeterRegistry()
        val service1 = EncryptionService(securityProperties1, meterRegistry1)
        service1.validateKeys()

        // Encrypt with old key
        val plaintext = "test data"
        val ciphertext = service1.encrypt(plaintext)

        // Create new service with rotated key (old key in oldKeys)
        val newKey = generateValidKey()
        val securityProperties2 = createSecurityProperties(newKey, oldKey)
        val meterRegistry2 = SimpleMeterRegistry()
        val service2 = EncryptionService(securityProperties2, meterRegistry2)
        service2.validateKeys()

        // Should decrypt with old key
        val decrypted = service2.decrypt(ciphertext)
        assertEquals(plaintext, decrypted)
    }

    @Test
    fun `should try multiple old keys until successful`() {
        // Create service with initial key
        val oldKey1 = generateValidKey()
        val securityProperties1 = createSecurityProperties(oldKey1)
        val meterRegistry1 = SimpleMeterRegistry()
        val service1 = EncryptionService(securityProperties1, meterRegistry1)
        service1.validateKeys()

        // Encrypt with first old key
        val plaintext = "test data"
        val ciphertext = service1.encrypt(plaintext)

        // Create new service with multiple old keys
        val newKey = generateValidKey()
        val oldKey2 = generateValidKey()
        val securityProperties2 = createSecurityProperties(newKey, "$oldKey2,$oldKey1")
        val meterRegistry2 = SimpleMeterRegistry()
        val service2 = EncryptionService(securityProperties2, meterRegistry2)
        service2.validateKeys()

        // Should decrypt with second old key
        val decrypted = service2.decrypt(ciphertext)
        assertEquals(plaintext, decrypted)
    }

    // Requirement 3.6: Error handling
    @Test
    fun `should throw EncryptionException for invalid ciphertext`() {
        val service = createValidService()

        val exception = assertThrows<EncryptionException> {
            service.decrypt("invalid-ciphertext")
        }
        assertTrue(exception.message!!.contains("Decryption failed"))
    }

    @Test
    fun `should throw EncryptionException for tampered ciphertext`() {
        val service = createValidService()
        val plaintext = "test data"
        val ciphertext = service.encrypt(plaintext)

        // Tamper with the ciphertext
        val tamperedCiphertext = ciphertext.dropLast(5) + "XXXXX"

        val exception = assertThrows<EncryptionException> {
            service.decrypt(tamperedCiphertext)
        }
        assertTrue(exception.message!!.contains("Decryption failed"))
    }

    @Test
    fun `should throw EncryptionException for ciphertext too short`() {
        val service = createValidService()
        val shortCiphertext = Base64.getEncoder().encodeToString(ByteArray(5))

        val exception = assertThrows<EncryptionException> {
            service.decrypt(shortCiphertext)
        }
        assertTrue(exception.message!!.contains("Decryption failed"))
    }

    @Test
    fun `should throw EncryptionException when no keys can decrypt`() {
        // Create service with one key
        val key1 = generateValidKey()
        val securityProperties1 = createSecurityProperties(key1)
        val meterRegistry1 = SimpleMeterRegistry()
        val service1 = EncryptionService(securityProperties1, meterRegistry1)
        service1.validateKeys()

        // Encrypt with first key
        val plaintext = "test data"
        val ciphertext = service1.encrypt(plaintext)

        // Create service with completely different keys
        val key2 = generateValidKey()
        val key3 = generateValidKey()
        val securityProperties2 = createSecurityProperties(key2, key3)
        val meterRegistry2 = SimpleMeterRegistry()
        val service2 = EncryptionService(securityProperties2, meterRegistry2)
        service2.validateKeys()

        // Should fail to decrypt
        val exception = assertThrows<EncryptionException> {
            service2.decrypt(ciphertext)
        }
        assertTrue(exception.message!!.contains("Decryption failed with all available keys"))
    }

    // Requirement 3.5: Performance monitoring
    @Test
    fun `should record encryption performance metrics`() {
        val meterRegistry = SimpleMeterRegistry()
        val service = createValidService(meterRegistry)
        val plaintext = "test data"

        service.encrypt(plaintext)

        val timer = meterRegistry.find("encryption.duration")
            .tag("operation", "encrypt")
            .timer()
        assertNotNull(timer)
        assertEquals(1, timer!!.count())
    }

    @Test
    fun `should record decryption performance metrics`() {
        val meterRegistry = SimpleMeterRegistry()
        val service = createValidService(meterRegistry)
        val plaintext = "test data"

        val ciphertext = service.encrypt(plaintext)
        service.decrypt(ciphertext)

        val timer = meterRegistry.find("encryption.duration")
            .tag("operation", "decrypt")
            .timer()
        assertNotNull(timer)
        assertEquals(1, timer!!.count())
    }

    @Test
    @Suppress("SwallowedException")
    fun `should record encryption error metrics`() {
        val meterRegistry = SimpleMeterRegistry()
        val service = createValidService(meterRegistry)

        try {
            service.decrypt("invalid-ciphertext")
        } catch (e: EncryptionException) {
            // Expected
        }

        val counter = meterRegistry.find("encryption.errors")
            .tag("operation", "decrypt")
            .counter()
        assertNotNull(counter)
        assertEquals(1.0, counter!!.count())
    }

    // Edge cases
    @Test
    fun `should handle whitespace-only plaintext`() {
        val service = createValidService()
        val plaintext = "   \t\n   "

        val ciphertext = service.encrypt(plaintext)
        val decrypted = service.decrypt(ciphertext)

        assertEquals(plaintext, decrypted)
    }

    @Test
    fun `should handle newlines in plaintext`() {
        val service = createValidService()
        val plaintext = "line1\nline2\nline3"

        val ciphertext = service.encrypt(plaintext)
        val decrypted = service.decrypt(ciphertext)

        assertEquals(plaintext, decrypted)
    }

    @Test
    fun `should handle tabs in plaintext`() {
        val service = createValidService()
        val plaintext = "col1\tcol2\tcol3"

        val ciphertext = service.encrypt(plaintext)
        val decrypted = service.decrypt(ciphertext)

        assertEquals(plaintext, decrypted)
    }

    // Task 3.4: Encryption edge cases (Requirements 3.6, 3.8)

    @Test
    fun `should handle empty string encryption and decryption`() {
        val service = createValidService()
        val plaintext = ""

        val ciphertext = service.encrypt(plaintext)
        assertNotNull(ciphertext)
        assertTrue(ciphertext.isNotBlank())

        val decrypted = service.decrypt(ciphertext)
        assertEquals(plaintext, decrypted)
    }

    @Test
    fun `should handle very long string encryption and decryption`() {
        val service = createValidService()
        // Test with 100KB of data
        val plaintext = "a".repeat(100_000)

        val ciphertext = service.encrypt(plaintext)
        assertNotNull(ciphertext)

        val decrypted = service.decrypt(ciphertext)
        assertEquals(plaintext, decrypted)
        assertEquals(100_000, decrypted.length)
    }

    @Test
    fun `should throw EncryptionException for invalid Base64 in decryption`() {
        val service = createValidService()

        // Test with completely invalid Base64
        val exception1 = assertThrows<EncryptionException> {
            service.decrypt("not-valid-base64!!!")
        }
        assertTrue(exception1.message!!.contains("Decryption failed"))

        // Test with valid Base64 but invalid ciphertext structure
        val invalidBase64 = Base64.getEncoder().encodeToString("invalid".toByteArray())
        val exception2 = assertThrows<EncryptionException> {
            service.decrypt(invalidBase64)
        }
        assertTrue(exception2.message!!.contains("Decryption failed"))
    }

    @Test
    fun `should throw EncryptionException for decryption with wrong key`() {
        // Encrypt with one key
        val key1 = generateValidKey()
        val securityProperties1 = createSecurityProperties(key1)
        val meterRegistry1 = SimpleMeterRegistry()
        val service1 = EncryptionService(securityProperties1, meterRegistry1)
        service1.validateKeys()

        val plaintext = "sensitive data"
        val ciphertext = service1.encrypt(plaintext)

        // Try to decrypt with a different key (no old keys)
        val key2 = generateValidKey()
        val securityProperties2 = createSecurityProperties(key2)
        val meterRegistry2 = SimpleMeterRegistry()
        val service2 = EncryptionService(securityProperties2, meterRegistry2)
        service2.validateKeys()

        val exception = assertThrows<EncryptionException> {
            service2.decrypt(ciphertext)
        }
        assertTrue(exception.message!!.contains("Decryption failed with all available keys"))
    }

    @Test
    fun `should support key rotation with old keys`() {
        // Encrypt with original key
        val originalKey = generateValidKey()
        val securityProperties1 = createSecurityProperties(originalKey)
        val meterRegistry1 = SimpleMeterRegistry()
        val service1 = EncryptionService(securityProperties1, meterRegistry1)
        service1.validateKeys()

        val plaintext = "data encrypted with old key"
        val ciphertext = service1.encrypt(plaintext)

        // Rotate to new key, keeping original as old key
        val newKey = generateValidKey()
        val securityProperties2 = createSecurityProperties(newKey, originalKey)
        val meterRegistry2 = SimpleMeterRegistry()
        val service2 = EncryptionService(securityProperties2, meterRegistry2)
        service2.validateKeys()

        // Should successfully decrypt with old key
        val decrypted = service2.decrypt(ciphertext)
        assertEquals(plaintext, decrypted)

        // New encryptions should use the new key
        val newPlaintext = "data encrypted with new key"
        val newCiphertext = service2.encrypt(newPlaintext)
        val newDecrypted = service2.decrypt(newCiphertext)
        assertEquals(newPlaintext, newDecrypted)
    }

    @Test
    fun `should handle multiple key rotations with multiple old keys`() {
        // Encrypt with first generation key
        val key1 = generateValidKey()
        val securityProperties1 = createSecurityProperties(key1)
        val meterRegistry1 = SimpleMeterRegistry()
        val service1 = EncryptionService(securityProperties1, meterRegistry1)
        service1.validateKeys()

        val plaintext1 = "data from key generation 1"
        val ciphertext1 = service1.encrypt(plaintext1)

        // Rotate to second generation key
        val key2 = generateValidKey()
        val securityProperties2 = createSecurityProperties(key2, key1)
        val meterRegistry2 = SimpleMeterRegistry()
        val service2 = EncryptionService(securityProperties2, meterRegistry2)
        service2.validateKeys()

        val plaintext2 = "data from key generation 2"
        val ciphertext2 = service2.encrypt(plaintext2)

        // Rotate to third generation key with both old keys
        val key3 = generateValidKey()
        val securityProperties3 = createSecurityProperties(key3, "$key2,$key1")
        val meterRegistry3 = SimpleMeterRegistry()
        val service3 = EncryptionService(securityProperties3, meterRegistry3)
        service3.validateKeys()

        // Should decrypt data from all generations
        assertEquals(plaintext1, service3.decrypt(ciphertext1))
        assertEquals(plaintext2, service3.decrypt(ciphertext2))

        // New encryption with current key
        val plaintext3 = "data from key generation 3"
        val ciphertext3 = service3.encrypt(plaintext3)
        assertEquals(plaintext3, service3.decrypt(ciphertext3))
    }

    // Helper methods
    private fun generateValidKey(): String {
        val keyBytes = ByteArray(32) // 256 bits
        SecureRandom().nextBytes(keyBytes)
        return Base64.getEncoder().encodeToString(keyBytes)
    }

    private fun createSecurityProperties(key: String, oldKeys: String = ""): SecurityProperties {
        val properties = SecurityProperties()
        properties.encryption.key = key
        properties.encryption.oldKeys = oldKeys
        return properties
    }

    private fun createValidService(meterRegistry: SimpleMeterRegistry = SimpleMeterRegistry()): EncryptionService {
        val key = generateValidKey()
        val securityProperties = createSecurityProperties(key)
        val service = EncryptionService(securityProperties, meterRegistry)
        service.validateKeys()
        return service
    }

    // Property-Based Tests

    /**
     * Property 2: Encryption round-trip consistency
     * **Validates: Requirements 3.8**
     *
     * For any valid TOTP secret string, encrypting then decrypting should produce an equivalent value.
     */
    @Test
    fun `property - encryption round-trip should preserve TOTP secrets`() {
        val service = createValidService()

        repeat(100) {
            // Generate a valid TOTP secret (Base32 characters: A-Z and 2-7)
            val length = (16..64).random()
            val base32Chars = ('A'..'Z') + ('2'..'7')
            val totpSecret = (1..length)
                .map { base32Chars.random() }
                .joinToString("")

            // Encrypt the TOTP secret
            val encrypted = service.encrypt(totpSecret)

            // Decrypt it back
            val decrypted = service.decrypt(encrypted)

            // Should be identical to the original
            assertEquals(totpSecret, decrypted)
        }
    }

    /**
     * Property 3: IV uniqueness
     * **Validates: Requirements 3.3**
     *
     * For any plaintext encrypted multiple times, each encryption should produce a different ciphertext
     * (due to unique IVs). This ensures that the same plaintext never produces the same ciphertext,
     * preventing pattern analysis attacks.
     */
    @Test
    fun `property - encrypting same plaintext multiple times produces different ciphertexts`() {
        val service = createValidService()

        repeat(100) {
            // Generate arbitrary plaintext (varying lengths and character sets)
            val length = (1..100).random()
            val plaintext = when ((0..2).random()) {
                0 -> {
                    // Base32 TOTP secrets
                    val base32Chars = ('A'..'Z') + ('2'..'7')
                    (1..length).map { base32Chars.random() }.joinToString("")
                }
                1 -> {
                    // Alphanumeric strings
                    val alphanumeric = ('a'..'z') + ('A'..'Z') + ('0'..'9')
                    (1..length).map { alphanumeric.random() }.joinToString("")
                }
                else -> {
                    // Random printable ASCII
                    (1..length).map { (' '..'~').random() }.joinToString("")
                }
            }

            // Encrypt the same plaintext multiple times (3-10 times)
            val encryptionCount = (3..10).random()
            val ciphertexts = (1..encryptionCount).map { service.encrypt(plaintext) }

            // Property: All ciphertexts should be different (unique IVs)
            val uniqueCiphertexts = ciphertexts.toSet()
            assertEquals(
                ciphertexts.size,
                uniqueCiphertexts.size,
                "Expected all $encryptionCount ciphertexts to be unique, but found ${uniqueCiphertexts.size} unique values"
            )

            // Verify all ciphertexts decrypt to the same original plaintext
            ciphertexts.forEach { ciphertext ->
                val decrypted = service.decrypt(ciphertext)
                assertEquals(plaintext, decrypted, "Ciphertext should decrypt to original plaintext")
            }
        }
    }
}
