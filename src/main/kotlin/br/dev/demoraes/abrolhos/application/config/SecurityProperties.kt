package br.dev.demoraes.abrolhos.application.config

import jakarta.annotation.PostConstruct
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.validation.annotation.Validated

/**
 * Configuration properties for security hardening features.
 *
 * This class centralizes all security-related configuration including:
 * - CORS allowed origins
 * - Rate limiting settings
 * - Encryption key management
 * - Redis connection settings
 *
 * Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6
 */
@Configuration
@ConfigurationProperties(prefix = "security")
@Validated
class SecurityProperties {

    private val logger = LoggerFactory.getLogger(SecurityProperties::class.java)

    val cors = CorsProperties()
    val rateLimit = RateLimitProperties()
    val encryption = EncryptionProperties()

    @PostConstruct
    fun validateConfiguration() {
        logger.info("Security configuration loaded:")
        logger.info("  CORS allowed origins: ${cors.allowedOrigins}")
        logger.info("  Rate limit max requests: ${rateLimit.maxRequests}")
        logger.info("  Rate limit window: ${rateLimit.windowMinutes} minutes")
        logger.info("  Encryption key configured: ${encryption.key.isNotBlank()}")
    }

    class CorsProperties {
        /**
         * Comma-separated list of allowed CORS origins.
         * Example: https://app.example.com,https://admin.example.com
         *
         * Requirement 6.1: Configuration via environment variables
         */
        @NotBlank(message = "CORS allowed origins must be configured")
        var allowedOrigins: String = ""
    }

    class RateLimitProperties {
        /**
         * Maximum number of requests allowed within the time window.
         * Default: 5 requests
         *
         * Requirement 6.2: Rate limit configuration
         */
        @Min(MIN_REQUESTS, message = "Rate limit max requests must be at least 1")
        var maxRequests: Int = DEFAULT_MAX_REQUESTS

        /**
         * Time window in minutes for rate limiting.
         * Default: 15 minutes
         *
         * Requirement 6.2: Rate limit configuration
         */
        @Min(MIN_WINDOW, message = "Rate limit window must be at least 1 minute")
        var windowMinutes: Int = DEFAULT_WINDOW_MINUTES

        companion object {
            private const val MIN_REQUESTS = 1L
            private const val MIN_WINDOW = 1L
            private const val DEFAULT_MAX_REQUESTS = 5
            private const val DEFAULT_WINDOW_MINUTES = 15
        }
    }

    class EncryptionProperties {
        /**
         * Base64-encoded AES-256 encryption key for TOTP secrets.
         * Must be at least 256 bits (32 bytes).
         *
         * Requirement 6.3: Encryption key configuration
         */
        @NotBlank(message = "Encryption key must be configured")
        var key: String = ""

        /**
         * Comma-separated list of old encryption keys for key rotation.
         * Optional - used when rotating encryption keys.
         *
         * Requirement 3.9: Key rotation support
         */
        var oldKeys: String = ""
    }
}
