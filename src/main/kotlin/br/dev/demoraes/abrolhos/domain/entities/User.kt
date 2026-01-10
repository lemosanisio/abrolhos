package br.dev.demoraes.abrolhos.domain.entities

import com.fasterxml.jackson.annotation.JsonValue
import ulid.ULID
import java.time.OffsetDateTime

data class User(
    val id: ULID,
    val username: Username,
    val email: Email,
    val passwordHash: PasswordHash,
    val role: Role,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)

enum class Role {
    ADMIN,
    USER,
}

@JvmInline
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
value class PasswordHash(@get:JsonValue val value: String) {
    companion object {
        const val MAX_LENGTH = 255
    }

    init {
        require(value.isNotBlank()) { "Password hash cannot be blank." }
        require(value.length <= MAX_LENGTH) { "Password hash is too long." }
    }

    override fun toString(): String = value
}

@JvmInline
value class Email(@get:JsonValue val value: String) {
    companion object {
        private val EMAIL_REGEX =
            Regex(
                "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$",
            )
    }

    init {
        require(value.isNotBlank()) { "Email address cannot be blank." }

        require(EMAIL_REGEX.matches(value)) { "Invalid email address format." }
    }

    override fun toString(): String = value
}
