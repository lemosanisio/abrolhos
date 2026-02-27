package br.dev.demoraes.abrolhos.application.services

import br.dev.demoraes.abrolhos.application.audit.AuditLogger
import br.dev.demoraes.abrolhos.application.config.PasswordProperties
import br.dev.demoraes.abrolhos.domain.entities.PasswordHash
import br.dev.demoraes.abrolhos.domain.entities.PasswordResetToken
import br.dev.demoraes.abrolhos.domain.entities.PasswordResetTokenEntity
import br.dev.demoraes.abrolhos.domain.entities.PlaintextPassword
import br.dev.demoraes.abrolhos.domain.entities.Username
import br.dev.demoraes.abrolhos.domain.exceptions.InvalidPasswordException
import br.dev.demoraes.abrolhos.domain.exceptions.PasswordPolicyViolationException
import br.dev.demoraes.abrolhos.domain.exceptions.PasswordResetTokenExpiredException
import br.dev.demoraes.abrolhos.domain.exceptions.PasswordResetTokenNotFoundException
import br.dev.demoraes.abrolhos.domain.repository.PasswordResetTokenRepository
import br.dev.demoraes.abrolhos.domain.repository.UserRepository
import io.micrometer.core.instrument.MeterRegistry
import java.security.SecureRandom
import java.time.OffsetDateTime
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ulid.ULID

/**
 * Service responsible for all password-related operations.
 *
 * Handles:
 * - Password policy validation
 * - Bcrypt hashing and verification
 * - Authenticated password changes
 * - Password reset token generation and consumption
 * - Scheduled cleanup of expired tokens
 *
 * Integrates with [AuditLogger] for security event logging and [MeterRegistry] for metrics.
 */
@Service
class PasswordService(
        private val passwordEncoder: PasswordEncoder,
        private val passwordResetTokenRepository: PasswordResetTokenRepository,
        private val userRepository: UserRepository,
        private val auditLogger: AuditLogger,
        private val rateLimitService: RateLimitService,
        private val meterRegistry: MeterRegistry,
        private val passwordProperties: PasswordProperties,
        private val secureRandom: SecureRandom,
) {
    private val logger = LoggerFactory.getLogger(PasswordService::class.java)

    companion object {
        private const val PERFORMANCE_THRESHOLD_MS = 500L
        private const val RATE_LIMIT_ENDPOINT_CHANGE = "password:change"
        private const val RATE_LIMIT_ENDPOINT_RESET = "password:reset"
    }

    // ---------------------------------------------------------------------------
    // Password Validation
    // ---------------------------------------------------------------------------

    /**
     * Validates a password against the configured password policy.
     *
     * Returns a list of human-readable violations — the list is empty when the password is valid.
     *
     * _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7_
     */
    fun validatePassword(password: PlaintextPassword): List<String> {
        val violations = mutableListOf<String>()
        val value = password.value

        if (value.length < passwordProperties.minLength) {
            violations += "Password must be at least ${passwordProperties.minLength} characters"
        }
        if (value.length > passwordProperties.maxLength) {
            violations += "Password cannot exceed ${passwordProperties.maxLength} characters"
        }
        if (passwordProperties.requireUppercase && value.none { it.isUpperCase() }) {
            violations += "Password must contain at least one uppercase letter"
        }
        if (passwordProperties.requireLowercase && value.none { it.isLowerCase() }) {
            violations += "Password must contain at least one lowercase letter"
        }
        if (passwordProperties.requireDigit && value.none { it.isDigit() }) {
            violations += "Password must contain at least one digit"
        }
        if (passwordProperties.requireSpecialChar &&
                        value.none { it in passwordProperties.specialChars }
        ) {
            violations +=
                    "Password must contain at least one special character (${passwordProperties.specialChars})"
        }

        return violations
    }

    // ---------------------------------------------------------------------------
    // Hash & Verify
    // ---------------------------------------------------------------------------

    /**
     * Hashes a password using BCrypt after validating it against the password policy.
     *
     * Throws [PasswordPolicyViolationException] if the password does not meet policy requirements.
     *
     * _Requirements: 1.1, 1.4, 2.8, 9.1, 9.3, 9.4_
     */
    fun hashPassword(password: PlaintextPassword): PasswordHash {
        val violations = validatePassword(password)
        if (violations.isNotEmpty()) {
            throw PasswordPolicyViolationException(violations)
        }

        val startTime = System.currentTimeMillis()
        val hash = passwordEncoder.encode(password.value)
        val duration = System.currentTimeMillis() - startTime

        recordOperationMetrics("password.hash", duration)
        if (duration > PERFORMANCE_THRESHOLD_MS) {
            logger.warn(
                    "Password hashing took ${duration}ms, exceeding threshold of ${PERFORMANCE_THRESHOLD_MS}ms"
            )
        }

        return PasswordHash(hash)
    }

    /**
     * Verifies a plaintext password against a stored bcrypt hash using constant-time comparison.
     *
     * Records operation duration metrics and logs a warning for slow operations.
     *
     * _Requirements: 3.6, 9.2, 9.3, 9.4_
     */
    fun verifyPassword(password: PlaintextPassword, hash: PasswordHash): Boolean {
        val startTime = System.currentTimeMillis()
        val matches = passwordEncoder.matches(password.value, hash.value)
        val duration = System.currentTimeMillis() - startTime

        recordOperationMetrics("password.verify", duration)
        if (duration > PERFORMANCE_THRESHOLD_MS) {
            logger.warn(
                    "Password verification took ${duration}ms, exceeding threshold of ${PERFORMANCE_THRESHOLD_MS}ms"
            )
        }

        return matches
    }

    // ---------------------------------------------------------------------------
    // Password Change
    // ---------------------------------------------------------------------------

    /**
     * Changes a user's password after verifying their current password.
     *
     * Enforces rate limiting, validates the new password against policy, ensures the new password
     * differs from the current one, and logs an audit event on success.
     *
     * _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 8.1_
     *
     * @param userId ID of the authenticated user requesting the change
     * @param currentPassword The user's current plaintext password
     * @param newPassword The desired new plaintext password
     * @param currentHash The stored bcrypt hash of the current password
     * @param clientIp Originating IP address (for audit logging)
     * @return The new bcrypt [PasswordHash] that should be persisted by the caller
     */
    @Transactional
    fun changePassword(
            userId: ULID,
            currentPassword: PlaintextPassword,
            newPassword: PlaintextPassword,
            currentHash: PasswordHash,
            clientIp: String = "unknown",
    ): PasswordHash {
        // Rate limiting
        val rateLimitResult =
                rateLimitService.tryConsume(userId.toString(), RATE_LIMIT_ENDPOINT_CHANGE)
        if (!rateLimitResult.isAllowed) {
            auditLogger.logRateLimitExceeded(clientIp, RATE_LIMIT_ENDPOINT_CHANGE)
            throw br.dev.demoraes.abrolhos.domain.exceptions.PasswordResetException(
                    "Too many password change attempts. Try again later."
            )
        }

        // Verify current password
        if (!verifyPassword(currentPassword, currentHash)) {
            throw InvalidPasswordException("Current password is incorrect")
        }

        // Validate new password policy
        val violations = validatePassword(newPassword)
        if (violations.isNotEmpty()) {
            throw PasswordPolicyViolationException(violations)
        }

        // Ensure new password differs from current
        if (verifyPassword(newPassword, currentHash)) {
            throw InvalidPasswordException("New password must differ from the current password")
        }

        val newHash = hashPassword(newPassword)

        // Audit log
        auditLogger.logPasswordChanged(userId.toString(), clientIp)

        meterRegistry.counter("password.change.success").increment()
        return newHash
    }

    // ---------------------------------------------------------------------------
    // Password Reset
    // ---------------------------------------------------------------------------

    /**
     * Generates a cryptographically secure password reset token for the given user.
     *
     * Any existing tokens for the user are deleted before saving the new one. Always returns 202
     * Accepted at the controller layer to prevent user enumeration.
     *
     * _Requirements: 6.1, 6.2, 6.3, 7.3, 7.4, 8.2_
     *
     * @param userId ID of the user requesting the reset
     * @param username Username (used in audit log and rate limit key)
     * @param clientIp Originating IP address (for audit logging)
     * @return The generated [PasswordResetToken]
     */
    @Transactional
    fun generateResetToken(
            userId: ULID,
            username: Username,
            clientIp: String = "unknown",
    ): PasswordResetToken {
        // Rate limiting by username
        val rateLimitResult = rateLimitService.tryConsume(username.value, RATE_LIMIT_ENDPOINT_RESET)
        if (!rateLimitResult.isAllowed) {
            auditLogger.logRateLimitExceeded(clientIp, RATE_LIMIT_ENDPOINT_RESET)
            throw br.dev.demoraes.abrolhos.domain.exceptions.PasswordResetException(
                    "Too many password reset requests. Try again later."
            )
        }

        // Invalidate any existing tokens for this user
        passwordResetTokenRepository.deleteByUserId(userId)

        val token = generateSecureToken()
        val now = OffsetDateTime.now()
        val entity =
                PasswordResetTokenEntity(
                        id = ULID.parseULID(ULID.randomULID()),
                        userId = userId,
                        token = token,
                        expiresAt = now.plusHours(passwordProperties.resetTokenExpiryHours),
                        createdAt = now,
                )
        passwordResetTokenRepository.save(entity)

        auditLogger.logPasswordResetRequested(username.value, clientIp)
        meterRegistry.counter("password.reset.requested").increment()
        return token
    }

    /**
     * Validates and consumes a password reset token, updating the user's password.
     *
     * The token is deleted after use to prevent reuse.
     *
     * _Requirements: 6.4, 6.5, 6.6, 6.7, 8.3_
     *
     * @param token The password reset token submitted by the user
     * @param newPassword The desired new plaintext password
     * @param clientIp Originating IP address (for audit logging)
     * @return The [ULID] of the user whose password was reset
     */
    @Transactional
    fun resetPassword(
            token: PasswordResetToken,
            newPassword: PlaintextPassword,
            clientIp: String = "unknown",
    ): ULID {
        val tokenEntity =
                passwordResetTokenRepository.findByToken(token)
                        ?: throw PasswordResetTokenNotFoundException()

        if (tokenEntity.isExpired()) {
            passwordResetTokenRepository.deleteById(tokenEntity.id)
            throw PasswordResetTokenExpiredException()
        }

        val violations = validatePassword(newPassword)
        if (violations.isNotEmpty()) {
            throw PasswordPolicyViolationException(violations)
        }

        val newHash = hashPassword(newPassword)

        // Persist the new password hash on the user
        val user =
                userRepository.findById(tokenEntity.userId)
                        ?: throw br.dev.demoraes.abrolhos.domain.exceptions.PasswordResetException(
                                "User not found for password reset"
                        )
        userRepository.save(user.copy(passwordHash = newHash))

        // Invalidate the token (single-use)
        passwordResetTokenRepository.deleteById(tokenEntity.id)

        auditLogger.logPasswordResetCompleted(tokenEntity.userId.toString(), clientIp)
        meterRegistry.counter("password.reset.completed").increment()
        return tokenEntity.userId
    }

    // ---------------------------------------------------------------------------
    // Scheduled Cleanup
    // ---------------------------------------------------------------------------

    /**
     * Deletes all expired password reset tokens from the database.
     *
     * Runs hourly by default (configurable via `security.password.cleanup-cron`).
     *
     * _Requirement: 6.8_
     */
    @Scheduled(cron = "\${security.password.cleanup-cron:0 0 * * * *}")
    fun cleanupExpiredTokens() {
        val startTime = System.currentTimeMillis()
        passwordResetTokenRepository.deleteExpiredTokens()
        val duration = System.currentTimeMillis() - startTime
        logger.info("Cleaned up expired password reset tokens in ${duration}ms")
        meterRegistry.counter("password.reset.tokens.cleaned").increment()
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    /**
     * Creates a cryptographically secure password reset token using [SecureRandom].
     *
     * Generates [PasswordProperties.resetTokenByteSize] random bytes (default 32) and encodes them
     * as a lowercase hex string (64 characters = 256 bits of entropy).
     */
    private fun generateSecureToken(): PasswordResetToken {
        val bytes = ByteArray(passwordProperties.resetTokenByteSize)
        secureRandom.nextBytes(bytes)
        val hex = bytes.joinToString("") { "%02x".format(it) }
        return PasswordResetToken(hex)
    }

    private fun recordOperationMetrics(operation: String, durationMs: Long) {
        meterRegistry
                .timer(operation)
                .record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS)
    }
}
