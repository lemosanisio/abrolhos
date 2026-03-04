package br.dev.demoraes.abrolhos.domain.repository

import br.dev.demoraes.abrolhos.domain.entities.User
import br.dev.demoraes.abrolhos.domain.entities.Username
import ulid.ULID

/**
 * Repository interface for User entity persistence.
 *
 * Defines the contract for User data access, including finding by ID and Username. Implementations
 * should handle the underlying storage details (e.g., SQL queries).
 */
interface UserRepository {
    fun findById(id: ULID): User?
    fun findByUsername(username: Username): User?
    fun save(user: User): User
    fun existsByUsername(username: Username): Boolean
}
