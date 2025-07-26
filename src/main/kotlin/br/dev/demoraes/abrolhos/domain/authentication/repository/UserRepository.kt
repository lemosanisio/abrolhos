package br.dev.demoraes.abrolhos.domain.authentication.repository

import br.dev.demoraes.abrolhos.domain.authentication.entities.User
import br.dev.demoraes.abrolhos.domain.authentication.entities.Username
import ulid.ULID

interface UserRepository {
    fun save(user: User): User
    fun findById(id: ULID): User?
    fun findByUsername(username: Username): User?
}