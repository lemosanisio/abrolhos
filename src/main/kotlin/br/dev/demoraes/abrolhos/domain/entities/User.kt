package br.dev.demoraes.abrolhos.domain.entities

import com.fasterxml.jackson.annotation.JsonValue
import ulid.ULID
import java.time.OffsetDateTime

/**
 * Represents a user in the system.
 *
 * Users are the central entity for authentication and authorization. They hold the credentials
 * (like TOTP secret) and role information.
 *
 * @property id Unique identifier (ULID)
 * @property username Unique username
 * @property totpSecret Encrypted TOTP secret for 2FA
 * @property isActive Whether the account is activated
 * @property role User role (ADMIN, USER)
 */
data class User(
    val id: ULID,
    val username: Username,
    val totpSecret: TotpSecret?,
    val passwordHash: PasswordHash?, // Nullable during migration period
    val isActive: Boolean,
    val role: Role,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)

enum class Role {
    ADMIN,
    USER,
}

@JvmInline
/**
 * Value class for Username.
 *
 * Enforces validation rules:
 * - Length: 3-20 characters
 * - Characters: lowercase letters, numbers, underscores
 * - No reserved words
 */
value class Username(@get:JsonValue val value: String) {
    companion object {
        private const val MIN_LENGTH = 3
        private const val MAX_LENGTH = 20
        private val USERNAME_REGEX =
            Regex("^[a-z0-9_]+$") // Only lowercase, numbers, and underscore
        private val RESERVED_WORDS =
            setOf(
                "root",
                "admin",
                "administrator",
                "support",
                "contact",
                "user",
                "guest",
            )
    }

    init {
        val normalizedValue = value.lowercase()

        require(normalizedValue.isNotBlank()) { "Username cannot be blank." }
        require(normalizedValue.length in MIN_LENGTH..MAX_LENGTH) {
            "Username must be between $MIN_LENGTH and $MAX_LENGTH characters."
        }
        require(USERNAME_REGEX.matches(normalizedValue)) {
            "Username can only contain lowercase letters, numbers, and underscores."
        }
        require(normalizedValue !in RESERVED_WORDS) {
            "Username '$value' is a reserved word and cannot be used."
        }
    }

    override fun toString(): String = value.lowercase()
}

@JvmInline
value class TotpSecret(@get:JsonValue val value: String) {
    companion object {
        private val BASE32_REGEX = Regex("^[A-Z2-7]+$")
    }

    init {
        require(value.isNotBlank()) { "TOTP secret cannot be blank." }
        require(BASE32_REGEX.matches(value)) { "TOTP secret must be base32 encoded." }
    }

    override fun toString(): String = value
}
