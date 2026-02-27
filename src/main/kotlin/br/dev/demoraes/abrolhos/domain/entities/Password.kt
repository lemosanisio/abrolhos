package br.dev.demoraes.abrolhos.domain.entities

import com.fasterxml.jackson.annotation.JsonValue

/**
 * Value class representing a plaintext password.
 *
 * Used only in memory during authentication flows — never stored or logged.
 *
 * @property value The raw plaintext password string
 */
@JvmInline
value class PlaintextPassword(val value: String) {
    init {
        require(value.isNotBlank()) { "Password cannot be blank" }
        require(value.length <= 128) { "Password cannot exceed 128 characters" }
    }
}

/**
 * Value class representing a bcrypt password hash.
 *
 * Validates that the stored value follows the standard bcrypt format.
 *
 * @property value The bcrypt hashed password string
 */
@JvmInline
value class PasswordHash(@get:JsonValue val value: String) {
    init {
        require(value.isNotBlank()) { "Password hash cannot be blank" }
        require(
            value.startsWith("\$2a\$") ||
                value.startsWith("\$2b\$") ||
                value.startsWith("\$2y\$")
        ) { "Password hash must be in bcrypt format" }
    }
}

/**
 * Value class representing a password reset token.
 *
 * Must be exactly 64 lowercase hexadecimal characters (256 bits of entropy).
 *
 * @property value The 64-character hex token string
 */
@JvmInline
value class PasswordResetToken(@get:JsonValue val value: String) {
    companion object {
        const val TOKEN_LENGTH = 64 // 256 bits stored as hex
        private val HEX_REGEX = Regex("^[a-f0-9]{64}$")
    }

    init {
        require(value.length == TOKEN_LENGTH) { "Password reset token must be 64 characters" }
        require(HEX_REGEX.matches(value)) { "Password reset token must be hexadecimal" }
    }
}
