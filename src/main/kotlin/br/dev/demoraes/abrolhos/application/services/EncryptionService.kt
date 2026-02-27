package br.dev.demoraes.abrolhos.application.services

import br.dev.demoraes.abrolhos.domain.exceptions.EncryptionException
import br.dev.demoraes.abrolhos.infrastructure.web.config.SecurityProperties
import io.micrometer.core.instrument.MeterRegistry
import jakarta.annotation.PostConstruct
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Service for encrypting and decrypting sensitive data using AES-256-GCM.
 *
 * This service provides secure encryption with:
 * - AES-256-GCM authenticated encryption
 * - Unique IV generation for each encryption
 * - Key rotation support for decryption
 * - Performance monitoring
 *
 * Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7
 */
@Service
class EncryptionService(
        private val securityProperties: SecurityProperties,
        private val meterRegistry: MeterRegistry
) {
    private val logger = LoggerFactory.getLogger(EncryptionService::class.java)
    private val secureRandom = SecureRandom()

    private lateinit var currentKey: SecretKey
    private var oldKeys: List<SecretKey> = emptyList()

    companion object {
        private const val ALGORITHM = "AES"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val IV_LENGTH = 12
        private const val KEY_SIZE = 32 // 256 bits
        private const val PERFORMANCE_THRESHOLD_MS = 10L
        private const val BITS_PER_BYTE = 8
    }

    /**
     * Validates encryption keys at application startup.
     *
     * Requirement 3.4: Key validation in @PostConstruct
     */
    @PostConstruct
    @Suppress("TooGenericExceptionCaught")
    fun validateKeys() {
        try {
            // Validate and load current key
            val keyBytes = Base64.getDecoder().decode(securityProperties.encryption.key)
            require(keyBytes.size >= KEY_SIZE) {
                "Encryption key must be at least 256 bits (32 bytes). Current size: ${keyBytes.size} bytes"
            }
            currentKey = SecretKeySpec(keyBytes, ALGORITHM)
            logger.info(
                    "Encryption key validated successfully (${keyBytes.size * BITS_PER_BYTE} bits)"
            )

            // Load old keys for rotation support
            if (securityProperties.encryption.oldKeys.isNotBlank()) {
                oldKeys =
                        securityProperties
                                .encryption
                                .oldKeys
                                .split(",")
                                .map { it.trim() }
                                .filter { it.isNotBlank() }
                                .map { oldKey ->
                                    val oldKeyBytes = Base64.getDecoder().decode(oldKey)
                                    require(oldKeyBytes.size >= KEY_SIZE) {
                                        "Old encryption key must be at least 256 bits (32 bytes)"
                                    }
                                    SecretKeySpec(oldKeyBytes, ALGORITHM)
                                }
                logger.info("Loaded ${oldKeys.size} old encryption key(s) for rotation support")
            }
        } catch (e: IllegalArgumentException) {
            throw IllegalStateException("Invalid encryption key configuration: ${e.message}", e)
        } catch (e: Exception) {
            throw IllegalStateException("Failed to initialize encryption keys: ${e.message}", e)
        }
    }

    /**
     * Encrypts plaintext using AES-256-GCM with a unique IV.
     *
     * The encrypted output format is: IV (12 bytes) + ciphertext + authentication tag The result is
     * Base64-encoded for storage.
     *
     * Requirements:
     * - 3.1: AES-256-GCM encryption
     * - 3.2: Unique IV generation
     * - 3.5: Performance monitoring
     *
     * @param plaintext The data to encrypt
     * @return Base64-encoded encrypted data (IV + ciphertext + tag)
     * @throws EncryptionException if encryption fails
     */
    @Suppress("TooGenericExceptionCaught")
    fun encrypt(plaintext: String): String {
        val startTime = System.currentTimeMillis()

        try {
            // Generate unique IV for this encryption
            val iv = ByteArray(IV_LENGTH)
            secureRandom.nextBytes(iv)

            // Initialize cipher with GCM mode
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.ENCRYPT_MODE, currentKey, gcmSpec)

            // Encrypt the plaintext
            val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

            // Combine IV + ciphertext for storage
            val combined = iv + ciphertext

            // Record performance metrics
            recordPerformance("encrypt", startTime)

            return Base64.getEncoder().encodeToString(combined)
        } catch (e: Exception) {
            meterRegistry.counter("encryption.errors", "operation", "encrypt").increment()
            throw EncryptionException("Encryption failed: ${e.message}", e)
        }
    }

    /**
     * Decrypts ciphertext using AES-256-GCM with key rotation support.
     *
     * Attempts decryption with the current key first, then tries old keys if decryption fails
     * (supporting key rotation scenarios).
     *
     * Requirements:
     * - 3.1: AES-256-GCM decryption
     * - 3.3: Key rotation support
     * - 3.5: Performance monitoring
     *
     * @param ciphertext Base64-encoded encrypted data (IV + ciphertext + tag)
     * @return Decrypted plaintext
     * @throws EncryptionException if decryption fails with all keys
     */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    fun decrypt(ciphertext: String): String {
        val startTime = System.currentTimeMillis()

        try {
            val combined = Base64.getDecoder().decode(ciphertext)

            // Extract IV and ciphertext
            require(combined.size > IV_LENGTH) {
                "Invalid ciphertext: too short to contain IV and encrypted data"
            }

            val iv = combined.sliceArray(0 until IV_LENGTH)
            val encryptedData = combined.sliceArray(IV_LENGTH until combined.size)

            // Try current key first
            try {
                val result = decryptWithKey(iv, encryptedData, currentKey)
                recordPerformance("decrypt", startTime)
                return result
            } catch (e: Exception) {
                logger.debug("Decryption with current key failed, trying old keys")
            }

            // Try old keys for key rotation support
            for ((index, oldKey) in oldKeys.withIndex()) {
                try {
                    val result = decryptWithKey(iv, encryptedData, oldKey)
                    logger.info("Successfully decrypted with old key #$index (key rotation)")
                    recordPerformance("decrypt", startTime)
                    return result
                } catch (e: Exception) {
                    logger.debug("Decryption with old key #$index failed")
                }
            }

            // All keys failed
            meterRegistry.counter("encryption.errors", "operation", "decrypt").increment()
            throw EncryptionException("Decryption failed with all available keys")
        } catch (e: EncryptionException) {
            throw e
        } catch (e: Exception) {
            meterRegistry.counter("encryption.errors", "operation", "decrypt").increment()
            throw EncryptionException("Decryption failed: ${e.message}", e)
        }
    }

    /** Decrypts data with a specific key. */
    private fun decryptWithKey(iv: ByteArray, encryptedData: ByteArray, key: SecretKey): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)
        val plaintext = cipher.doFinal(encryptedData)
        return String(plaintext, Charsets.UTF_8)
    }

    /**
     * Records performance metrics and logs warnings if threshold exceeded.
     *
     * Requirement 3.5: Performance monitoring with < 10ms threshold
     */
    private fun recordPerformance(operation: String, startTime: Long) {
        val duration = System.currentTimeMillis() - startTime

        meterRegistry
                .timer("encryption.duration", "operation", operation)
                .record(duration, java.util.concurrent.TimeUnit.MILLISECONDS)

        if (duration > PERFORMANCE_THRESHOLD_MS) {
            logger.warn(
                    "Encryption operation '$operation' took ${duration}ms " +
                            "(threshold: ${PERFORMANCE_THRESHOLD_MS}ms)"
            )
            meterRegistry.counter("encryption.slow_operations", "operation", operation).increment()
        }
    }
}
