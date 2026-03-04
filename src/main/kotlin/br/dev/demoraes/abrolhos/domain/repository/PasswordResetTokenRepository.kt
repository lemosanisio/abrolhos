package br.dev.demoraes.abrolhos.domain.repository

import br.dev.demoraes.abrolhos.domain.entities.PasswordResetToken
import br.dev.demoraes.abrolhos.domain.entities.PasswordResetTokenEntity
import ulid.ULID

/**
 * Domain repository interface for password reset tokens.
 *
 * Implementations handle persistence of tokens. The interface is kept in the domain layer to
 * respect hexagonal architecture boundaries.
 */
interface PasswordResetTokenRepository {
    /** Persists a new token and returns it with a generated [PasswordResetTokenEntity.id]. */
    fun save(token: PasswordResetTokenEntity): PasswordResetTokenEntity

    /** Finds a token by its value, or returns null if not found. */
    fun findByToken(token: PasswordResetToken): PasswordResetTokenEntity?

    /** Deletes a specific token by its ULID. */
    fun deleteById(id: ULID)

    /** Deletes all tokens that have passed their expiration time. */
    fun deleteExpiredTokens()

    /** Deletes all tokens associated with a given user. */
    fun deleteByUserId(userId: ULID)
}
