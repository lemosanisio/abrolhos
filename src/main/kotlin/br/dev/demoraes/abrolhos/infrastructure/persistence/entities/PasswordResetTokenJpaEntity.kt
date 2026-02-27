package br.dev.demoraes.abrolhos.infrastructure.persistence.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.OffsetDateTime

/**
 * JPA entity for the `password_reset_tokens` table.
 *
 * Tokens are indexed by `token` (unique lookups) and `expires_at` (cleanup queries).
 */
@Entity
@Table(
        name = "password_reset_tokens",
        indexes =
                [
                        Index(name = "idx_password_reset_token", columnList = "token"),
                        Index(name = "idx_password_reset_expires_at", columnList = "expires_at"),
                        Index(name = "idx_password_reset_user_id", columnList = "user_id"),
                ]
)
open class PasswordResetTokenJpaEntity(
        @Column(name = "user_id", nullable = false, length = 26) open var userId: String,
        @Column(name = "token", nullable = false, unique = true, length = 64)
        open var token: String,
        @Column(name = "expires_at", nullable = false) open var expiresAt: OffsetDateTime,
) : BaseEntity()
