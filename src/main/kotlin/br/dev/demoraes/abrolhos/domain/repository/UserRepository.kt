package br.dev.demoraes.abrolhos.domain.repository

import br.dev.demoraes.abrolhos.domain.entities.User
import br.dev.demoraes.abrolhos.domain.entities.Username
import ulid.ULID

interface UserRepository {
    fun findById(id: ULID): User?
    fun findByUsername(username: Username): User?
    fun save(user: User): User
    fun existsByUsername(username: Username): Boolean
}
