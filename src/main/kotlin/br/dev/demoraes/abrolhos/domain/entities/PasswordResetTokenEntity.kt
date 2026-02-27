
package br.dev.demoraes.abrolhos.domain.entities

import ulid.ULID
import java.time.OffsetDateTime

/**
 * Domain entity representing a password reset token.
 *
 * Tokens are cryptographically secure (256 bits of entropy), single-use,
 * and expire after a configurable period (default 1 hour).
 *
 * @property id Unique identifier (ULID)
 * @property userId ID of the user this token belongs to
 * @property token The cryptographically secure token value
 * @property expiresAt Timestamp when this token expires
 * @property createdAt Timestamp when this token was created
 */
data class PasswordResetTokenEntity(
    val id: ULID,
    val userId: ULID,
    val token: PasswordResetToken,
    val expiresAt: OffsetDateTime,
    val createdAt: OffsetDateTime,
) {
    /** Returns true if this token has passed its expiration time. */
    fun isExpired(): Boolean = OffsetDateTime.now().isAfter(expiresAt)
}
