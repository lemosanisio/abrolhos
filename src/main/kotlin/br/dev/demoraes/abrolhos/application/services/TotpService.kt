package br.dev.demoraes.abrolhos.application.services

import br.dev.demoraes.abrolhos.domain.entities.TotpCode
import br.dev.demoraes.abrolhos.domain.entities.TotpSecret
import dev.turingcomplete.kotlinonetimepassword.HmacAlgorithm
import dev.turingcomplete.kotlinonetimepassword.TimeBasedOneTimePasswordConfig
import dev.turingcomplete.kotlinonetimepassword.TimeBasedOneTimePasswordGenerator
import org.apache.commons.codec.binary.Base32
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

/**
 * Data class representing TOTP codes for diagnostic purposes. Contains codes for previous, current,
 * and next time windows.
 *
 * @property previous Code for the previous 30-second window
 * @property current Code for the current window
 * @property next Code for the next window
 * @property timestamp Epoch milliseconds when codes were generated
 */
data class WindowCodes(
    val previous: String,
    val current: String,
    val next: String,
    val timestamp: Long
)

/**
 * Data class representing the validation result of a TOTP secret. Used to verify that a secret can
 * be properly decoded before use.
 *
 * @property isValid Whether the secret is valid and can be decoded
 * @property byteCount The number of bytes in the decoded secret (null if invalid)
 * @property expectedByteCount The expected number of bytes for a valid secret (null if invalid)
 * @property error Error message if validation failed (null if valid)
 */
data class SecretValidation(
    val isValid: Boolean,
    val byteCount: Int? = null,
    val expectedByteCount: Int? = null,
    val error: String? = null
)

/**
 * Service for Time-Based One-Time Password (TOTP) operations.
 *
 * This service handles the cryptographic aspects of 2FA:
 * - Generating secure random secrets for new users.
 * - Verifying provided codes against stored secrets.
 * - Generating provisioning URIs for QR codes.
 *
 * It is a core component of the security architecture, ensuring that authentication requires
 * possession of the registered device.
 */
@Service
class TotpService {
    private val logger = LoggerFactory.getLogger(TotpService::class.java)

    companion object {
        private const val SECRET_BYTE_SIZE = 20 // 160 bits
        private const val WINDOW_MILLIS = 30_000L
        const val SECRET_PREFIX_LENGTH = 8
    }

    fun generateSecret(): TotpSecret {
        val random = SecureRandom()
        val bytes = ByteArray(SECRET_BYTE_SIZE)
        random.nextBytes(bytes)
        val base32 = Base32()
        val secret = base32.encodeToString(bytes).replace("=", "")

        // Diagnostic logging - Requirement 1.1
        logger.debug(
            "Generated TOTP secret (first {} chars): {}",
            SECRET_PREFIX_LENGTH,
            secret.take(SECRET_PREFIX_LENGTH)
        )

        return TotpSecret(secret)
    }

    fun verifyCode(secret: TotpSecret, code: TotpCode): Boolean {
        return try {
            val base32 = Base32()
            val secretBytes = base32.decode(secret.value.uppercase())

            // Diagnostic logging - Requirements 1.1, 1.2, 1.3
            logger.debug("Verifying TOTP code")
            logger.debug(
                "Secret (first {} chars): {}",
                SECRET_PREFIX_LENGTH,
                secret.value.take(SECRET_PREFIX_LENGTH)
            )
            logger.debug("Decoded byte count: {}", secretBytes.size)
            logger.debug("System timestamp: {}", System.currentTimeMillis())

            val config =
                TimeBasedOneTimePasswordConfig(
                    codeDigits = 6,
                    hmacAlgorithm = HmacAlgorithm.SHA1,
                    timeStep = 30,
                    timeStepUnit = TimeUnit.SECONDS
                )
            val generator = TimeBasedOneTimePasswordGenerator(secretBytes, config)
            val now = java.util.Date()

            // Generate codes for all windows for debugging - Requirement 1.4
            val codes = generateCodesForWindows(secret, now)
            logger.debug(
                "Generated codes - Previous: {}, Current: {}, Next: {}",
                codes.previous,
                codes.current,
                codes.next
            )

            val isValid =
                generator.isValid(code.value, now) ||
                    generator.isValid(
                        code.value,
                        java.util.Date(now.time - WINDOW_MILLIS)
                    ) ||
                    generator.isValid(code.value, java.util.Date(now.time + WINDOW_MILLIS))

            if (!isValid) {
                logger.warn("TOTP verification failed for code: {}", code.value)
            } else {
                logger.info("TOTP verification succeeded for code: {}", code.value)
            }

            isValid
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            // Enhanced error logging - Requirement 1.6
            logger.error("TOTP verification exception: {}", e.message, e)
            logger.error(
                "Failed to decode secret (first {} chars): {}",
                SECRET_PREFIX_LENGTH,
                secret.value.take(SECRET_PREFIX_LENGTH)
            )
            false
        }
    }

    /**
     * Generates TOTP codes for previous, current, and next time windows. Used for diagnostic
     * logging to help debug TOTP verification issues.
     *
     * @param secret The TOTP secret to generate codes for
     * @param timestamp The timestamp to use as the current time window
     * @return WindowCodes containing codes for all three windows
     */
    fun generateCodesForWindows(secret: TotpSecret, timestamp: java.util.Date): WindowCodes {
        val base32 = Base32()
        val secretBytes = base32.decode(secret.value.uppercase())
        val config =
            TimeBasedOneTimePasswordConfig(
                codeDigits = 6,
                hmacAlgorithm = HmacAlgorithm.SHA1,
                timeStep = 30,
                timeStepUnit = TimeUnit.SECONDS
            )
        val generator = TimeBasedOneTimePasswordGenerator(secretBytes, config)

        return WindowCodes(
            previous = generator.generate(java.util.Date(timestamp.time - WINDOW_MILLIS)),
            current = generator.generate(timestamp),
            next = generator.generate(java.util.Date(timestamp.time + WINDOW_MILLIS)),
            timestamp = timestamp.time
        )
    }

    /**
     * Validates that a TOTP secret can be properly decoded. Used to verify secrets before
     * persisting them to the database.
     *
     * @param secret The TOTP secret to validate
     * @return SecretValidation containing validation results
     */
    fun validateSecret(secret: TotpSecret): SecretValidation {
        return try {
            val base32 = Base32()
            val secretBytes = base32.decode(secret.value.uppercase())
            SecretValidation(
                isValid = true,
                byteCount = secretBytes.size,
                expectedByteCount = SECRET_BYTE_SIZE
            )
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            logger.error("Failed to validate TOTP secret: {}", e.message)
            SecretValidation(isValid = false, error = e.message)
        }
    }

    fun generateProvisioningUri(
        username: String,
        secret: TotpSecret,
        issuer: String = "Abrolhos",
    ): String {
        val encodedIssuer = java.net.URLEncoder.encode(issuer, "UTF-8").replace("+", "%20")
        val encodedUsername = java.net.URLEncoder.encode(username, "UTF-8").replace("+", "%20")
        val label = "$encodedIssuer:$encodedUsername"
        return "otpauth://totp/$label?secret=${secret.value}&issuer=$encodedIssuer"
    }
}
