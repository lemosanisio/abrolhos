package br.dev.demoraes.abrolhos.domain.repository

import br.dev.demoraes.abrolhos.domain.entities.User
import br.dev.demoraes.abrolhos.domain.entities.Username
import ulid.ULID

interface UserRepository {
    fun save(user: User): User
    fun findById(id: ULID): User?
    fun findByUsername(username: Username): User?
}
